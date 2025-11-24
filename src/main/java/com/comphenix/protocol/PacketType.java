package com.comphenix.protocol;

// TODO: Replace this stub with the real ProtocolLib PacketType when dependencies are available.

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

            public static PacketType getInstance() {
                return new PacketType("PLAY_CLIENT");
            }
        }
    }
}
