package com.yourcompany.uac.mitigation;

import com.yourcompany.uac.checks.PlayerCheckState;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Structured alert payload used by alert sinks (console/staff/webhooks/structured logs).
 */
public class AlertEvent {
    private final Instant timestamp;
    private final String playerName;
    private final String checkName;
    private final String message;
    private final int severity;
    private final PlayerCheckState.MitigationLevel mitigationLevel;
    private final Map<String, Object> context;

    public AlertEvent(Instant timestamp, String playerName, String checkName, String message, int severity,
                      PlayerCheckState.MitigationLevel mitigationLevel, Map<String, Object> context) {
        this.timestamp = Objects.requireNonNullElse(timestamp, Instant.now());
        this.playerName = playerName;
        this.checkName = checkName;
        this.message = message;
        this.severity = severity;
        this.mitigationLevel = mitigationLevel;
        this.context = context == null ? Collections.emptyMap() : Map.copyOf(context);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCheckName() {
        return checkName;
    }

    public String getMessage() {
        return message;
    }

    public int getSeverity() {
        return severity;
    }

    public PlayerCheckState.MitigationLevel getMitigationLevel() {
        return mitigationLevel;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
