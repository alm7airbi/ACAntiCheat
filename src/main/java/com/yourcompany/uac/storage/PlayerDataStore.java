package com.yourcompany.uac.storage;

import com.yourcompany.uac.checks.PlayerCheckState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read/write persistence for per-player anti-cheat data.
 */
public interface PlayerDataStore {

    Optional<PlayerSnapshot> load(UUID playerId);

    void saveState(PlayerCheckState state);

    void appendHistory(UUID playerId, String entry);

    List<String> loadHistory(UUID playerId, int limit);
}
