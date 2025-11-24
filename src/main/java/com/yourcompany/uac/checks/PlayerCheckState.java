package com.yourcompany.uac.checks;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Aggregates per-player state across checks so each module can reason about
 * trust, packet pacing, movement recency, and entity/log spam without doing
 * its own lookups.
 */
public class PlayerCheckState {

    private static final double MAX_TRUST = 100.0;

    private final UUID playerId;
    private final Map<String, Integer> flagCounts = new ConcurrentHashMap<>();
    private final Map<String, FlagRecord> flagRecords = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Long>> packetWindows = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Long>> entityWindows = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Long>> consoleWindows = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, Deque<Long>>> actionWindows = new ConcurrentHashMap<>();
    private final Deque<String> mitigationHistory = new ConcurrentLinkedDeque<>();

    private volatile double trustScore = MAX_TRUST;
    private volatile long lastTrustRecovery = System.currentTimeMillis();
    private volatile long lastMovementMillis;
    private volatile long lastTeleportMillis;
    private volatile boolean lastTeleportServerInitiated;
    private volatile long lastInventoryMillis;
    private volatile long lastEntityInteractionMillis;
    private volatile long lastConsoleActivityMillis;
    private volatile boolean underMitigation;
    private volatile MitigationLevel lastMitigationLevel = MitigationLevel.NONE;
    private volatile long lastMitigationAt;
    private volatile String lastMitigationReason;
    private volatile Position lastKnownPosition;
    private volatile Position lastPlacementPosition;
    private volatile String lastInventorySnapshot;

    public PlayerCheckState(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void recoverTrust(long now) {
        long elapsed = now - lastTrustRecovery;
        if (elapsed >= 3000) {
            int steps = (int) (elapsed / 3000);
            trustScore = Math.min(MAX_TRUST, trustScore + steps);
            lastTrustRecovery = now;
        }
    }

    public void recordFlag(String checkId, String reason, int severity, long timestamp) {
        flagCounts.merge(checkId, 1, Integer::sum);
        trustScore = Math.max(0, trustScore - severity * 2.5);
        flagRecords.compute(checkId, (id, existing) -> {
            if (existing == null) {
                return new FlagRecord(1, reason, severity, timestamp);
            }
            return new FlagRecord(existing.count() + 1, reason, severity, timestamp);
        });
    }

    public void clearFlags() {
        flagCounts.clear();
        flagRecords.clear();
    }

    public void resetTrust() {
        trustScore = MAX_TRUST;
    }

    public Map<String, Integer> getFlagCounts() {
        return flagCounts;
    }

    public Map<String, FlagRecord> getFlagRecords() {
        return flagRecords;
    }

    public double getTrustScore() {
        return trustScore;
    }

    public int recordPacketInWindow(int windowSeconds, long now) {
        Deque<Long> window = packetWindows.computeIfAbsent(windowSeconds, key -> new ConcurrentLinkedDeque<>());
        window.addLast(now);
        prune(window, now - windowSeconds * 1000L);
        return window.size();
    }

    public int getPacketWindowCount(int windowSeconds, long now) {
        Deque<Long> window = packetWindows.get(windowSeconds);
        if (window == null) {
            return 0;
        }
        prune(window, now - windowSeconds * 1000L);
        return window.size();
    }

    public int recordEntityWindow(int windowSeconds, long now) {
        Deque<Long> window = entityWindows.computeIfAbsent(windowSeconds, key -> new ConcurrentLinkedDeque<>());
        window.addLast(now);
        prune(window, now - windowSeconds * 1000L);
        lastEntityInteractionMillis = now;
        return window.size();
    }

    public int recordConsoleWindow(int windowSeconds, long now) {
        Deque<Long> window = consoleWindows.computeIfAbsent(windowSeconds, key -> new ConcurrentLinkedDeque<>());
        window.addLast(now);
        prune(window, now - windowSeconds * 1000L);
        lastConsoleActivityMillis = now;
        return window.size();
    }

    public int recordActionWindow(String windowKey, int windowSeconds, long now) {
        Map<Integer, Deque<Long>> windows = actionWindows.computeIfAbsent(windowKey, k -> new ConcurrentHashMap<>());
        Deque<Long> window = windows.computeIfAbsent(windowSeconds, key -> new ConcurrentLinkedDeque<>());
        window.addLast(now);
        prune(window, now - windowSeconds * 1000L);
        return window.size();
    }

    public int getActionWindowCount(String windowKey, int windowSeconds, long now) {
        Map<Integer, Deque<Long>> windows = actionWindows.get(windowKey);
        if (windows == null) {
            return 0;
        }
        Deque<Long> window = windows.get(windowSeconds);
        if (window == null) {
            return 0;
        }
        prune(window, now - windowSeconds * 1000L);
        return window.size();
    }

    public long getLastMovementMillis() {
        return lastMovementMillis;
    }

    public long getLastTeleportMillis() {
        return lastTeleportMillis;
    }

    public boolean wasTeleportServerInitiated() {
        return lastTeleportServerInitiated;
    }

    public long getLastInventoryMillis() {
        return lastInventoryMillis;
    }

    public long getLastEntityInteractionMillis() {
        return lastEntityInteractionMillis;
    }

    public long getLastConsoleActivityMillis() {
        return lastConsoleActivityMillis;
    }

    public void recordMovement(Position position, long timestamp, boolean serverTeleport) {
        this.lastMovementMillis = timestamp;
        if (serverTeleport) {
            this.lastTeleportMillis = timestamp;
            this.lastTeleportServerInitiated = true;
        } else {
            this.lastTeleportServerInitiated = false;
        }
        this.lastKnownPosition = position;
    }

    public Position getLastKnownPosition() {
        return lastKnownPosition;
    }

    public Position getLastPlacementPosition() {
        return lastPlacementPosition;
    }

    public void recordInventoryInteraction(long timestamp) {
        this.lastInventoryMillis = timestamp;
    }

    public void recordPlacement(Position position) {
        this.lastPlacementPosition = position;
    }

    public void recordInventorySnapshot(String snapshot) {
        this.lastInventorySnapshot = snapshot;
    }

    public String getLastInventorySnapshot() {
        return lastInventorySnapshot;
    }

    public void setUnderMitigation(boolean underMitigation) {
        this.underMitigation = underMitigation;
    }

    public boolean isUnderMitigation() {
        return underMitigation;
    }

    public void recordMitigation(MitigationLevel level, long timestamp) {
        this.lastMitigationLevel = level;
        this.lastMitigationAt = timestamp;
        this.underMitigation = level != MitigationLevel.NONE;
    }

    public void setMitigationNote(String reason, long timestamp) {
        this.lastMitigationReason = reason;
        this.lastMitigationAt = timestamp;
    }

    public void addMitigationHistory(String entry) {
        mitigationHistory.addFirst(entry);
        while (mitigationHistory.size() > 5) {
            mitigationHistory.pollLast();
        }
    }

    public java.util.List<String> getMitigationHistory() {
        return java.util.List.copyOf(mitigationHistory);
    }

    public void restoreSnapshot(double trust, Map<String, Integer> flags, java.util.List<String> mitigation) {
        this.trustScore = Math.max(0, Math.min(MAX_TRUST, trust));
        if (flags != null) {
            this.flagCounts.putAll(flags);
        }
        if (mitigation != null) {
            mitigation.forEach(this::addMitigationHistory);
        }
    }

    public MitigationLevel getLastMitigationLevel() {
        return lastMitigationLevel;
    }

    public long getLastMitigationAt() {
        return lastMitigationAt;
    }

    public String getLastMitigationReason() {
        return lastMitigationReason;
    }

    private void prune(Deque<Long> window, long cutoff) {
        while (!window.isEmpty() && window.peekFirst() < cutoff) {
            window.pollFirst();
        }
    }

    public static Position position(double x, double y, double z) {
        return new Position(x, y, z);
    }

    public record Position(double x, double y, double z) {
        public double distanceSquared(Position other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    public record FlagRecord(int count, String lastReason, int lastSeverity, long lastFlagAt) {
    }

    public enum MitigationLevel {
        NONE,
        WARN,
        ROLLBACK,
        THROTTLE,
        RUBBERBAND,
        KICK,
        TEMP_BAN,
        PERM_BAN
    }
}
