package org.bukkit;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

public class Server {
    private final PluginManager pluginManager = new PluginManager();
    private final BanList banList = new BanList();
    private final BukkitScheduler scheduler = new BukkitScheduler();
    private final ConsoleCommandSender console = new ConsoleCommandSender();

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public java.util.List<org.bukkit.entity.Player> getOnlinePlayers() {
        return pluginManager.getOnlinePlayers();
    }

    public BanList getBanList(BanList.Type type) {
        return banList;
    }

    public BukkitScheduler getScheduler() {
        return scheduler;
    }

    public ConsoleCommandSender getConsoleSender() {
        return console;
    }
}
