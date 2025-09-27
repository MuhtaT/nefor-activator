package dev.nefor.activator.plugin;

import dev.nefor.activator.plugin.command.ActivatorCommand;
import dev.nefor.activator.plugin.config.ActivatorConfig;
import dev.nefor.activator.plugin.config.ActivatorConfigLoader;
import dev.nefor.activator.plugin.license.LicenseServiceImpl;
import dev.nefor.activator.plugin.util.Localization;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ActivatorPlugin extends JavaPlugin implements Listener {
    private ActivatorConfig config;
    private Localization localization;
    private LicenseServiceImpl licenseService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadActivatorConfig();
        getServer().getPluginManager().registerEvents(this, this);
        registerCommands();
        licenseService.bootstrap();
    }

    @Override
    public void onDisable() {
        if (licenseService != null) {
            licenseService.shutdown();
        }
    }

    public void reloadActivatorConfig() {
        reloadConfig();
        this.config = new ActivatorConfigLoader(this).load();
        this.localization = new Localization(this, config.locale());
        if (this.licenseService != null) {
            this.licenseService.shutdown();
        }
        this.licenseService = new LicenseServiceImpl(this, config, localization);
    }

    private void registerCommands() {
        PluginCommand command = Objects.requireNonNull(getCommand("activator"), "activator command not found");
        ActivatorCommand executor = new ActivatorCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public LicenseServiceImpl getLicenseService() {
        return licenseService;
    }

    public Localization getLocalization() {
        return localization;
    }

    public ActivatorConfig getActivatorConfig() {
        return config;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (licenseService != null) {
            licenseService.unsubscribe(event.getPlugin());
        }
    }
}
