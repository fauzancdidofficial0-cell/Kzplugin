// ============================================================
// PATH: src/main/java/com/kz/plugin/listeners/CrateListener.java
// ============================================================
package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.CrateSystem;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CrateListener implements Listener {

    private final KZPlugin plugin;
    private static final String EDITOR_TITLE_PREFIX = "§8§lCrate Editor: §r§b";
    private static final String PREVIEW_TITLE_PREFIX = "§8Preview: §b";

    public CrateListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER INTERACT - Right-click to open, Left-click to edit
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getCrateSystem() == null) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Only handle shulker box blocks
        if (!block.getType().name().contains("SHULKER_BOX")) return;

        // Check if this is a registered crate
        if (!plugin.getCrateSystem().isCrate(block)) return;

        Player player = event.getPlayer();
        CrateSystem crateSystem = plugin.getCrateSystem();

        // ── RIGHT CLICK = Open crate (player) ──
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // Prevent opening shulker inventory

            crateSystem.handleCrateRightClick(player, block);
        }

        // ── LEFT CLICK = Edit crate (admin only) ──
        else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.hasPermission("kzplugin.admin") && player.isSneaking()) {
                event.setCancelled(true);
                crateSystem.handleCrateLeftClick(player, block);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY CLICK - Editor GUI handling
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getCrateSystem() == null) return;

        String title = player.getOpenInventory().getTitle();

        // ── Editor GUI ──
        if (title.startsWith(EDITOR_TITLE_PREFIX)) {
            boolean handled = plugin.getCrateSystem().handleEditorClick(
                    player, event.getInventory(), event.getRawSlot(), event.isShiftClick());

            if (handled) {
                event.setCancelled(true);
            }
            return;
        }

        // ── Preview GUI - block ALL clicks (read-only) ──
        if (title.startsWith(PREVIEW_TITLE_PREFIX)) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY CLOSE - Cleanup editor tracking
    // ════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (plugin.getCrateSystem() == null) return;

        String title = player.getOpenInventory().getTitle();

        if (title.startsWith(EDITOR_TITLE_PREFIX)) {
            // If player closes without clicking SAVE, we still auto-save
            if (plugin.getCrateSystem().isEditing(player)) {
                // Auto-save on close
                plugin.getCrateSystem().handleEditorClose(player);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PROTECT CRATE BLOCKS - Prevent breaking
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getCrateSystem() == null) return;

        if (plugin.getCrateSystem().isCrate(event.getBlock())) {
            if (!event.getPlayer().hasPermission("kzplugin.admin")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c§lKZ §8» §cYou cannot break a crate!");
                event.getPlayer().sendMessage("  §7Use §e/gachadelete §7to remove it (admin only).");
            } else {
                // Even admin can't break directly, must use command
                event.setCancelled(true);
                event.getPlayer().sendMessage("§e§lKZ §8» §eUse §f/gachadelete §eto remove crates.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.getCrateSystem() == null) return;
        event.blockList().removeIf(block -> plugin.getCrateSystem().isCrate(block));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.getCrateSystem() == null) return;
        event.blockList().removeIf(block -> plugin.getCrateSystem().isCrate(block));
    }

    // ════════════════════════════════════════════════════════════════
    //  PROTECT HOLOGRAMS - Prevent interaction
    // ════════════════════════════════════════════════════════════════

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (plugin.getCrateSystem() == null) return;

        if (event.getEntity() instanceof ArmorStand) {
            if (plugin.getCrateSystem().isHologram(event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }
}
