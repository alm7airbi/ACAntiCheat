package org.bukkit;

import org.bukkit.plugin.PluginManager;

public class Server {
    private final PluginManager pluginManager = new PluginManager();
    private final BanList banList = new BanList();

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public java.util.List<org.bukkit.entity.Player> getOnlinePlayers() {
        return pluginManager.getOnlinePlayers();
    }

    public BanList getBanList(BanList.Type type) {
        return banList;
    }
}
