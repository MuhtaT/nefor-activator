package dev.nefor.activator.api;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

/**
 * Helper entry points intended for runtime wiring inside protected plugins.
 */
public final class LicenseIntegrationSupport {
    private LicenseIntegrationSupport() {
    }

    public static LicenseService loadServiceOrThrow() {
        LicenseService svc = Bukkit.getServicesManager().load(LicenseService.class);
        if (svc == null) {
            throw new IllegalStateException("Activator plugin not found");
        }
        return svc;
    }

    public static void integrateAndSubscribe(Plugin plugin, LicenseListener listener) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(listener, "listener");
        LicenseService service = loadServiceOrThrow();

        Map<String, Set<String>> featuresByProduct = Arrays.stream(plugin.getClass().getAnnotationsByType(FeatureFlag.class))
            .collect(Collectors.groupingBy(FeatureFlag::productId, Collectors.mapping(FeatureFlag::value, Collectors.toUnmodifiableSet())));

        RequiresLicense requires = plugin.getClass().getAnnotation(RequiresLicense.class);
        if (requires != null) {
            service.registerDependentPlugin(plugin, requires.productId(), featuresByProduct.getOrDefault(requires.productId(), Set.of()));
        } else {
            featuresByProduct.forEach((productId, features) -> service.registerDependentPlugin(plugin, productId, features));
        }

        for (Map.Entry<String, Set<String>> entry : featuresByProduct.entrySet()) {
            for (String feature : entry.getValue()) {
                if (!service.hasFeature(entry.getKey(), feature)) {
                    throw new LicenseException(
                        LicenseStatus.INVALID,
                        "Required feature '" + feature + "' missing for product '" + entry.getKey() + "'");
                }
            }
        }

        service.subscribe(plugin, listener);
    }

    public static void disablePlugin(Plugin plugin, LicenseException ex) {
        PluginManager pm = Bukkit.getPluginManager();
        HandlerList.unregisterAll(plugin);
        pm.disablePlugin(plugin);
        throw ex;
    }
}
