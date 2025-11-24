package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<UUID, PlayerCheckState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, UUID> lastSeenNameToId = new ConcurrentHashMap<>();

    public CheckManager(UltimateAntiCheatPlugin plugin, TrustScoreManager trustScoreManager) {
        this.plugin = plugin;
        this.trustScoreManager = trustScoreManager;
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
    }

    public void handleMovement(Player player, double x, double y, double z, boolean serverTeleport) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int packetsLastSecond = state.recordPacketInWindow(1, now);
        int packetsLastFiveSeconds = state.recordPacketInWindow(5, now);
        state.recordMovement(PlayerCheckState.position(x, y, z), now, serverTeleport);

        MovementContext context = new MovementContext(player, null, state, now, packetsLastSecond, packetsLastFiveSeconds, x, y, z, true, serverTeleport);
        dispatch(context);
    }

    public void handleEntityAction(Player player, String actionType) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().entityWindowSeconds;
        int count = state.recordEntityWindow(windowSeconds, now);
        dispatch(new EntityActionContext(player, state, actionType, now, count));
    }

    public void handleConsoleMessage(Player player, String message) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().consoleWindowSeconds;
        int count = state.recordConsoleWindow(windowSeconds, now);
        dispatch(new ConsoleMessageContext(player, state, message, now, count));
    }

    public void handleInventoryAction(Player player, String actionType, int slot, Object item) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().inventoryWindowSeconds;
        int count = state.recordActionWindow("inventory", windowSeconds, now);
        dispatch(new InventoryActionContext(player, state, actionType, slot, item, now, count));
    }

    public void handlePlacement(Player player, PlayerCheckState.Position position, String material) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        int windowSeconds = plugin.getConfigManager().getSettings().placementWindowSeconds;
        int count = state.recordActionWindow("placement", windowSeconds, now);
        dispatch(new PlacementContext(player, state, material, position, now, count));
    }

    public void handlePayload(Player player, String channel, String preview, int sizeBytes) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        dispatch(new PayloadContext(player, state, channel, preview, sizeBytes, now));
    }

    public void handleRedstone(Player player, PlayerCheckState.Position position, int updates) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recoverTrust(now);
        dispatch(new RedstoneContext(player, state, updates, now, position));
    }

    public void recordFlag(Player player, String checkName, String reason, int severity, Object data) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        long now = System.currentTimeMillis();
        state.recordFlag(checkName, reason, severity, now);
        trustScoreManager.addViolation(player.getUniqueId(), severity);

        String mitigation = applyMitigation(state, checkName, severity, now);
        String message = "[ACAC] " + player.getName() + " flagged by " + checkName + ": " + reason;
        if (!"NONE".equals(mitigation)) {
            message += " | Mitigation=" + mitigation;
        }
        plugin.getLogger().warning(message + " Data=" + data);
        alertStaff(message);
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
        return playerStates.computeIfAbsent(playerId, PlayerCheckState::new);
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
                state.getLastMitigationAt()
        );
    }

    public void alertStaff(String message) {
        // TODO: Replace stub logging with broadcasting to players having "acac.notify" permission.
        plugin.getLogger().info(message);
    }

    private void recordPlayerName(Player player) {
        lastSeenNameToId.put(player.getName().toLowerCase(), player.getUniqueId());
    }

    private void dispatch(Object context) {
        for (AbstractCheck check : checks) {
            check.handle(context);
        }
    }

    public record PlayerStats(double trustScore,
                               Map<String, Integer> flagCounts,
                               Map<String, PlayerCheckState.FlagRecord> summaries,
                               double packetsPerSecond,
                               boolean underMitigation,
                               PlayerCheckState.MitigationLevel lastMitigation,
                               long lastMitigationAt) {
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

    private String applyMitigation(PlayerCheckState state, String checkName, int severity, long now) {
        int flags = state.getFlagCounts().getOrDefault(checkName, 0);
        double trust = state.getTrustScore();

        PlayerCheckState.MitigationLevel level = PlayerCheckState.MitigationLevel.NONE;
        if (trust < 15 || severity >= 4 || flags >= 8) {
            level = PlayerCheckState.MitigationLevel.HARD;
        } else if (trust < 35 || severity >= 3 || flags >= 5) {
            level = PlayerCheckState.MitigationLevel.MEDIUM;
        } else if (trust < 60 || flags >= 2) {
            level = PlayerCheckState.MitigationLevel.SOFT;
        }

        // Avoid spamming repeated mitigations
        long sinceLast = now - state.getLastMitigationAt();
        if (sinceLast < 2000 && level.ordinal() <= state.getLastMitigationLevel().ordinal()) {
            level = PlayerCheckState.MitigationLevel.NONE;
        }

        if (level != PlayerCheckState.MitigationLevel.NONE) {
            state.recordMitigation(level, now);
            switch (level) {
                case SOFT -> performSoftAction(checkName);
                case MEDIUM -> performMediumAction(checkName);
                case HARD -> performHardAction(checkName);
                default -> {
                }
            }
        }
        return level.name();
    }

    private void performSoftAction(String checkName) {
        // TODO: send staff-only warning message via Paper audiences.
        plugin.getLogger().info("[ACAC] Soft mitigation queued for " + checkName);
    }

    private void performMediumAction(String checkName) {
        // TODO: cancel the offending action or rollback container/placement when using real server APIs.
        plugin.getLogger().warning("[ACAC] Medium mitigation placeholder for " + checkName);
    }

    private void performHardAction(String checkName) {
        // TODO: temporarily kick/ban via Bukkit API once real server is available.
        plugin.getLogger().warning("[ACAC] Hard mitigation placeholder for " + checkName);
    }
}
