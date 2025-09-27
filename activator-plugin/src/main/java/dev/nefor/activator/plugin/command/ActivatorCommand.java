package dev.nefor.activator.plugin.command;

import dev.nefor.activator.api.LicenseStatus;
import dev.nefor.activator.api.LicenseToken;
import dev.nefor.activator.plugin.ActivatorPlugin;
import dev.nefor.activator.plugin.license.LicenseServiceImpl;
import dev.nefor.activator.plugin.util.Localization;
import dev.nefor.activator.plugin.util.TimeUtil;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class ActivatorCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("status", "refresh", "diag", "reload");

    private final ActivatorPlugin plugin;

    public ActivatorCommand(ActivatorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("activator.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            return handleStatus(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "status" -> handleStatus(sender);
            case "refresh" -> handleRefresh(sender);
            case "diag" -> handleDiagnostics(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Available: " + SUBCOMMANDS);
                yield true;
            }
        };
    }

    private Localization i18n() {
        return plugin.getLocalization();
    }

    private boolean handleStatus(CommandSender sender) {
        LicenseServiceImpl service = plugin.getLicenseService();
        LicenseStatus status = service.getStatus();
        sender.sendMessage(ChatColor.GOLD + i18n().format("commands.status-header", Map.of("status", status)));
        sender.sendMessage(ChatColor.GRAY + i18n().format("commands.status-fingerprint", Map.of("fingerprint", service.getFingerprint())));
        LicenseToken token = service.getTokenMeta().orElse(null);
        if (token != null) {
            sender.sendMessage(ChatColor.GREEN + i18n().format("commands.status-products", Map.of(
                "products", String.join(", ", token.getProducts())
            )));
            sender.sendMessage(ChatColor.GREEN + i18n().format("commands.status-token-exp", Map.of(
                "expiry", TimeUtil.formatInstant(token.getExpiresAt())
            )));
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No token cached");
        }
        return true;
    }

    private boolean handleRefresh(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Triggering immediate license refresh...");
        plugin.getLicenseService().triggerRefresh();
        return true;
    }

    private boolean handleDiagnostics(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + i18n().tr("commands.diag-start"));
        CompletableFuture<LicenseServiceImpl.DiagnosticsResult> future = plugin.getLicenseService().runDiagnostics();
        future.thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (result.success()) {
                sender.sendMessage(ChatColor.GREEN + i18n().format("commands.diag-network-ok", Map.of(
                    "latency", result.latencyMs()
                )));
            } else {
                sender.sendMessage(ChatColor.RED + i18n().format("commands.diag-network-fail", Map.of(
                    "reason", result.message()
                )));
            }
        }));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading Activator configuration...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.reloadActivatorConfig();
            plugin.getLicenseService().bootstrap();
            Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.GREEN + i18n().tr("messages.command-reload")));
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                .filter(it -> it.startsWith(prefix))
                .toList();
        }
        return List.of();
    }
}
