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
import org.bukkit.scheduler.BukkitScheduler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Bridges ProtocolLib packet events to the plugin's internal interceptor.
 */
public class ProtocolLibHook {

    private final UltimateAntiCheatPlugin plugin;
    private ProtocolManager protocolManager;
    private PacketInterceptor interceptor;

    public ProtocolLibHook(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void bind(PacketInterceptor interceptor) {
        this.interceptor = interceptor;
        try {
            protocolManager = ProtocolLibrary.getProtocolManager();
        } catch (Exception e) {
            plugin.getLogger().warning("[ACAC] Unable to acquire ProtocolLib ProtocolManager: " + e.getMessage());
            return;
        }
        registerMovementListeners();
        registerInteractionListeners();
        registerPayloadListeners();
        registerCatchAllListener();
    }

    private void registerMovementListeners() {
        PacketType[] movementTypes = new PacketType[]{
                PacketType.Play.Client.FLYING,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK,
                PacketType.Play.Client.LOOK,
                PacketType.Play.Client.TELEPORT_ACCEPT
        };
        protocolManager.addPacketListener(new PacketAdapter(plugin, movementTypes) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                PlayerCheckState.Position position = extractPosition(event.getPacket(), player);
                boolean teleportAck = isTeleportAck(event);
                schedule(() -> {
                    interceptor.handleIncoming(player, event.getPacket());
                    if (position != null) {
                    ProtocolLibHook.this.plugin.getCheckManager().handleMovement(player, position.x(), position.y(), position.z(), teleportAck);
                    }
                });
            }
        });
    }

    private void registerInteractionListeners() {
        PacketType[] interactionTypes = new PacketType[]{
                PacketType.Play.Client.BLOCK_PLACE,
                PacketType.Play.Client.USE_ITEM,
                PacketType.Play.Client.USE_ENTITY,
                PacketType.Play.Client.ARM_ANIMATION,
                PacketType.Play.Client.HELD_ITEM_SLOT,
                PacketType.Play.Client.CLIENT_COMMAND
        };
        protocolManager.addPacketListener(new PacketAdapter(plugin, interactionTypes) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                schedule(() -> {
                    interceptor.handleIncoming(player, event.getPacket());
                    ProtocolLibHook.this.plugin.getCheckManager().handlePlacement(player,
                            PlayerCheckState.position(
                                    player.getLocation().getX(),
                                    player.getLocation().getY(),
                                    player.getLocation().getZ()),
                            name(event));
                });
            }
        });
    }

    private void registerPayloadListeners() {
        PacketType[] payloadTypes = new PacketType[]{
                PacketType.Play.Client.CUSTOM_PAYLOAD,
                PacketType.Play.Client.UPDATE_SIGN,
                PacketType.Play.Client.WINDOW_CLICK,
                PacketType.Play.Client.SET_CREATIVE_SLOT
        };
        protocolManager.addPacketListener(new PacketAdapter(plugin, payloadTypes) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                String channel = name(event);
                int size = readLength(event.getPacket());
                schedule(() -> {
                    interceptor.handleIncoming(player, event.getPacket());
                    ProtocolLibHook.this.plugin.getCheckManager().handlePayload(player, channel, channel, size);
                });
            }
        });
    }

    private void registerCatchAllListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.getInstance()) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) {
                    return;
                }
                schedule(() -> interceptor.handleIncoming(player, event.getPacket()));
            }
        });
    }

    private void schedule(Runnable runnable) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        if (scheduler != null) {
            scheduler.runTask(plugin, runnable);
        } else {
            runnable.run();
        }
    }

    private boolean isTeleportAck(PacketEvent event) {
        String name = name(event).toLowerCase();
        Object packet = event.getPacket();
        String packetName = packet != null ? packet.getClass().getSimpleName().toLowerCase() : "";
        return name.contains("teleport") || packetName.contains("teleport");
    }

    private PlayerCheckState.Position extractPosition(Object packet, Player player) {
        try {
            // ProtocolLib PacketContainer exposes getters; fallback to reflection for safety in this stub environment.
            double x = readNumeric(packet, new String[]{"getX", "readDouble", "getDouble"}, new String[]{"x"}, player != null && player.getLocation() != null ? player.getLocation().getX() : 0);
            double y = readNumeric(packet, new String[]{"getY", "readDouble", "getDouble"}, new String[]{"y"}, player != null && player.getLocation() != null ? player.getLocation().getY() : 0);
            double z = readNumeric(packet, new String[]{"getZ", "readDouble", "getDouble"}, new String[]{"z"}, player != null && player.getLocation() != null ? player.getLocation().getZ() : 0);
            return new PlayerCheckState.Position(x, y, z);
        } catch (Exception e) {
            plugin.getLogger().fine("[ACAC] Could not extract position from packet: " + e.getMessage());
            return null;
        }
    }

    private String name(PacketEvent event) {
        if (event == null || event.getPacketType() == null) {
            return "unknown";
        }
        return event.getPacketType().name();
    }

    private double readNumeric(Object packet, String[] methodNames, String[] fieldNames, double fallback) {
        Set<String> attempts = new HashSet<>();
        if (packet == null) {
            return fallback;
        }
        for (String methodName : methodNames) {
            if (methodName == null || attempts.contains(methodName)) {
                continue;
            }
            attempts.add(methodName);
            try {
                Method method = packet.getClass().getMethod(methodName);
                Object value = method.invoke(packet);
                if (value instanceof Number num) {
                    return num.doubleValue();
                }
            } catch (Exception ignored) {
            }
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null || attempts.contains(fieldName)) {
                continue;
            }
            attempts.add(fieldName);
            try {
                Field field = packet.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value instanceof Number num) {
                    return num.doubleValue();
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private int readLength(Object packet) {
        if (packet == null) {
            return 0;
        }
        try {
            Method method = packet.getClass().getMethod("getLength");
            Object value = method.invoke(packet);
            if (value instanceof Number num) {
                return num.intValue();
            }
        } catch (Exception ignored) {
        }
        for (String fieldName : Arrays.asList("length", "size", "dataLength")) {
            try {
                Field field = packet.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(packet);
                if (value instanceof Number num) {
                    return num.intValue();
                }
            } catch (Exception ignored) {
            }
        }
        return 0;
    }
}
