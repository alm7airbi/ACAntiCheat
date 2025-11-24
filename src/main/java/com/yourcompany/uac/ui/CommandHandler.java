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
    private boolean debugMode = false;

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

        if ("gui".equalsIgnoreCase(args[0])) {
            plugin.getGuiManager().openMainGui(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            plugin.getConfigManager().load();
            sender.sendMessage("§aACAC config reloaded.");
            return true;
        }

        if ("stats".equalsIgnoreCase(args[0])) {
            return handleStats(sender, args);
        }

        if ("inspect".equalsIgnoreCase(args[0])) {
            return handleInspect(sender, args);
        }

        if ("history".equalsIgnoreCase(args[0])) {
            return handleHistory(sender, args);
        }

        if ("debug".equalsIgnoreCase(args[0])) {
            debugMode = !debugMode;
            sender.sendMessage("Debug mode " + (debugMode ? "enabled" : "disabled"));
            return true;
        }

        if ("perf".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§aACAC per-check timings (ms avg):");
            plugin.getCheckManager().getPerformanceSnapshot().forEach((check, avg) ->
                    sender.sendMessage(" §7" + check + ": §f" + TWO_DECIMALS.format(avg)));
            return true;
        }

        if ("selftest".equalsIgnoreCase(args[0])) {
            runSelfTest(sender);
            return true;
        }

        sender.sendMessage("Unknown subcommand. Use stats/gui/reload/inspect/history/perf/selftest.");
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
                (stats.underMitigation() ? " §c(under mitigation: " + stats.lastMitigation() + ")" : ""));
        sender.sendMessage(" §7Avg packets/sec (last 5s): §f" + TWO_DECIMALS.format(stats.packetsPerSecond()));
        if (stats.mitigationNote() != null) {
            sender.sendMessage(" §cMitigation note: §f" + stats.mitigationNote());
        }
        if (debugMode) {
            sender.sendMessage(" §7Mitigation level: §f" + stats.lastMitigation());
            sender.sendMessage(" §7History: §f" + String.join(", ", stats.mitigationHistory()));
        }
        if (flags.isEmpty()) {
            sender.sendMessage(" §7Flags: §fNone recorded");
        } else {
            sender.sendMessage(" §7Flags:");
            stats.summaries().forEach((check, record) -> {
                String risk = stats.riskFor(check, record.lastSeverity());
                sender.sendMessage("  §8- §e" + check + "§7: §f" + record.count() +
                        " §7risk=§" + ("HIGH".equals(risk) ? "c" : "MED".equals(risk) ? "e" : "a") + risk +
                        " §7last=§f" + record.lastReason());
            });
        }

        if (!stats.mitigationHistory().isEmpty()) {
            sender.sendMessage(" §7Mitigations: " + String.join(" | ", stats.mitigationHistory()));
        }

        return true;
    }

    private boolean handleInspect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /acac inspect <player>");
            return true;
        }
        Optional<UUID> playerId = checkManager.findPlayerId(args[1]);
        if (playerId.isEmpty()) {
            sender.sendMessage("No recent data for " + args[1] + ".");
            return true;
        }

        CheckManager.PlayerStats stats = checkManager.getStatsForPlayer(playerId.get());
        sender.sendMessage("§aInspecting §b" + args[1] + "§7 …");
        sender.sendMessage(" §7Trust: §f" + TWO_DECIMALS.format(stats.trustScore()) +
                " §7Mitigation: §f" + stats.lastMitigation());
        sender.sendMessage(" §7Packets/sec: §f" + TWO_DECIMALS.format(stats.packetsPerSecond()));
        if (stats.mitigationNote() != null) {
            sender.sendMessage(" §7Mitigation note: §f" + stats.mitigationNote());
        }
        if (stats.summaries().isEmpty()) {
            sender.sendMessage(" §7No flags recorded.");
            return true;
        }

        stats.summaries().forEach((check, record) -> {
            String risk = stats.riskFor(check, record.lastSeverity());
            sender.sendMessage("  §8- §e" + check + " §7count=§f" + record.count() +
                    " §7risk=§" + ("HIGH".equals(risk) ? "c" : "MED".equals(risk) ? "e" : "a") + risk +
                    " §7last=§f" + record.lastReason());
        });
        if (!stats.mitigationHistory().isEmpty()) {
            sender.sendMessage(" §7Recent mitigations:");
            stats.mitigationHistory().forEach(entry -> sender.sendMessage("  §8- §f" + entry));
        }
        plugin.getGuiManager().openInspectGui(sender, args[1]);
        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /acac history <player>");
            return true;
        }
        Optional<UUID> playerId = checkManager.findPlayerId(args[1]);
        if (playerId.isEmpty()) {
            sender.sendMessage("No recent data for " + args[1] + ".");
            return true;
        }
        int limit = plugin.getConfigManager().getSettings().historyLimit;
        var history = checkManager.getHistory(playerId.get(), limit);
        sender.sendMessage("§aHistory for §b" + args[1]);
        if (history.isEmpty()) {
            sender.sendMessage(" §7No stored history.");
            return true;
        }
        history.forEach(line -> sender.sendMessage(" §8- §f" + line));
        return true;
    }

    private void runSelfTest(CommandSender sender) {
        sender.sendMessage("§aRunning ACAC self-test…");
        sender.sendMessage(" Integration mode: " + plugin.getIntegrationService().name());
        sender.sendMessage(" Using stub: " + plugin.getIntegrationService().isUsingStub());
        sender.sendMessage(" ProtocolLib present: " + plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib"));
        sender.sendMessage(" Event bridge: " + plugin.getIntegrationService().getEventBridge().name());
        sender.sendMessage(" Packet bridge: " + plugin.getIntegrationService().getPacketBridge().name());
        sender.sendMessage(" Config valid: " + (plugin.getConfigManager().getSettings() != null));

        // Simulate benign traffic
        org.bukkit.entity.Player safePlayer = new org.bukkit.entity.Player("SelfTest", java.util.UUID.randomUUID());
        plugin.getCheckManager().handleMovement(safePlayer, 0, 65, 0, false);
        plugin.getCheckManager().handlePacket(new com.yourcompany.uac.packet.PacketPayload(safePlayer, new Object()));
        sender.sendMessage(" §7Safe traffic processed with trust=" + TWO_DECIMALS.format(plugin.getCheckManager().getTrustScore(safePlayer.getUniqueId())));

        // Simulate bursty packets to verify mitigation decision path without kicking real users
        for (int i = 0; i < 30; i++) {
            plugin.getCheckManager().handlePacket(new com.yourcompany.uac.packet.PacketPayload(safePlayer, new Object()));
        }
        var stats = plugin.getCheckManager().getStatsForPlayer(safePlayer.getUniqueId());
        sender.sendMessage(" §eSynthetic spike flags=" + stats.flagCounts().values().stream().mapToInt(Integer::intValue).sum()
                + " mitigation=" + stats.lastMitigation());
    }
}
