package org.bukkit.event.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerTeleportEvent {
    private final Player player;
    private final Location from;
    private final Location to;
    private boolean cancelled;
    private final TeleportCause cause;

    public PlayerTeleportEvent(Player player, Location from, Location to, TeleportCause cause) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.cause = cause;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public TeleportCause getCause() {
        return cause;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public enum TeleportCause {
        UNKNOWN,
        PLUGIN,
        COMMAND,
        ENDER_PEARL
    }
}
