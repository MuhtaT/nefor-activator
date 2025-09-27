package dev.nefor.activator.plugin.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RevocationEntry {
    @JsonProperty("jti")
    private String tokenId;
    @JsonProperty("rev")
    private int revision;
    @JsonProperty("exp")
    private Instant expiresAt;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
