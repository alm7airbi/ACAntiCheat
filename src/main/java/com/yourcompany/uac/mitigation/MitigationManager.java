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

    public MitigationResult evaluate(Player player, String checkName, String reason, int severity, PlayerCheckState state, long now, Object context) {
        Settings settings = plugin.getConfigManager().getSettings();
        double trust = state.getTrustScore();
        int flags = state.getFlagCounts().getOrDefault(checkName, 0);
        double riskScore = clamp(((100 - trust) / 100.0) + (severity * 0.15) + (flags * 0.1));

        if (flags < settings.minViolationsBeforeMitigation && severity < settings.mitigationSensitivity) {
            riskScore *= 0.5;
        }

        PlayerCheckState.MitigationLevel level = chooseLevel(settings, riskScore);

        PlayerCheckState.MitigationLevel configured = settings.getMitigationMode(checkName);
        if (configured != null && configured.ordinal() > level.ordinal()) {
            level = configured;
        }

        if (logOnly && level.ordinal() > PlayerCheckState.MitigationLevel.WARN.ordinal()) {
            level = PlayerCheckState.MitigationLevel.WARN;
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
                case WARN -> mitigationActions.warn(player, checkName, reason);
                case ROLLBACK -> {
                    mitigationActions.warn(player, checkName, reason);
                    dispatchRollback(player, checkName, reason, context, state);
                }
                case THROTTLE -> {
                    mitigationActions.throttle(player, checkName, reason);
                    mitigationActions.warn(player, checkName, "Throttling after repeated flags");
                }
                case RUBBERBAND -> {
                    mitigationActions.cancelAction(player, checkName, reason);
                    mitigationActions.rubberBand(player, checkName, state.getLastKnownPosition(), reason);
                }
                case KICK -> {
                    mitigationActions.cancelAction(player, checkName, reason);
                    mitigationActions.temporaryKick(player, checkName, reason);
                }
                case TEMP_BAN -> {
                    mitigationActions.cancelAction(player, checkName, reason);
                    mitigationActions.temporaryBan(player, checkName, reason);
                }
                case PERM_BAN -> {
                    mitigationActions.cancelAction(player, checkName, reason);
                    mitigationActions.permanentBan(player, checkName, reason);
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

    private PlayerCheckState.MitigationLevel chooseLevel(Settings settings, double riskScore) {
        if (riskScore >= settings.banSuggestThreshold) {
            return PlayerCheckState.MitigationLevel.PERM_BAN;
        }
        if (riskScore >= settings.temporaryBanThreshold) {
            return PlayerCheckState.MitigationLevel.TEMP_BAN;
        }
        if (riskScore >= settings.temporaryKickThreshold) {
            return PlayerCheckState.MitigationLevel.KICK;
        }
        if (riskScore >= settings.rubberBandThreshold) {
            return PlayerCheckState.MitigationLevel.RUBBERBAND;
        }
        if (riskScore >= settings.throttleThreshold) {
            return PlayerCheckState.MitigationLevel.THROTTLE;
        }
        if (riskScore >= settings.rollbackThreshold) {
            return PlayerCheckState.MitigationLevel.ROLLBACK;
        }
        if (riskScore >= settings.warnThreshold) {
            return PlayerCheckState.MitigationLevel.WARN;
        }
        return PlayerCheckState.MitigationLevel.NONE;
    }

    private void dispatchRollback(Player player, String checkName, String reason, Object context, PlayerCheckState state) {
        if (context instanceof com.yourcompany.uac.checks.context.PlacementContext) {
            mitigationActions.rollbackPlacement(player, checkName, reason);
            return;
        }
        if (context instanceof com.yourcompany.uac.checks.context.InventoryActionContext) {
            mitigationActions.rollbackInventory(player, checkName, reason);
            return;
        }
        mitigationActions.cancelAction(player, checkName, reason);
    }
}
