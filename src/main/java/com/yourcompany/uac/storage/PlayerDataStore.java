package com.yourcompany.uac.storage;

import java.util.UUID;

/**
 * Placeholder API for reading/writing player session data (flags, trust scores).
 */
public interface PlayerDataStore {

    void incrementFlag(UUID playerId, String checkName);

    int getTrustScore(UUID playerId);
}
