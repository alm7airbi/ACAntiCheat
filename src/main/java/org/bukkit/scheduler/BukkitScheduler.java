package org.bukkit.scheduler;

/**
 * Minimal scheduler stub for offline builds.
 */
public class BukkitScheduler {
    public void runTask(org.bukkit.plugin.Plugin plugin, Runnable runnable) {
        runnable.run();
    }

    public void runTaskLater(org.bukkit.plugin.Plugin plugin, Runnable runnable, long delayTicks) {
        runnable.run();
    }
}
