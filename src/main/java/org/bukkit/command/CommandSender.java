package org.bukkit.command;

// Stub-only CommandSender for offline builds; Paper deployments use Bukkit's implementation.

public interface CommandSender {
    void sendMessage(String message);

    default boolean hasPermission(String permission) {
        return true;
    }

    default String getName() {
        return "CONSOLE";
    }
}
