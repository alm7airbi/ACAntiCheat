package com.yourcompany.uac.integration.bridge;

import com.yourcompany.uac.packet.PacketInterceptor;

/**
 * Abstraction for packet interception (ProtocolLib, PacketEvents, or a stub).
 */
public interface PacketBridge {
    void bind(PacketInterceptor interceptor);

    String name();
}
