package com.yourcompany.uac.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view over config.yml with sensible defaults and comments
 * that mirror the detection modules.
 */
public class Settings {
    public boolean packetRateLimiterEnabled;
    public int packetRateLimitPerSecond;
    public int packetRateLimitPerFiveSeconds;
    public int packetRateKickThreshold;
    public int packetRateSeverity;

    public boolean invalidPacketEnabled;
    public double maxCoordinateMagnitude;
    public double maxTeleportDelta;
    public int invalidPacketSeverity;

    public boolean invalidTeleportEnabled;
    public double invalidTeleportMaxDistance;
    public int invalidTeleportSeverity;

    public boolean enableNettyCrashProtection;
    public boolean enableInvalidItemCheck;
    public int invalidItemSeverity;
    public int maxConfiguredStackSize;
    public int maxConfiguredEnchantLevel;

    public boolean inventoryExploitEnabled;
    public int inventoryActionsPerWindow;
    public int inventoryWindowSeconds;
    public int inventoryExploitSeverity;
    public int maxInventorySlotIndex;

    public boolean invalidPlacementEnabled;
    public int placementWindowSeconds;
    public int placementActionsPerWindow;
    public double maxBuildHeight;
    public int invalidPlacementSeverity;

    public boolean entityOverloadEnabled;
    public int entityActionsPerWindow;
    public int entityWindowSeconds;
    public int entityOverloadSeverity;

    public boolean signPayloadEnabled;
    public int maxSignCharacters;
    public int maxPayloadBytes;
    public int signPayloadSeverity;

    public boolean consoleSpamEnabled;
    public int consoleMessagesPerWindow;
    public int consoleWindowSeconds;
    public int consoleSpamSeverity;

    public boolean enableRedstoneMitigation;
    public boolean redstoneEnabled;
    public int maxRedstoneUpdatesPerTick;
    public int redstoneSeverity;

    public boolean disablerEnabled;
    public int disablerHighWaterMark;
    public int disablerSilenceThreshold;
    public int disablerSeverity;

    public double warnThreshold;
    public double temporaryKickThreshold;
    public double banSuggestThreshold;
    public long mitigationCooldownMillis;

    public boolean alertsEnabled;
    public int alertMinSeverity;
    public String notifyPermission;
    public java.util.Map<String, com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel> mitigationModes = new java.util.HashMap<>();
    public int historyLimit;
    public boolean flushOnFlag;
    public String integrationMode;

    public static Settings fromYaml(FileConfiguration cfg) {
        Settings s = new Settings();
        s.packetRateLimiterEnabled = cfg.getBoolean("checks.packet-rate-limit.enabled", true);
        s.packetRateLimitPerSecond = cfg.getInt("checks.packet-rate-limit.max-packets-per-second", 750);
        s.packetRateLimitPerFiveSeconds = cfg.getInt("checks.packet-rate-limit.max-packets-per-5s", 2500);
        s.packetRateKickThreshold = cfg.getInt("checks.packet-rate-limit.kick-threshold-per-second", 1500);
        s.packetRateSeverity = cfg.getInt("checks.packet-rate-limit.severity", 2);

        s.invalidPacketEnabled = cfg.getBoolean("checks.invalid-packet.enabled", true);
        s.maxCoordinateMagnitude = cfg.getDouble("checks.invalid-packet.max-coordinate", 30000000.0);
        s.maxTeleportDelta = cfg.getDouble("checks.invalid-packet.max-teleport-delta", 64.0);
        s.invalidPacketSeverity = cfg.getInt("checks.invalid-packet.severity", 2);

        s.invalidTeleportEnabled = cfg.getBoolean("checks.invalid-teleport.enabled", true);
        s.invalidTeleportMaxDistance = cfg.getDouble("checks.invalid-teleport.max-distance", 256.0);
        s.invalidTeleportSeverity = cfg.getInt("checks.invalid-teleport.severity", 2);

        s.enableNettyCrashProtection = cfg.getBoolean("checks.netty-crash-protection.enabled", true);
        s.enableInvalidItemCheck = cfg.getBoolean("checks.invalid-item.enabled", true);
        s.invalidItemSeverity = cfg.getInt("checks.invalid-item.severity", 2);
        s.maxConfiguredStackSize = cfg.getInt("checks.invalid-item.max-stack-size", 128);
        s.maxConfiguredEnchantLevel = cfg.getInt("checks.invalid-item.max-enchant-level", 10);

        s.inventoryExploitEnabled = cfg.getBoolean("checks.inventory-exploit.enabled", true);
        s.inventoryActionsPerWindow = cfg.getInt("checks.inventory-exploit.max-actions", 25);
        s.inventoryWindowSeconds = cfg.getInt("checks.inventory-exploit.window-seconds", 5);
        s.inventoryExploitSeverity = cfg.getInt("checks.inventory-exploit.severity", 2);
        s.maxInventorySlotIndex = cfg.getInt("checks.inventory-exploit.max-slot-index", 53);

        s.invalidPlacementEnabled = cfg.getBoolean("checks.invalid-placement.enabled", true);
        s.placementWindowSeconds = cfg.getInt("checks.invalid-placement.window-seconds", 5);
        s.placementActionsPerWindow = cfg.getInt("checks.invalid-placement.max-placements", 45);
        s.maxBuildHeight = cfg.getDouble("checks.invalid-placement.max-build-height", 320);
        s.invalidPlacementSeverity = cfg.getInt("checks.invalid-placement.severity", 2);

        s.entityOverloadEnabled = cfg.getBoolean("checks.entity-overload.enabled", true);
        s.entityActionsPerWindow = cfg.getInt("checks.entity-overload.max-actions-per-window", 40);
        s.entityWindowSeconds = cfg.getInt("checks.entity-overload.window-seconds", 5);
        s.entityOverloadSeverity = cfg.getInt("checks.entity-overload.severity", 2);

        s.signPayloadEnabled = cfg.getBoolean("checks.sign-payload.enabled", true);
        s.maxSignCharacters = cfg.getInt("checks.sign-payload.max-sign-characters", 80);
        s.maxPayloadBytes = cfg.getInt("checks.sign-payload.max-bytes", 2048);
        s.signPayloadSeverity = cfg.getInt("checks.sign-payload.severity", 2);

        s.consoleSpamEnabled = cfg.getBoolean("checks.console-spam.enabled", true);
        s.consoleMessagesPerWindow = cfg.getInt("checks.console-spam.max-messages", 20);
        s.consoleWindowSeconds = cfg.getInt("checks.console-spam.window-seconds", 5);
        s.consoleSpamSeverity = cfg.getInt("checks.console-spam.severity", 1);

        s.enableRedstoneMitigation = cfg.getBoolean("checks.redstone-mitigation.enabled", true);
        s.redstoneEnabled = cfg.getBoolean("checks.redstone-exploit.enabled", true);
        s.maxRedstoneUpdatesPerTick = cfg.getInt("checks.redstone-exploit.max-updates-per-tick", 512);
        s.redstoneSeverity = cfg.getInt("checks.redstone-exploit.severity", 3);

        s.disablerEnabled = cfg.getBoolean("checks.disabler.enabled", true);
        s.disablerHighWaterMark = cfg.getInt("checks.disabler.high-water-mark", 400);
        s.disablerSilenceThreshold = cfg.getInt("checks.disabler.silence-threshold", 5);
        s.disablerSeverity = cfg.getInt("checks.disabler.severity", 3);

        s.warnThreshold = cfg.getDouble("mitigation.warn-threshold", 0.3);
        s.temporaryKickThreshold = cfg.getDouble("mitigation.kick-threshold", 0.65);
        s.banSuggestThreshold = cfg.getDouble("mitigation.ban-suggest-threshold", 0.9);
        s.mitigationCooldownMillis = cfg.getLong("mitigation.cooldown-millis", 2000);

        s.alertsEnabled = cfg.getBoolean("alerts.enabled", true);
        s.alertMinSeverity = cfg.getInt("alerts.min-severity", 1);
        s.notifyPermission = cfg.getString("alerts.notify-permission", "acac.alerts");
        s.historyLimit = cfg.getInt("persistence.history-limit", 50);
        s.flushOnFlag = cfg.getBoolean("persistence.flush-on-flag", true);
        s.integrationMode = cfg.getString("integrations.mode", "auto");

        s.mitigationModes.put("PacketRateLimiter", parseMitigation(cfg.getString("checks.packet-rate-limit.action", "auto")));
        s.mitigationModes.put("InvalidPacket", parseMitigation(cfg.getString("checks.invalid-packet.action", "auto")));
        s.mitigationModes.put("InvalidTeleport", parseMitigation(cfg.getString("checks.invalid-teleport.action", "auto")));
        s.mitigationModes.put("InvalidItemCheck", parseMitigation(cfg.getString("checks.invalid-item.action", "soft")));
        s.mitigationModes.put("InventoryDupeCheck", parseMitigation(cfg.getString("checks.inventory-exploit.action", "medium")));
        s.mitigationModes.put("InvalidPlacementCheck", parseMitigation(cfg.getString("checks.invalid-placement.action", "medium")));
        s.mitigationModes.put("EntityOverload", parseMitigation(cfg.getString("checks.entity-overload.action", "medium")));
        s.mitigationModes.put("InvalidSignPayloadCheck", parseMitigation(cfg.getString("checks.sign-payload.action", "soft")));
        s.mitigationModes.put("RedstoneExploitCheck", parseMitigation(cfg.getString("checks.redstone-exploit.action", "soft")));
        s.mitigationModes.put("ConsoleSpam", parseMitigation(cfg.getString("checks.console-spam.action", "log")));
        s.mitigationModes.put("AntiCheatDisablerCheck", parseMitigation(cfg.getString("checks.disabler.action", "hard")));
        return s;
    }

    public com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel getMitigationMode(String checkName) {
        return mitigationModes.getOrDefault(checkName, null);
    }

    private static com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel parseMitigation(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase()) {
            case "log" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.NONE;
            case "soft", "warn" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.SOFT;
            case "medium", "cancel", "kick" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.MEDIUM;
            case "hard", "ban" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.HARD;
            default -> null;
        };
    }
}
