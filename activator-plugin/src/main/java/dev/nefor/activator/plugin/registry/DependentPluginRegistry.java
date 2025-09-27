package dev.nefor.activator.plugin.registry;

import dev.nefor.activator.api.LicenseService;
import dev.nefor.activator.api.LicenseStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class DependentPluginRegistry {
    private final JavaPlugin activator;
    private final Map<String, RegisteredPluginImpl> dependents = new ConcurrentHashMap<>();

    public DependentPluginRegistry(JavaPlugin activator) {
        this.activator = Objects.requireNonNull(activator, "activator");
    }

    public void register(Plugin plugin, String productId, Set<String> features) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(productId, "productId");
        dependents.put(plugin.getName(), new RegisteredPluginImpl(plugin, productId, features == null ? Set.of() : Set.copyOf(features)));
    }

    public void unregister(Plugin plugin) {
        if (plugin != null) {
            dependents.remove(plugin.getName());
        }
    }

    public Collection<LicenseService.RegisteredPlugin> snapshot() {
        return Collections.unmodifiableCollection(dependents.values());
    }

    public void disableAll(LicenseStatus reason, long delayTicks, boolean log) {
        for (RegisteredPluginImpl descriptor : dependents.values()) {
            Plugin plugin = descriptor.plugin();
            if (plugin.isEnabled()) {
                if (log) {
                    Bukkit.getLogger().log(Level.WARNING, () -> "License " + reason + ": disabling dependent " + plugin.getName());
                }
                Bukkit.getScheduler().runTaskLater(activator, () -> Bukkit.getPluginManager().disablePlugin(plugin), delayTicks);
            }
        }
    }

    public record RegisteredPluginImpl(Plugin plugin, String productId, Set<String> requestedFeatures) implements LicenseService.RegisteredPlugin {
    }
}
