package org.bukkit.plugin.java;

import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.logging.Logger;

public class JavaPlugin implements org.bukkit.plugin.Plugin {
    private final Logger logger = Logger.getLogger("JavaPlugin");
    private final FileConfiguration configuration = new FileConfiguration();
    private final Server server = new Server();
    private final File dataFolder = new File("plugins/ACAntiCheat");

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

    public File getDataFolder() {
        return dataFolder;
    }

    public PluginCommand getCommand(String name) {
        return new PluginCommand(name);
    }

    public Server getServer() {
        return server;
    }
}
