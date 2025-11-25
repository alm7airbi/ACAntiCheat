package com.yourcompany.uac.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted snapshot of a player's anti-cheat state, including a schema
 * version so migrations can safely upgrade older records.
 */
public record PlayerSnapshot(int schemaVersion,
                             double trustScore,
                             Map<String, Integer> flagCounts,
                             List<String> mitigationHistory,
                             long lastUpdated) {

    public static PlayerSnapshot fromLegacy(double trustScore,
                                            Map<String, Integer> flagCounts,
                                            List<String> mitigationHistory,
                                            int targetSchema) {
        return new PlayerSnapshot(targetSchema,
                trustScore,
                flagCounts == null ? new HashMap<>() : new HashMap<>(flagCounts),
                mitigationHistory == null ? new ArrayList<>() : new ArrayList<>(mitigationHistory),
                System.currentTimeMillis());
    }
}
