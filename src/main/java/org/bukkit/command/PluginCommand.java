package org.bukkit.command;

// Stub-only PluginCommand for offline builds; replaced by Bukkit's implementation in Paper deployments.

public class PluginCommand extends Command {
    private CommandExecutor executor;

    public PluginCommand(String name) {
        super(name);
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }
}
