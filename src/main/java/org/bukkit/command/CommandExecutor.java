package org.bukkit.command;

// Stub-only CommandExecutor for offline compilation; swapped for Bukkit's version in Paper builds.

public interface CommandExecutor {
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
