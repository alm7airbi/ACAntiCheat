package com.yourcompany.uac.packet;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.checks.checktypes.ConsoleSpamCheck;
import com.yourcompany.uac.checks.checktypes.EntityOverloadCheck;
import com.yourcompany.uac.checks.checktypes.InvalidPacketCheck;
import com.yourcompany.uac.checks.checktypes.InvalidTeleportCheck;
import com.yourcompany.uac.checks.checktypes.PacketRateLimiterCheck;
import com.yourcompany.uac.integration.ProtocolLibHook;

/**
 * Registers packet interception according to available libraries.
 */
public class PacketListenerManager {

    private final UltimateAntiCheatPlugin plugin;
    private final CheckManager checkManager;

    public PacketListenerManager(UltimateAntiCheatPlugin plugin, CheckManager checkManager) {
        this.plugin = plugin;
        this.checkManager = checkManager;
    }

    public void registerListeners() {
        PacketInterceptor interceptor = new PacketInterceptor(checkManager);
        plugin.getHookManager().registerPacketInterceptor(interceptor);

        // Register built-in packet checks (rate limiter, invalid packets, etc.)
        interceptor.registerCheck(new PacketRateLimiterCheck(plugin));
        interceptor.registerCheck(new InvalidPacketCheck(plugin));
        interceptor.registerCheck(new InvalidTeleportCheck(plugin));
        interceptor.registerCheck(new EntityOverloadCheck(plugin));
        interceptor.registerCheck(new ConsoleSpamCheck(plugin));
        // TODO: add NettyCrashProtectionCheck when real networking hooks are available.

        // ProtocolLib example registration
        if (plugin.getHookManager().getProtocolLibHook() != null) {
            ProtocolLibHook protocolLibHook = plugin.getHookManager().getProtocolLibHook();
            protocolLibHook.bind(interceptor);
        }
    }
}
