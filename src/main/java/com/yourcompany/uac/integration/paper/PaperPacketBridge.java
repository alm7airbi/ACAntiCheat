package com.yourcompany.uac.integration.paper;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.integration.ProtocolLibHook;
import com.yourcompany.uac.integration.bridge.PacketBridge;
import com.yourcompany.uac.packet.PacketInterceptor;

/**
 * Real binding that uses ProtocolLib when available. Compiles against the
 * bundled stubs here, but is intended to run with Paper + ProtocolLib.
 */
public class PaperPacketBridge implements PacketBridge {

    private final UltimateAntiCheatPlugin plugin;

    public PaperPacketBridge(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void bind(PacketInterceptor interceptor) {
        new ProtocolLibHook(plugin).bind(interceptor);
    }

    @Override
    public String name() {
        return "paper-protocollib";
    }
}
