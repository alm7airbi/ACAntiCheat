package com.yourcompany.uac.integration.paper;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Best-effort adapter that prefers external punishment plugins/commands when configured
 * while safely falling back to Bukkit's built-in ban list.
 */
public class ExternalPunishmentBridge {

    private final UltimateAntiCheatPlugin plugin;
    private final Settings settings;

    public ExternalPunishmentBridge(UltimateAntiCheatPlugin plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public boolean dispatchKick(Player player, String checkName, String reason) {
        if (!settings.preferExternalPunishments || settings.externalPunishmentCommand == null
                || settings.externalPunishmentCommand.isEmpty()) {
            return false;
        }
        return runCommand(player, reason, 0, "kick", checkName);
    }

    public boolean dispatchTemporaryBan(Player player, String checkName, String reason, int durationMinutes) {
        if (!settings.preferExternalPunishments || settings.externalPunishmentCommand == null
                || settings.externalPunishmentCommand.isEmpty()) {
            return false;
        }
        return runCommand(player, reason, durationMinutes, "tempban", checkName);
    }

    public boolean dispatchPermanentBan(Player player, String checkName, String reason) {
        if (!settings.preferExternalPunishments || settings.externalPunishmentCommand == null
                || settings.externalPunishmentCommand.isEmpty()) {
            return false;
        }
        return runCommand(player, reason, -1, "permban", checkName);
    }

    private boolean runCommand(Player player, String reason, int durationMinutes, String action, String checkName) {
        try {
            String command = settings.externalPunishmentCommand
                    .replace("{player}", player.getName())
                    .replace("{reason}", sanitize(reason))
                    .replace("{durationMinutes}", String.valueOf(durationMinutes))
                    .replace("{action}", action)
                    .replace("{check}", checkName);
            ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
            boolean success = Bukkit.dispatchCommand(console, command);
            plugin.getLogger().log(success ? Level.INFO : Level.WARNING,
                    "[ACAC] External punishment command=" + command + " success=" + success);
            return success;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "[ACAC] Failed external punishment dispatch", ex);
            return false;
        }
    }

    private String sanitize(String input) {
        return input == null ? "" : input.replace('\n', ' ').replace('\r', ' ');
    }
}
