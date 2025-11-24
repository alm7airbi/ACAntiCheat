package org.bukkit.plugin.java;

import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class JavaPlugin {
    private final Logger logger = Logger.getLogger("JavaPlugin");
    private final FileConfiguration configuration = new FileConfiguration();
    private final Server server = new Server();

    public void onEnable() {}

    public void onDisable() {}

    public void saveDefaultConfig() {
        // no-op stub
    }

    public void reloadConfig() {
        // no-op stub
    }

    public FileConfiguration getConfig() {
        return configuration;
    }

    public Logger getLogger() {
        return logger;
    }

    public PluginCommand getCommand(String name) {
        return new PluginCommand(name);
    }

    public Server getServer() {
        return server;
    }
}
