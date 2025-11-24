package com.yourcompany.uac.integration;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.packet.PacketInterceptor;
import org.bukkit.entity.Player;

/**
 * Bridges ProtocolLib packet events to the plugin's internal interceptor.
 */
public class ProtocolLibHook {

    private final UltimateAntiCheatPlugin plugin;
    private ProtocolManager protocolManager;

    public ProtocolLibHook(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void bind(PacketInterceptor interceptor) {
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.getInstance()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                interceptor.handleIncoming(player, event.getPacket());
                // When built against real ProtocolLib, extract movement data to feed movement-aware checks.
                feedMovementIfPresent(player, event);
            }
        });
    }

    private void feedMovementIfPresent(Player player, PacketEvent event) {
        if (player == null || event == null || event.getPacket() == null) {
            return;
        }
        PlayerCheckState.Position position = extractPosition(event.getPacket(), player);
        if (position != null) {
            boolean teleportAck = isTeleportAck(event);
            plugin.getCheckManager().handleMovement(player, position.x(), position.y(), position.z(), teleportAck);
        }
    }

    private boolean isTeleportAck(PacketEvent event) {
        String name = event.getPacketType() != null ? event.getPacketType().name() : "";
        Object packet = event.getPacket();
        String packetName = packet != null ? packet.getClass().getSimpleName().toLowerCase() : "";
        return name.toLowerCase().contains("teleport") || packetName.contains("teleport");
    }

    private PlayerCheckState.Position extractPosition(Object packet, Player player) {
        try {
            // ProtocolLib PacketContainer exposes getters; fallback to reflection for safety in this stub environment.
            double x = readNumeric(packet, "getX", "x", player != null && player.getLocation() != null ? player.getLocation().getX() : 0);
            double y = readNumeric(packet, "getY", "y", player != null && player.getLocation() != null ? player.getLocation().getY() : 0);
            double z = readNumeric(packet, "getZ", "z", player != null && player.getLocation() != null ? player.getLocation().getZ() : 0);
            return new PlayerCheckState.Position(x, y, z);
        } catch (Exception e) {
            plugin.getLogger().fine("[ACAC] Could not extract position from packet: " + e.getMessage());
            return null;
        }
    }

    private double readNumeric(Object packet, String methodName, String fieldName, double fallback) {
        try {
            java.lang.reflect.Method method = packet.getClass().getMethod(methodName);
            Object value = method.invoke(packet);
            if (value instanceof Number num) {
                return num.doubleValue();
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Field field = packet.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(packet);
            if (value instanceof Number num) {
                return num.doubleValue();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
