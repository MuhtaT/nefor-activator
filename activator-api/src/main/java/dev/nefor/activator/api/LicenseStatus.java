package dev.nefor.activator.api;

/**
 * Describes the current validation state of the license held by the Activator plugin.
 */
public enum LicenseStatus {
    /** License is valid and all products/features tied to the token can be used. */
    VALID,
    /** License is within the permitted offline grace window. Functionality may be restricted. */
    GRACE,
    /** License is not present or failed validation. */
    INVALID,
    /** License token expired and no refresh is available. */
    EXPIRED,
    /** License revoked by the licensing backend. */
    REVOKED,
    /** Temporary network issues prevent validation. */
    NETWORK_ISSUE
}
