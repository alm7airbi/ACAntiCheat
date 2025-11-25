package com.yourcompany.uac;

import com.yourcompany.uac.checks.checktypes.PacketRateLimiterCheck;
import com.yourcompany.uac.config.Settings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PluginSmokeTest {

    @Test
    void endToEndFlaggingUpdatesState() {
        Settings settings = Settings.fromYaml(new org.bukkit.configuration.file.YamlConfiguration());
        settings.packetRateLimitPerSecond = 1;
        settings.packetRateLimitPerFiveSeconds = 1;
        settings.packetRateKickThreshold = 3;
        settings.mitigationCooldownMillis = 0;
        settings.minViolationsBeforeMitigation = 0;
        settings.configVersion = Settings.CURRENT_CONFIG_VERSION;

        TestPluginHarness plugin = new TestPluginHarness(settings, new File("build/test-data/smoke" + System.nanoTime()));
        plugin.getCheckManager().registerCheck(new PacketRateLimiterCheck(plugin));

        Player player = new Player("Smoke", UUID.randomUUID());
        plugin.getIntegrationService(); // ensure integration initializes

        for (int i = 0; i < 4; i++) {
            plugin.getCheckManager().handlePacket(new com.yourcompany.uac.packet.PacketPayload(player, new Object()));
        }

        var stats = plugin.getCheckManager().getStatsForPlayer(player.getUniqueId());
        assertTrue(stats.flagCounts().getOrDefault("PacketRateLimiter", 0) > 0, "flags should be recorded");
        assertFalse(stats.mitigationHistory().isEmpty(), "mitigation history should be populated after flags");
        plugin.shutdown();
    }
}

