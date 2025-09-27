package dev.nefor.activator.plugin.config;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ActivatorConfig {
    private final String licenseKey;
    private final String clientSecret;
    private final EndpointConfig endpoint;
    private final OfflineConfig offline;
    private final EnforcementConfig enforcement;
    private final List<ProductSpec> products;
    private final LoggingConfig logging;
    private final Locale locale;
    private final FingerprintConfig fingerprint;
    private final TelemetryConfig telemetry;

    public ActivatorConfig(
        String licenseKey,
        String clientSecret,
        EndpointConfig endpoint,
        OfflineConfig offline,
        EnforcementConfig enforcement,
        List<ProductSpec> products,
        LoggingConfig logging,
        Locale locale,
        FingerprintConfig fingerprint,
        TelemetryConfig telemetry
    ) {
        this.licenseKey = Objects.requireNonNull(licenseKey, "licenseKey");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.offline = Objects.requireNonNull(offline, "offline");
        this.enforcement = Objects.requireNonNull(enforcement, "enforcement");
        this.products = products == null ? List.of() : List.copyOf(products);
        this.logging = Objects.requireNonNull(logging, "logging");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    public String licenseKey() {
        return licenseKey;
    }

    public String clientSecret() {
        return clientSecret;
    }

    public EndpointConfig endpoint() {
        return endpoint;
    }

    public OfflineConfig offline() {
        return offline;
    }

    public EnforcementConfig enforcement() {
        return enforcement;
    }

    public List<ProductSpec> products() {
        return products;
    }

    public LoggingConfig logging() {
        return logging;
    }

    public Locale locale() {
        return locale;
    }

    public FingerprintConfig fingerprint() {
        return fingerprint;
    }

    public TelemetryConfig telemetry() {
        return telemetry;
    }

    public record EndpointConfig(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        public EndpointConfig {
            Objects.requireNonNull(baseUrl, "baseUrl");
            Objects.requireNonNull(connectTimeout, "connectTimeout");
            Objects.requireNonNull(readTimeout, "readTimeout");
        }
    }

    public record OfflineConfig(Duration gracePeriod, Duration refreshSkew, String cacheFileName, String jwksFileName, String crlFileName) {
        public OfflineConfig {
            Objects.requireNonNull(gracePeriod, "gracePeriod");
            Objects.requireNonNull(refreshSkew, "refreshSkew");
            Objects.requireNonNull(cacheFileName, "cacheFileName");
            Objects.requireNonNull(jwksFileName, "jwksFileName");
            Objects.requireNonNull(crlFileName, "crlFileName");
        }
    }

    public record EnforcementConfig(boolean blockOnInvalid, long disableDelayTicks, boolean requireCrl) {
    }

    public record ProductSpec(String id, String maxVersion) {
        public ProductSpec {
            Objects.requireNonNull(id, "id");
        }
    }

    public record LoggingConfig(String level, boolean redactSensitive) {
        public LoggingConfig {
            Objects.requireNonNull(level, "level");
        }
    }

    public record FingerprintConfig(String salt) {
        public FingerprintConfig {
            Objects.requireNonNull(salt, "salt");
        }
    }

    public record TelemetryConfig(boolean enabled) {
    }

    public List<String> productIds() {
        return products.stream().map(ProductSpec::id).collect(java.util.stream.Collectors.toUnmodifiableList());
    }

    public static ActivatorConfig defaults() {
        return new ActivatorConfig(
            "",
            "",
            new EndpointConfig("https://lic.example.com", Duration.ofSeconds(3), Duration.ofSeconds(3)),
            new OfflineConfig(Duration.ofHours(72), Duration.ofMinutes(15), "offline-token.json", "jwks-cache.json", "crl-cache.json"),
            new EnforcementConfig(true, 1, true),
            Collections.emptyList(),
            new LoggingConfig("INFO", true),
            Locale.ENGLISH,
            new FingerprintConfig("change-me"),
            new TelemetryConfig(true)
        );
    }
}
