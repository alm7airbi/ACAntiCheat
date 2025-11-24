package com.yourcompany.uac.ui;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Basic command dispatcher. Full GUI and chat menus will be added later.
 */
public class CommandHandler implements CommandExecutor {

    private final UltimateAntiCheatPlugin plugin;

    public CommandHandler(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§aUltimateAntiCheat §7- use /uac gui|stats|config");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui":
                plugin.getGuiManager().openMainGui(sender);
                return true;
            case "stats":
                plugin.getGuiManager().openStatsGui(sender);
                return true;
            case "config":
                plugin.getGuiManager().openConfigGui(sender);
                return true;
            default:
                sender.sendMessage("Unknown subcommand. Use gui, stats or config.");
                return true;
        }
    }
}
