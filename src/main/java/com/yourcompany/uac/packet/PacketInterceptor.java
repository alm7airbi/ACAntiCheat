package com.yourcompany.uac.packet;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.AbstractCheck;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple holder for checks that should react to packet events. The concrete
 * binding to ProtocolLib/PacketEvents lives in the integration hooks.
 */
public class PacketInterceptor {

    private final UltimateAntiCheatPlugin plugin;
    private final List<AbstractCheck> checks = new ArrayList<>();

    public PacketInterceptor(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleIncoming(Player player, Object packet) {
        for (AbstractCheck check : checks) {
            check.handle(new PacketPayload(player, packet));
        }
    }

    public void registerCheck(AbstractCheck check) {
        this.checks.add(check);
    }

    public List<AbstractCheck> getChecks() {
        return checks;
    }
}
