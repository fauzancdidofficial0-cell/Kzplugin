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
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CrateListener implements Listener {

    private final KZPlugin plugin;

    private static final String EDITOR_PREFIX  = "§8§lCrate Editor: §r§b";
    private static final String PREVIEW_PREFIX = "§8Preview: §b";
    private static final String SPIN_PREFIX    = "§8§l✦ §6§lGACHA §8§l✦";

    public CrateListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    private CrateSystem cs() { return plugin.getCrateSystem(); }

    private boolean isEditorOpen(Player p) {
        return p.getOpenInventory().getTitle().startsWith(EDITOR_PREFIX);
    }

    private boolean isPreviewOpen(Player p) {
        return p.getOpenInventory().getTitle().startsWith(PREVIEW_PREFIX);
    }

    private boolean isSpinOpen(Player p) {
        return p.getOpenInventory().getTitle().startsWith(SPIN_PREFIX);
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER INTERACT
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (cs() == null) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!block.getType().name().contains("SHULKER_BOX")) return;
        if (!cs().isCrate(block)) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            cs().handleCrateRightClick(player, block);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (player.hasPermission("kzplugin.admin") && player.isSneaking()) {
                cs().handleCrateLeftClick(player, block);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY CLICK
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (cs() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();

        // ── Spin GUI: block SEMUA klik ──
        if (isSpinOpen(player) || cs().isSpinningGUI(player, topInv)) {
            event.setCancelled(true);
            return;
        }

        // ── Preview GUI: block semua ──
        if (isPreviewOpen(player)) {
            event.setCancelled(true);
            return;
        }

        // ── Editor GUI ──
        if (!isEditorOpen(player)) return;

        int rawSlot = event.getRawSlot();
        int invSize = event.getInventory().getSize(); // 54
        ClickType clickType = event.getClick();

        // Klik di inventory player (bawah) dengan shift → coba taruh ke reward slot
        if (rawSlot >= invSize) {
            if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                boolean placed = tryPlaceInRewardSlot(topInv, clickedItem.clone());
                if (placed) {
                    if (clickedItem.getAmount() > 1)
                        clickedItem.setAmount(clickedItem.getAmount() - 1);
                    else
                        event.setCurrentItem(null);
                    player.sendMessage("§a§lKZ §8» §7Item added to reward slot!");
                } else {
                    player.sendMessage("§e§lKZ §8» §eAll reward slots are full!");
                }
            }
            return;
        }

        // Number key / double click → cancel kalau locked
        if (clickType == ClickType.NUMBER_KEY || clickType == ClickType.DOUBLE_CLICK) {
            if (cs().isLockedSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }

        if (clickType == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        boolean shouldCancel = cs().handleEditorClick(player, topInv, rawSlot);
        if (shouldCancel) event.setCancelled(true);
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY DRAG
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (cs() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();

        // Block drag di spin GUI
        if (isSpinOpen(player) || cs().isSpinningGUI(player, topInv)) {
            event.setCancelled(true);
            return;
        }

        if (!isEditorOpen(player)) return;

        int invSize = event.getInventory().getSize();

        for (int slot : event.getRawSlots()) {
            if (slot < invSize && cs().isLockedSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY CLOSE
    // ════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (cs() == null) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();

        if (title.startsWith(EDITOR_PREFIX) && cs().isEditing(player)) {
            cs().handleEditorClose(player);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PROTECT CRATE BLOCKS
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (cs() == null) return;
        if (cs().isCrate(event.getBlock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    "§c§lKZ §8» §cUse §f/gachadelete §cto remove crates.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (cs() == null) return;
        event.blockList().removeIf(b -> cs().isCrate(b));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (cs() == null) return;
        event.blockList().removeIf(b -> cs().isCrate(b));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (cs() == null) return;
        if (event.getEntity() instanceof ArmorStand
                && cs().isHologram(event.getEntity()))
            event.setCancelled(true);
    }

    // ════════════════════════════════════════════════════════════════
    //  ANTI-EXPLOIT
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (cs() == null) return;
        for (int i = 0; i < 2; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item != null && cs().isAnyCrateKey(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (cs() == null) return;
        for (int i = 0; i < 2; i++) {
            ItemStack item = event.getInventory().getItem(i);
            if (item != null && cs().isAnyCrateKey(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (cs() == null) return;
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && cs().isAnyCrateKey(item)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (cs() == null) return;
        for (ItemStack item : new ItemStack[]{
                event.getInventory().getInputEquipment(),
                event.getInventory().getInputMineral()}) {
            if (item != null && cs().isAnyCrateKey(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPER: Taruh item ke reward slot kosong
    // ════════════════════════════════════════════════════════════════

    private boolean tryPlaceInRewardSlot(Inventory gui, ItemStack item) {
        int[] rewardCols = {0, 2, 4, 6, 8};
        for (int col : rewardCols) {
            for (int row = 1; row <= 4; row++) {
                int slot = row * 9 + col;
                ItemStack existing = gui.getItem(slot);
                if (existing == null || existing.getType() == Material.AIR) {
                    gui.setItem(slot, item);
                    return true;
                }
            }
        }
        return false;
    }
}
