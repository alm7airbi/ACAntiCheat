package com.yourcompany.uac.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view over config.yml with sensible defaults and comments
 * that mirror the detection modules.
 */
public class Settings {
    public int packetRateLimit;
    public boolean enableNettyCrashProtection;
    public boolean enableInvalidItemCheck;
    public boolean enableEntityOverloadCheck;
    public boolean enableRedstoneMitigation;

    public static Settings fromYaml(FileConfiguration cfg) {
        Settings s = new Settings();
        s.packetRateLimit = cfg.getInt("checks.packet-rate-limit", 1000);
        s.enableNettyCrashProtection = cfg.getBoolean("checks.netty-crash-protection", true);
        s.enableInvalidItemCheck = cfg.getBoolean("checks.invalid-item", true);
        s.enableEntityOverloadCheck = cfg.getBoolean("checks.entity-overload", true);
        s.enableRedstoneMitigation = cfg.getBoolean("checks.redstone-mitigation", true);
        return s;
    }
}
