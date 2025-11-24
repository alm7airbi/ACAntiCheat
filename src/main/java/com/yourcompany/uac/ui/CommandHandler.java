package com.yourcompany.uac.ui;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Basic command dispatcher. Full GUI and chat menus will be added later.
 */
public class CommandHandler implements CommandExecutor {

    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("0.00");

    private final UltimateAntiCheatPlugin plugin;
    private final CheckManager checkManager;

    public CommandHandler(UltimateAntiCheatPlugin plugin, CheckManager checkManager) {
        this.plugin = plugin;
        this.checkManager = checkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§aACAntiCheat §7- use /acac stats <player>");
            return true;
        }

        if ("stats".equalsIgnoreCase(args[0])) {
            return handleStats(sender, args);
        }

        sender.sendMessage("Unknown subcommand. Use stats.");
        return true;
    }

    private boolean handleStats(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /acac stats <player>");
            return true;
        }

        String targetName = args[1];
        Optional<UUID> playerId = checkManager.findPlayerId(targetName);

        if (playerId.isEmpty()) {
            sender.sendMessage("No recent data for " + targetName + ".");
            return true;
        }

        CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(playerId.get());
        Map<String, Integer> flags = stats.flagCounts();

        sender.sendMessage("§aACAntiCheat §fstats for §b" + targetName);
        sender.sendMessage(" §7Trust: §f" + TWO_DECIMALS.format(stats.trustScore()) + "§7/100" +
                (stats.underMitigation() ? " §c(under mitigation)" : ""));
        sender.sendMessage(" §7Avg packets/sec (last 5s): §f" + TWO_DECIMALS.format(stats.packetsPerSecond()));
        if (flags.isEmpty()) {
            sender.sendMessage(" §7Flags: §fNone recorded");
        } else {
            sender.sendMessage(" §7Flags:");
            flags.forEach((check, count) -> sender.sendMessage("  §8- §e" + check + "§7: §f" + count));
        }

        return true;
    }
}
