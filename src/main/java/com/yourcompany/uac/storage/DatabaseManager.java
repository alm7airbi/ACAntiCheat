package com.yourcompany.uac.storage;

import com.mongodb.client.MongoClient;
import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;

/**
 * Simple MongoDB connector placeholder for player profiles, logs, and trust scores.
 */
public class DatabaseManager {

    private final UltimateAntiCheatPlugin plugin;
    private MongoClient client;
    private PlayerDataStore playerDataStore;

    public DatabaseManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        var settings = plugin.getConfigManager().getSettings();
        if (settings.useDatabase) {
            try {
                Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                java.lang.reflect.Method create = mongoClients.getMethod("create", String.class);
                client = (MongoClient) create.invoke(null, settings.mongoUri);
                plugin.getLogger().info("[UAC] Connected to MongoDB backend for persistence.");
                // TODO: wire Mongo-backed PlayerDataStore
            } catch (Exception ex) {
                plugin.getLogger().warning("[UAC] Failed to init Mongo backend (" + ex.getMessage() + "), falling back to flat-file storage.");
            }
        }

        java.nio.file.Path baseDir = java.nio.file.Paths.get("build/offline-data");
        if (client == null) {
            playerDataStore = new FlatFilePlayerDataStore(baseDir);
        } else if (playerDataStore == null) {
            plugin.getLogger().warning("[UAC] Mongo connected but falling back to flat-file store until DB-backed store is wired.");
            playerDataStore = new FlatFilePlayerDataStore(baseDir);
        }
    }

    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }

    public MongoClient getClient() {
        return client;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }

    public void saveSnapshot(PlayerCheckState state) {
        if (playerDataStore != null) {
            playerDataStore.saveState(state);
        }
    }
}
