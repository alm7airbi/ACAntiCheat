package org.bukkit.plugin;

public class PluginManager {
    private final java.util.List<org.bukkit.event.Listener> listeners = new java.util.ArrayList<>();
    private final java.util.List<org.bukkit.entity.Player> onlinePlayers = new java.util.ArrayList<>();

    public boolean isPluginEnabled(String name) {
        return false;
    }

    public java.util.List<org.bukkit.entity.Player> getOnlinePlayers() {
        return java.util.Collections.unmodifiableList(onlinePlayers);
    }

    public void registerEvents(org.bukkit.event.Listener listener, org.bukkit.plugin.Plugin plugin) {
        // No-op in stub; record for inspection/testing only.
        listeners.add(listener);
        plugin.getLogger().info("[StubPluginManager] registered listener " + listener.getClass().getSimpleName());
    }

    public void addOnlinePlayer(org.bukkit.entity.Player player) {
        onlinePlayers.add(player);
    }
}
