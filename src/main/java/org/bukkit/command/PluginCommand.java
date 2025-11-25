package org.bukkit.command;

// TODO: Replace this stub with the real Bukkit PluginCommand when targeting Paper.

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
