package com.comphenix.protocol;

public final class ProtocolLibrary {
    private ProtocolLibrary() {}

    public static ProtocolManager getProtocolManager() {
        return new ProtocolManager();
    }
}
