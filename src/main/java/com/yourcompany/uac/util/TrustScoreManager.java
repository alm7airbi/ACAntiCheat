package com.yourcompany.uac.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player trust/violation scores to dampen false positives and stage ban waves.
 */
public class TrustScoreManager {

    private final Map<UUID, Integer> scores = new ConcurrentHashMap<>();

    public int addViolation(UUID uuid, int weight) {
        return scores.merge(uuid, weight, Integer::sum);
    }

    public int getScore(UUID uuid) {
        return scores.getOrDefault(uuid, 0);
    }

    public void decay(UUID uuid, int amount) {
        scores.computeIfPresent(uuid, (id, value) -> Math.max(0, value - amount));
    }
}
