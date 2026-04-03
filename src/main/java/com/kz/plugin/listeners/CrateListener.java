// ============================================================
// PATH: src/main/java/com/kz/plugin/listeners/CrateListener.java
// ============================================================
package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
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
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CrateListener implements Listener {

    private final KZPlugin plugin;
    private static final String EDITOR_PREFIX = "§8§lCrate Editor: §r§b";
    private static final String PREVIEW_PREFIX = "§8Preview: §b";

    public CrateListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER INTERACT
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getCrateSystem() == null) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!block.getType().name().contains("SHULKER_BOX")) return;
        if (!plugin.getCrateSystem().isCrate(block)) return;

        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getCrateSystem().handleCrateRightClick(player, block);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.hasPermission("kzplugin.admin") && player.isSneaking()) {
                event.setCancelled(true);
                plugin.getCrateSystem().handleCrateLeftClick(player, block);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  EDITOR GUI
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getCrateSystem() == null) return;

        String title = player.getOpenInventory().getTitle();

        if (title.startsWith(EDITOR_PREFIX)) {
            boolean handled = plugin.getCrateSystem().handleEditorClick(
                    player, event.getInventory(), event.getRawSlot());
            if (handled) event.setCancelled(true);
            return;
        }

        if (title.startsWith(PREVIEW_PREFIX)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (plugin.getCrateSystem() == null) return;

        String title = player.getOpenInventory().getTitle();
        if (title.startsWith(EDITOR_PREFIX) && plugin.getCrateSystem().isEditing(player)) {
            plugin.getCrateSystem().handleEditorClose(player);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PROTECT CRATE BLOCKS
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getCrateSystem() == null) return;
        if (plugin.getCrateSystem().isCrate(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c§lKZ §8» §cUse §f/gachadelete §cto remove crates.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.getCrateSystem() == null) return;
        event.blockList().removeIf(b -> plugin.getCrateSystem().isCrate(b));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.getCrateSystem() == null) return;
        event.blockList().removeIf(b -> plugin.getCrateSystem().isCrate(b));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (plugin.getCrateSystem() == null) return;
        if (event.getEntity() instanceof ArmorStand && plugin.getCrateSystem().isHologram(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ANTI-EXPLOIT: Block anvil/craft/grindstone/smithing on keys
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (plugin.getCrateSystem() == null) return;

        var anvil = event.getInventory();
        for (int i = 0; i < 2; i++) {
            ItemStack item = anvil.getItem(i);
            if (item != null && plugin.getCrateSystem().isAnyCrateKey(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (plugin.getCrateSystem() == null) return;

        var inv = event.getInventory();
        for (int i = 0; i < 2; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && plugin.getCrateSystem().isAnyCrateKey(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (plugin.getCrateSystem() == null) return;

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && plugin.getCrateSystem().isAnyCrateKey(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (plugin.getCrateSystem() == null) return;

        var inv = event.getInventory();
        for (ItemStack item : new ItemStack[]{inv.getInputEquipment(), inv.getInputMineral()}) {
            if (item != null && plugin.getCrateSystem().isAnyCrateKey(item)) {
                event.setResult(null);
                return;
            }
        }
    }
}
