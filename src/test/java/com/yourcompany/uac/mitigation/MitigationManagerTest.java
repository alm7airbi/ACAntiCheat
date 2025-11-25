package com.yourcompany.uac.mitigation;

import com.yourcompany.uac.TestPluginHarness;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.config.Settings;
import com.yourcompany.uac.integration.bridge.MitigationActions;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MitigationManagerTest {

    private RecordingMitigationActions actions;
    private MitigationManager mitigationManager;
    private PlayerCheckState state;
    private Player player;
    private TestPluginHarness plugin;

    @BeforeEach
    void setup() {
        Settings settings = Settings.fromYaml(new org.bukkit.configuration.file.YamlConfiguration());
        settings.configVersion = Settings.CURRENT_CONFIG_VERSION;
        settings.warnThreshold = 0.1;
        settings.rollbackThreshold = 0.2;
        settings.throttleThreshold = 0.3;
        settings.rubberBandThreshold = 0.4;
        settings.temporaryKickThreshold = 0.6;
        settings.temporaryBanThreshold = 1.5;
        settings.banSuggestThreshold = 2.0;
        settings.minViolationsBeforeMitigation = 0;

        this.plugin = new TestPluginHarness(settings, new File("build/test-data/mitigation" + System.nanoTime()));
        this.actions = new RecordingMitigationActions();
        this.mitigationManager = new MitigationManager(plugin, actions);
        this.state = new PlayerCheckState(UUID.randomUUID());
        this.state.restoreSnapshot(70, java.util.Collections.emptyMap(), java.util.Collections.emptyList());
        this.player = new Player("Mitigator", state.getPlayerId());
    }

    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        plugin.shutdown();
    }

    @Test
    void escalatesThroughLadder() {
        MitigationManager.MitigationResult result = mitigationManager.evaluate(player, "TestCheck", "severe", 2, state, System.currentTimeMillis(), null);
        assertEquals(PlayerCheckState.MitigationLevel.KICK, result.level());
        assertTrue(actions.kicked, "kick should be issued for elevated risk");
    }

    @Test
    void respectsCooldownSuppression() throws InterruptedException {
        mitigationManager.evaluate(player, "TestCheck", "first", 3, state, System.currentTimeMillis(), null);
        MitigationManager.MitigationResult second = mitigationManager.evaluate(player, "TestCheck", "repeat", 3, state, System.currentTimeMillis(), null);
        assertEquals(PlayerCheckState.MitigationLevel.NONE, second.level(), "cooldown should suppress immediate repeats");
    }

    private static class RecordingMitigationActions implements MitigationActions {
        boolean warned;
        boolean rolledBack;
        boolean throttled;
        boolean rubberBanded;
        boolean kicked;
        boolean tempBanned;
        boolean permBanned;

        @Override
        public void warn(Player player, String checkName, String reason) { warned = true; }

        @Override
        public void cancelAction(Player player, String checkName, String reason) {}

        @Override
        public void rollbackPlacement(Player player, String checkName, String reason) { rolledBack = true; }

        @Override
        public void rollbackInventory(Player player, String checkName, String reason) { rolledBack = true; }

        @Override
        public void throttle(Player player, String checkName, String reason) { throttled = true; }

        @Override
        public void clearEntitiesNear(Player player, String checkName, int radius, String reason) {}

        @Override
        public void temporaryKick(Player player, String checkName, String reason) { kicked = true; }

        @Override
        public void temporaryBan(Player player, String checkName, String reason) { tempBanned = true; }

        @Override
        public void permanentBan(Player player, String checkName, String reason) { permBanned = true; }

        @Override
        public void rubberBand(Player player, String checkName, com.yourcompany.uac.checks.PlayerCheckState.Position lastPosition, String reason) { rubberBanded = true; }
    }
}

