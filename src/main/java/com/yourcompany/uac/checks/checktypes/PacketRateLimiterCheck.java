package com.yourcompany.uac.checks.checktypes;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.packet.PacketPayload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks per-player incoming packet counts to detect flood/DoS attempts.
 */
public class PacketRateLimiterCheck extends AbstractCheck {

    private final Map<UUID, AtomicInteger> packetCounts = new ConcurrentHashMap<>();

    public PacketRateLimiterCheck(UltimateAntiCheatPlugin plugin) {
        super(plugin, "PacketRateLimiter");
    }

    @Override
    public void handle(Object context) {
        if (!(context instanceof PacketPayload payload)) {
            return;
        }

        UUID uuid = payload.getPlayer().getUniqueId();
        int count = packetCounts.computeIfAbsent(uuid, id -> new AtomicInteger()).incrementAndGet();

        // Simple placeholder rate limit until buffering manager is implemented
        int limit = plugin.getConfigManager().getSettings().packetRateLimit;
        if (count > limit) {
            flag("Exceeded packet rate limit: " + count + " > " + limit, payload.getPlayer().getName());
        }
    }
}
