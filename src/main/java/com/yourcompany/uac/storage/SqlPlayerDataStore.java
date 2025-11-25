package com.yourcompany.uac.storage;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.config.Settings;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal JDBC-backed store for environments that prefer SQL over Mongo.
 * Uses a simple connection pool built on DriverManager to avoid new deps.
 */
public class SqlPlayerDataStore implements PlayerDataStore {

    private final UltimateAntiCheatPlugin plugin;
    private final Settings settings;
    private final Queue<Connection> pool = new ArrayDeque<>();
    private final int maxPoolSize;
    private final AtomicInteger createdConnections = new AtomicInteger();

    public SqlPlayerDataStore(UltimateAntiCheatPlugin plugin, Settings settings, int maxPoolSize) throws SQLException {
        this.plugin = plugin;
        this.settings = settings;
        this.maxPoolSize = Math.max(1, maxPoolSize);
        init();
    }

    private void init() throws SQLException {
        try (Connection conn = borrow()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS acac_state (" +
                        "id VARCHAR(64) PRIMARY KEY," +
                        "schema_version INT," +
                        "trust DOUBLE," +
                        "flags TEXT," +
                        "mitigations TEXT," +
                        "last_updated BIGINT," +
                        "history TEXT"
                        + ")");
            }
        }
    }

    @Override
    public Optional<PlayerSnapshot> load(UUID playerId) {
        String sql = "SELECT schema_version, trust, flags, mitigations, last_updated FROM acac_state WHERE id=?";
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    int schema = rs.getInt("schema_version");
                    double trust = rs.getDouble("trust");
                    Map<String, Integer> flags = decodeFlags(rs.getString("flags"));
                    List<String> mitigations = decodeList(rs.getString("mitigations"));
                    long updated = rs.getLong("last_updated");
                    return Optional.of(new PlayerSnapshot(schema, trust, flags, mitigations, updated));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[UAC] SQL load failed: " + ex.getMessage());
            return Optional.empty();
        } finally {
            release(conn);
        }
    }

    @Override
    public void saveState(PlayerCheckState state) {
        String updateSql = "UPDATE acac_state SET schema_version=?, trust=?, flags=?, mitigations=?, last_updated=?, history=? WHERE id=?";
        String insertSql = "INSERT INTO acac_state (id, schema_version, trust, flags, mitigations, last_updated, history) VALUES (?,?,?,?,?,?,?)";
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setInt(1, state.getSchemaVersion());
                update.setDouble(2, state.getTrustScore());
                update.setString(3, encodeFlags(state.getFlagCounts()));
                update.setString(4, encodeList(state.getMitigationHistory()));
                update.setLong(5, System.currentTimeMillis());
                update.setString(6, encodeList(loadHistory(state.getPlayerId(), settings.historyLimit)));
                update.setString(7, state.getPlayerId().toString());
                int updated = update.executeUpdate();
                if (updated == 0) {
                    try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                        insert.setString(1, state.getPlayerId().toString());
                        insert.setInt(2, state.getSchemaVersion());
                        insert.setDouble(3, state.getTrustScore());
                        insert.setString(4, encodeFlags(state.getFlagCounts()));
                        insert.setString(5, encodeList(state.getMitigationHistory()));
                        insert.setLong(6, System.currentTimeMillis());
                        insert.setString(7, encodeList(loadHistory(state.getPlayerId(), settings.historyLimit)));
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[UAC] SQL save failed for " + state.getPlayerId() + ": " + ex.getMessage());
        } finally {
            release(conn);
        }
    }

    @Override
    public void appendHistory(UUID playerId, String entry, int limit) {
        List<String> history = new ArrayList<>(loadHistory(playerId, limit + 1));
        history.add(entry);
        if (history.size() > limit) {
            history = history.subList(history.size() - limit, history.size());
        }
        String updateSql = "UPDATE acac_state SET history=?, last_updated=? WHERE id=?";
        String insertSql = "INSERT INTO acac_state (id, schema_version, trust, flags, mitigations, last_updated, history) VALUES (?,?,?,?,?,?,?)";
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setString(1, encodeList(history));
                update.setLong(2, System.currentTimeMillis());
                update.setString(3, playerId.toString());
                int updated = update.executeUpdate();
                if (updated == 0) {
                    try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                        insert.setString(1, playerId.toString());
                        insert.setInt(2, settings.persistenceSchemaVersion);
                        insert.setDouble(3, 100.0);
                        insert.setString(4, encodeFlags(new HashMap<>()));
                        insert.setString(5, "");
                        insert.setLong(6, System.currentTimeMillis());
                        insert.setString(7, encodeList(history));
                        insert.executeUpdate();
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[UAC] SQL history append failed for " + playerId + ": " + ex.getMessage());
        } finally {
            release(conn);
        }
    }

    @Override
    public List<String> loadHistory(UUID playerId, int limit) {
        String sql = "SELECT history FROM acac_state WHERE id=?";
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return List.of();
                    }
                    List<String> list = decodeList(rs.getString("history"));
                    if (list.size() <= limit) {
                        return list;
                    }
                    return new ArrayList<>(list.subList(list.size() - limit, list.size()));
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("[UAC] SQL loadHistory failed: " + ex.getMessage());
            return List.of();
        } finally {
            release(conn);
        }
    }

    private String encodeFlags(Map<String, Integer> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : flags.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(';');
            }
            sb.append(entry.getKey().replace(";", ":"))
                    .append('=')
                    .append(entry.getValue());
        }
        return sb.toString();
    }

    private Map<String, Integer> decodeFlags(String raw) {
        Map<String, Integer> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }
        for (String part : raw.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(kv[0].replace(":", ";"), Integer.parseInt(kv[1]));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }

    private String encodeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join("|", list.stream().map(s -> s.replace("|", "/")).toList());
    }

    private List<String> decodeList(String raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        String[] parts = raw.split("\\|", -1);
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            out.add(p);
        }
        return out;
    }

    private Connection borrow() throws SQLException {
        while (true) {
            Connection conn = pool.poll();
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        return conn;
                    }
                } catch (SQLException ignored) {
                }
            }
            if (createdConnections.get() < maxPoolSize) {
                Connection created = DriverManager.getConnection(settings.sqlUrl, settings.sqlUsername, settings.sqlPassword);
                createdConnections.incrementAndGet();
                return created;
            }
            // simple backoff if pool exhausted
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void release(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            if (conn.isClosed()) {
                return;
            }
            pool.offer(conn);
        } catch (SQLException ignored) {
        }
    }

    public void close() {
        Connection c;
        while ((c = pool.poll()) != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
