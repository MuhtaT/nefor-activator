package dev.nefor.activator.api;

/**
 * Listener notified whenever the Activator license state changes.
 */
public interface LicenseListener {
    /** Called when the license becomes valid. */
    void onLicenseGranted();

    /**
     * Called when the license becomes invalid.
     *
     * @param reason reason for revocation or invalidation
     */
    void onLicenseRevoked(LicenseStatus reason);
}
