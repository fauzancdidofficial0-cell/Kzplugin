package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.commands.AuctionCommand;
import com.kz.plugin.commands.SellCommand;
import com.kz.plugin.commands.ShopCommand;
import com.kz.plugin.data.ItemDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

public class GUIListener implements Listener {

    private final KZPlugin plugin;
    private final ShopCommand shopCommand;
    private final SellCommand sellCommand;
    private final AuctionCommand auctionCommand;

    public GUIListener(KZPlugin plugin) {
        this.plugin = plugin;
        this.shopCommand = new ShopCommand(plugin);
        this.sellCommand = new SellCommand(plugin);
        this.auctionCommand = new AuctionCommand(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        Inventory inv = event.getInventory();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) return;

        // ===== SHOP MAIN MENU =====
        if (title.equals("§6§lKZ Shop")) {
            event.setCancelled(true);
            Map<String, ItemDatabase.Category> cats = plugin.getItemDatabase().getCategories();
            int[] catSlots = {10, 12, 14, 16, 28, 30, 32, 34};
            int i = 0;
            for (Map.Entry<String, ItemDatabase.Category> entry : cats.entrySet()) {
                if (i >= catSlots.length) break;
                if (event.getRawSlot() == catSlots[i]) {
                    shopCommand.openCategory(player, entry.getKey(), 1);
                    return;
                }
                i++;
            }
            return;
        }

        // ===== SHOP CATEGORY =====
        if (title.contains(" §8| §7")) {
            if (title.startsWith("§") && !title.contains("Auction House")) {
                event.setCancelled(true);
                UUID uuid = player.getUniqueId();
                String category = ShopCommand.getPlayerCategory(uuid);
                int page = ShopCommand.getPlayerPage(uuid);

                if (event.getRawSlot() == 45) { shopCommand.openMainMenu(player); return; }
                if (event.getRawSlot() == 48) { shopCommand.openCategory(player, category, page - 1); return; }
                if (event.getRawSlot() == 50) { shopCommand.openCategory(player, category, page + 1); return; }
                if (ShopCommand.isItemSlot(event.getRawSlot())) {
                    int itemIndex = ShopCommand.getItemIndex(event.getRawSlot(), page);
                    if (itemIndex >= 0 && category != null) {
                        shopCommand.openBuyGUI(player, category, itemIndex);
                    }
                }
                return;
            }
        }

        // ===== SHOP BUY GUI =====
        if (title.startsWith("§6§lPurchase: ")) {
            event.setCancelled(true);
            UUID uuid = player.getUniqueId();
            String categoryId = ShopCommand.getBuyItemCategory(uuid);
            Integer itemIndex = ShopCommand.getBuyItemIndex(uuid);

            if (categoryId == null || itemIndex == null) return;
            ItemDatabase.Category category = plugin.getItemDatabase().getCategory(categoryId);
            if (category == null || itemIndex >= category.items.size()) return;
            ItemDatabase.ShopItem shopItem = category.items.get(itemIndex);

            if (event.getRawSlot() == 1) ShopCommand.updateQuantity(uuid, 1);
            if (event.getRawSlot() == 2) ShopCommand.updateQuantity(uuid, 16);
            if (event.getRawSlot() == 3) ShopCommand.updateQuantity(uuid, 64);
            if (event.getRawSlot() == 5) ShopCommand.updateQuantity(uuid, -64);
            if (event.getRawSlot() == 6) ShopCommand.updateQuantity(uuid, -16);
            if (event.getRawSlot() == 7) ShopCommand.updateQuantity(uuid, -1);
            if (event.getRawSlot() == 18) { shopCommand.openCategory(player, categoryId, ShopCommand.getPlayerPage(uuid)); return; }
            if (event.getRawSlot() == 22 && clicked.getType() == Material.EMERALD) { shopCommand.processPurchase(player); return; }

            shopCommand.refreshBuyDisplay(inv, player, category, shopItem, ShopCommand.getBuyQuantity(uuid));
            return;
        }

        // ===== SELL GUI =====
        if (title.equals(SellCommand.SELL_TITLE)) {
            int raw = event.getRawSlot();
            if (raw >= 45 && raw < 54) {
                event.setCancelled(true);
                if (raw == 47) { sellCommand.processSellAll(player, inv); return; }
                if (raw == 51) { sellCommand.cancelSell(player, inv); return; }
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.getOpenInventory() != null && SellCommand.SELL_TITLE.equals(player.getOpenInventory().getTitle())) {
                    Inventory top = player.getOpenInventory().getTopInventory();
                    int total = sellCommand.calculateTotal(top);
                    ItemStack item = new ItemStack(Material.GOLD_INGOT);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName("§6§lESTIMATED TOTAL");
                    meta.setLore(Arrays.asList("§7Place items above to see estimate.", "§7Current: §a$" + total));
                    item.setItemMeta(meta);
                    top.setItem(45, item);
                }
            }, 1L);
            return;
        }

        // ===== AUCTION HOUSE =====
        if (title.startsWith("§3§lAuction House")) {
            event.setCancelled(true);
            int page = AuctionCommand.getPlayerPageNum(player.getUniqueId());
            if (event.getRawSlot() == 45) { AuctionCommand.setPlayerPage(player.getUniqueId(), Math.max(1, page - 1)); auctionCommand.openAuctionHouse(player, Math.max(1, page - 1)); return; }
            if (event.getRawSlot() == 47) { auctionCommand.openMyListings(player); return; }
            if (event.getRawSlot() == 53) { AuctionCommand.setPlayerPage(player.getUniqueId(), page + 1); auctionCommand.openAuctionHouse(player, page + 1); return; }
            if (AuctionCommand.isContentSlot(event.getRawSlot())) {
                int listingId = AuctionCommand.getListingIdBySlot(event.getRawSlot(), page);
                if (listingId != -1) auctionCommand.processPurchase(player, listingId);
            }
            return;
        }

        // ===== MY LISTINGS =====
        if (title.equals("§e§lMy Listings")) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49) { auctionCommand.openAuctionHouse(player, 1); return; }
            if (AuctionCommand.isContentSlot(event.getRawSlot())) {
                int slotIndex = AuctionCommand.getContentSlotIndex(event.getRawSlot());
                if (slotIndex != -1) auctionCommand.removeMyListing(player, slotIndex);
            }
            return;
        }

        // ===== INBOX =====
        if (title.equals("§e§lInbox - Expired Items")) {
            event.setCancelled(true);
            if (AuctionCommand.isContentSlot(event.getRawSlot())) {
                int slotIndex = AuctionCommand.getContentSlotIndex(event.getRawSlot());
                if (slotIndex != -1) auctionCommand.collectInboxItem(player, slotIndex);
            }
            return;
        }

        // ===== LAND RULES =====
        if (title.startsWith("§b§lMember Rules") || title.startsWith("§b§lTrust Rules")) {
            event.setCancelled(true);
            if (clicked.getType() == Material.COMPARATOR) {
                if (title.contains("Member")) plugin.getLandSystem().openTrustRulesGUI(player);
                else plugin.getLandSystem().openMemberRulesGUI(player);
                return;
            }
            int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40};
            for (int i = 0; i < slots.length; i++) {
                if (event.getRawSlot() == slots[i]) {
                    if (title.contains("Member")) plugin.getLandSystem().toggleMemberRule(player, i);
                    else plugin.getLandSystem().toggleTrustRule(player, i);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (event.getView().getTitle().equals(SellCommand.SELL_TITLE)) {
            if (SellCommand.isInSellSession(player.getUniqueId())) {
                sellCommand.returnItems(player, event.getInventory());
                SellCommand.removeSellSession(player.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith("§6§l") || title.startsWith("§3§l") || title.startsWith("§e§l") || title.startsWith("§b§l")) {
            event.setCancelled(true);
        }
        if (title.equals(SellCommand.SELL_TITLE)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= 45 && rawSlot < 54) { event.setCancelled(true); return; }
            }
        }
    }
}
