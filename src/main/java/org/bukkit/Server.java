package org.bukkit;

import org.bukkit.plugin.PluginManager;

public class Server {
    private final PluginManager pluginManager = new PluginManager();

    public PluginManager getPluginManager() {
        return pluginManager;
    }
}
