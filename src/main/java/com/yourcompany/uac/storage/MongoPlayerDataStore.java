package com.yourcompany.uac.storage;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.mongodb.client.MongoClient;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Reflection-based MongoDB store that avoids a hard dependency on the driver in
 * stub builds, while allowing real servers to persist player state/history.
 */
public class MongoPlayerDataStore implements PlayerDataStore {

    private final UltimateAntiCheatPlugin plugin;
    private final MongoClient client;
    private final Object collection;
    private final Class<?> documentClass;
    private final Class<?> bsonClass;

    private final Method findMethod;
    private final Method firstMethod;
    private final Method deleteOneMethod;
    private final Method insertOneMethod;

    public MongoPlayerDataStore(UltimateAntiCheatPlugin plugin, MongoClient client, String databaseName) throws Exception {
        this.plugin = plugin;
        this.client = client;
        this.documentClass = Class.forName("org.bson.Document");
        this.bsonClass = Class.forName("org.bson.conversions.Bson");

        // database = client.getDatabase(databaseName)
        Object database = client.getClass().getMethod("getDatabase", String.class).invoke(client, databaseName);
        // collection = database.getCollection("acac_state")
        this.collection = database.getClass().getMethod("getCollection", String.class).invoke(database, "acac_state");

        Class<?> collectionClass = collection.getClass();
        this.findMethod = collectionClass.getMethod("find", bsonClass);
        this.deleteOneMethod = collectionClass.getMethod("deleteOne", bsonClass);
        this.insertOneMethod = collectionClass.getMethod("insertOne", documentClass);

        Class<?> iterableClass = Class.forName("com.mongodb.client.FindIterable");
        this.firstMethod = iterableClass.getMethod("first");
    }

    @Override
    public Optional<PlayerSnapshot> load(UUID playerId) {
        try {
            Object filter = filterDoc(playerId);
            Object iterable = findMethod.invoke(collection, filter);
            Object doc = firstMethod.invoke(iterable);
            if (doc == null) {
                return Optional.empty();
            }
            return Optional.of(toSnapshot(doc));
        } catch (Exception ex) {
            plugin.getLogger().warning("[UAC] Mongo load failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void saveState(PlayerCheckState state) {
        try {
            Object doc = newDocument();
            Map<String, Object> map = castDocument(doc);
            map.put("_id", state.getPlayerId().toString());
            map.put("trust", state.getTrustScore());
            map.put("flags", new HashMap<>(state.getFlagCounts()));
            map.put("mitigations", new ArrayList<>(state.getMitigationHistory()));
            // Preserve a bounded history array if present
            List<String> existingHistory = loadHistory(state.getPlayerId(), plugin.getConfigManager().getSettings().historyLimit);
            if (!existingHistory.isEmpty()) {
                map.put("history", existingHistory);
            }
            upsert(doc, state.getPlayerId());
        } catch (Exception ex) {
            plugin.getLogger().warning("[UAC] Mongo save failed for " + state.getPlayerId() + ": " + ex.getMessage());
        }
    }

    @Override
    public void appendHistory(UUID playerId, String entry, int limit) {
        try {
            Object doc = newDocument();
            Map<String, Object> map = castDocument(doc);
            map.put("_id", playerId.toString());
            map.put("trust", load(playerId).map(PlayerSnapshot::trustScore).orElse(100.0));
            map.put("flags", load(playerId).map(PlayerSnapshot::flagCounts).orElseGet(HashMap::new));
            map.put("mitigations", load(playerId).map(PlayerSnapshot::mitigationHistory).orElseGet(ArrayList::new));

            List<String> history = loadHistory(playerId, limit + 1);
            List<String> next = new ArrayList<>(history);
            next.add(entry);
            if (next.size() > limit) {
                next = next.subList(next.size() - limit, next.size());
            }
            map.put("history", next);
            upsert(doc, playerId);
        } catch (Exception ex) {
            plugin.getLogger().warning("[UAC] Mongo history append failed for " + playerId + ": " + ex.getMessage());
        }
    }

    @Override
    public List<String> loadHistory(UUID playerId, int limit) {
        try {
            Object filter = filterDoc(playerId);
            Object iterable = findMethod.invoke(collection, filter);
            Object doc = firstMethod.invoke(iterable);
            if (doc == null) {
                return List.of();
            }
            Map<String, Object> map = castDocument(doc);
            Object raw = map.get("history");
            if (!(raw instanceof List<?> history)) {
                return List.of();
            }
            if (history.size() <= limit) {
                return new ArrayList<>(history.stream().map(Object::toString).toList());
            }
            int start = Math.max(0, history.size() - limit);
            return new ArrayList<>(history.subList(start, history.size()).stream().map(Object::toString).toList());
        } catch (Exception ex) {
            plugin.getLogger().warning("[UAC] Mongo loadHistory failed: " + ex.getMessage());
            return List.of();
        }
    }

    private void upsert(Object doc, UUID playerId) throws Exception {
        Object filter = filterDoc(playerId);
        deleteOneMethod.invoke(collection, filter);
        insertOneMethod.invoke(collection, doc);
    }

    private Object newDocument() throws Exception {
        return documentClass.getConstructor().newInstance();
    }

    private Object filterDoc(UUID playerId) throws Exception {
        Object filter = newDocument();
        castDocument(filter).put("_id", playerId.toString());
        return filter;
    }

    private Map<String, Object> castDocument(Object doc) {
        return (Map<String, Object>) doc;
    }

    private PlayerSnapshot toSnapshot(Object doc) {
        Map<String, Object> map = castDocument(doc);
        double trust = map.getOrDefault("trust", 100.0) instanceof Number n ? n.doubleValue() : 100.0;
        Map<String, Integer> flags = new HashMap<>();
        Object rawFlags = map.get("flags");
        if (rawFlags instanceof Map<?, ?> f) {
            for (var entry : f.entrySet()) {
                if (entry.getKey() != null && entry.getValue() instanceof Number n) {
                    flags.put(entry.getKey().toString(), n.intValue());
                }
            }
        }
        List<String> mitigations = new ArrayList<>();
        Object rawMitigation = map.get("mitigations");
        if (rawMitigation instanceof List<?> list) {
            list.stream().map(Object::toString).forEach(mitigations::add);
        }
        return new PlayerSnapshot(trust, flags, mitigations);
    }
}
