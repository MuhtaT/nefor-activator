package dev.nefor.activator.plugin.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OfflineCacheEntry {
    @JsonProperty("token")
    private String token;
    @JsonProperty("fingerprint")
    private String fingerprint;
    @JsonProperty("fetchedAt")
    private Instant fetchedAt;
    @JsonProperty("expiresAt")
    private Instant expiresAt;
    @JsonProperty("refreshTtlSeconds")
    private long refreshTtlSeconds;
    @JsonProperty("jwks")
    private String jwksJson;
    @JsonProperty("crl")
    private String crlJson;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public void setRefreshTtlSeconds(long refreshTtlSeconds) {
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String getJwksJson() {
        return jwksJson;
    }

    public void setJwksJson(String jwksJson) {
        this.jwksJson = jwksJson;
    }

    public String getCrlJson() {
        return crlJson;
    }

    public void setCrlJson(String crlJson) {
        this.crlJson = crlJson;
    }
}
