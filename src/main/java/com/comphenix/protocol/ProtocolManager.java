package com.comphenix.protocol;

// Stub-only ProtocolManager for offline builds; real ProtocolLib is pulled when -PrealPaper is enabled.

import com.comphenix.protocol.events.PacketAdapter;

public class ProtocolManager {
    public void addPacketListener(PacketAdapter adapter) {
        // no-op stub
    }

    public int getProtocolVersion(org.bukkit.entity.Player player) {
        return 0;
    }
}
