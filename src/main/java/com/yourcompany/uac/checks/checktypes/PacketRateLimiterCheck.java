package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.packet.PacketPayload;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Tracks per-player incoming packet counts to detect flood/DoS attempts.
 */
public class PacketRateLimiterCheck extends AbstractCheck {

    private static final int WINDOW_MS = 1000;

    private final Map<UUID, Deque<Long>> packetWindows = new ConcurrentHashMap<>();

    public PacketRateLimiterCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "PacketRateLimiter");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PacketPayload payload)) {
            return;
        }

        UUID uuid = payload.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();
        Deque<Long> window = packetWindows.computeIfAbsent(uuid, id -> new ConcurrentLinkedDeque<>());
        window.addLast(now);
        pruneOld(window, now - WINDOW_MS);

        int limit = plugin.getConfigManager().getSettings().packetRateLimit;
        int count = window.size();
        if (count > limit) {
            flag(payload.getPlayer(), "Exceeded packet rate limit: " + count + " > " + limit, payload.getRawPacket());
        }
    }

    public double getRecentPacketsPerSecond(UUID uuid) {
        Deque<Long> window = packetWindows.get(uuid);
        if (window == null || window.isEmpty()) {
            return 0.0;
        }
        long now = System.currentTimeMillis();
        pruneOld(window, now - WINDOW_MS);
        return window.size();
    }

    private void pruneOld(Deque<Long> window, long cutoff) {
        while (!window.isEmpty() && window.peekFirst() < cutoff) {
            window.pollFirst();
        }
    }
}
