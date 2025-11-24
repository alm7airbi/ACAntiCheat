package com.yourcompany.uac.integration.bridge;

import com.yourcompany.uac.checks.CheckManager;

/**
 * Bridges Bukkit/Paper events to the CheckManager. The stub implementation is
 * a no-op while the Paper implementation registers real listeners.
 */
public interface EventBridge {
    void register(CheckManager checkManager);

    String name();
}
