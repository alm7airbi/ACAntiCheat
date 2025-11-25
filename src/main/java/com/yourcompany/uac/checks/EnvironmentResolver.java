package com.yourcompany.uac.checks;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.context.EnvironmentSnapshot;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Resolves environmental data (TPS/ping/protocol) using reflection-friendly
 * lookups so the code remains compatible with stub builds while taking
 * advantage of Paper/ProtocolLib at runtime.
 */
public final class EnvironmentResolver {

    private EnvironmentResolver() {
    }

    public static EnvironmentSnapshot capture(UltimateAntiCheatPlugin plugin, Player player) {
        double tps = resolveTps(plugin.getServer());
        int ping = resolvePing(player);
        int protocol = resolveProtocolVersion(player);
        return new EnvironmentSnapshot(tps, ping, protocol);
    }

    private static double resolveTps(Server server) {
        if (server == null) {
            return 20.0;
        }
        try {
            var method = server.getClass().getMethod("getTPS");
            Object value = method.invoke(server);
            if (value instanceof double[] array && array.length > 0) {
                return array[0];
            }
        } catch (Exception ignored) {
        }
        try {
            var method = server.getClass().getMethod("getAverageTps");
            Object value = method.invoke(server);
            if (value instanceof Double d) {
                return d;
            }
        } catch (Exception ignored) {
        }
        return 20.0; // best-effort fallback for stubs/offline builds
    }

    private static int resolvePing(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            var method = player.getClass().getMethod("getPing");
            Object value = method.invoke(player);
            if (value instanceof Number num) {
                return num.intValue();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static int resolveProtocolVersion(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            Class<?> protocolLibrary = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object manager = protocolLibrary.getMethod("getProtocolManager").invoke(null);
            Object version = manager.getClass().getMethod("getProtocolVersion", Player.class).invoke(manager, player);
            if (version instanceof Number num) {
                return num.intValue();
            }
        } catch (ClassNotFoundException ignored) {
            // ProtocolLib not present in stub/offline builds
        } catch (Exception e) {
            // keep silent but allow fallback
        }
        try {
            // ViaVersion API: Via.getAPI().getPlayerVersion(UUID)
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = viaClass.getMethod("getAPI").invoke(null);
            Object version = api.getClass().getMethod("getPlayerVersion", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
            if (version instanceof Number num) {
                return num.intValue();
            }
        } catch (ClassNotFoundException ignored) {
            // ViaVersion absent; fall through
        } catch (Exception ignored) {
        }
        return 0;
    }
}
