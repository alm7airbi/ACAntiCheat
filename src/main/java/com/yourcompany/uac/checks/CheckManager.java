package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.context.CommandContext;
import com.yourcompany.uac.checks.context.ConsoleMessageContext;
import com.yourcompany.uac.checks.context.EntityActionContext;
import com.yourcompany.uac.checks.context.InventoryActionContext;
import com.yourcompany.uac.checks.context.MovementContext;
import com.yourcompany.uac.checks.context.PacketContext;
import com.yourcompany.uac.checks.context.PayloadContext;
import com.yourcompany.uac.checks.context.PlacementContext;
import com.yourcompany.uac.checks.context.RedstoneContext;
import com.yourcompany.uac.packet.PacketPayload;
import com.yourcompany.uac.util.TrustScoreManager;
import com.yourcompany.uac.mitigation.AlertManager;
import com.yourcompany.uac.mitigation.MitigationManager;
import com.yourcompany.uac.storage.DatabaseManager;
import com.yourcompany.uac.storage.PlayerSnapshot;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Centralized registry and dispatcher for all anti-cheat checks.
 *
 * TODO: When wiring against real Paper/ProtocolLib APIs, replace the stubbed alert/broadcast
 * implementation with proper permission-gated messaging (e.g., via Audience API).
 */
public class CheckManager {

    private final UltimateAntiCheatPlugin plugin;
    private final TrustScoreManager trustScoreManager;
    private final List<AbstractCheck> checks = new ArrayList<>();
    private final MitigationManager mitigationManager;
    private final AlertManager alertManager;
    private final Map<UUID, PlayerCheckState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, UUID> lastSeenNameToId = new ConcurrentHashMap<>();
    private final DatabaseManager databaseManager;
    private final ConcurrentMap<UUID, Boolean> restoredFromStore = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> timingNanos = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> timingCounts = new ConcurrentHashMap<>();

    public CheckManager(UltimateAntiCheatPlugin plugin, TrustScoreManager trustScoreManager, MitigationManager mitigationManager, AlertManager alertManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.trustScoreManager = trustScoreManager;
        this.mitigationManager = mitigationManager;
        this.alertManager = alertManager;
        this.databaseManager = databaseManager;
    }

    public void registerCheck(AbstractCheck check) {
        check.attachCheckManager(this);
        this.checks.add(check);
    }

    public void handlePacket(PacketPayload payload) {
        Player player = payload.getPlayer();
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        recordPlayerName(player);
        long now = System.currentTimeMillis();
        state.recoverTrust(now);

        int packetsLastSecond = state.recordPacketInWindow(1, now);
        int packetsLastFiveSeconds = state.recordPacketInWindow(5, now);

        PacketContext ctx = new PacketContext(player, payload.getRawPacket(), state, now, packetsLastSecond, packetsLastFiveSeconds);
        dispatch(ctx);
        cleanupInactive(now);
    }

    public void handleMovement(Player player, double x, double y, double z, boolean serverTeleport) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int packetsLastSecond = state.recordPacketInWindow(1, now);
        int packetsLastFiveSeconds = state.recordPacketInWindow(5, now);
        int chunkChanges = 0;
        var lastPos = state.getLastKnownPosition();
        if (lastPos != null) {
            int lastChunkX = (int) lastPos.x() >> 4;
            int lastChunkZ = (int) lastPos.z() >> 4;
            int newChunkX = (int) x >> 4;
            int newChunkZ = (int) z >> 4;
            if (lastChunkX != newChunkX || lastChunkZ != newChunkZ) {
                chunkChanges = state.recordActionWindow("chunk-hop", plugin.getConfigManager().getSettings().chunkWindowSeconds, now);
            } else {
                chunkChanges = state.getActionWindowCount("chunk-hop", plugin.getConfigManager().getSettings().chunkWindowSeconds, now);
            }
        }
        state.recordMovement(PlayerCheckState.position(x, y, z), now, serverTeleport);

        MovementContext context = new MovementContext(player, null, state, now, packetsLastSecond, packetsLastFiveSeconds, x, y, z, true, serverTeleport, chunkChanges, plugin.getConfigManager().getSettings().chunkWindowSeconds);
        dispatch(context);
        cleanupInactive(now);
    }

    public void handleCommand(Player player, String commandLine) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int count = state.recordActionWindow("commands", plugin.getConfigManager().getSettings().commandWindowSeconds, now);
        dispatch(new CommandContext(player, state, commandLine, now, count));
        cleanupInactive(now);
    }

    public void handleEntityAction(Player player, String actionType) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().entityWindowSeconds;
        int count = state.recordEntityWindow(windowSeconds, now);
        dispatch(new EntityActionContext(player, state, actionType, now, count));
        cleanupInactive(now);
    }

    public void handleConsoleMessage(Player player, String message) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().consoleWindowSeconds;
        int count = state.recordConsoleWindow(windowSeconds, now);
        dispatch(new ConsoleMessageContext(player, state, message, now, count));
        cleanupInactive(now);
    }

    public void handleInventoryAction(Player player, String actionType, int slot, Object item) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().inventoryWindowSeconds;
        int count = state.recordActionWindow("inventory", windowSeconds, now);
        state.recordInventoryInteraction(now);
        if (slot >= 0 && item != null) {
            state.recordInventorySnapshot(actionType + "@" + slot + "=" + item);
        }
        dispatch(new InventoryActionContext(player, state, actionType, slot, item, now, count));
        cleanupInactive(now);
    }

    public void handlePlacement(Player player, PlayerCheckState.Position position, String material) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().placementWindowSeconds;
        int count = state.recordActionWindow("placement", windowSeconds, now);
        state.recordPlacement(position);
        dispatch(new PlacementContext(player, state, material, position, now, count));
        cleanupInactive(now);
    }

    public void handlePayload(Player player, String channel, String preview, int sizeBytes) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        dispatch(new PayloadContext(player, state, channel, preview, sizeBytes, now));
        cleanupInactive(now);
    }

    public void handleRedstone(Player player, PlayerCheckState.Position position, int updates) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        dispatch(new RedstoneContext(player, state, updates, now, position));
        cleanupInactive(now);
    }

    public void recordFlag(Player player, String checkName, String reason, int severity, Object data) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recordFlag(checkName, reason, severity, now);
        trustScoreManager.addViolation(player.getUniqueId(), severity);

        MitigationManager.MitigationResult mitigation = mitigationManager.evaluate(player, checkName, reason, severity, state, now, data);
        String message = "[ACAC] " + player.getName() + " flagged by " + checkName + ": " + reason;
        if (mitigation.level() != PlayerCheckState.MitigationLevel.NONE) {
            message += " | Mitigation=" + mitigation.level() + " risk=" + mitigation.riskScore();
        }
        alertManager.log(message + " Data=" + data, java.util.logging.Level.WARNING);
        alertManager.alert(player.getName(), checkName, message, severity, mitigation.level());
        if (databaseManager.getPlayerDataStore() != null) {
            databaseManager.getPlayerDataStore().appendHistory(player.getUniqueId(), now + "|" + checkName + "|" + reason + "|sev=" + severity + "|mitigation=" + mitigation.level(), plugin.getConfigManager().getSettings().historyLimit);
            if (plugin.getConfigManager().getSettings().flushOnFlag) {
                databaseManager.saveSnapshot(state);
            }
        }
    }

    public Map<String, Integer> getFlagCounts(UUID playerId) {
        return getOrCreateState(playerId).getFlagCounts();
    }

    public double getTrustScore(UUID playerId) {
        return getOrCreateState(playerId).getTrustScore();
    }

    public double getPacketsPerSecond(UUID playerId) {
        PlayerCheckState state = getOrCreateState(playerId);
        long now = System.currentTimeMillis();
        int count = state.getPacketWindowCount(5, now);
        return count / 5.0;
    }

    public Optional<UUID> findPlayerId(String playerName) {
        return Optional.ofNullable(lastSeenNameToId.get(playerName.toLowerCase()));
    }

    public PlayerCheckState getOrCreateState(UUID playerId) {
        PlayerCheckState state = playerStates.computeIfAbsent(playerId, PlayerCheckState::new);
        if (restoredFromStore.putIfAbsent(playerId, Boolean.TRUE) == null && databaseManager.getPlayerDataStore() != null) {
            databaseManager.getPlayerDataStore().load(playerId).ifPresent(snapshot -> applySnapshot(state, snapshot));
        }
        return state;
    }

    public void removeState(UUID playerId) {
        playerStates.remove(playerId);
        lastSeenNameToId.values().removeIf(id -> id.equals(playerId));
    }

    public List<String> getHistory(UUID playerId, int limit) {
        if (databaseManager.getPlayerDataStore() == null) {
            return List.of();
        }
        return databaseManager.getPlayerDataStore().loadHistory(playerId, limit);
    }

    public void resetTrust(UUID playerId) {
        getOrCreateState(playerId).resetTrust();
        plugin.getAlertManager().logTrustChange("Reset trust for " + playerId);
    }

    public void clearFlags(UUID playerId) {
        getOrCreateState(playerId).clearFlags();
        plugin.getAlertManager().log("Cleared flags for " + playerId, java.util.logging.Level.INFO);
    }

    public PlayerStats getStatsForPlayer(UUID playerId) {
        PlayerCheckState state = getOrCreateState(playerId);
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        return new PlayerStats(
                state.getTrustScore(),
                state.getFlagCounts(),
                state.getFlagRecords(),
                state.getPacketWindowCount(5, now) / 5.0,
                state.isUnderMitigation(),
                state.getLastMitigationLevel(),
                state.getLastMitigationAt(),
                state.getMitigationHistory(),
                state.getLastMitigationReason()
        );
    }

    public void alertStaff(String message) {
        // TODO: Replace stub logging with broadcasting to players having "acac.notify" permission.
        plugin.getLogger().info(message);
    }

    private void recordPlayerName(Player player) {
        lastSeenNameToId.put(player.getName().toLowerCase(), player.getUniqueId());
    }

    private void applySnapshot(PlayerCheckState state, PlayerSnapshot snapshot) {
        state.restoreSnapshot(snapshot.trustScore(), snapshot.flagCounts(), snapshot.mitigationHistory());
    }

    private void dispatch(Object context) {
        for (AbstractCheck check : checks) {
            long start = System.nanoTime();
            check.handle(context);
            long elapsed = System.nanoTime() - start;
            timingNanos.computeIfAbsent(check.getCheckName(), k -> new LongAdder()).add(elapsed);
            timingCounts.computeIfAbsent(check.getCheckName(), k -> new LongAdder()).increment();
        }
    }

    public Map<String, Double> getPerformanceSnapshot() {
        Map<String, Double> snapshot = new java.util.HashMap<>();
        timingNanos.forEach((check, nanos) -> {
            long count = timingCounts.getOrDefault(check, new LongAdder()).sum();
            if (count > 0) {
                snapshot.put(check, nanos.doubleValue() / count / 1_000_000.0);
            }
        });
        return snapshot;
    }

    private void cleanupInactive(long now) {
        long cutoff = plugin.getConfigManager().getSettings().inactivePurgeMillis;
        if (cutoff <= 0) {
            return;
        }
        playerStates.entrySet().removeIf(entry -> entry.getValue().isInactive(cutoff, now));
    }

    public record PlayerStats(double trustScore,
                               Map<String, Integer> flagCounts,
                               Map<String, PlayerCheckState.FlagRecord> summaries,
                               double packetsPerSecond,
                               boolean underMitigation,
                               PlayerCheckState.MitigationLevel lastMitigation,
                               long lastMitigationAt,
                               java.util.List<String> mitigationHistory,
                               String mitigationNote) {
        public String riskFor(String check, int severity) {
            double trust = trustScore;
            int flags = flagCounts.getOrDefault(check, 0);
            if (trust < 25 || severity >= 3 || flags >= 5) {
                return "HIGH";
            }
            if (trust < 60 || flags >= 2) {
                return "MED";
            }
            return "LOW";
        }
    }
}
