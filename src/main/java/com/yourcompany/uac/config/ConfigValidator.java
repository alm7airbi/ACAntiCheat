package com.yourcompany.uac.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Performs lightweight validation of loaded settings to surface obvious
 * misconfigurations to administrators.
 */
public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static List<String> validate(Settings settings) {
        List<String> errors = new ArrayList<>();

        if (settings.packetRateLimitPerSecond <= 0 || settings.packetRateLimitPerFiveSeconds <= 0) {
            errors.add("Packet-rate limits must be positive.");
        }
        if (settings.maxCoordinateMagnitude <= 0 || settings.maxTeleportDelta <= 0) {
            errors.add("Invalid packet coordinate/teleport bounds must be positive.");
        }
        if (settings.warnThreshold < 0 || settings.warnThreshold > 1
                || settings.rollbackThreshold < 0 || settings.rollbackThreshold > 1
                || settings.throttleThreshold < 0 || settings.rubberBandThreshold < 0) {
            errors.add("Mitigation thresholds must be between 0 and 1.");
        }
        if (settings.alertStructuredMaxBytes <= 0 || settings.alertStructuredMaxFiles <= 0) {
            errors.add("Alert log rotation settings must be positive.");
        }
        if (settings.persistenceCacheMaxEntries < 0) {
            errors.add("Persistence cache entries cannot be negative.");
        }
        if (settings.persistenceFlushIntervalTicks < 0) {
            errors.add("Persistence flush interval must be zero (disabled) or positive.");
        }
        if (settings.mongoMaxRetries < 1 || settings.sqlMaxRetries < 1) {
            errors.add("Database retry counts must be at least 1.");
        }
        if (settings.mitigationCooldownMillis < 0) {
            errors.add("Mitigation cooldown must be non-negative.");
        }
        if (settings.alertMinSeverity < 0 || settings.alertDiscordMinSeverity < 0) {
            errors.add("Alert severities must be non-negative.");
        }
        if (settings.experimentsSampleRateDetection < 0 || settings.experimentsSampleRateDetection > 1
                || settings.experimentsSampleRateMitigation < 0 || settings.experimentsSampleRateMitigation > 1) {
            errors.add("Experiment sample rates must be between 0 and 1.");
        }
        if (settings.experimentsMaxFileSizeMb <= 0) {
            errors.add("Experiment log max-file-size-mb must be positive.");
        }
        if (settings.configVersion < Settings.CURRENT_CONFIG_VERSION) {
            errors.add("Config version " + settings.configVersion + " is older than supported "
                    + Settings.CURRENT_CONFIG_VERSION + ".");
        }

        return errors;
    }
}
