package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.packet.PacketPayload;
import com.yourcompany.uac.util.TrustScoreManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Centralized registry and dispatcher for all anti-cheat checks.
 *
 * TODO: When wiring against real Paper/ProtocolLib APIs, replace the stubbed alert/broadcast
 * implementation with proper permission-gated messaging (e.g., via Audience API).
 */
public class CheckManager {

    private static final int DEFAULT_PACKET_WINDOW_SECONDS = 5;

    private final UltimateAntiCheatPlugin plugin;
    private final TrustScoreManager trustScoreManager;
    private final List<AbstractCheck> checks = new ArrayList<>();
    private final Map<UUID, Map<String, Integer>> flagCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> packetWindows = new ConcurrentHashMap<>();
    private final Map<String, UUID> lastSeenNameToId = new ConcurrentHashMap<>();

    public CheckManager(UltimateAntiCheatPlugin plugin, TrustScoreManager trustScoreManager) {
        this.plugin = plugin;
        this.trustScoreManager = trustScoreManager;
    }

    public void registerCheck(AbstractCheck check) {
        check.attachCheckManager(this);
        this.checks.add(check);
    }

    public void handlePacket(PacketPayload payload) {
        recordPlayerName(payload.getPlayer());
        recordPacket(payload.getPlayer().getUniqueId());
        for (AbstractCheck check : checks) {
            check.handle(payload);
        }
    }

    public void flagPlayer(Player player, String checkName, String reason, Object data) {
        UUID uuid = player.getUniqueId();
        flagCounts
                .computeIfAbsent(uuid, id -> new ConcurrentHashMap<>())
                .merge(checkName, 1, Integer::sum);
        trustScoreManager.addViolation(uuid, 1);

        String message = "[ACAC] " + player.getName() + " flagged by " + checkName + ": " + reason;
        plugin.getLogger().warning(message + " Data=" + data);
        alertStaff(message);
    }

    public Map<String, Integer> getFlagCounts(UUID playerId) {
        return flagCounts.getOrDefault(playerId, Map.of());
    }

    public int getTrustScore(UUID playerId) {
        return trustScoreManager.getScore(playerId);
    }

    public double getPacketsPerSecond(UUID playerId) {
        return getPacketsPerSecond(playerId, DEFAULT_PACKET_WINDOW_SECONDS);
    }

    public double getPacketsPerSecond(UUID playerId, int windowSeconds) {
        Deque<Long> timestamps = packetWindows.get(playerId);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0.0;
        }
        long cutoff = System.currentTimeMillis() - windowSeconds * 1000L;
        pruneOlderThan(timestamps, cutoff);
        if (timestamps.isEmpty()) {
            return 0.0;
        }
        long windowDurationMillis = Math.max(1, System.currentTimeMillis() - timestamps.peekFirst());
        return (timestamps.size() * 1000.0) / windowDurationMillis;
    }

    public Optional<UUID> findPlayerId(String playerName) {
        return Optional.ofNullable(lastSeenNameToId.get(playerName.toLowerCase()));
    }

    public void alertStaff(String message) {
        // TODO: Replace stub logging with broadcasting to players having "acac.notify" permission.
        plugin.getLogger().info(message);
    }

    private void recordPacket(UUID playerId) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = packetWindows.computeIfAbsent(playerId, id -> new ConcurrentLinkedDeque<>());
        timestamps.addLast(now);
        pruneOlderThan(timestamps, now - DEFAULT_PACKET_WINDOW_SECONDS * 1000L);
    }

    private void pruneOlderThan(Deque<Long> timestamps, long cutoff) {
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }
    }

    private void recordPlayerName(Player player) {
        lastSeenNameToId.put(player.getName().toLowerCase(), player.getUniqueId());
    }
}
