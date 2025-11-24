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
    public boolean entityOverloadEnabled;
    public int entityActionsPerWindow;
    public int entityWindowSeconds;

    public boolean consoleSpamEnabled;
    public int consoleMessagesPerWindow;
    public int consoleWindowSeconds;

    public boolean enableRedstoneMitigation;

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
        s.entityOverloadEnabled = cfg.getBoolean("checks.entity-overload.enabled", true);
        s.entityActionsPerWindow = cfg.getInt("checks.entity-overload.max-actions-per-window", 40);
        s.entityWindowSeconds = cfg.getInt("checks.entity-overload.window-seconds", 5);

        s.consoleSpamEnabled = cfg.getBoolean("checks.console-spam.enabled", true);
        s.consoleMessagesPerWindow = cfg.getInt("checks.console-spam.max-messages", 20);
        s.consoleWindowSeconds = cfg.getInt("checks.console-spam.window-seconds", 5);

        s.enableRedstoneMitigation = cfg.getBoolean("checks.redstone-mitigation.enabled", true);
        return s;
    }
}
