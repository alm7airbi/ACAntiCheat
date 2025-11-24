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
    private final Map<Integer, Deque<Long>> packetWindows = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Long>> entityWindows = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<Long>> consoleWindows = new ConcurrentHashMap<>();

    private volatile double trustScore = MAX_TRUST;
    private volatile long lastTrustRecovery = System.currentTimeMillis();
    private volatile long lastMovementMillis;
    private volatile long lastTeleportMillis;
    private volatile boolean lastTeleportServerInitiated;
    private volatile long lastInventoryMillis;
    private volatile long lastEntityInteractionMillis;
    private volatile long lastConsoleActivityMillis;
    private volatile boolean underMitigation;
    private volatile Position lastKnownPosition;

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

    public void recordFlag(String checkId, int severity) {
        flagCounts.merge(checkId, 1, Integer::sum);
        trustScore = Math.max(0, trustScore - severity * 2.5);
    }

    public Map<String, Integer> getFlagCounts() {
        return flagCounts;
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

    public void recordInventoryInteraction(long timestamp) {
        this.lastInventoryMillis = timestamp;
    }

    public void setUnderMitigation(boolean underMitigation) {
        this.underMitigation = underMitigation;
    }

    public boolean isUnderMitigation() {
        return underMitigation;
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
}
