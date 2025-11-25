package org.bukkit.command;

// Stub-only CommandSender for offline builds; Paper deployments use Bukkit's implementation.

public interface CommandSender {
    void sendMessage(String message);
}
