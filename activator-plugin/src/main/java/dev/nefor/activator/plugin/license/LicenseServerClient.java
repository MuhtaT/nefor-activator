package dev.nefor.activator.plugin.license;

import dev.nefor.activator.plugin.config.ActivatorConfig;
import dev.nefor.activator.plugin.util.HmacSigner;
import dev.nefor.activator.plugin.util.NonceGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class LicenseServerClient {
    private final JavaPlugin plugin;
    private final ActivatorConfig config;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public LicenseServerClient(JavaPlugin plugin, ActivatorConfig config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.client = HttpClient.newBuilder()
            .connectTimeout(config.endpoint().connectTimeout())
            .build();
    }

    public CompletableFuture<LicenseServerResponse> activate(String fingerprint) {
        Map<String, Object> payload = basePayload();
        payload.put("licenseKey", config.licenseKey());
        payload.put("srv_fp", fingerprint);
        sign(payload, config.licenseKey(), fingerprint, payload.get("nonce"), payload.get("timestamp"));
        return send("/v1/activate", payload);
    }

    public CompletableFuture<LicenseServerResponse> refresh(String token, String fingerprint) {
        Map<String, Object> payload = basePayload();
        payload.put("token", token);
        payload.put("srv_fp", fingerprint);
        sign(payload, token, fingerprint, payload.get("nonce"), payload.get("timestamp"));
        return send("/v1/refresh", payload);
    }

    public CompletableFuture<Void> ping() {
        Map<String, Object> payload = basePayload();
        payload.put("licenseKey", config.licenseKey());
        sign(payload, config.licenseKey(), payload.get("nonce"), payload.get("timestamp"));
        return send("/v1/diag", payload).thenApply(res -> null);
    }

    public CompletableFuture<String> fetchJwks(String url) {
        return get(url);
    }

    public CompletableFuture<String> fetchCrl(String url) {
        return get(url);
    }

    private Map<String, Object> basePayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("nonce", NonceGenerator.randomNonce(16));
        payload.put("timestamp", Instant.now().getEpochSecond());
        return payload;
    }

    private void sign(Map<String, Object> payload, Object... parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append('|');
            }
            builder.append(String.valueOf(parts[i]));
        }
        payload.put("hmac", HmacSigner.hmacSha256Hex(config.clientSecret(), builder.toString()));
    }

    private CompletableFuture<LicenseServerResponse> send(String path, Map<String, Object> payload) {
        try {
            String json = mapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalize(config.endpoint().baseUrl()) + path))
                .timeout(config.endpoint().readTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Activator/" + plugin.getDescription().getVersion())
                .build();
            Logger logger = plugin.getLogger();
            logger.fine(() -> "POST " + request.uri());
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int statusCode = response.statusCode();
                    String body = response.body();
                    if (statusCode >= 200 && statusCode < 300) {
                        try {
                            return mapper.readValue(body, LicenseServerResponse.class);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse licensing server response", e);
                        }
                    }
                    throw new RuntimeException("License server returned status " + statusCode + ": " + body);
                });
        } catch (JsonProcessingException e) {
            CompletableFuture<LicenseServerResponse> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    private CompletableFuture<String> get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(config.endpoint().readTimeout())
            .GET()
            .header("User-Agent", "Activator/" + plugin.getDescription().getVersion())
            .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                }
                throw new RuntimeException("GET " + url + " returned status " + status);
            });
    }

    private String normalize(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    public record LicenseServerResponse(
        String token,
        long refresh_ttl,
        String jwks_url,
        String crl_url
    ) {
        public Duration refreshTtl() {
            return Duration.ofSeconds(refresh_ttl);
        }
    }
}
