package dev.nefor.activator.plugin.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Simplistic localization helper backed by YAML resource bundles.
 */
public final class Localization {
    private final JavaPlugin plugin;
    private final Locale locale;
    private final FileConfiguration messages;
    private final FileConfiguration fallback;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public Localization(JavaPlugin plugin, Locale locale) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.locale = Objects.requireNonNull(locale, "locale");
        this.messages = loadForLocale(locale);
        this.fallback = loadForLocale(Locale.ENGLISH);
    }

    private FileConfiguration loadForLocale(Locale locale) {
        String tag = locale.toLanguageTag().toLowerCase(Locale.ROOT);
        String shortTag = tag.contains("-") ? tag.substring(0, tag.indexOf('-')) : tag;
        String resourceName = "lang/" + shortTag + ".yml";
        File dataFile = new File(plugin.getDataFolder(), resourceName);
        dataFile.getParentFile().mkdirs();
        if (!dataFile.exists()) {
            plugin.saveResource(resourceName, false);
        }
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.setDefaults(loadDefaults(resourceName));
            cfg.options().copyDefaults(true);
            cfg.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to load localization for " + resourceName + ": " + e.getMessage());
        }
        return cfg;
    }

    private FileConfiguration loadDefaults(String resourcePath) throws IOException, InvalidConfigurationException {
        YamlConfiguration defaults = new YamlConfiguration();
        String content = ResourceUtil.readResource(plugin, resourcePath, StandardCharsets.UTF_8);
        defaults.loadFromString(content);
        return defaults;
    }

    public String tr(String path) {
        return cache.computeIfAbsent(path, key -> {
            String value = messages.getString(key, fallback.getString(key, key));
            return value == null ? key : value;
        });
    }

    public String format(String path, Map<String, Object> params) {
        String template = tr(path);
        String result = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    public Locale getLocale() {
        return locale;
    }
}
