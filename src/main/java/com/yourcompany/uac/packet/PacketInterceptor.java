package com.yourcompany.uac.packet;

import com.yourcompany.uac.checks.AbstractCheck;
import com.yourcompany.uac.checks.CheckManager;
import org.bukkit.entity.Player;

/**
 * Simple holder for checks that should react to packet events. The concrete
 * binding to ProtocolLib/PacketEvents lives in the integration hooks.
 */
public class PacketInterceptor {

    private final CheckManager checkManager;

    public PacketInterceptor(CheckManager checkManager) {
        this.checkManager = checkManager;
    }

    public void handleIncoming(Player player, Object packet) {
        checkManager.handlePacket(new PacketPayload(player, packet));
    }

    public void registerCheck(AbstractCheck check) {
        this.checkManager.registerCheck(check);
    }
}
