package com.yourcompany.uac.checks;

import com.yourcompany.uac.TestPluginHarness;
import com.yourcompany.uac.config.Settings;
import com.yourcompany.uac.checks.checktypes.InvalidTeleportCheck;
import com.yourcompany.uac.checks.checktypes.PacketRateLimiterCheck;
import com.yourcompany.uac.checks.checktypes.inventory.InventoryDupeCheck;
import com.yourcompany.uac.checks.context.EnvironmentSnapshot;
import com.yourcompany.uac.checks.context.MovementContext;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckManagerTest {

    private TestPluginHarness plugin;
    private CheckManager manager;
    private Player player;
    private Settings settings;

    @BeforeEach
    void setup() {
        settings = Settings.fromYaml(new org.bukkit.configuration.file.YamlConfiguration());
        settings.packetRateLimiterEnabled = true;
        settings.packetRateLimitPerSecond = 3;
        settings.packetRateLimitPerFiveSeconds = 3;
        settings.packetRateKickThreshold = 6;
        settings.invalidPacketEnabled = false;
        settings.invalidTeleportEnabled = true;
        settings.invalidTeleportMaxDistance = 10;
        settings.teleportExemptMillis = 50;
        settings.inventoryExploitEnabled = true;
        settings.inventoryActionsPerWindow = 2;
        settings.inventoryWindowSeconds = 5;
        settings.maxInventorySlotIndex = 100;
        settings.configVersion = Settings.CURRENT_CONFIG_VERSION;

        File dataDir = new File("build/test-data/check-manager" + System.nanoTime());
        dataDir.mkdirs();
        plugin = new TestPluginHarness(settings, dataDir);
        manager = plugin.getCheckManager();
        manager.registerCheck(new PacketRateLimiterCheck(plugin));
        manager.registerCheck(new InvalidTeleportCheck(plugin));
        manager.registerCheck(new InventoryDupeCheck(plugin));
        player = new Player("Tester", UUID.randomUUID());
    }

    @AfterEach
    void cleanup() {
        plugin.shutdown();
    }

    @Test
    void packetRateLimiterFlagsBurstTraffic() {
        for (int i = 0; i < 5; i++) {
            manager.handlePacket(new com.yourcompany.uac.packet.PacketPayload(player, new Object()));
        }
        Map<String, Integer> flags = manager.getStatsForPlayer(player.getUniqueId()).flagCounts();
        assertTrue(flags.getOrDefault("PacketRateLimiter", 0) > 0, "burst traffic should flag packet rate limiter");
    }

    @Test
    void invalidTeleportFlagsLargeJump() {
        settings.teleportExemptMillis = 0;
        settings.invalidTeleportLagBuffer = 0;
        PlayerCheckState state = new PlayerCheckState(player.getUniqueId());
        state.recordMovement(PlayerCheckState.position(0, 64, 0), System.currentTimeMillis() - 1000, false);
        injectState(state);

        InvalidTeleportCheck check = new InvalidTeleportCheck(plugin);
        check.attachCheckManager(manager);
        EnvironmentSnapshot environment = new EnvironmentSnapshot(20.0, 0, 754);
        MovementContext context = new MovementContext(player, new Object(), state, System.currentTimeMillis(), 0, 0,
                100, 64, 0, true, false, 0, settings.chunkWindowSeconds, environment);
        check.handle(context);

        Map<String, Integer> flags = manager.getStatsForPlayer(player.getUniqueId()).flagCounts();
        assertTrue(flags.getOrDefault("InvalidTeleport", 0) > 0, "large non-server teleport should be flagged");
    }

    private void injectState(PlayerCheckState state) {
        try {
            Field field = CheckManager.class.getDeclaredField("playerStates");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, PlayerCheckState> map = (Map<UUID, PlayerCheckState>) field.get(manager);
            map.put(state.getPlayerId(), state);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void inventoryDupeCheckFlagsSpam() {
        for (int i = 0; i < 3; i++) {
            manager.handleInventoryAction(player, "click", 1, "item");
        }
        Map<String, Integer> flags = manager.getStatsForPlayer(player.getUniqueId()).flagCounts();
        assertTrue(flags.getOrDefault("InventoryDupeCheck", 0) > 0, "inventory spam should be flagged");
    }
}

