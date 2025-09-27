package dev.nefor.activator.plugin.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResourceUtil {
    private ResourceUtil() {
    }

    public static String readResource(JavaPlugin plugin, String resourcePath, Charset charset) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            inputStream.transferTo(out);
            return out.toString(charset);
        }
    }
}
