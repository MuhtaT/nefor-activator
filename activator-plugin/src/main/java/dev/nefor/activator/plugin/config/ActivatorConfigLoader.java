package dev.nefor.activator.plugin.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads and validates plugin configuration files.
 */
public final class ActivatorConfigLoader {
    private final JavaPlugin plugin;

    public ActivatorConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public ActivatorConfig load() {
        FileConfiguration cfg = plugin.getConfig();
        String licenseKey = cfg.getString("licenseKey", "").trim();
        String clientSecret = cfg.getString("secret", "").trim();

        ConfigurationSection endpointSection = cfg.getConfigurationSection("endpoint");
        String baseUrl = endpointSection != null ? endpointSection.getString("baseUrl", "https://lic.example.com") : "https://lic.example.com";
        long connectTimeoutMs = endpointSection != null ? endpointSection.getLong("connectTimeoutMs", 3000) : 3000;
        long readTimeoutMs = endpointSection != null ? endpointSection.getLong("readTimeoutMs", 3000) : 3000;
        ActivatorConfig.EndpointConfig endpoint = new ActivatorConfig.EndpointConfig(
            baseUrl,
            Duration.ofMillis(Math.max(connectTimeoutMs, 100)),
            Duration.ofMillis(Math.max(readTimeoutMs, 100))
        );

        ConfigurationSection offlineSection = cfg.getConfigurationSection("offline");
        long graceSeconds = offlineSection != null ? offlineSection.getLong("graceSeconds", 259200) : 259200;
        long refreshSkewSeconds = offlineSection != null ? offlineSection.getLong("refreshSkewSeconds", 900) : 900;
        String cacheFile = offlineSection != null ? offlineSection.getString("cacheFile", "offline-token.json") : "offline-token.json";
        String jwksFile = offlineSection != null ? offlineSection.getString("jwksFile", "jwks-cache.json") : "jwks-cache.json";
        String crlFile = offlineSection != null ? offlineSection.getString("crlFile", "crl-cache.json") : "crl-cache.json";
        ActivatorConfig.OfflineConfig offline = new ActivatorConfig.OfflineConfig(
            Duration.ofSeconds(Math.max(graceSeconds, 60)),
            Duration.ofSeconds(Math.max(refreshSkewSeconds, 60)),
            cacheFile,
            jwksFile,
            crlFile
        );

        ConfigurationSection enforcementSection = cfg.getConfigurationSection("enforcement");
        boolean blockOnInvalid = enforcementSection == null || enforcementSection.getBoolean("blockOnInvalid", true);
        long disableDelayTicks = enforcementSection == null ? 1 : Math.max(0, enforcementSection.getLong("disableDelayTicks", 1));
        boolean requireCrl = enforcementSection == null || enforcementSection.getBoolean("requireCrl", true);
        ActivatorConfig.EnforcementConfig enforcement = new ActivatorConfig.EnforcementConfig(blockOnInvalid, disableDelayTicks, requireCrl);

        List<ActivatorConfig.ProductSpec> products = new ArrayList<>();
        if (cfg.isList("products")) {
            for (Object obj : cfg.getList("products")) {
                if (obj instanceof ConfigurationSection section) {
                    String id = section.getString("id");
                    if (id != null && !id.isBlank()) {
                        products.add(new ActivatorConfig.ProductSpec(id.trim(), section.getString("maxVersion")));
                    }
                }
            }
        } else if (cfg.isConfigurationSection("products")) {
            ConfigurationSection section = cfg.getConfigurationSection("products");
            for (String key : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(key);
                String id = entry.getString("id", key);
                products.add(new ActivatorConfig.ProductSpec(id, entry.getString("maxVersion")));
            }
        }

        ConfigurationSection loggingSection = cfg.getConfigurationSection("logging");
        String level = loggingSection != null ? loggingSection.getString("level", "INFO") : "INFO";
        boolean redact = loggingSection == null || loggingSection.getBoolean("redactSensitive", true);
        ActivatorConfig.LoggingConfig loggingConfig = new ActivatorConfig.LoggingConfig(level.toUpperCase(Locale.ROOT), redact);

        ConfigurationSection fingerprintSection = cfg.getConfigurationSection("fingerprint");
        String salt = fingerprintSection != null ? fingerprintSection.getString("salt", "change-me") : "change-me";
        ActivatorConfig.FingerprintConfig fingerprint = new ActivatorConfig.FingerprintConfig(salt);

        boolean telemetryEnabled = true;
        if (cfg.isConfigurationSection("telemetry")) {
            telemetryEnabled = cfg.getConfigurationSection("telemetry").getBoolean("enabled", true);
        } else if (cfg.isBoolean("telemetry")) {
            telemetryEnabled = cfg.getBoolean("telemetry");
        }
        ActivatorConfig.TelemetryConfig telemetry = new ActivatorConfig.TelemetryConfig(telemetryEnabled);

        String localeTag = cfg.getString("i18n", "en");
        Locale locale = Locale.forLanguageTag(localeTag.replace('_', '-'));

        if (licenseKey.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "licenseKey is empty in config.yml; Activator will stay in INVALID state");
        }
        if (clientSecret.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "secret is empty in config.yml; outbound calls will fail HMAC validation");
        }

        return new ActivatorConfig(
            licenseKey,
            clientSecret,
            endpoint,
            offline,
            enforcement,
            products,
            loggingConfig,
            locale,
            fingerprint,
            telemetry
        );
    }
}
