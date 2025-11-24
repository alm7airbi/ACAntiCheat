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

    public boolean invalidPacketEnabled;
    public double maxCoordinateMagnitude;
    public double maxTeleportDelta;

    public boolean invalidTeleportEnabled;
    public double invalidTeleportMaxDistance;

    public boolean enableNettyCrashProtection;
    public boolean enableInvalidItemCheck;
    public int invalidItemSeverity;
    public int maxConfiguredStackSize;

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

    public boolean signPayloadEnabled;
    public int maxSignCharacters;
    public int maxPayloadBytes;
    public int signPayloadSeverity;

    public boolean consoleSpamEnabled;
    public int consoleMessagesPerWindow;
    public int consoleWindowSeconds;

    public boolean enableRedstoneMitigation;
    public boolean redstoneEnabled;
    public int maxRedstoneUpdatesPerTick;
    public int redstoneSeverity;

    public boolean disablerEnabled;
    public int disablerHighWaterMark;
    public int disablerSilenceThreshold;
    public int disablerSeverity;

    public static Settings fromYaml(FileConfiguration cfg) {
        Settings s = new Settings();
        s.packetRateLimiterEnabled = cfg.getBoolean("checks.packet-rate-limit.enabled", true);
        s.packetRateLimitPerSecond = cfg.getInt("checks.packet-rate-limit.max-packets-per-second", 750);
        s.packetRateLimitPerFiveSeconds = cfg.getInt("checks.packet-rate-limit.max-packets-per-5s", 2500);
        s.packetRateKickThreshold = cfg.getInt("checks.packet-rate-limit.kick-threshold-per-second", 1500);

        s.invalidPacketEnabled = cfg.getBoolean("checks.invalid-packet.enabled", true);
        s.maxCoordinateMagnitude = cfg.getDouble("checks.invalid-packet.max-coordinate", 30000000.0);
        s.maxTeleportDelta = cfg.getDouble("checks.invalid-packet.max-teleport-delta", 64.0);

        s.invalidTeleportEnabled = cfg.getBoolean("checks.invalid-teleport.enabled", true);
        s.invalidTeleportMaxDistance = cfg.getDouble("checks.invalid-teleport.max-distance", 256.0);

        s.enableNettyCrashProtection = cfg.getBoolean("checks.netty-crash-protection.enabled", true);
        s.enableInvalidItemCheck = cfg.getBoolean("checks.invalid-item.enabled", true);
        s.invalidItemSeverity = cfg.getInt("checks.invalid-item.severity", 2);
        s.maxConfiguredStackSize = cfg.getInt("checks.invalid-item.max-stack-size", 128);

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

        s.signPayloadEnabled = cfg.getBoolean("checks.sign-payload.enabled", true);
        s.maxSignCharacters = cfg.getInt("checks.sign-payload.max-sign-characters", 80);
        s.maxPayloadBytes = cfg.getInt("checks.sign-payload.max-bytes", 2048);
        s.signPayloadSeverity = cfg.getInt("checks.sign-payload.severity", 2);

        s.consoleSpamEnabled = cfg.getBoolean("checks.console-spam.enabled", true);
        s.consoleMessagesPerWindow = cfg.getInt("checks.console-spam.max-messages", 20);
        s.consoleWindowSeconds = cfg.getInt("checks.console-spam.window-seconds", 5);

        s.enableRedstoneMitigation = cfg.getBoolean("checks.redstone-mitigation.enabled", true);
        s.redstoneEnabled = cfg.getBoolean("checks.redstone-exploit.enabled", true);
        s.maxRedstoneUpdatesPerTick = cfg.getInt("checks.redstone-exploit.max-updates-per-tick", 512);
        s.redstoneSeverity = cfg.getInt("checks.redstone-exploit.severity", 3);

        s.disablerEnabled = cfg.getBoolean("checks.disabler.enabled", true);
        s.disablerHighWaterMark = cfg.getInt("checks.disabler.high-water-mark", 400);
        s.disablerSilenceThreshold = cfg.getInt("checks.disabler.silence-threshold", 5);
        s.disablerSeverity = cfg.getInt("checks.disabler.severity", 3);
        return s;
    }
}
