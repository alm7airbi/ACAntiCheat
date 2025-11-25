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
            return help(sender);
        }

        if ("help".equalsIgnoreCase(args[0])) {
            return help(sender);
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
            var settings = plugin.getConfigManager().getSettings();
            double budget = settings.perfCheckBudgetMs;
            double totalBudget = settings.perfTotalBudgetMs;
            sender.sendMessage("§aACAC per-check timings (ms avg):");
            double total = 0;
            for (var entry : plugin.getCheckManager().getPerformanceSnapshot().entrySet()) {
                double avg = entry.getValue();
                total += avg;
                String color = avg > budget ? "§c" : avg > budget * 0.8 ? "§e" : "§a";
                sender.sendMessage(" §7" + entry.getKey() + ": " + color + TWO_DECIMALS.format(avg) +
                        "§7 ms (budget " + budget + ")");
            }
            String totalColor = total > totalBudget ? "§c" : total > totalBudget * 0.8 ? "§e" : "§a";
            sender.sendMessage(" §7Total: " + totalColor + TWO_DECIMALS.format(total) + "§7 ms (budget " + totalBudget + ")");
            sender.sendMessage(" §7Webhook status: §f" + plugin.getAlertManager().getLastWebhookStatus());
            sender.sendMessage(" §7Persistence: §f" + plugin.getDatabaseManager().getPersistenceStatus());
            return true;
        }

        if ("selftest".equalsIgnoreCase(args[0])) {
            runSelfTest(sender);
            return true;
        }

        sender.sendMessage("Unknown subcommand. Use stats/gui/reload/inspect/history/perf/selftest.");
        return true;
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage("§aACAntiCheat commands:");
        sender.sendMessage(" §7/acac help §f- show this help");
        sender.sendMessage(" §7/acac gui §f- open staff GUI");
        sender.sendMessage(" §7/acac stats <player> §f- quick stats");
        sender.sendMessage(" §7/acac inspect <player> §f- detailed stats + GUI");
        sender.sendMessage(" §7/acac history <player> §f- persisted history");
        sender.sendMessage(" §7/acac perf §f- per-check timings, webhook/persistence status");
        sender.sendMessage(" §7/acac selftest §f- simulate checks/mitigations");
        sender.sendMessage(" §7/acac reload §f- reload config");
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

        org.bukkit.entity.Player safePlayer = new org.bukkit.entity.Player("SelfTest", java.util.UUID.randomUUID());
        int scenarios = 0;
        int passed = 0;

        // Scenario 1: benign
        scenarios++;
        plugin.getCheckManager().handleMovement(safePlayer, 0, 65, 0, false);
        plugin.getCheckManager().handlePacket(new com.yourcompany.uac.packet.PacketPayload(safePlayer, new Object()));
        var benignStats = plugin.getCheckManager().getStatsForPlayer(safePlayer.getUniqueId());
        if (benignStats.flagCounts().isEmpty()) {
            passed++;
            sender.sendMessage(" §aBenign traffic OK (no flags)");
        } else {
            sender.sendMessage(" §cBenign traffic flagged unexpectedly: " + benignStats.flagCounts());
        }

        // Scenario 2: packet burst
        scenarios++;
        for (int i = 0; i < 200; i++) {
            plugin.getCheckManager().handlePacket(new com.yourcompany.uac.packet.PacketPayload(safePlayer, new Object()));
        }
        var burstStats = plugin.getCheckManager().getStatsForPlayer(safePlayer.getUniqueId());
        int burstFlags = burstStats.flagCounts().values().stream().mapToInt(Integer::intValue).sum();
        if (burstFlags > 0) {
            passed++;
            sender.sendMessage(" §aPacket burst flagged (" + burstFlags + ") mitigation=" + burstStats.lastMitigation());
        } else {
            sender.sendMessage(" §cPacket burst not detected");
        }

        // Scenario 3: invalid teleport
        scenarios++;
        plugin.getCheckManager().handleMovement(safePlayer, 0, 65, 0, true);
        plugin.getCheckManager().handleMovement(safePlayer, 10_000, 80, 10_000, false);
        var tpStats = plugin.getCheckManager().getStatsForPlayer(safePlayer.getUniqueId());
        if (tpStats.flagCounts().getOrDefault("InvalidTeleport", 0) > 0) {
            passed++;
            sender.sendMessage(" §aInvalid teleport flagged");
        } else {
            sender.sendMessage(" §cInvalid teleport not detected");
        }

        // Scenario 4: inventory/dupe spike
        scenarios++;
        for (int i = 0; i < 40; i++) {
            plugin.getCheckManager().handleInventoryAction(safePlayer, "click", -1, null);
        }
        var invStats = plugin.getCheckManager().getStatsForPlayer(safePlayer.getUniqueId());
        if (invStats.flagCounts().getOrDefault("InventoryDupeCheck", 0) > 0) {
            passed++;
            sender.sendMessage(" §aInventory spike flagged");
        } else {
            sender.sendMessage(" §cInventory spike not detected");
        }

        sender.sendMessage("§aSelf-test summary: " + passed + "/" + scenarios + " scenarios flagged as expected.");
    }
}
