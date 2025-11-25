package com.yourcompany.uac.mitigation;

/**
 * Simple extension hook for alert destinations (console, staff chat, webhooks, structured logs).
 */
public interface AlertSink {
    void send(AlertEvent event);
}
