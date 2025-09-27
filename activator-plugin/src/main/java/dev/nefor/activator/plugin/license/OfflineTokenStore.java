package dev.nefor.activator.plugin.license;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class OfflineTokenStore {
    private final JavaPlugin plugin;
    private final ObjectMapper mapper;
    private final File cacheFile;

    public OfflineTokenStore(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.cacheFile = new File(plugin.getDataFolder(), fileName);
        this.mapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Optional<OfflineCacheEntry> load() {
        if (!cacheFile.exists()) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(cacheFile.toPath());
            OfflineCacheEntry entry = mapper.readValue(bytes, OfflineCacheEntry.class);
            return Optional.of(entry);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load offline token cache: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void save(OfflineCacheEntry entry) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, entry);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write offline token cache: " + e.getMessage());
        }
    }

    public void clear() {
        if (cacheFile.exists() && !cacheFile.delete()) {
            plugin.getLogger().log(Level.WARNING, "Unable to delete offline cache file: " + cacheFile.getName());
        }
    }

    public void touch() {
        cacheFile.getParentFile().mkdirs();
        try {
            if (!cacheFile.exists()) {
                Files.writeString(cacheFile.toPath(), "{\"createdAt\":\"" + Instant.now() + "\"}\n");
            }
        } catch (IOException ignored) {
        }
    }
}
