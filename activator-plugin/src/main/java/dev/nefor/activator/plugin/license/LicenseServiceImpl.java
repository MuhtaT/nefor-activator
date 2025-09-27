package dev.nefor.activator.plugin.license;

import dev.nefor.activator.api.LicenseException;
import dev.nefor.activator.api.LicenseListener;
import dev.nefor.activator.api.LicenseService;
import dev.nefor.activator.api.LicenseStatus;
import dev.nefor.activator.api.LicenseToken;
import dev.nefor.activator.plugin.config.ActivatorConfig;
import dev.nefor.activator.plugin.event.LicenseGrantedEvent;
import dev.nefor.activator.plugin.event.LicenseRevokedEvent;
import dev.nefor.activator.plugin.fingerprint.ServerFingerprintCalculator;
import dev.nefor.activator.plugin.registry.DependentPluginRegistry;
import dev.nefor.activator.plugin.util.Localization;
import dev.nefor.activator.plugin.util.TimeUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class LicenseServiceImpl implements LicenseService {
    private final JavaPlugin plugin;
    private final ActivatorConfig config;
    private final DependentPluginRegistry registry;
    private final Localization localization;
    private final LicenseValidator validator;
    private final LicenseServerClient serverClient;
    private final OfflineTokenStore offlineTokenStore;
    private final ServerFingerprintCalculator fingerprintCalculator;
    private final Logger logger;

    private final Map<String, CopyOnWriteArrayList<LicenseListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.initial());

    private volatile String fingerprint;
    private volatile boolean started;
    private volatile int watchdogTaskId = -1;

    public LicenseServiceImpl(JavaPlugin plugin, ActivatorConfig config, Localization localization) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.localization = Objects.requireNonNull(localization, "localization");
        this.registry = new DependentPluginRegistry(plugin);
        this.validator = new LicenseValidator(plugin.getLogger(), config.enforcement().requireCrl());
        this.serverClient = new LicenseServerClient(plugin, config);
        this.offlineTokenStore = new OfflineTokenStore(plugin, config.offline().cacheFileName());
        this.fingerprintCalculator = new ServerFingerprintCalculator(plugin);
        this.logger = plugin.getLogger();
    }

    public void bootstrap() {
        if (started) {
            return;
        }
        started = true;
        this.fingerprint = fingerprintCalculator.calculate(config);
        logger.info(() -> "Server fingerprint: " + fingerprint);
        plugin.getServer().getServicesManager().register(LicenseService.class, this, plugin, ServicePriority.Highest);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!offlineBootstrap()) {
                requestActivation(true);
            }
            startWatchdog();
        });
    }

    public void shutdown() {
        plugin.getServer().getServicesManager().unregister(LicenseService.class, this);
        if (watchdogTaskId != -1) {
            Bukkit.getScheduler().cancelTask(watchdogTaskId);
            watchdogTaskId = -1;
        }
        listeners.clear();
        state.set(State.initial());
    }

    public void triggerRefresh() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> requestActivation(false));
    }

    public void triggerReactivation() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> requestActivation(true));
    }

    public CompletableFuture<DiagnosticsResult> runDiagnostics() {
        Instant start = Instant.now();
        return serverClient.ping()
            .thenApply(v -> new DiagnosticsResult(true, "OK", Duration.between(start, Instant.now()).toMillis()))
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "Diagnostics failed", ex);
                return new DiagnosticsResult(false, ex.getMessage(), Duration.between(start, Instant.now()).toMillis());
            });
    }

    private boolean offlineBootstrap() {
        return offlineTokenStore.load().map(entry -> {
            if (entry.getJwksJson() != null) {
                validator.loadJwks(entry.getJwksJson());
            }
            if (entry.getCrlJson() != null) {
                validator.loadCrl(entry.getCrlJson());
            }
            try {
                LicenseToken token = validator.verify(entry.getToken(), fingerprint);
                Instant fetchedAt = Optional.ofNullable(entry.getFetchedAt()).orElse(Instant.now());
                Instant offlineDeadline = token.getOfflineTtl() != null && !token.getOfflineTtl().isZero()
                    ? fetchedAt.plus(token.getOfflineTtl())
                    : null;
                if (offlineDeadline != null && offlineDeadline.isBefore(Instant.now())) {
                    updateState(LicenseStatus.INVALID, token, fetchedAt, offlineDeadline, null, "GRACE_EXPIRED");
                    return false;
                }
                if (token.isExpired(Instant.now())) {
                    updateState(LicenseStatus.EXPIRED, token, fetchedAt, offlineDeadline, null, "TOKEN_EXPIRED");
                    return false;
                }
                LicenseStatus status = offlineDeadline != null ? LicenseStatus.GRACE : LicenseStatus.VALID;
                updateState(status, token, fetchedAt, offlineDeadline, null, "OFFLINE_CACHE");
                scheduleRefresh(Duration.ofSeconds(Math.max(entry.getRefreshTtlSeconds(), 60)));
                return true;
            } catch (LicenseException ex) {
                logger.log(Level.WARNING, "Offline token invalid: " + ex.getMessage());
                return false;
            }
        }).orElse(false);
    }

    private void requestActivation(boolean initial) {
        if (config.licenseKey().isEmpty() || config.clientSecret().isEmpty()) {
            logger.severe("Cannot activate without licenseKey and secret configured");
            updateState(LicenseStatus.INVALID, state.get().token, Instant.now(), null, null, "CONFIG_MISSING");
            return;
        }
        logger.info(initial ? "Contacting license server for activation" : "Refreshing license token");
        CompletableFuture<LicenseServerClient.LicenseServerResponse> call;
        LicenseToken currentToken = state.get().token;
        if (currentToken != null && !initial) {
            call = serverClient.refresh(currentToken.getRawToken(), fingerprint);
        } else {
            call = serverClient.activate(fingerprint);
        }
        call.thenCompose(response -> handleServerResponse(response, fingerprint))
            .exceptionally(ex -> {
                logger.log(Level.WARNING, "License request failed: " + ex.getMessage());
                transitionToGrace("NETWORK_ERROR");
                return null;
            });
    }

    private CompletableFuture<Void> handleServerResponse(LicenseServerClient.LicenseServerResponse response, String fingerprint) {
        CompletableFuture<String> jwksFuture = response.jwks_url() != null ? serverClient.fetchJwks(response.jwks_url()) : CompletableFuture.completedFuture(null);
        CompletableFuture<String> crlFuture = response.crl_url() != null ? serverClient.fetchCrl(response.crl_url()) : CompletableFuture.completedFuture(null);
        return CompletableFuture.allOf(jwksFuture, crlFuture).thenRun(() -> {
            try {
                String jwks = jwksFuture.join();
                if (jwks != null) {
                    validator.loadJwks(jwks);
                }
                String crl = crlFuture.join();
                if (crl != null) {
                    validator.loadCrl(crl);
                }
                LicenseToken token = validator.verify(response.token(), fingerprint);
                Instant fetchedAt = Instant.now();
                Instant offlineDeadline = token.getOfflineTtl() != null && !token.getOfflineTtl().isZero()
                    ? fetchedAt.plus(token.getOfflineTtl())
                    : null;
                cacheOffline(response, token, jwks, crl, fetchedAt);
                updateState(LicenseStatus.VALID, token, fetchedAt, offlineDeadline, fetchedAt.plus(response.refreshTtl()), "NETWORK_OK");
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Failed to process server response: " + ex.getMessage(), ex);
                transitionToGrace("PROCESSING_ERROR");
            }
        });
    }

    private void cacheOffline(LicenseServerClient.LicenseServerResponse response, LicenseToken token, String jwks, String crl, Instant fetchedAt) {
        OfflineCacheEntry entry = new OfflineCacheEntry();
        entry.setToken(token.getRawToken());
        entry.setFingerprint(fingerprint);
        entry.setFetchedAt(fetchedAt);
        entry.setExpiresAt(token.getExpiresAt());
        entry.setRefreshTtlSeconds(Math.max(response.refresh_ttl(), 60));
        entry.setJwksJson(jwks);
        entry.setCrlJson(crl);
        offlineTokenStore.save(entry);
    }

    private void transitionToGrace(String reason) {
        State current = state.get();
        LicenseToken token = current.token;
        if (token == null) {
            updateState(LicenseStatus.INVALID, null, Instant.now(), null, null, reason);
            return;
        }
        Instant offlineDeadline = current.offlineDeadline != null
            ? current.offlineDeadline
            : Instant.now().plus(Optional.ofNullable(token.getOfflineTtl()).orElse(Duration.ZERO));
        if (offlineDeadline != null && offlineDeadline.isAfter(Instant.now())) {
            updateState(LicenseStatus.GRACE, token, current.fetchedAt != null ? current.fetchedAt : Instant.now(), offlineDeadline, current.nextRefresh, reason);
        } else {
            updateState(LicenseStatus.INVALID, token, Instant.now(), offlineDeadline, current.nextRefresh, reason);
        }
    }

    private void startWatchdog() {
        if (watchdogTaskId != -1) {
            return;
        }
        watchdogTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::watchdogTick, 200L, 1200L).getTaskId();
    }

    private void watchdogTick() {
        State current = state.get();
        Instant now = Instant.now();
        if (current.token == null) {
            return;
        }
        if ((current.status == LicenseStatus.VALID || current.status == LicenseStatus.GRACE)
            && current.offlineDeadline != null && now.isAfter(current.offlineDeadline)) {
            updateState(LicenseStatus.EXPIRED, current.token, current.fetchedAt, current.offlineDeadline, current.nextRefresh, "GRACE_EXPIRED");
            return;
        }
        if (current.status == LicenseStatus.VALID && current.nextRefresh != null && now.isAfter(current.nextRefresh.minus(config.offline().refreshSkew()))) {
            requestActivation(false);
        }
    }

    private void scheduleRefresh(Duration refreshTtl) {
        Duration effective = refreshTtl.minus(config.offline().refreshSkew());
        if (effective.isNegative()) {
            effective = Duration.ofMinutes(1);
        }
        Instant nextRefresh = Instant.now().plus(effective);
        state.updateAndGet(prev -> prev.withNextRefresh(nextRefresh));
    }

    private void updateState(LicenseStatus newStatus, LicenseToken token, Instant fetchedAt, Instant offlineDeadline, Instant nextRefresh, String reason) {
        State previous = state.getAndUpdate(prev -> prev.withState(newStatus, token, fetchedAt, offlineDeadline, nextRefresh, reason));
        if (previous.status != newStatus) {
            logger.log(Level.INFO, () -> "License status -> " + newStatus + " (" + reason + ")");
            dispatchEvent(newStatus);
            notifyListeners(newStatus);
            if (newStatus == LicenseStatus.VALID || newStatus == LicenseStatus.GRACE) {
                logger.info(() -> localization.format("messages.license-valid", Map.of(
                    "expiry", token != null ? TimeUtil.formatInstant(token.getExpiresAt()) : "n/a",
                    "remaining", token != null ? TimeUtil.humanize(token.getOfflineTtl()) : "n/a"
                )));
            } else if (config.enforcement().blockOnInvalid()) {
                registry.disableAll(newStatus, config.enforcement().disableDelayTicks(), true);
            }
        }
    }

    private void notifyListeners(LicenseStatus status) {
        listeners.values().forEach(list -> {
            for (LicenseListener listener : list) {
                try {
                    if (status == LicenseStatus.VALID || status == LicenseStatus.GRACE) {
                        listener.onLicenseGranted();
                    } else {
                        listener.onLicenseRevoked(status);
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Listener threw exception", ex);
                }
            }
        });
    }

    private void dispatchEvent(LicenseStatus status) {
        Event event = switch (status) {
            case VALID, GRACE -> new LicenseGrantedEvent();
            default -> new LicenseRevokedEvent(status);
        };
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(event));
    }

    @Override
    public LicenseStatus getStatus() {
        return state.get().status;
    }

    @Override
    public boolean isProductAllowed(String productId) {
        State current = state.get();
        if (current.token == null) {
            return false;
        }
        return switch (current.status) {
            case VALID, GRACE -> current.token.getProducts().contains(productId);
            default -> false;
        };
    }

    @Override
    public boolean hasFeature(String productId, String feature) {
        State current = state.get();
        if (current.token == null) {
            return false;
        }
        return switch (current.status) {
            case VALID, GRACE -> current.token.getFeatures().getOrDefault(productId, Set.of()).contains(feature);
            default -> false;
        };
    }

    @Override
    public Set<String> getEnabledFeatures(String productId) {
        State current = state.get();
        if (current.token == null) {
            return Set.of();
        }
        return current.token.getFeatures().getOrDefault(productId, Set.of());
    }

    @Override
    public Set<String> getLicensedProducts() {
        State current = state.get();
        return current.token == null ? Set.of() : current.token.getProducts();
    }

    @Override
    public Optional<String> getMaxVersion(String productId) {
        State current = state.get();
        if (current.token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(current.token.getMaxVersions().get(productId));
    }

    @Override
    public void require(Plugin plugin, String productId) throws LicenseException {
        if (!isProductAllowed(productId)) {
            throw new LicenseException(getStatus(), "Product '" + productId + "' not permitted", "PRODUCT_NOT_ALLOWED");
        }
        registry.register(plugin, productId, Collections.emptySet());
    }

    @Override
    public void registerDependentPlugin(Plugin plugin, String productId, Set<String> features) {
        if (!isProductAllowed(productId)) {
            throw new LicenseException(getStatus(), "Product '" + productId + "' not permitted", "PRODUCT_NOT_ALLOWED");
        }
        registry.register(plugin, productId, features == null ? Set.of() : Set.copyOf(features));
    }

    @Override
    public void subscribe(Plugin plugin, LicenseListener listener) {
        listeners.computeIfAbsent(plugin.getName(), key -> new CopyOnWriteArrayList<>()).add(listener);
        if (getStatus() == LicenseStatus.VALID || getStatus() == LicenseStatus.GRACE) {
            listener.onLicenseGranted();
        }
    }

    @Override
    public void unsubscribe(Plugin plugin) {
        listeners.remove(plugin.getName());
        registry.unregister(plugin);
    }

    @Override
    public String getFingerprint() {
        return fingerprint;
    }

    @Override
    public Optional<LicenseToken> getTokenMeta() {
        return Optional.ofNullable(state.get().token);
    }

    @Override
    public Collection<RegisteredPlugin> getRegisteredPlugins() {
        return registry.snapshot();
    }

    public record DiagnosticsResult(boolean success, String message, long latencyMs) {}

    private record State(
        LicenseStatus status,
        LicenseToken token,
        Instant fetchedAt,
        Instant offlineDeadline,
        Instant nextRefresh,
        String reason
    ) {
        static State initial() {
            return new State(LicenseStatus.INVALID, null, Instant.EPOCH, null, null, "BOOT");
        }

        State withState(LicenseStatus status, LicenseToken token, Instant fetchedAt, Instant offlineDeadline, Instant nextRefresh, String reason) {
            return new State(status, token, fetchedAt, offlineDeadline, nextRefresh, reason);
        }

        State withNextRefresh(Instant nextRefresh) {
            return new State(this.status, this.token, this.fetchedAt, this.offlineDeadline, nextRefresh, this.reason);
        }
    }
}
