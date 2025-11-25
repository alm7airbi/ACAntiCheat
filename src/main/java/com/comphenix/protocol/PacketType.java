package com.comphenix.protocol;

// Stub-only PacketType for offline builds; excluded when real ProtocolLib is present.

public class PacketType {
    private final String name;

    public PacketType() {
        this.name = "generic";
    }

    public PacketType(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public boolean is(PacketType other) {
        return other != null && other.name().equalsIgnoreCase(this.name);
    }

    @Override
    public String toString() {
        return name;
    }

    public static class Play {
        public static class Client {
            public static final PacketType FLYING = new PacketType("FLYING");
            public static final PacketType POSITION = new PacketType("POSITION");
            public static final PacketType POSITION_LOOK = new PacketType("POSITION_LOOK");
            public static final PacketType LOOK = new PacketType("LOOK");
            public static final PacketType TELEPORT_ACCEPT = new PacketType("TELEPORT_ACCEPT");
            public static final PacketType BLOCK_PLACE = new PacketType("BLOCK_PLACE");
            public static final PacketType USE_ITEM = new PacketType("USE_ITEM");
            public static final PacketType USE_ENTITY = new PacketType("USE_ENTITY");
            public static final PacketType ARM_ANIMATION = new PacketType("ARM_ANIMATION");
            public static final PacketType HELD_ITEM_SLOT = new PacketType("HELD_ITEM_SLOT");
            public static final PacketType CLIENT_COMMAND = new PacketType("CLIENT_COMMAND");
            public static final PacketType CUSTOM_PAYLOAD = new PacketType("CUSTOM_PAYLOAD");
            public static final PacketType WINDOW_CLICK = new PacketType("WINDOW_CLICK");
            public static final PacketType SET_CREATIVE_SLOT = new PacketType("SET_CREATIVE_SLOT");
            public static final PacketType UPDATE_SIGN = new PacketType("UPDATE_SIGN");

            public static PacketType getInstance() {
                return new PacketType("PLAY_CLIENT");
            }
        }
    }
}
