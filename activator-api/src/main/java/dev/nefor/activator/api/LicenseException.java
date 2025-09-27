package dev.nefor.activator.api;

/**
 * Exception thrown when a license requirement cannot be fulfilled.
 */
public class LicenseException extends RuntimeException {
    private final LicenseStatus reason;
    private final String errorCode;

    public LicenseException(LicenseStatus reason, String message) {
        this(reason, message, null, null);
    }

    public LicenseException(LicenseStatus reason, String message, Throwable cause) {
        this(reason, message, null, cause);
    }

    public LicenseException(LicenseStatus reason, String message, String errorCode) {
        this(reason, message, errorCode, null);
    }

    public LicenseException(LicenseStatus reason, String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.errorCode = errorCode;
    }

    public LicenseStatus getReason() {
        return reason;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
