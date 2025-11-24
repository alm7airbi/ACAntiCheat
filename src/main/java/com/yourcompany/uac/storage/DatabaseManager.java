package com.yourcompany.uac.storage;

import com.mongodb.client.MongoClient;
import com.yourcompany.uac.UltimateAntiCheatPlugin;

/**
 * Simple MongoDB connector placeholder for player profiles, logs, and trust scores.
 */
public class DatabaseManager {

    private final UltimateAntiCheatPlugin plugin;
    private MongoClient client;

    public DatabaseManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        // TODO: read URI/credentials from config
        plugin.getLogger().info("[UAC] DatabaseManager stubbed; enable storage.use-database to activate.");
        // client = MongoClients.create(plugin.getConfig().getString("storage.mongo-uri"));
    }

    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }

    public MongoClient getClient() {
        return client;
    }
}
