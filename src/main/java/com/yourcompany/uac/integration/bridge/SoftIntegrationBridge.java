package com.yourcompany.uac.integration.bridge;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.util.logging.Logger;

/**
 * Optional hooks into common ecosystem plugins (ViaVersion, LuckPerms, DiscordSRV).
 * All calls are reflection/soft-check based to avoid hard dependencies and are
 * safe no-ops when the corresponding plugin is absent.
 */
public class SoftIntegrationBridge {

    private final UltimateAntiCheatPlugin plugin;
    private final Logger logger;

    public SoftIntegrationBridge(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void logDetectedPresence() {
        PluginManager pm = plugin.getServer().getPluginManager();
        if (pm.isPluginEnabled("ViaVersion")) {
            logger.info("[ACAC] ViaVersion detected; protocol lookups enabled.");
        }
        if (pm.isPluginEnabled("LuckPerms")) {
            logger.info("[ACAC] LuckPerms detected; advanced permission checks enabled.");
        }
        if (pm.isPluginEnabled("DiscordSRV")) {
            logger.info("[ACAC] DiscordSRV detected; staff relay available.");
        }
    }

    public boolean viaVersionPresent() {
        return plugin.getServer().getPluginManager().isPluginEnabled("ViaVersion");
    }

    public int resolveViaProtocol(Player player) {
        if (player == null || !viaVersionPresent()) {
            return 0;
        }
        try {
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = viaClass.getMethod("getAPI").invoke(null);
            Object version = api.getClass().getMethod("getPlayerVersion", java.util.UUID.class)
                    .invoke(api, player.getUniqueId());
            if (version instanceof Number num) {
                return num.intValue();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    public boolean luckPermsPresent() {
        return plugin.getServer().getPluginManager().isPluginEnabled("LuckPerms");
    }

    public boolean hasLuckPermsPermission(Player player, String permission) {
        if (player == null) {
            return false;
        }
        if (!luckPermsPresent()) {
            return player.hasPermission(permission);
        }
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = provider.getMethod("get").invoke(null);
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            Object user = userManager.getClass().getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, player.getUniqueId());
            if (user != null) {
                Object cached = user.getClass().getMethod("getCachedData").invoke(user);
                Object data = cached.getClass().getMethod("getPermissionData").invoke(cached);
                Object result = data.getClass().getMethod("checkPermission", String.class).invoke(data, permission);
                if (result != null) {
                    Boolean value = (Boolean) result.getClass().getMethod("asBoolean").invoke(result);
                    return Boolean.TRUE.equals(value);
                }
            }
        } catch (Exception ignored) {
        }
        return player.hasPermission(permission);
    }

    public boolean discordSrvPresent() {
        return plugin.getServer().getPluginManager().isPluginEnabled("DiscordSRV");
    }

    public void sendDiscordStaffAlert(String message) {
        if (!discordSrvPresent() || message == null || message.isEmpty()) {
            return;
        }
        try {
            Class<?> discordSrv = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object instance = discordSrv.getMethod("getPlugin").invoke(null);
            Object jda = discordSrv.getMethod("getJda").invoke(instance);
            Object channel = jda.getClass().getMethod("getTextChannels").invoke(jda);
            if (channel instanceof java.util.List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                first.getClass().getMethod("sendMessage", CharSequence.class)
                        .invoke(first, message);
            }
        } catch (Exception e) {
            logger.fine("[ACAC] DiscordSRV hook failed: " + e.getMessage());
        }
    }
}
