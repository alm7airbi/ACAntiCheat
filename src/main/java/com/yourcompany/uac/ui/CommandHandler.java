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

        double pps = checkManager.getPacketsPerSecond(playerId.get());
        Map<String, Integer> flags = checkManager.getFlagCounts(playerId.get());
        int trust = checkManager.getTrustScore(playerId.get());

        sender.sendMessage("§aACAntiCheat stats for §f" + targetName);
        sender.sendMessage(" §7Packets/sec: §f" + TWO_DECIMALS.format(pps));
        sender.sendMessage(" §7Trust score: §f" + trust);
        if (flags.isEmpty()) {
            sender.sendMessage(" §7Flags: §fNone recorded");
        } else {
            sender.sendMessage(" §7Flags:");
            flags.forEach((check, count) -> sender.sendMessage("  - " + check + ": " + count));
        }

        return true;
    }
}
