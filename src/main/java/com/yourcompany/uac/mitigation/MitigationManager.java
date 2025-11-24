package com.yourcompany.uac.mitigation;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.config.Settings;
import com.yourcompany.uac.integration.bridge.MitigationActions;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

/**
 * Applies configured mitigation thresholds and delegates to the active
 * MitigationActions bridge (stub vs. real Paper) so we can enforce or log
 * actions without coupling checks to server APIs.
 */
public class MitigationManager {

    private static final DecimalFormat THREE_DECIMALS = new DecimalFormat("0.000");

    private final UltimateAntiCheatPlugin plugin;
    private final MitigationActions mitigationActions;
    private volatile boolean logOnly = false;

    public MitigationManager(UltimateAntiCheatPlugin plugin, MitigationActions mitigationActions) {
        this.plugin = plugin;
        this.mitigationActions = mitigationActions;
    }

    public MitigationResult evaluate(Player player, String checkName, String reason, int severity, PlayerCheckState state, long now) {
        Settings settings = plugin.getConfigManager().getSettings();
        double trust = state.getTrustScore();
        int flags = state.getFlagCounts().getOrDefault(checkName, 0);
        double riskScore = clamp(((100 - trust) / 100.0) + (severity * 0.15) + (flags * 0.1));

        PlayerCheckState.MitigationLevel level = PlayerCheckState.MitigationLevel.NONE;
        if (riskScore >= settings.banSuggestThreshold) {
            level = PlayerCheckState.MitigationLevel.HARD;
        } else if (riskScore >= settings.temporaryKickThreshold) {
            level = PlayerCheckState.MitigationLevel.MEDIUM;
        } else if (riskScore >= settings.warnThreshold) {
            level = PlayerCheckState.MitigationLevel.SOFT;
        }

        PlayerCheckState.MitigationLevel configured = settings.getMitigationMode(checkName);
        if (configured != null && configured.ordinal() > level.ordinal()) {
            level = configured;
        }

        if (logOnly && level.ordinal() > PlayerCheckState.MitigationLevel.SOFT.ordinal()) {
            level = PlayerCheckState.MitigationLevel.SOFT;
        }

        // Mitigation cooldown to avoid spamming actions.
        long sinceLastMitigation = now - state.getLastMitigationAt();
        if (sinceLastMitigation < settings.mitigationCooldownMillis
                && level.ordinal() <= state.getLastMitigationLevel().ordinal()) {
            level = PlayerCheckState.MitigationLevel.NONE;
        }

        if (level != PlayerCheckState.MitigationLevel.NONE) {
            state.recordMitigation(level, now);
            state.setMitigationNote(reason + " via " + checkName, now);
            String summary = "Risk=" + THREE_DECIMALS.format(riskScore) + " level=" + level + " check=" + checkName;
            state.addMitigationHistory(summary);
            switch (level) {
                case SOFT -> mitigationActions.warn(player, checkName, reason);
                case MEDIUM -> {
                    mitigationActions.cancelAction(player, checkName, reason);
                    mitigationActions.temporaryKick(player, checkName, reason);
                }
                case HARD -> {
                    mitigationActions.cancelAction(player, checkName, reason);
                    mitigationActions.clearEntitiesNear(player, checkName, 12, "High risk action");
                    mitigationActions.temporaryBan(player, checkName, reason);
                }
                default -> {
                }
            }
            return new MitigationResult(level, riskScore, summary);
        }

        return new MitigationResult(PlayerCheckState.MitigationLevel.NONE, riskScore, "NONE");
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        if (value > 1) {
            return 1;
        }
        return value;
    }

    public void setLogOnly(boolean logOnly) {
        this.logOnly = logOnly;
    }

    public boolean isLogOnly() {
        return logOnly;
    }

    public record MitigationResult(PlayerCheckState.MitigationLevel level, double riskScore, String actionSummary) {
    }
}
