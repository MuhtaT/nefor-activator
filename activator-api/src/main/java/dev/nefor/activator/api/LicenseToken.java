package dev.nefor.activator.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable metadata snapshot extracted from a validated license token.
 */
public final class LicenseToken {
    private final String tokenId;
    private final String subject;
    private final String issuer;
    private final Instant issuedAt;
    private final Instant notBefore;
    private final Instant expiresAt;
    private final String fingerprint;
    private final Set<String> products;
    private final Map<String, Set<String>> features;
    private final Map<String, String> maxVersions;
    private final Duration offlineTtl;
    private final int revision;
    private final String rawToken;
    private final String keyId;
    private final Map<String, Object> claims;

    private LicenseToken(Builder builder) {
        this.tokenId = builder.tokenId;
        this.subject = builder.subject;
        this.issuer = builder.issuer;
        this.issuedAt = builder.issuedAt;
        this.notBefore = builder.notBefore;
        this.expiresAt = builder.expiresAt;
        this.fingerprint = builder.fingerprint;
        this.products = builder.products == null ? Set.of() : Collections.unmodifiableSet(builder.products);
        this.features = builder.features == null ? Collections.emptyMap() : Collections.unmodifiableMap(builder.features);
        this.maxVersions = builder.maxVersions == null ? Collections.emptyMap() : Collections.unmodifiableMap(builder.maxVersions);
        this.offlineTtl = builder.offlineTtl;
        this.revision = builder.revision;
        this.rawToken = builder.rawToken;
        this.keyId = builder.keyId;
        this.claims = builder.claims == null ? Collections.emptyMap() : Collections.unmodifiableMap(builder.claims);
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Duration getOfflineTtl() {
        return offlineTtl;
    }

    public int getRevision() {
        return revision;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Set<String> getProducts() {
        return products;
    }

    public Map<String, Set<String>> getFeatures() {
        return features;
    }

    public Map<String, String> getMaxVersions() {
        return maxVersions;
    }

    public String getRawToken() {
        return rawToken;
    }

    public String getKeyId() {
        return keyId;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String tokenId;
        private String subject;
        private String issuer;
        private Instant issuedAt;
        private Instant notBefore;
        private Instant expiresAt;
        private String fingerprint;
        private Set<String> products;
        private Map<String, Set<String>> features;
        private Map<String, String> maxVersions;
        private Duration offlineTtl;
        private int revision;
        private String rawToken;
        private String keyId;
        private Map<String, Object> claims;

        private Builder() {
        }

        public Builder tokenId(String tokenId) {
            this.tokenId = tokenId;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder issuedAt(Instant issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }

        public Builder notBefore(Instant notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder products(Set<String> products) {
            this.products = products;
            return this;
        }

        public Builder features(Map<String, Set<String>> features) {
            this.features = features;
            return this;
        }

        public Builder maxVersions(Map<String, String> maxVersions) {
            this.maxVersions = maxVersions;
            return this;
        }

        public Builder offlineTtl(Duration offlineTtl) {
            this.offlineTtl = offlineTtl;
            return this;
        }

        public Builder revision(int revision) {
            this.revision = revision;
            return this;
        }

        public Builder rawToken(String rawToken) {
            this.rawToken = rawToken;
            return this;
        }

        public Builder keyId(String keyId) {
            this.keyId = keyId;
            return this;
        }

        public Builder claims(Map<String, Object> claims) {
            this.claims = claims;
            return this;
        }

        public LicenseToken build() {
            Objects.requireNonNull(subject, "subject");
            Objects.requireNonNull(issuer, "issuer");
            Objects.requireNonNull(fingerprint, "fingerprint");
            Objects.requireNonNull(rawToken, "rawToken");
            return new LicenseToken(this);
        }
    }
}
