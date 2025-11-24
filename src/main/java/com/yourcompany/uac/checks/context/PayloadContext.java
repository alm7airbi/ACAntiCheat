package com.yourcompany.uac.checks.context;

import com.yourcompany.uac.checks.PlayerCheckState;
import org.bukkit.entity.Player;

/**
 * Represents arbitrary client payloads such as sign updates or plugin
 * messages. Size and content sanity are validated by specific checks.
 */
public class PayloadContext {
    private final Player player;
    private final PlayerCheckState state;
    private final String channel;
    private final String payloadPreview;
    private final int bytes;
    private final long timestamp;

    public PayloadContext(Player player, PlayerCheckState state, String channel, String payloadPreview, int bytes,
                          long timestamp) {
        this.player = player;
        this.state = state;
        this.channel = channel;
        this.payloadPreview = payloadPreview;
        this.bytes = bytes;
        this.timestamp = timestamp;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerCheckState getState() {
        return state;
    }

    public String getChannel() {
        return channel;
    }

    public String getPayloadPreview() {
        return payloadPreview;
    }

    public int getBytes() {
        return bytes;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
