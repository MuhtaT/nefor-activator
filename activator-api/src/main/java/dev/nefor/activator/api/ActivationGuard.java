package dev.nefor.activator.api;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Convenience utilities that allow third-party plugins to enforce Activator presence with minimal code.
 */
public final class ActivationGuard {
    private ActivationGuard() {
    }

    public static void guardOrDisable(Plugin plugin, String productId) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(productId, "productId");

        LicenseService svc = Bukkit.getServicesManager().load(LicenseService.class);
        if (svc == null) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Activator plugin not found");
        }
        try {
            svc.require(plugin, productId);
        } catch (LicenseException ex) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw ex;
        }
    }

    public static void guardFeatureOrDisable(Plugin plugin, String productId, String feature) {
        Objects.requireNonNull(feature, "feature");
        LicenseService svc = Bukkit.getServicesManager().load(LicenseService.class);
        if (svc == null) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("Activator plugin not found");
        }
        try {
            svc.require(plugin, productId);
            if (!svc.hasFeature(productId, feature)) {
                throw new LicenseException(
                    LicenseStatus.INVALID,
                    "Required feature '" + feature + "' not granted for product '" + productId + "'");
            }
        } catch (LicenseException ex) {
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw ex;
        }
    }

    public static void guardAnnotatedPlugin(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        RequiresLicense requires = plugin.getClass().getAnnotation(RequiresLicense.class);
        if (requires != null) {
            guardOrDisable(plugin, requires.productId());
        }

        Set<FeatureFlag> featureAnnotations = Optional.of(plugin.getClass().getAnnotationsByType(FeatureFlag.class))
            .map(Arrays::asList)
            .map(Set::copyOf)
            .orElseGet(Set::of);

        if (!featureAnnotations.isEmpty()) {
            LicenseService svc = Optional.ofNullable(Bukkit.getServicesManager().load(LicenseService.class))
                .orElseThrow(() -> new IllegalStateException("Activator plugin not found"));
            for (FeatureFlag featureFlag : featureAnnotations) {
                if (!svc.hasFeature(featureFlag.productId(), featureFlag.value())) {
                    Bukkit.getPluginManager().disablePlugin(plugin);
                    throw new LicenseException(
                        LicenseStatus.INVALID,
                        "Feature '" + featureFlag.value() + "' not granted for product '" + featureFlag.productId() + "'");
                }
            }
        }
    }

    public static String describeState() {
        LicenseService svc = Bukkit.getServicesManager().load(LicenseService.class);
        if (svc == null) {
            return "Activator service unavailable";
        }
        return "status=" + svc.getStatus() + " products=" + String.join(",", svc.getLicensedProducts());
    }

    public static String describeFeatures(String productId) {
        LicenseService svc = Bukkit.getServicesManager().load(LicenseService.class);
        if (svc == null) {
            return "Activator service unavailable";
        }
        return svc.getEnabledFeatures(productId).stream().sorted().collect(Collectors.joining(","));
    }
}
