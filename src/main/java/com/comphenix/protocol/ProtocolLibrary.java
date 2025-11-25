package com.comphenix.protocol;

// Stub-only ProtocolLib facade for offline builds; excluded when -PrealPaper is used.

public final class ProtocolLibrary {
    private ProtocolLibrary() {}

    public static ProtocolManager getProtocolManager() {
        return new ProtocolManager();
    }
}
