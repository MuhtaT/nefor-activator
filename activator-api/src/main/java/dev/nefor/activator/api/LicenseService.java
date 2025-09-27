package dev.nefor.activator.api;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.bukkit.plugin.Plugin;

/**
 * Primary entry point for secure plugins to interrogate the Activator runtime.
 */
public interface LicenseService {

    /** @return current status of the license. */
    LicenseStatus getStatus();

    /**
     * Checks whether the given product is present in the active license and
     * available under the current status.
     */
    boolean isProductAllowed(String productId);

    /**
     * Checks whether a feature is enabled for the given product.
     */
    boolean hasFeature(String productId, String feature);

    /**
     * Returns the set of features enabled for the provided product identifier.
     */
    Set<String> getEnabledFeatures(String productId);

    /**
     * Returns all licensed products currently known to the Activator runtime.
     */
    Set<String> getLicensedProducts();

    /**
     * Returns the maximum supported version constraint (if any) for a given product.
     */
    Optional<String> getMaxVersion(String productId);

    /**
     * Ensures the given plugin is allowed to run under the specified product id.
     *
     * @throws LicenseException when the product is not granted or the license is invalid.
     */
    void require(Plugin plugin, String productId) throws LicenseException;

    /**
     * Registers a listener to be notified about license state changes.
     *
     * @param plugin    plugin subscribing (used for auto-unsubscribe on disable)
     * @param listener  listener implementation
     */
    void subscribe(Plugin plugin, LicenseListener listener);

    /**
     * Unsubscribes listeners associated with the plugin.
     */
    void unsubscribe(Plugin plugin);

    /**
     * @return server fingerprint calculated by Activator for diagnostics.
     */
    String getFingerprint();

    /**
     * Returns metadata extracted from the current license token, if available.
     */
    Optional<LicenseToken> getTokenMeta();

    /**
     * Registers a dependent plugin/product pair so Activator can keep track for enforcement.
     * The default implementation delegates to {@link #require(Plugin, String)} and can be overridden.
     */
    default void registerDependentPlugin(Plugin plugin, String productId) {
        registerDependentPlugin(plugin, productId, Set.of());
    }

    /**
     * Registers a dependent plugin with explicit feature requirements.
     */
    default void registerDependentPlugin(Plugin plugin, String productId, Set<String> features) {
        require(plugin, productId);
    }

    /**
     * @return immutable snapshot of registered dependents.
     */
    Collection<RegisteredPlugin> getRegisteredPlugins();

    /**
     * Immutable descriptor of a plugin registered with Activator.
     */
    interface RegisteredPlugin {
        Plugin plugin();
        String productId();
        Set<String> requestedFeatures();
    }
}
