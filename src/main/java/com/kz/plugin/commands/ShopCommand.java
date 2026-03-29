package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.data.ItemDatabase;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopCommand implements CommandExecutor {

    private final KZPlugin plugin;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int[] ITEM_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };

    // Player session
    private static final Map<UUID, String> playerCategory = new HashMap<>();
    private static final Map<UUID, Integer> playerPage = new HashMap<>();
    private static final Map<UUID, Integer> buyItemIndex = new HashMap<>();
    private static final Map<UUID, String> buyItemCategory = new HashMap<>();
    private static final Map<UUID, Integer> buyQuantity = new HashMap<>();

    public ShopCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        Player player = (Player) sender;
        openMainMenu(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        return true;
    }

    // ══════════════════════════════════════
    //  MAIN MENU (Layer 1)
    // ══════════════════════════════════════

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§lKZ Shop");

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        Map<String, ItemDatabase.Category> cats = plugin.getItemDatabase().getCategories();
        int[] catSlots = {10, 12, 14, 16, 28, 30, 32, 34};
        int i = 0;

        for (ItemDatabase.Category cat : cats.values()) {
            if (i >= catSlots.length) break;

            ItemStack item = createItem(cat.icon, cat.name,
                "§7" + cat.items.size() + " items available",
                "",
                "§eClick to browse.");
            inv.setItem(catSlots[i], item);
            i++;
        }

        // Balance display
        double bal = plugin.getEconomyManager().getBalance(player);
        String mode = plugin.getEconomyManager().getPlayerMode(player);
        ItemStack balItem = createItem(Material.NETHER_STAR,
            "§f§lBalance: §a" + plugin.getEconomyManager().formatBalance(bal),
            "§7Mode: " + plugin.getEconomyManager().getModeName(mode));
        inv.setItem(22, balItem);

        player.openInventory(inv);
    }

    // ══════════════════════════════════════
    //  CATEGORY PAGE (Layer 2)
    // ══════════════════════════════════════

    public void openCategory(Player player, String categoryId, int page) {
        ItemDatabase.Category category = plugin.getItemDatabase().getCategory(categoryId);
        if (category == null) return;

        int totalItems = category.items.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
        page = Math.max(1, Math.min(page, maxPage));

        playerCategory.put(player.getUniqueId(), categoryId);
        playerPage.put(player.getUniqueId(), page);

        String title = category.name + " §8| §7" + page + "/" + maxPage;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Items
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEM_SLOTS.length && (startIndex + i) < totalItems; i++) {
            ItemDatabase.ShopItem shopItem = category.items.get(startIndex + i);

            ItemStack item = createItem(shopItem.material,
                category.color + "§l" + plugin.getItemDatabase().formatItemName(shopItem.material),
                "§7━━━━━━━━━━━━━━━━",
                "§7Buy Price  : §a$" + shopItem.buyPrice,
                "§7Sell Price : §e$" + shopItem.sellPrice,
                "§7━━━━━━━━━━━━━━━━",
                "",
                "§aClick to select quantity.");
            inv.setItem(ITEM_SLOTS[i], item);
        }

        // Navigation
        inv.setItem(45, createItem(Material.ARROW, "§c§l← Back to Menu"));

        if (page > 1) {
            inv.setItem(48, createItem(Material.ARROW, "§e← Previous Page"));
        }

        double bal = plugin.getEconomyManager().getBalance(player);
        inv.setItem(49, createItem(Material.NETHER_STAR,
            "§f§lBalance: §a" + plugin.getEconomyManager().formatBalance(bal),
            "§7Page " + page + "/" + maxPage,
            "§7Items: " + totalItems));

        if (page < maxPage) {
            inv.setItem(50, createItem(Material.ARROW, "§a→ Next Page"));
        }

        player.openInventory(inv);
    }

    // ══════════════════════════════════════
    //  BUY QUANTITY GUI (Layer 3)
    // ══════════════════════════════════════

    public void openBuyGUI(Player player, String categoryId, int itemIndex) {
        ItemDatabase.Category category = plugin.getItemDatabase().getCategory(categoryId);
        if (category == null || itemIndex >= category.items.size()) return;

        ItemDatabase.ShopItem shopItem = category.items.get(itemIndex);

        buyItemIndex.put(player.getUniqueId(), itemIndex);
        buyItemCategory.put(player.getUniqueId(), categoryId);
        buyQuantity.put(player.getUniqueId(), 1);

        String title = "§6§lPurchase: " + plugin.getItemDatabase().formatItemName(shopItem.material);
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        // Quantity buttons
        inv.setItem(1, createItem(Material.LIME_STAINED_GLASS_PANE, "§a§l+1"));
        inv.setItem(2, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§l+16"));
        inv.setItem(3, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§l+64"));

        inv.setItem(5, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l-64"));
        inv.setItem(6, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l-16"));
        inv.setItem(7, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l-1"));

        // Display
        refreshBuyDisplay(inv, player, category, shopItem, 1);

        // Buttons
        inv.setItem(18, createItem(Material.ARROW, "§c§l← Back"));
        inv.setItem(22, createItem(Material.EMERALD, "§a§lCONFIRM PURCHASE",
            "§7Total: §a$" + shopItem.buyPrice));

        player.openInventory(inv);
    }

    public void refreshBuyDisplay(Inventory inv, Player player,
            ItemDatabase.Category category, ItemDatabase.ShopItem shopItem, int qty) {

        double balance = plugin.getEconomyManager().getBalance(player);
        int totalPrice = shopItem.buyPrice * qty;
        boolean canAfford = balance >= totalPrice;

        ItemStack display = new ItemStack(shopItem.material, Math.min(qty, 64));
        ItemMeta meta = display.getItemMeta();
        meta.setDisplayName(category.color + "§l" + plugin.getItemDatabase().formatItemName(shopItem.material));
        meta.setLore(Arrays.asList(
            "§7━━━━━━━━━━━━━━━━",
            "§7Quantity  : §f" + qty,
            "§7Price/ea  : §a$" + shopItem.buyPrice,
            "§7Total     : §a$" + totalPrice,
            "§7Balance   : " + (canAfford ? "§a" : "§c") + plugin.getEconomyManager().formatBalance(balance),
            "§7━━━━━━━━━━━━━━━━",
            canAfford ? "§aYou can afford this." : "§cInsufficient balance."
        ));
        display.setItemMeta(meta);
        inv.setItem(4, display);

        // Update confirm button
        inv.setItem(22, createItem(
            canAfford ? Material.EMERALD : Material.BARRIER,
            canAfford ? "§a§lCONFIRM PURCHASE" : "§c§lINSUFFICIENT BALANCE",
            "§7Total: §a$" + totalPrice));
    }

    // ══════════════════════════════════════
    //  PROCESS PURCHASE
    // ══════════════════════════════════════

    public void processPurchase(Player player) {
        UUID uuid = player.getUniqueId();
        String catId = buyItemCategory.get(uuid);
        Integer index = buyItemIndex.get(uuid);
        Integer qty = buyQuantity.get(uuid);

        if (catId == null || index == null || qty == null) return;

        ItemDatabase.Category category = plugin.getItemDatabase().getCategory(catId);
        if (category == null || index >= category.items.size()) return;

        ItemDatabase.ShopItem shopItem = category.items.get(index);
        int totalPrice = shopItem.buyPrice * qty;

        if (!plugin.getEconomyManager().removeBalance(uuid, totalPrice)) {
            player.sendMessage("§c§lKZ §8» §7Insufficient balance. Required: §a$" + totalPrice);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        player.getInventory().addItem(new ItemStack(shopItem.material, qty));
        player.closeInventory();

        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  PURCHASE SUCCESSFUL");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Item    : §f" + plugin.getItemDatabase().formatItemName(shopItem.material) + " x" + qty);
        player.sendMessage("§7  Cost    : §c-$" + totalPrice);
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(uuid)));
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // Cleanup
        buyItemIndex.remove(uuid);
        buyItemCategory.remove(uuid);
        buyQuantity.remove(uuid);
    }

    // ══════════════════════════════════════
    //  STATIC GETTERS (for GUIListener)
    // ══════════════════════════════════════

    public static String getPlayerCategory(UUID uuid) { return playerCategory.get(uuid); }
    public static int getPlayerPage(UUID uuid) { return playerPage.getOrDefault(uuid, 1); }
    public static Integer getBuyItemIndex(UUID uuid) { return buyItemIndex.get(uuid); }
    public static String getBuyItemCategory(UUID uuid) { return buyItemCategory.get(uuid); }
    public static int getBuyQuantity(UUID uuid) { return buyQuantity.getOrDefault(uuid, 1); }

    public static void updateQuantity(UUID uuid, int change) {
        int qty = buyQuantity.getOrDefault(uuid, 1) + change;
        buyQuantity.put(uuid, Math.max(1, qty));
    }

    public static boolean isItemSlot(int slot) {
        for (int s : ITEM_SLOTS) if (s == slot) return true;
        return false;
    }

    public static int getItemIndex(int slot, int page) {
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) return (page - 1) * ITEMS_PER_PAGE + i;
        }
        return -1;
    }

    // ══════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
