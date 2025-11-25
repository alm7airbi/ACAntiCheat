package org.bukkit.event.player;

import org.bukkit.entity.Player;

public class PlayerCommandPreprocessEvent {

    private boolean cancelled;
    private String message;
    private final Player player;

    public PlayerCommandPreprocessEvent(Player who, String message) {
        this.player = who;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public Player getPlayer() {
        return player;
    }
}
