package com.yourcompany.uac.packet;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.checks.checktypes.ConsoleSpamCheck;
import com.yourcompany.uac.checks.checktypes.EntityOverloadCheck;
import com.yourcompany.uac.checks.checktypes.InvalidPacketCheck;
import com.yourcompany.uac.checks.checktypes.InvalidTeleportCheck;
import com.yourcompany.uac.checks.checktypes.PacketRateLimiterCheck;
import com.yourcompany.uac.checks.checktypes.inventory.InventoryDupeCheck;
import com.yourcompany.uac.checks.checktypes.network.AntiCheatDisablerCheck;
import com.yourcompany.uac.checks.checktypes.network.NettyCrashProtectionCheck;
import com.yourcompany.uac.checks.checktypes.payload.InvalidSignPayloadCheck;
import com.yourcompany.uac.checks.checktypes.world.InvalidPlacementCheck;
import com.yourcompany.uac.checks.checktypes.world.RedstoneExploitCheck;
import com.yourcompany.uac.integration.ProtocolLibHook;

/**
 * Registers packet interception according to available libraries.
 */
public class PacketListenerManager {

    private final UltimateAntiCheatPlugin plugin;
    private final CheckManager checkManager;
    private final com.yourcompany.uac.integration.IntegrationService integrationService;

    public PacketListenerManager(UltimateAntiCheatPlugin plugin, CheckManager checkManager, com.yourcompany.uac.integration.IntegrationService integrationService) {
        this.plugin = plugin;
        this.checkManager = checkManager;
        this.integrationService = integrationService;
    }

    public void registerListeners() {
        PacketInterceptor interceptor = new PacketInterceptor(checkManager);
        integrationService.getPacketBridge().bind(interceptor);

        // Register Bukkit/Paper event listeners for non-packet checks.
        integrationService.getEventBridge().register(checkManager);

        // Register built-in packet checks (rate limiter, invalid packets, etc.)
        interceptor.registerCheck(new PacketRateLimiterCheck(plugin));
        interceptor.registerCheck(new InvalidPacketCheck(plugin));
        interceptor.registerCheck(new InvalidTeleportCheck(plugin));
        interceptor.registerCheck(new EntityOverloadCheck(plugin));
        interceptor.registerCheck(new ConsoleSpamCheck(plugin));
        interceptor.registerCheck(new InventoryDupeCheck(plugin));
        interceptor.registerCheck(new InvalidPlacementCheck(plugin));
        interceptor.registerCheck(new InvalidSignPayloadCheck(plugin));
        interceptor.registerCheck(new RedstoneExploitCheck(plugin));
        interceptor.registerCheck(new AntiCheatDisablerCheck(plugin));
        interceptor.registerCheck(new NettyCrashProtectionCheck(plugin));

    }
}
