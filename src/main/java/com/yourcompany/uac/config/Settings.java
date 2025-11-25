package com.yourcompany.uac.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Typed view over config.yml with sensible defaults and comments
 * that mirror the detection modules.
 */
public class Settings {
    public static final int CURRENT_CONFIG_VERSION = 2;

    public int configVersion;
    public boolean packetRateLimiterEnabled;
    public int packetRateLimitPerSecond;
    public int packetRateLimitPerFiveSeconds;
    public int packetRateKickThreshold;
    public int packetRateSeverity;

    public boolean invalidPacketEnabled;
    public double maxCoordinateMagnitude;
    public double maxTeleportDelta;
    public int invalidPacketSeverity;
    public int invalidPacketLagPingThreshold;
    public double invalidPacketLagTpsFloor;
    public double maxHorizontalSpeed;
    public double maxVerticalSpeed;

    public boolean invalidTeleportEnabled;
    public double invalidTeleportMaxDistance;
    public int invalidTeleportSeverity;
    public int invalidTeleportLagBuffer;
    public long teleportExemptMillis;

    public boolean enableNettyCrashProtection;
    public int nettyCrashMaxBytes;
    public int nettyCrashSeverity;
    public boolean nettyMitigateOversized;
    public boolean enableInvalidItemCheck;
    public int invalidItemSeverity;
    public int maxConfiguredStackSize;
    public int maxConfiguredEnchantLevel;
    public int maxDisplayNameLength;
    public int maxLoreLength;

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

    public boolean chunkCrashEnabled;
    public int chunkWindowSeconds;
    public int maxChunkChanges;
    public int chunkCrashSeverity;
    public double chunkCrashTpsFloor;

    public boolean commandAbuseEnabled;
    public int commandWindowSeconds;
    public int maxCommandsPerWindow;
    public int commandAbuseSeverity;

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
    public double rollbackThreshold;
    public double throttleThreshold;
    public double rubberBandThreshold;
    public double temporaryKickThreshold;
    public double temporaryBanThreshold;
    public double banSuggestThreshold;
    public long mitigationCooldownMillis;
    public double mitigationSensitivity;
    public int minViolationsBeforeMitigation;
    public int temporaryBanMinutes;
    public String kickMessage;
    public String banMessage;
    public boolean preferExternalPunishments;
    public String externalPunishmentCommand;
    public boolean auditMitigations;
    public boolean rollbackForceChunkLoad;
    public int inventorySnapshotMaxAgeSeconds;
    public long inactivePurgeMillis;
    public double perfCheckBudgetMs;
    public double perfTotalBudgetMs;
    public int persistenceSchemaVersion;
    public int persistenceCacheMaxEntries;
    public long persistenceFlushIntervalTicks;
    public int mongoConnectTimeoutMs;
    public int mongoSocketTimeoutMs;
    public int mongoMaxRetries;
    public long mongoRetryDelayMillis;
    public boolean useSqlDatabase;
    public String sqlUrl;
    public String sqlUsername;
    public String sqlPassword;
    public int sqlMaxRetries;
    public long sqlRetryDelayMillis;
    public int sqlMaxPoolSize;
    public int sqlLoginTimeoutSeconds;

    public boolean alertsEnabled;
    public int alertMinSeverity;
    public String notifyPermission;
    public String alertPermission;
    public boolean alertConsole;
    public boolean alertStaff;
    public boolean alertDiscord;
    public boolean alertStructuredLogging;
    public String alertStructuredDirectory;
    public String alertStructuredFileName;
    public long alertStructuredMaxBytes;
    public int alertStructuredMaxFiles;
    public int alertStructuredRetentionDays;
    public int alertThrottleSeconds;
    public int alertDiscordThrottleSeconds;
    public int alertDiscordMinSeverity;
    public int alertDiscordMaxRetries;
    public long alertDiscordBackoffMillis;
    public boolean alertDiscordDetailedPayload;
    public String discordWebhookUrl;
    public String alertChannelName;
    public boolean experimentsEnabled;
    public String experimentsLogFile;
    public double experimentsSampleRateDetection;
    public double experimentsSampleRateMitigation;
    public boolean experimentsIncludePlayerName;
    public boolean experimentsIncludeLocation;
    public long experimentsMaxFileSizeMb;
    public java.util.Map<String, com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel> mitigationModes = new java.util.HashMap<>();
    public int historyLimit;
    public boolean flushOnFlag;
    public long logMaxBytes;
    public int logMaxFiles;
    public String integrationMode;
    public boolean useDatabase;
    public String mongoUri;
    public String mongoUsername;
    public String mongoPassword;

    public static Settings fromYaml(FileConfiguration cfg) {
        Settings s = new Settings();
        s.configVersion = cfg.getInt("config-version", CURRENT_CONFIG_VERSION);
        s.packetRateLimiterEnabled = cfg.getBoolean("checks.packet-rate-limit.enabled", true);
        s.packetRateLimitPerSecond = cfg.getInt("checks.packet-rate-limit.max-packets-per-second", 750);
        s.packetRateLimitPerFiveSeconds = cfg.getInt("checks.packet-rate-limit.max-packets-per-5s", 2500);
        s.packetRateKickThreshold = cfg.getInt("checks.packet-rate-limit.kick-threshold-per-second", 1500);
        s.packetRateSeverity = cfg.getInt("checks.packet-rate-limit.severity", 2);

        s.invalidPacketEnabled = cfg.getBoolean("checks.invalid-packet.enabled", true);
        s.maxCoordinateMagnitude = cfg.getDouble("checks.invalid-packet.max-coordinate", 30000000.0);
        s.maxTeleportDelta = cfg.getDouble("checks.invalid-packet.max-teleport-delta", 64.0);
        s.invalidPacketSeverity = cfg.getInt("checks.invalid-packet.severity", 2);
        s.invalidPacketLagPingThreshold = cfg.getInt("checks.invalid-packet.lag-ping-threshold", 250);
        s.invalidPacketLagTpsFloor = cfg.getDouble("checks.invalid-packet.lag-tps-threshold", 18.0);
        s.maxHorizontalSpeed = cfg.getDouble("checks.invalid-packet.max-horizontal-speed", 40.0);
        s.maxVerticalSpeed = cfg.getDouble("checks.invalid-packet.max-vertical-speed", 30.0);

        s.invalidTeleportEnabled = cfg.getBoolean("checks.invalid-teleport.enabled", true);
        s.invalidTeleportMaxDistance = cfg.getDouble("checks.invalid-teleport.max-distance", 256.0);
        s.invalidTeleportSeverity = cfg.getInt("checks.invalid-teleport.severity", 2);
        s.invalidTeleportLagBuffer = cfg.getInt("checks.invalid-teleport.lag-distance-buffer", 16);
        s.teleportExemptMillis = cfg.getLong("checks.invalid-teleport.teleport-exempt-ms", 500);

        s.enableNettyCrashProtection = cfg.getBoolean("checks.netty-crash-protection.enabled", true);
        s.nettyCrashMaxBytes = cfg.getInt("checks.netty-crash-protection.max-bytes", 65536);
        s.nettyMitigateOversized = cfg.getBoolean("checks.netty-crash-protection.mitigate-oversized-payloads", true);
        s.nettyCrashSeverity = cfg.getInt("checks.netty-crash-protection.severity", 3);
        s.enableInvalidItemCheck = cfg.getBoolean("checks.invalid-item.enabled", true);
        s.invalidItemSeverity = cfg.getInt("checks.invalid-item.severity", 2);
        s.maxConfiguredStackSize = cfg.getInt("checks.invalid-item.max-stack-size", 128);
        s.maxConfiguredEnchantLevel = cfg.getInt("checks.invalid-item.max-enchant-level", 10);
        s.maxDisplayNameLength = cfg.getInt("checks.invalid-item.max-display-name-length", 80);
        s.maxLoreLength = cfg.getInt("checks.invalid-item.max-lore-length", 256);

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

        s.chunkCrashEnabled = cfg.getBoolean("checks.chunk-crash.enabled", true);
        s.chunkWindowSeconds = cfg.getInt("checks.chunk-crash.window-seconds", 5);
        s.maxChunkChanges = cfg.getInt("checks.chunk-crash.max-chunk-changes", 40);
        s.chunkCrashSeverity = cfg.getInt("checks.chunk-crash.severity", 3);
        s.chunkCrashTpsFloor = cfg.getDouble("checks.chunk-crash.tps-floor", 17.0);

        s.commandAbuseEnabled = cfg.getBoolean("checks.command-abuse.enabled", true);
        s.commandWindowSeconds = cfg.getInt("checks.command-abuse.window-seconds", 4);
        s.maxCommandsPerWindow = cfg.getInt("checks.command-abuse.max-commands", 15);
        s.commandAbuseSeverity = cfg.getInt("checks.command-abuse.severity", 2);

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

        s.warnThreshold = cfg.getDouble("mitigation.warn-threshold", 0.25);
        s.rollbackThreshold = cfg.getDouble("mitigation.rollback-threshold", 0.35);
        s.throttleThreshold = cfg.getDouble("mitigation.throttle-threshold", 0.45);
        s.rubberBandThreshold = cfg.getDouble("mitigation.rubberband-threshold", 0.55);
        s.temporaryKickThreshold = cfg.getDouble("mitigation.kick-threshold", 0.7);
        s.temporaryBanThreshold = cfg.getDouble("mitigation.temp-ban-threshold", 0.82);
        s.banSuggestThreshold = cfg.getDouble("mitigation.ban-suggest-threshold", 0.92);
        s.mitigationCooldownMillis = cfg.getLong("mitigation.cooldown-millis", 2000);
        s.mitigationSensitivity = cfg.getDouble("mitigation.sensitivity", 1.0);
        s.minViolationsBeforeMitigation = cfg.getInt("mitigation.min-violations-before-mitigating", 2);
        s.temporaryBanMinutes = cfg.getInt("mitigation.temp-ban-minutes", 30);
        s.kickMessage = cfg.getString("mitigation.kick-message", "ACAntiCheat: Behaviour blocked");
        s.banMessage = cfg.getString("mitigation.ban-message", "ACAntiCheat: Banned");
        s.preferExternalPunishments = cfg.getBoolean("mitigation.prefer-external-punishments", true);
        s.externalPunishmentCommand = cfg.getString("mitigation.external-punishment-command", "");
        s.auditMitigations = cfg.getBoolean("mitigation.audit-log", true);
        s.rollbackForceChunkLoad = cfg.getBoolean("mitigation.rollback.force-chunk-load", true);
        s.inventorySnapshotMaxAgeSeconds = cfg.getInt("mitigation.rollback.inventory-snapshot-max-age-seconds", 20);
        s.inactivePurgeMillis = cfg.getLong("state.inactive-purge-millis", 900000);
        s.perfCheckBudgetMs = cfg.getDouble("performance.check-budget-ms", 5.0);
        s.perfTotalBudgetMs = cfg.getDouble("performance.total-budget-ms", 20.0);

        s.alertsEnabled = cfg.getBoolean("alerts.enabled", true);
        s.alertMinSeverity = cfg.getInt("alerts.min-severity", 1);
        s.notifyPermission = cfg.getString("alerts.notify-permission", "acac.alerts");
        s.alertPermission = cfg.getString("alerts.staff-permission", "acac.alerts");
        s.alertConsole = cfg.getBoolean("alerts.channels.console", true);
        s.alertStaff = cfg.getBoolean("alerts.channels.staff", true);
        s.alertDiscord = cfg.getBoolean("alerts.channels.discord", false);
        s.alertStructuredLogging = cfg.getBoolean("alerts.logging.structured", false);
        s.alertStructuredDirectory = cfg.getString("alerts.logging.directory", "logs");
        s.alertStructuredFileName = cfg.getString("alerts.logging.file-name", "alerts.jsonl");
        long legacyMaxBytes = cfg.getLong("persistence.log-max-bytes", 1024 * 1024 * 2L);
        int legacyMaxFiles = cfg.getInt("persistence.log-max-files", 5);
        s.alertStructuredMaxBytes = cfg.getLong("alerts.logging.max-bytes", legacyMaxBytes);
        s.alertStructuredMaxFiles = cfg.getInt("alerts.logging.max-files", legacyMaxFiles);
        s.alertStructuredRetentionDays = cfg.getInt("alerts.logging.retention-days", 7);
        s.alertThrottleSeconds = cfg.getInt("alerts.throttle-seconds", 3);
        s.alertDiscordThrottleSeconds = cfg.getInt("alerts.discord-throttle-seconds", 10);
        s.alertDiscordMinSeverity = cfg.getInt("alerts.discord-min-severity", 2);
        s.alertDiscordMaxRetries = cfg.getInt("alerts.discord-max-retries", 3);
        s.alertDiscordBackoffMillis = cfg.getLong("alerts.discord-retry-backoff-millis", 1_000);
        s.alertDiscordDetailedPayload = cfg.getBoolean("alerts.discord-detailed-payload", true);
        s.discordWebhookUrl = cfg.getString("alerts.discord-webhook", "");
        s.alertChannelName = cfg.getString("alerts.channel-name", "ACAntiCheat");
        s.experimentsEnabled = cfg.getBoolean("experiments.enabled", true);
        s.experimentsLogFile = cfg.getString("experiments.log-file", "logs/acac-experiments.jsonl");
        s.experimentsSampleRateDetection = cfg.getDouble("experiments.sample-rate.detection-events", 1.0);
        s.experimentsSampleRateMitigation = cfg.getDouble("experiments.sample-rate.mitigation-events", 1.0);
        s.experimentsIncludePlayerName = cfg.getBoolean("experiments.include.player-name", true);
        s.experimentsIncludeLocation = cfg.getBoolean("experiments.include.location", true);
        s.experimentsMaxFileSizeMb = cfg.getLong("experiments.max-file-size-mb", 25);
        s.historyLimit = cfg.getInt("persistence.history-limit", 50);
        s.flushOnFlag = cfg.getBoolean("persistence.flush-on-flag", true);
        s.persistenceSchemaVersion = cfg.getInt("persistence.schema-version", 1);
        s.persistenceCacheMaxEntries = cfg.getInt("persistence.cache.max-entries", 512);
        s.persistenceFlushIntervalTicks = cfg.getLong("persistence.cache.flush-interval-ticks", 40);
        s.logMaxBytes = cfg.getLong("persistence.log-max-bytes", 1024 * 1024 * 2L);
        s.logMaxFiles = cfg.getInt("persistence.log-max-files", 5);
        s.integrationMode = cfg.getString("integrations.mode", "auto");
        s.useDatabase = cfg.getBoolean("storage.use-database", false);
        s.mongoUri = cfg.getString("storage.mongo-uri", "mongodb://localhost:27017/uac");
        s.mongoUsername = cfg.getString("storage.mongo-username", "");
        s.mongoPassword = cfg.getString("storage.mongo-password", "");
        s.mongoConnectTimeoutMs = cfg.getInt("storage.mongo-connect-timeout-ms", 5000);
        s.mongoSocketTimeoutMs = cfg.getInt("storage.mongo-socket-timeout-ms", 5000);
        s.mongoMaxRetries = cfg.getInt("storage.mongo-max-retries", 3);
        s.mongoRetryDelayMillis = cfg.getLong("storage.mongo-retry-delay-millis", 500);
        s.useSqlDatabase = cfg.getBoolean("storage.use-sql-database", false);
        s.sqlUrl = cfg.getString("storage.sql.url", "jdbc:postgresql://localhost:5432/acac");
        s.sqlUsername = cfg.getString("storage.sql.username", "");
        s.sqlPassword = cfg.getString("storage.sql.password", "");
        s.sqlMaxRetries = cfg.getInt("storage.sql.max-retries", 3);
        s.sqlRetryDelayMillis = cfg.getLong("storage.sql.retry-delay-millis", 500);
        s.sqlMaxPoolSize = cfg.getInt("storage.sql.max-pool-size", 4);
        s.sqlLoginTimeoutSeconds = cfg.getInt("storage.sql.login-timeout-seconds", 5);

        s.mitigationModes.put("PacketRateLimiter", parseMitigation(cfg.getString("checks.packet-rate-limit.action", "auto")));
        s.mitigationModes.put("InvalidPacket", parseMitigation(cfg.getString("checks.invalid-packet.action", "auto")));
        s.mitigationModes.put("InvalidTeleport", parseMitigation(cfg.getString("checks.invalid-teleport.action", "auto")));
        s.mitigationModes.put("InvalidItemCheck", parseMitigation(cfg.getString("checks.invalid-item.action", "soft")));
        s.mitigationModes.put("InventoryDupeCheck", parseMitigation(cfg.getString("checks.inventory-exploit.action", "medium")));
        s.mitigationModes.put("InvalidPlacementCheck", parseMitigation(cfg.getString("checks.invalid-placement.action", "medium")));
        s.mitigationModes.put("EntityOverload", parseMitigation(cfg.getString("checks.entity-overload.action", "medium")));
        s.mitigationModes.put("ChunkCrashCheck", parseMitigation(cfg.getString("checks.chunk-crash.action", "hard")));
        s.mitigationModes.put("InvalidSignPayloadCheck", parseMitigation(cfg.getString("checks.sign-payload.action", "soft")));
        s.mitigationModes.put("RedstoneExploitCheck", parseMitigation(cfg.getString("checks.redstone-exploit.action", "soft")));
        s.mitigationModes.put("ConsoleSpam", parseMitigation(cfg.getString("checks.console-spam.action", "log")));
        s.mitigationModes.put("AntiCheatDisablerCheck", parseMitigation(cfg.getString("checks.disabler.action", "hard")));
        s.mitigationModes.put("CommandAbuseCheck", parseMitigation(cfg.getString("checks.command-abuse.action", "medium")));
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
            case "soft", "warn" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.WARN;
            case "rollback" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.ROLLBACK;
            case "throttle" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.THROTTLE;
            case "rubberband", "rubber-band" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.RUBBERBAND;
            case "medium", "cancel", "kick" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.KICK;
            case "tempban", "temporary-ban", "ban" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.TEMP_BAN;
            case "permban", "hard" -> com.yourcompany.uac.checks.PlayerCheckState.MitigationLevel.PERM_BAN;
            default -> null;
        };
    }
}
