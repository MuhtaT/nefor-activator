package dev.nefor.activator.plugin.license;

import dev.nefor.activator.api.LicenseException;
import dev.nefor.activator.api.LicenseStatus;
import dev.nefor.activator.api.LicenseToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class LicenseValidator {
    private final Logger logger;
    private final boolean requireCrl;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, ECKey> keyCache = new ConcurrentHashMap<>();
    private volatile List<RevocationEntry> revocations = List.of();

    public LicenseValidator(Logger logger, boolean requireCrl) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.requireCrl = requireCrl;
    }

    public void loadJwks(String jwksJson) {
        try {
            JWKSet set = JWKSet.parse(jwksJson);
            for (JWK jwk : set.getKeys()) {
                if (jwk instanceof ECKey ecKey) {
                    keyCache.put(jwk.getKeyID(), ecKey);
                } else {
                    logger.warning("Skipping non-EC key in JWKS: " + jwk.getKeyID());
                }
            }
        } catch (ParseException e) {
            throw new IllegalStateException("Invalid JWKS payload", e);
        }
    }

    public void loadCrl(String crlJson) {
        try {
            List<RevocationEntry> list = mapper.readValue(crlJson, new TypeReference<List<RevocationEntry>>() {});
            this.revocations = Collections.unmodifiableList(list);
        } catch (Exception e) {
            if (requireCrl) {
                throw new IllegalStateException("Unable to parse CRL payload", e);
            }
            logger.warning("Failed to parse CRL payload: " + e.getMessage());
        }
    }

    public LicenseToken verify(String token, String expectedFingerprint) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String kid = Optional.ofNullable(signedJWT.getHeader().getKeyID())
                .orElseThrow(() -> new LicenseException(LicenseStatus.INVALID, "Token missing key id"));
            ECKey key = keyCache.get(kid);
            if (key == null) {
                throw new LicenseException(LicenseStatus.INVALID, "Unknown key id: " + kid, "UNKNOWN_KID");
            }
            if (!signedJWT.verify(new ECDSAVerifier(key.toECPublicKey()))) {
                throw new LicenseException(LicenseStatus.INVALID, "Signature verification failed", "SIGNATURE_INVALID");
            }
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Instant now = Instant.now();
            Instant expiry = Optional.ofNullable(claims.getExpirationTime()).map(java.util.Date::toInstant).orElse(null);
            Instant notBefore = Optional.ofNullable(claims.getNotBeforeTime()).map(java.util.Date::toInstant).orElse(null);
            if (expiry != null && now.isAfter(expiry)) {
                throw new LicenseException(LicenseStatus.EXPIRED, "License token expired at " + expiry);
            }
            if (notBefore != null && now.isBefore(notBefore)) {
                throw new LicenseException(LicenseStatus.INVALID, "Token not valid before " + notBefore, "NOT_BEFORE");
            }
            String fingerprint = claims.getStringClaim("srv_fp");
            if (expectedFingerprint != null && !expectedFingerprint.equals(fingerprint)) {
                throw new LicenseException(LicenseStatus.INVALID, "Fingerprint mismatch", "FINGERPRINT_MISMATCH");
            }
            if (requireCrl && isRevoked(claims)) {
                throw new LicenseException(LicenseStatus.REVOKED, "Token revoked", "REVOKED");
            }
            Set<String> products = new HashSet<>(Optional.ofNullable(claims.getStringListClaim("products")).orElse(List.of()));
            Map<String, Set<String>> features = toFeatureMap(claims.getJSONObjectClaim("features"));
            Map<String, String> maxVersion = toStringMap(claims.getJSONObjectClaim("max_version"));
            Duration offline = Optional.ofNullable(claims.getLongClaim("offline_ttl")).map(Duration::ofSeconds).orElse(Duration.ZERO);
            LicenseToken.Builder builder = LicenseToken.builder()
                .tokenId(Optional.ofNullable(claims.getJWTID()).orElse(null))
                .subject(claims.getSubject())
                .issuer(claims.getIssuer())
                .issuedAt(Optional.ofNullable(claims.getIssueTime()).map(java.util.Date::toInstant).orElse(null))
                .notBefore(notBefore)
                .expiresAt(expiry)
                .fingerprint(fingerprint)
                .products(Collections.unmodifiableSet(products))
                .features(features)
                .maxVersions(maxVersion)
                .offlineTtl(offline)
                .revision(extractRevision(claims))
                .rawToken(token)
                .keyId(kid)
                .claims(claims.getClaims());
            return builder.build();
        } catch (ParseException e) {
            throw new LicenseException(LicenseStatus.INVALID, "Malformed token", "TOKEN_PARSE", e);
        } catch (JOSEException e) {
            throw new LicenseException(LicenseStatus.INVALID, "Unable to verify token", "TOKEN_VERIFY", e);
        }
    }

    private boolean isRevoked(JWTClaimsSet claims) {
        String tokenId = claims.getJWTID();
        int revision = extractRevision(claims);
        for (RevocationEntry entry : revocations) {
            if (entry.getTokenId() != null && tokenId != null && entry.getTokenId().equals(tokenId)) {
                return true;
            }
            if (entry.getRevision() > 0 && revision > 0 && revision <= entry.getRevision()) {
                return true;
            }
        }
        return false;
    }

    private int extractRevision(JWTClaimsSet claims) {
        try {
            Object value = claims.getClaim("rev");
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value != null) {
                return Integer.parseInt(value.toString());
            }
        } catch (Exception e) {
            logger.fine(() -> "Unable to parse revision claim: " + e.getMessage());
        }
        return 0;
    }

    private Map<String, Set<String>> toFeatureMap(Map<String, Object> obj) {
        if (obj == null) {
            return Map.of();
        }
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                Set<String> features = new HashSet<>();
                for (Object item : list) {
                    features.add(String.valueOf(item));
                }
                result.put(entry.getKey(), Collections.unmodifiableSet(features));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, String> toStringMap(Map<String, Object> obj) {
        if (obj == null) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}





