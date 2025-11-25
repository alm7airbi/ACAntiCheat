package org.bukkit;

import java.util.Date;

/**
 * Minimal stub of Bukkit's BanList so mitigation actions can compile offline.
 */
public class BanList {
    public enum Type { NAME, IP }

    public void addBan(String target, String reason, Date expires, String source) {
        // offline stub: no-op
    }
}
