package com.comphenix.protocol;

public class PacketType {
    public static class Play {
        public static class Client {
            public static PacketType getInstance() {
                return new PacketType();
            }
        }
    }
}
