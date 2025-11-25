package com.yourcompany.uac.storage;

import com.mongodb.client.MongoClient;
import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.config.Settings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages persistence backends (flat-file by default, Mongo when enabled).
 */
public class DatabaseManager {

    private final UltimateAntiCheatPlugin plugin;
    private MongoClient client;
    private SqlPlayerDataStore sqlStore;
    private PlayerDataStore playerDataStore;
    private String persistenceStatus = "flat-file";
    private String lastErrorMessage = "";
    private volatile long lastErrorAt = -1L;
    private volatile boolean migrationRan;
    private volatile long lastMigrationAt = -1L;
    private volatile int consecutiveFailures;

    private final Map<UUID, PlayerSnapshot> cache;
    private final Queue<PendingWrite> pendingWrites = new ConcurrentLinkedQueue<>();
    private final Set<UUID> migratedPlayers = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService scheduler;
    private int schemaVersion;
    private int cacheMaxEntries;
    private long flushIntervalMillis;
    private int mongoMaxRetries;
    private long mongoRetryDelayMillis;
    private int sqlMaxRetries;
    private long sqlRetryDelayMillis;
    private int sqlPoolSize;
    private int sqlLoginTimeoutSeconds;

    public DatabaseManager(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, PlayerSnapshot> eldest) {
                return cacheMaxEntries > 0 && size() > cacheMaxEntries;
            }
        });
    }

    public void connect() {
        var settings = plugin.getConfigManager().getSettings();
        applySettings(settings);

        Path baseDir = plugin.getDataFolder().toPath().resolve("offline-data");
        if (settings.useSqlDatabase && settings.sqlUrl != null && !settings.sqlUrl.isBlank()) {
            connectSqlWithRetries(settings, baseDir);
        }
        if (playerDataStore == null && settings.useDatabase && settings.mongoUri != null && !settings.mongoUri.isBlank()) {
            connectMongoWithRetries(settings, baseDir);
        } else if (settings.useDatabase) {
            plugin.getLogger().warning("[UAC] storage.use-database enabled but mongo-uri is missing; using flat-file mode.");
        }

        if (playerDataStore == null) {
            playerDataStore = new FlatFilePlayerDataStore(baseDir);
            persistenceStatus = "flat-file";
        }

        scheduleFlush();
    }

    public void disconnect() {
        flushPendingWrites();
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        if (client != null) {
            client.close();
        }
        if (sqlStore != null) {
            sqlStore.close();
        }
    }

    public synchronized void refresh(Settings settings) {
        applySettings(settings);
        flushPendingWrites();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduleFlush();
    }

    public MongoClient getClient() {
        return client;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }

    public Optional<PlayerSnapshot> loadSnapshot(UUID playerId) {
        PlayerSnapshot cached = cache.get(playerId);
        if (cached != null) {
            return Optional.of(cached);
        }
        if (playerDataStore == null) {
            return Optional.empty();
        }
        Optional<PlayerSnapshot> loaded = playerDataStore.load(playerId);
        if (loaded.isEmpty()) {
            return Optional.empty();
        }
        PlayerSnapshot migrated = migrateIfNeeded(playerId, loaded.get());
        cache.put(playerId, migrated);
        return Optional.of(migrated);
    }

    public boolean isUsingDatabase() {
        return playerDataStore instanceof MongoPlayerDataStore || playerDataStore instanceof SqlPlayerDataStore;
    }

    public String getPersistenceStatus() {
        return persistenceStatus;
    }

    public void saveSnapshot(PlayerCheckState state) {
        if (playerDataStore == null) {
            return;
        }
        PlayerSnapshot cached = cache.get(state.getPlayerId());
        if (cached != null && !stateChanged(cached, state)) {
            return;
        }
        PlayerSnapshot snapshot = toSnapshot(state);
        cache.put(state.getPlayerId(), snapshot);
        pendingWrites.add(new PendingWrite(state.getPlayerId(), snapshot));
    }

    private boolean stateChanged(PlayerSnapshot cached, PlayerCheckState state) {
        if (cached == null) {
            return true;
        }
        if (cached.schemaVersion() != schemaVersion) {
            return true;
        }
        if (Double.compare(cached.trustScore(), state.getTrustScore()) != 0) {
            return true;
        }
        if (!cached.flagCounts().equals(state.getFlagCounts())) {
            return true;
        }
        return !cached.mitigationHistory().equals(state.getMitigationHistory());
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

    private void applySettings(Settings settings) {
        this.schemaVersion = settings.persistenceSchemaVersion;
        this.cacheMaxEntries = settings.persistenceCacheMaxEntries;
        this.flushIntervalMillis = settings.persistenceFlushIntervalTicks * 50L;
        this.mongoMaxRetries = Math.max(1, settings.mongoMaxRetries);
        this.mongoRetryDelayMillis = settings.mongoRetryDelayMillis;
        this.sqlMaxRetries = Math.max(1, settings.sqlMaxRetries);
        this.sqlRetryDelayMillis = settings.sqlRetryDelayMillis;
        this.sqlPoolSize = Math.max(1, settings.sqlMaxPoolSize);
        this.sqlLoginTimeoutSeconds = Math.max(1, settings.sqlLoginTimeoutSeconds);
    }

    private void connectSqlWithRetries(Settings settings, Path baseDir) {
        for (int attempt = 1; attempt <= sqlMaxRetries; attempt++) {
            try {
                java.sql.DriverManager.setLoginTimeout(sqlLoginTimeoutSeconds);
                sqlStore = new SqlPlayerDataStore(plugin, settings, sqlPoolSize);
                this.playerDataStore = sqlStore;
                this.persistenceStatus = "sql";
                plugin.getLogger().info("[UAC] Connected to SQL backend for persistence on attempt " + attempt + ".");
                consecutiveFailures = 0;
                return;
            } catch (Exception ex) {
                consecutiveFailures++;
                lastErrorAt = System.currentTimeMillis();
                lastErrorMessage = ex.getMessage();
                plugin.getLogger().log(Level.WARNING, "[UAC] SQL attempt " + attempt + " failed: " + ex.getMessage(), ex);
                if (attempt < sqlMaxRetries && sqlRetryDelayMillis > 0) {
                    try {
                        Thread.sleep(sqlRetryDelayMillis);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        plugin.getLogger().warning("[UAC] Falling back to other storage after SQL connection failures.");
        this.playerDataStore = null; // allow Mongo or flat-file fallback in connect()
        this.persistenceStatus = "flat-file (sql failed)";
    }

    private void connectMongoWithRetries(Settings settings, Path baseDir) {
        String uri = buildMongoUri(settings.mongoUri, settings.mongoUsername, settings.mongoPassword);
        String tunedUri = appendTimeouts(uri, settings);
        for (int attempt = 1; attempt <= mongoMaxRetries; attempt++) {
            try {
                Class<?> mongoClients = Class.forName("com.mongodb.client.MongoClients");
                java.lang.reflect.Method create = mongoClients.getMethod("create", String.class);
                client = (MongoClient) create.invoke(null, tunedUri);
                String dbName = extractDatabaseName(tunedUri);
                playerDataStore = new MongoPlayerDataStore(plugin, client, dbName);
                persistenceStatus = "mongo:" + dbName;
                plugin.getLogger().info("[UAC] Connected to MongoDB backend for persistence (db=" + dbName + ") on attempt " + attempt + ".");
                consecutiveFailures = 0;
                return;
            } catch (Exception ex) {
                consecutiveFailures++;
                lastErrorAt = System.currentTimeMillis();
                lastErrorMessage = ex.getMessage();
                plugin.getLogger().log(Level.WARNING, "[UAC] Mongo attempt " + attempt + " failed: " + ex.getMessage(), ex);
                client = null;
                if (attempt < mongoMaxRetries && mongoRetryDelayMillis > 0) {
                    try {
                        Thread.sleep(mongoRetryDelayMillis);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        plugin.getLogger().warning("[UAC] Falling back to flat-file storage after Mongo connection failures.");
        playerDataStore = new FlatFilePlayerDataStore(baseDir);
        persistenceStatus = "flat-file (mongo failed)";
    }

    private String appendTimeouts(String uri, Settings settings) {
        StringBuilder builder = new StringBuilder(uri);
        String separator = uri.contains("?") ? "&" : "?";
        if (!uri.contains("connectTimeoutMS")) {
            builder.append(separator).append("connectTimeoutMS=").append(settings.mongoConnectTimeoutMs);
            separator = "&";
        }
        if (!uri.contains("socketTimeoutMS")) {
            builder.append(separator).append("socketTimeoutMS=").append(settings.mongoSocketTimeoutMs);
        }
        return builder.toString();
    }

    private PlayerSnapshot migrateIfNeeded(UUID playerId, PlayerSnapshot snapshot) {
        if (snapshot.schemaVersion() == schemaVersion) {
            return snapshot;
        }
        if (snapshot.schemaVersion() > schemaVersion) {
            plugin.getLogger().warning("[UAC] Loaded snapshot for " + playerId + " with future schema " + snapshot.schemaVersion() + " (current " + schemaVersion + "). Using as-is.");
            return snapshot;
        }
        migrationRan = true;
        lastMigrationAt = System.currentTimeMillis();
        migratedPlayers.add(playerId);
        plugin.getLogger().info("[UAC] Migrating snapshot for " + playerId + " from schema " + snapshot.schemaVersion() + " to " + schemaVersion + ".");
        return PlayerSnapshot.fromLegacy(snapshot.trustScore(), snapshot.flagCounts(), snapshot.mitigationHistory(), schemaVersion);
    }

    private PlayerSnapshot toSnapshot(PlayerCheckState state) {
        state.setSchemaVersion(schemaVersion);
        return new PlayerSnapshot(schemaVersion,
                state.getTrustScore(),
                new java.util.HashMap<>(state.getFlagCounts()),
                new ArrayList<>(state.getMitigationHistory()),
                System.currentTimeMillis());
    }

    private void scheduleFlush() {
        if (flushIntervalMillis <= 0) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "uac-persistence-flush");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flushPendingWrites, flushIntervalMillis, flushIntervalMillis, TimeUnit.MILLISECONDS);
    }

    private void flushPendingWrites() {
        if (playerDataStore == null) {
            pendingWrites.clear();
            return;
        }
        List<PendingWrite> batch = new ArrayList<>();
        PendingWrite write;
        while ((write = pendingWrites.poll()) != null) {
            batch.add(write);
        }
        for (PendingWrite pending : batch) {
            try {
                playerDataStore.saveState(new PlayerCheckStateAdapter(pending.playerId(), pending.snapshot()));
                consecutiveFailures = 0;
            } catch (Exception ex) {
                consecutiveFailures++;
                lastErrorAt = System.currentTimeMillis();
                lastErrorMessage = ex.getMessage();
                plugin.getLogger().log(Level.WARNING, "[UAC] Failed to persist snapshot for " + pending.playerId() + ": " + ex.getMessage(), ex);
                if ((isUsingDatabase() || playerDataStore instanceof SqlPlayerDataStore) && consecutiveFailures >= Math.max(mongoMaxRetries, sqlMaxRetries)) {
                    plugin.getLogger().warning("[UAC] Switching to flat-file persistence after repeated database errors.");
                    playerDataStore = new FlatFilePlayerDataStore(plugin.getDataFolder().toPath().resolve("offline-data"));
                    persistenceStatus = "flat-file (persistence error)";
                    client = null;
                    sqlStore = null;
                }
                // re-queue to avoid data loss until fallback
                pendingWrites.add(pending);
                break;
            }
        }
    }

    public void flushNow() {
        flushPendingWrites();
    }

    public int getCacheSize() {
        return cache.size();
    }

    public int getPendingWriteCount() {
        return pendingWrites.size();
    }

    public long getLastErrorAt() {
        return lastErrorAt;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage == null ? "" : lastErrorMessage;
    }

    public boolean isMigrationRan() {
        return migrationRan;
    }

    public long getLastMigrationAt() {
        return lastMigrationAt;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public int getMigratedCount() {
        return migratedPlayers.size();
    }

    private record PendingWrite(UUID playerId, PlayerSnapshot snapshot) {
    }

    /**
     * Lightweight adapter to avoid leaking PlayerCheckState mutability into persistence layer during async flush.
     */
    private static final class PlayerCheckStateAdapter extends PlayerCheckState {
        PlayerCheckStateAdapter(UUID playerId, PlayerSnapshot snapshot) {
            super(playerId);
            setSchemaVersion(snapshot.schemaVersion());
            restoreSnapshot(snapshot);
        }
    }
}
