package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.context.ConsoleMessageContext;
import com.yourcompany.uac.checks.context.EntityActionContext;
import com.yourcompany.uac.checks.context.MovementContext;
import com.yourcompany.uac.checks.context.PacketContext;
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

    public void recordFlag(Player player, String checkName, String reason, int severity, Object data) {
        PlayerCheckState state = getOrCreateState(player.getUniqueId());
        state.recordFlag(checkName, severity);
        trustScoreManager.addViolation(player.getUniqueId(), severity);

        String message = "[ACAC] " + player.getName() + " flagged by " + checkName + ": " + reason;
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
                state.getPacketWindowCount(5, now) / 5.0,
                state.isUnderMitigation()
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

    public record PlayerStats(double trustScore, Map<String, Integer> flagCounts,
                              double packetsPerSecond, boolean underMitigation) {
    }
}
