package dev.nefor.activator.plugin.fingerprint;

import dev.nefor.activator.plugin.config.ActivatorConfig;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerFingerprintCalculator {
    private final JavaPlugin plugin;

    public ServerFingerprintCalculator(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public String calculate(ActivatorConfig config) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(config.fingerprint().salt().getBytes());
            digest.update(collectHardwareInfo().getBytes());
            digest.update(collectEnvironmentInfo().getBytes());
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create SHA-256 digest", e);
        }
    }

    private String collectHardwareInfo() {
        StringJoiner joiner = new StringJoiner("|");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface nif = interfaces.nextElement();
                if (nif.isLoopback() || nif.isVirtual()) {
                    continue;
                }
                byte[] mac = nif.getHardwareAddress();
                if (mac != null) {
                    joiner.add(HexFormat.of().formatHex(mac));
                }
            }
        } catch (Exception ignored) {
        }

        try {
            InetAddress local = InetAddress.getLocalHost();
            joiner.add(local.getHostName());
        } catch (IOException ignored) {
        }
        Path root = plugin.getDataFolder().toPath().getParent();
        if (root != null) {
            joiner.add(root.toAbsolutePath().normalize().toString());
        }
        return joiner.toString();
    }

    private String collectEnvironmentInfo() {
        return new StringJoiner("|")
            .add(System.getProperty("os.name", ""))
            .add(System.getProperty("os.arch", ""))
            .add(System.getProperty("os.version", ""))
            .add(System.getProperty("java.version", ""))
            .add(plugin.getServer().getBukkitVersion())
            .add(plugin.getServer().getVersion())
            .toString()
            .toLowerCase(Locale.ROOT);
    }
}
