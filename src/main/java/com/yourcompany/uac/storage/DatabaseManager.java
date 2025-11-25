package com.yourcompany.uac.storage;

import com.mongodb.client.MongoClient;
import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;

/**
 * Manages persistence backends (flat-file by default, Mongo when enabled).
 */
public class DatabaseManager {

    private final UltimateAntiCheatPlugin plugin;
    private MongoClient client;
    private PlayerDataStore playerDataStore;
    private String persistenceStatus = "flat-file";

    public DatabaseManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        var settings = plugin.getConfigManager().getSettings();
        java.nio.file.Path baseDir = plugin.getDataFolder().toPath().resolve("offline-data");
        if (settings.useDatabase && settings.mongoUri != null && !settings.mongoUri.isBlank()) {
            try {
                String uri = buildMongoUri(settings.mongoUri, settings.mongoUsername, settings.mongoPassword);
                Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                java.lang.reflect.Method create = mongoClients.getMethod("create", String.class);
                client = (MongoClient) create.invoke(null, uri);
                String dbName = extractDatabaseName(uri);
                playerDataStore = new MongoPlayerDataStore(plugin, client, dbName);
                persistenceStatus = "mongo:" + dbName;
                plugin.getLogger().info("[UAC] Connected to MongoDB backend for persistence (db=" + dbName + ").");
            } catch (Exception ex) {
                client = null;
                persistenceStatus = "flat-file (mongo error: " + ex.getClass().getSimpleName() + ")";
                plugin.getLogger().warning("[UAC] Failed to init Mongo backend (" + ex.getMessage() + ") falling back to flat-file storage.");
            }
        } else if (settings.useDatabase) {
            plugin.getLogger().warning("[UAC] storage.use-database enabled but mongo-uri is missing; using flat-file mode.");
        }

        if (client == null) {
            playerDataStore = new FlatFilePlayerDataStore(baseDir);
            persistenceStatus = "flat-file";
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

    public boolean isUsingDatabase() {
        return playerDataStore instanceof MongoPlayerDataStore;
    }

    public String getPersistenceStatus() {
        return persistenceStatus;
    }

    public void saveSnapshot(PlayerCheckState state) {
        if (playerDataStore != null) {
            playerDataStore.saveState(state);
        }
    }

    private String extractDatabaseName(String uri) {
        String trimmed = uri;
        int query = trimmed.indexOf('?');
        if (query > 0) {
            trimmed = trimmed.substring(0, query);
        }
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0 && slash < trimmed.length() - 1) {
            return trimmed.substring(slash + 1);
        }
        return "uac";
    }

    private String buildMongoUri(String base, String user, String pass) {
        if (user == null || user.isBlank() || base.contains("@")) {
            return base;
        }
        String sanitizedPass = pass == null ? "" : pass;
        int scheme = base.indexOf("//");
        if (scheme <= 0) {
            return base;
        }
        String prefix = base.substring(0, scheme + 2);
        String rest = base.substring(scheme + 2);
        return prefix + user + ":" + sanitizedPass + "@" + rest;
    }
}
