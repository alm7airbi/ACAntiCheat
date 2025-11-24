package com.yourcompany.uac.integration.paper;

import com.yourcompany.uac.UltimateAntiCheatPlugin;
import com.yourcompany.uac.checks.CheckManager;
import com.yourcompany.uac.checks.PlayerCheckState;
import com.yourcompany.uac.integration.bridge.EventBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Registers Bukkit/Paper listeners and funnels them to the CheckManager.
 * In this stubbed environment the listeners simply log, but on a real server
 * they will receive actual callbacks.
 */
public class PaperEventBridge implements EventBridge, Listener {

    private final UltimateAntiCheatPlugin plugin;
    private CheckManager checkManager;

    public PaperEventBridge(UltimateAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(CheckManager checkManager) {
        this.checkManager = checkManager;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String name() {
        return "paper-events";
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (player == null || to == null) {
            return;
        }
        checkManager.handleMovement(player, to.getX(), to.getY(), to.getZ(), false);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (player == null || to == null) {
            return;
        }
        boolean serverTeleport = event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND
                || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN;
        checkManager.handleMovement(player, to.getX(), to.getY(), to.getZ(), serverTeleport);
    }

    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        plugin.getGuiManager().handleClick(event);
        checkManager.handleInventoryAction(event.getWhoClicked(), "click", event.getSlot(), event.getCurrentItem());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        checkManager.handleInventoryAction(event.getWhoClicked(), "drag", -1, null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            checkManager.handlePlacement(event.getPlayer(),
                    PlayerCheckState.position(clicked.getLocation().getX(), clicked.getLocation().getY(), clicked.getLocation().getZ()),
                    clicked.getType().name());
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        if (block == null || player == null) {
            return;
        }
        plugin.getMitigationManager().setLogOnly(false);
        checkManager.handlePlacement(player,
                PlayerCheckState.position(block.getLocation().getX(), block.getLocation().getY(), block.getLocation().getZ()),
                block.getType().name());
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        if (event.getBlock() == null) {
            return;
        }
        // TODO: track per-chunk redstone intensity once chunk context is available.
    }

    @EventHandler
    public void onSign(SignChangeEvent event) {
        StringBuilder builder = new StringBuilder();
        for (String line : event.getLines()) {
            if (line != null) {
                builder.append(line);
            }
        }
        String preview = builder.toString();
        checkManager.handlePayload(event.getPlayer(), "sign", preview, preview.length());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        checkManager.handleConsoleMessage(event.getPlayer(), event.getMessage());
    }
}
