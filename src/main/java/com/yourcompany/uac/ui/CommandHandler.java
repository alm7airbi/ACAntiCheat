package com.yourcompany.uac.ui;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.checks.EnvironmentResolver;
import com.yourcompany.uac.checks.context.EnvironmentSnapshot;
import com.yourcompany.uac.config.Settings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Command dispatcher backing the documented /acac subcommands and GUIs.
 */
public class CommandHandler implements CommandExecutor {

    private static final DecimalFormat TWO_DECIMALS = new DecimalFormat("0.00");

    private final UltimateAntiCheatPlugin plugin;
    private final CheckManager checkManager;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private boolean debugMode = false;

    public CommandHandler(UltimateAntiCheatPlugin plugin, CheckManager checkManager) {
        this.plugin = plugin;
        this.checkManager = checkManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("acac.use")) {
            sender.sendMessage("§cYou do not have permission to use ACAC commands.");
            return true;
        }
        if (args.length == 0) {
            return help(sender);
        }

        if ("help".equalsIgnoreCase(args[0])) {
            return help(sender);
        }

        if ("gui".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cGUI can only be used by players.");
                return true;
            }
            plugin.getGuiManager().openMainGui(sender);
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            if (!requireAdmin(sender) || isCoolingDown(sender, "reload", 5_000)) {
                return true;
            }
            plugin.getConfigManager().load();
            plugin.getAlertManager().refresh(plugin.getConfigManager().getSettings());
            plugin.getDatabaseManager().refresh(plugin.getConfigManager().getSettings());
            sender.sendMessage("§aACAC config reloaded.");
            return true;
        }

        if ("stats".equalsIgnoreCase(args[0])) {
            return handleStats(sender, args);
        }

        if ("config".equalsIgnoreCase(args[0])) {
            if (!requireAdmin(sender) || isCoolingDown(sender, "config", 2_000)) {
                return true;
            }
            return handleConfig(sender);
        }

        if ("inspect".equalsIgnoreCase(args[0])) {
            return handleInspect(sender, args);
        }

        if ("history".equalsIgnoreCase(args[0])) {
            return handleHistory(sender, args);
        }

        if ("debug".equalsIgnoreCase(args[0])) {
            if (!requireAdmin(sender)) {
                return true;
            }
            debugMode = !debugMode;
            sender.sendMessage("Debug mode " + (debugMode ? "enabled" : "disabled"));
            return true;
        }

        if ("perf".equalsIgnoreCase(args[0])) {
            if (!requireAdmin(sender)) {
                return true;
            }
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
            sender.sendMessage(" §7Alerts: §f" + describeAlertStatus());
            var cfg = plugin.getConfigManager();
            sender.sendMessage(" §7Config: §f v" + cfg.getLoadedVersion() + " §7(expected "
                    + Settings.CURRENT_CONFIG_VERSION + ") " + (cfg.isValid() ? "§aOK" : "§cissues"));
            sender.sendMessage(" §7Persistence: §f" + plugin.getDatabaseManager().getPersistenceStatus()
                    + " §7(schema v" + plugin.getDatabaseManager().getSchemaVersion()
                    + ", cache " + plugin.getDatabaseManager().getCacheSize()
                    + ", queued " + plugin.getDatabaseManager().getPendingWriteCount()
                    + (plugin.getDatabaseManager().isMigrationRan() ? ", migrated " + plugin.getDatabaseManager().getMigratedCount() : "")
                    + ")");
            sender.sendMessage(" §7Experiments: §f" + describeExperiments());
            return true;
        }

        if ("storage".equalsIgnoreCase(args[0])) {
            if (!requireAdmin(sender) || isCoolingDown(sender, "storage", 3_000)) {
                return true;
            }
            return handleStorage(sender);
        }

        if ("selftest".equalsIgnoreCase(args[0])) {
            if (!requireAdmin(sender) || isCoolingDown(sender, "selftest", 30_000)) {
                return true;
            }
            runSelfTest(sender);
            return true;
        }

        sender.sendMessage("Unknown subcommand. Use stats/gui/reload/inspect/history/perf/storage/config/selftest/debug.");
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
        sender.sendMessage(" §7/acac config §f- config version/validation status");
        sender.sendMessage(" §7/acac storage §f- persistence backend and cache diagnostics");
        sender.sendMessage(" §7/acac selftest §f- simulate checks/mitigations");
        sender.sendMessage(" §7/acac debug §f- toggle verbose debugging (admin only)");
        sender.sendMessage(" §7/acac reload §f- reload config");
        return true;
    }

    private boolean handleConfig(CommandSender sender) {
        var config = plugin.getConfigManager();
        sender.sendMessage("§aACAC config status:");
        sender.sendMessage(" §7Version: §f" + config.getLoadedVersion() + " §7(expected "
                + Settings.CURRENT_CONFIG_VERSION + ")" + (config.isMigrationRan()
                ? " §7(migrated from v" + config.getMigratedFrom() + ")" : ""));
        sender.sendMessage(" §7Last load: §f" + formatTimestamp(config.getLastLoadedAt())
                + (config.isRegeneratedDefaults() ? " §7(defaults regenerated)" : ""));
        if (config.isValid()) {
            sender.sendMessage(" §7Validation: §aOK");
        } else {
            sender.sendMessage(" §7Validation: §c" + config.getValidationErrors().size() + " issue(s)");
            config.getValidationErrors().forEach(err -> sender.sendMessage("   §8- §f" + err));
        }
        sender.sendMessage(" §7Experiments: §f" + describeExperiments());
        return true;
    }

    private boolean handleStorage(CommandSender sender) {
        var db = plugin.getDatabaseManager();
        sender.sendMessage("§aACAC storage diagnostics:");
        sender.sendMessage(" §7Backend: §f" + db.getPersistenceStatus());
        sender.sendMessage(" §7Schema: §f" + db.getSchemaVersion() + (db.isMigrationRan() ? " §7(migrated " + db.getMigratedCount() + ")" : ""));
        sender.sendMessage(" §7Cache: §f" + db.getCacheSize() + " entries, queued writes: " + db.getPendingWriteCount());
        sender.sendMessage(" §7Last migration: §f" + formatTimestamp(db.getLastMigrationAt()));
        sender.sendMessage(" §7Last DB error: §f" + formatTimestamp(db.getLastErrorAt()) + (db.getLastErrorMessage().isEmpty() ? "" : " §7(" + db.getLastErrorMessage() + ")"));
        sender.sendMessage(" §7Alerts: §f" + describeAlertStatus());
        return true;
    }

    private String describeAlertStatus() {
        var alert = plugin.getAlertManager();
        String structured = alert.isStructuredLoggingEnabled()
                ? "structured on → " + alert.getStructuredLogPath().getFileName() + " (" + alert.getStructuredMaxFiles() + "x" + humanBytes(alert.getStructuredMaxBytes()) + ", " + alert.getStructuredRetentionDays() + "d)"
                : "structured off";
        String webhook = "webhook=" + alert.getLastWebhookStatus();
        if (alert.getLastWebhookErrorAt() > 0) {
            webhook += " last error " + formatTimestamp(alert.getLastWebhookErrorAt());
        }
        if (alert.getLastStructuredErrorAt() > 0) {
            structured += " last error " + formatTimestamp(alert.getLastStructuredErrorAt());
        }
        return structured + "; " + webhook;
    }

    private String describeExperiments() {
        var exp = plugin.getExperimentLogger();
        if (exp == null || !exp.isEnabled()) {
            return "disabled";
        }
        String path = exp.getLogPath() == null ? "unknown" : exp.getLogPath().getFileName().toString();
        String status = "enabled → " + path;
        if (exp.getLastErrorAt() > 0) {
            status += " last error " + formatTimestamp(exp.getLastErrorAt());
            if (exp.getLastErrorMessage() != null && !exp.getLastErrorMessage().isEmpty()) {
                status += " (" + exp.getLastErrorMessage() + ")";
            }
        }
        return status;
    }

    private String humanBytes(long bytes) {
        if (bytes <= 0) {
            return "0B";
        }
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        return (bytes / (1024 * 1024)) + "MB";
    }

    private String formatTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return "none";
        }
        return DateTimeFormatter.ofPattern("HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(epochMillis));
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("acac.admin")) {
            sender.sendMessage("§cYou need acac.admin to do that.");
            return false;
        }
        return true;
    }

    private boolean isCoolingDown(CommandSender sender, String key, long cooldownMillis) {
        String composite = sender.getName() + ":" + key;
        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(composite, 0L);
        if (now < next) {
            sender.sendMessage("§cPlease wait " + ((next - now) / 1000.0) + "s before using this again.");
            return true;
        }
        cooldowns.put(composite, now + cooldownMillis);
        return false;
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

        EnvironmentSnapshot env = sender instanceof Player self ? EnvironmentResolver.capture(plugin, self) : null;

        sender.sendMessage("§aACAntiCheat §fstats for §b" + targetName);
        sender.sendMessage(" §7Trust: §f" + TWO_DECIMALS.format(stats.trustScore()) + "§7/100" +
                (stats.underMitigation() ? " §c(under mitigation: " + stats.lastMitigation() + ")" : ""));
        sender.sendMessage(" §7Avg packets/sec (last 5s): §f" + TWO_DECIMALS.format(stats.packetsPerSecond()));
        if (env != null) {
            sender.sendMessage(" §7Your ping/TPS: §f" + env.ping() + "ms / " + TWO_DECIMALS.format(env.serverTps()));
        }
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

        sender.sendMessage(" §7Storage: §f" + plugin.getDatabaseManager().getPersistenceStatus());
        sender.sendMessage(" §7Webhook: §f" + plugin.getAlertManager().getLastWebhookStatus());

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
        sender.sendMessage(" §7Storage: §f" + plugin.getDatabaseManager().getPersistenceStatus());
        sender.sendMessage(" §7Integration: §f" + plugin.getIntegrationService().name());
        sender.sendMessage(" §7Webhook: §f" + plugin.getAlertManager().getLastWebhookStatus());
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
        if (plugin.getExperimentLogger() != null && plugin.getExperimentLogger().isEnabled()) {
            String result = passed == scenarios ? "PASS" : "FAIL";
            String notes = "selftest=" + passed + "/" + scenarios;
            plugin.getExperimentLogger().logSelfTest("default", result, notes);
        }
    }
}
