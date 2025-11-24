package org.bukkit.command;

// TODO: Replace this stub with the real Bukkit CommandExecutor when running on Paper.

public interface CommandExecutor {
    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
