package org.bukkit.command;

/**
 * Minimal console sender stub.
 */
public class ConsoleCommandSender implements CommandSender {
    @Override
    public void sendMessage(String message) {
        // no-op for offline builds
    }
}
