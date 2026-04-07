// ============================================================
// Path: src/main/java/com/kz/plugin/systems/OrderGUI.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.AdvancedOrderSystem.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;

import java.util.*;

public class OrderGUI {

    private final KZPlugin            plugin;
    private final AdvancedOrderSystem orderSystem;

    public OrderGUI(KZPlugin plugin, AdvancedOrderSystem orderSystem) {
        this.plugin      = plugin;
        this.orderSystem = orderSystem;
    }

    // ════════════════════════════════════════════════════════════════
    //  MAIN MENU - 54 slot
    // ════════════════════════════════════════════════════════════════

    public void openMainMenu(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54,
                "§8§l[ §b§lORDER MARKET §8§l]");

        List<BuyOrder> orders    = orderSystem.getFilteredOrders(player.getUniqueId());
        int            totalPages = Math.max(1, (int) Math.ceil(orders.size() / 45.0));
        page = Math.max(0, Math.min(page, totalPages - 1));
        orderSystem.setPage(player.getUniqueId(), page);

        // ── Slot 0-44: Order items ─────────────────────────────────
        int start = page * 45;
        int end   = Math.min(start + 45, orders.size());
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildOrderItem(orders.get(i)));
        }

        // Fill empty slot 0-44
        for (int i = 0; i < 45; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, glass(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // ── Bottom bar ─────────────────────────────────────────────
        for (int i = 45; i < 54; i++)
            inv.setItem(i, glass(Material.BLACK_STAINED_GLASS_PANE, " "));

        // Slot 46: Filter
        Category cat = orderSystem.getCategory(player.getUniqueId());
        inv.setItem(46, item(Material.HOPPER, "§e§lFILTER CATEGORY", List.of(
                "§7Current: §b" + cat.name(), "",
                (cat == Category.ALL    ? "§a▶ " : "§8  ") + "ALL",
                (cat == Category.BLOCKS ? "§a▶ " : "§8  ") + "BLOCKS",
                (cat == Category.TOOLS  ? "§a▶ " : "§8  ") + "TOOLS",
                (cat == Category.NATURE ? "§a▶ " : "§8  ") + "NATURE",
                (cat == Category.COMBAT ? "§a▶ " : "§8  ") + "COMBAT",
                (cat == Category.OTHERS ? "§a▶ " : "§8  ") + "OTHERS",
                "", "§eClick to cycle"
        )));

        // Slot 47: Sort
        SortType sort = orderSystem.getSort(player.getUniqueId());
        inv.setItem(47, item(Material.COMPASS, "§e§lSORT", List.of(
                "§7Current: §b" + sort.name().replace("_", " "), "",
                (sort == SortType.RECENTLY_LISTED ? "§a▶ " : "§8  ") + "Recently Listed",
                (sort == SortType.LOWER_PAID      ? "§a▶ " : "§8  ") + "Lower Price/ea",
                (sort == SortType.HIGHER_PAID     ? "§a▶ " : "§8  ") + "Higher Price/ea",
                "", "§eClick to cycle"
        )));

        // Slot 48: Info
        inv.setItem(48, item(Material.PAPER, "§f§lINFO", List.of(
                "§7Orders : §f" + orders.size(),
                "§7Page   : §f" + (page + 1) + "§7/§f" + totalPages,
                "§7Filter : §b" + cat.name(),
                "§7Sort   : §b" + sort.name().replace("_", " ")
        )));

        // Slot 49: Search
        String search = orderSystem.getSearch(player.getUniqueId());
        inv.setItem(49, item(Material.OAK_SIGN, "§e§lSEARCH", List.of(
                "§7Query: §f" + (search.isBlank() ? "§8(none)" : search),
                "",
                "§eClick to search",
                "§7Type item name in chat"
        )));

        // Slot 50: Clear search (hanya jika ada search)
        if (!search.isBlank()) {
            inv.setItem(50, item(Material.BARRIER, "§c§lCLEAR SEARCH", List.of(
                    "§7Click to clear search filter"
            )));
        }

        // Slot 51: Prev page
        if (page > 0) {
            inv.setItem(51, item(Material.ARROW, "§e§l◀ Previous", List.of(
                    "§7Page " + page + " / " + totalPages
            )));
        }

        // Slot 52: My Orders
        inv.setItem(52, item(Material.ENDER_CHEST, "§b§lMY ORDERS", List.of(
                "§7View & manage your orders",
                "§7Collect items from stash",
                "", "§eClick to open"
        )));

        // Slot 53: Next page
        if (page < totalPages - 1) {
            inv.setItem(53, item(Material.ARROW, "§e§lNext ▶", List.of(
                    "§7Page " + (page + 2) + " / " + totalPages
            )));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  MY ORDERS MENU
    // ════════════════════════════════════════════════════════════════

    public void openMyOrders(Player player) {
        List<BuyOrder> myOrders = orderSystem.getMyOrders(player.getUniqueId());
        int rows = Math.max(2, (int) Math.ceil((myOrders.size() + 1) / 9.0) + 1);
        rows = Math.min(rows, 6);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(null, size,
                "§8§l[ §b§lMY ORDERS §8§l]");

        // Slot 0: Buat order baru
        inv.setItem(0, item(Material.WRITABLE_BOOK, "§a§l+ CREATE ORDER", List.of(
                "§7Create a new buy order.",
                "§7Fixed amount or unlimited.",
                "§7Funds held in escrow.",
                "", "§eClick to start"
        )));

        // Slot 1+: Daftar order
        for (int i = 0; i < myOrders.size() && (i + 1) < size; i++) {
            inv.setItem(i + 1, buildMyOrderItem(myOrders.get(i)));
        }

        // Fill empty
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, glass(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Back button (last slot)
        inv.setItem(size - 1, item(Material.DARK_OAK_DOOR, "§7§l← Back", List.of(
                "§7Return to Order Market"
        )));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  SUPPLY GUI - 9 Slot
    // ════════════════════════════════════════════════════════════════

    public void openSupplyGUI(Player player, String orderId) {
        BuyOrder order = orderSystem.getOrder(orderId);
        if (order == null) {
            player.sendMessage("§cOrder not found."); return;
        }
        if (order.completed) {
            player.sendMessage("§cOrder already completed."); return;
        }
        if (order.buyerUUID.equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot supply your own order."); return;
        }

        // Label amount berbeda untuk unlimited
        String needLabel = order.unlimited
                ? "§eUNLIMITED ♾ §8(budget: §a"
                    + plugin.getLobbySystem().formatCoins(order.priceRemaining()) + "§8)"
                : "§c" + order.amountRemaining() + " §8/ §7" + order.amountNeeded;

        // Estimasi item yang bisa diterima dengan budget tersisa
        String canAcceptLabel = order.unlimited
                ? "§f~" + (long) Math.floor(order.priceRemaining() / order.pricePerItem) + " §7items"
                : "§f" + order.amountRemaining() + " §7items";

        Inventory inv = Bukkit.createInventory(null, 9,
                "§8Supply §b#" + orderId + " §8[" + order.displayName + "]");

        // Slot 0-6: Drop zone
        for (int i = 0; i < 7; i++) {
            inv.setItem(i, glass(Material.WHITE_STAINED_GLASS_PANE,
                    "§7Drop §f" + order.displayName + " §7here"));
        }

        // Slot 7: Info
        inv.setItem(7, item(new ItemStack(order.material), "§f§lORDER INFO", List.of(
                "§7Item      : §f" + order.displayName,
                "§7Type      : " + (order.unlimited ? "§eUnlimited ♾" : "§fFixed"),
                "§7Need      : " + needLabel,
                "§7Can take  : " + canAcceptLabel,
                "§7Pay/ea    : §a" + plugin.getLobbySystem().formatCoins(order.pricePerItem),
                "§7Budget    : §a" + plugin.getLobbySystem().formatCoins(order.priceRemaining()),
                "",
                "§7✔ Partial delivery supported",
                "§7✔ Paid proportionally"
        )));

        // Slot 8: Confirm
        inv.setItem(8, item(Material.LIME_CONCRETE, "§a§lCONFIRM SUPPLY", List.of(
                "§7Items in slots 0-6 will be",
                "§7delivered to buyer stash.",
                "",
                "§7Rate: §a" + plugin.getLobbySystem().formatCoins(order.pricePerItem) + " §7/ item",
                "", "§eClick to confirm"
        )));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  STASH GUI - 27 Slot
    // ════════════════════════════════════════════════════════════════

    public void openStashGUI(Player player, String orderId) {
        BuyOrder order = orderSystem.getOrder(orderId);
        if (order == null) {
            player.sendMessage("§cOrder not found."); return;
        }
        if (!order.buyerUUID.equals(player.getUniqueId())) {
            player.sendMessage("§cNot your order."); return;
        }

        Inventory inv = Bukkit.createInventory(null, 27,
                "§8Stash §b#" + orderId + " §8[" + order.displayName + "]");

        // Slot 0-17: Preview stash (read-only)
        int slot = 0;
        for (ItemStack stashItem : order.stash) {
            if (slot >= 18 || stashItem == null) break;
            ItemStack display = stashItem.clone();
            ItemMeta  meta    = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a" + stashItem.getAmount()
                        + "x " + orderSystem.formatMaterial(stashItem.getType()));
                meta.setLore(List.of("§7Use §bCollect All §7to retrieve"));
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
            slot++;
        }

        // Fill sisa slot 0-17
        for (int i = slot; i < 18; i++)
            inv.setItem(i, glass(Material.GRAY_STAINED_GLASS_PANE, " "));

        // Bottom bar slot 18-26
        for (int i = 18; i < 27; i++)
            inv.setItem(i, glass(Material.BLACK_STAINED_GLASS_PANE, " "));

        int     stashTotal = orderSystem.getStashTotal(order);
        boolean hasItems   = stashTotal > 0;

        // Slot 18: Order info
        String progLine = order.unlimited
                ? "§7Budget left: §a" + plugin.getLobbySystem().formatCoins(order.priceRemaining())
                : buildProgressBar(order.fillPercent(), 15);

        inv.setItem(18, item(new ItemStack(order.material), "§f§lORDER INFO", List.of(
                "§7Order  : §b#" + order.id,
                "§7Item   : §f" + order.displayName,
                "§7Type   : " + (order.unlimited ? "§eUnlimited ♾" : "§fFixed"),
                "§7Filled : §f" + order.amountFilled
                        + (order.unlimited ? "" : "§7/§f" + order.amountNeeded),
                "§7Pay/ea : §a" + plugin.getLobbySystem().formatCoins(order.pricePerItem),
                "§7        " + progLine,
                "§7Status : " + (order.completed ? "§a✔ Completed" : "§e⏳ Active")
        )));

        // Slot 22: Collect All
        inv.setItem(22, item(
                hasItems ? Material.CHEST_MINECART : Material.BARRIER,
                hasItems ? "§a§lCOLLECT ALL" : "§c§lSTASH EMPTY",
                hasItems
                        ? List.of(
                            "§7Move all items to inventory.",
                            "§7Items in stash: §a" + stashTotal,
                            "", "§eClick to collect"
                        )
                        : List.of(
                            "§7No items yet.",
                            "§7Wait for suppliers."
                        )
        ));

        // Slot 24: Cancel order
        if (!order.completed) {
            inv.setItem(24, item(Material.TNT, "§c§lCANCEL ORDER", List.of(
                    "§7Cancel this order.",
                    "§7Remaining escrow refunded.",
                    "",
                    "§7Refund: §a"
                            + plugin.getLobbySystem().formatCoins(order.priceRemaining()),
                    "",
                    "§c§lRight-Click to cancel"
            )));
        }

        // Slot 26: Back
        inv.setItem(26, item(Material.DARK_OAK_DOOR, "§7§l← Back", List.of(
                "§7Return to My Orders"
        )));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  ITEM BUILDERS
    // ════════════════════════════════════════════════════════════════

    private ItemStack buildOrderItem(BuyOrder order) {
        ItemStack base  = new ItemStack(order.material);
        int       pct   = order.fillPercent();

        // Warna: unlimited = kuning, filled = hijau, partial = kuning, kosong = merah
        String color = order.unlimited ? "§e" : pct >= 100 ? "§a" : pct > 0 ? "§e" : "§c";

        // Label jumlah
        String needLabel = order.unlimited
                ? "§eUNLIMITED ♾"
                : "§f" + order.amountRemaining() + " §8/ §7" + order.amountNeeded;

        // Progress line
        String progLine = order.unlimited
                ? "§7Budget: §a" + plugin.getLobbySystem().formatCoins(order.priceRemaining())
                        + " §7remaining"
                : " " + buildProgressBar(pct, 18) + " §f" + pct + "%";

        return item(base, color + "§l" + order.displayName, List.of(
                "§8#" + order.id + " §8| §7By: §f" + order.buyerName,
                "§7Type: " + (order.unlimited ? "§eUnlimited ♾" : "§fFixed Amount"),
                "",
                "§7Need    : " + needLabel,
                "§7Pay/ea  : §a" + plugin.getLobbySystem().formatCoins(order.pricePerItem),
                "§7Budget  : §a" + plugin.getLobbySystem().formatCoins(order.priceRemaining()),
                "",
                "§7Progress:",
                progLine,
                "",
                "§7Category: §b" + order.category,
                "",
                "§e▶ Click to supply this order"
        ));
    }

    private ItemStack buildMyOrderItem(BuyOrder order) {
        ItemStack base  = new ItemStack(order.material);
        int       pct   = order.fillPercent();
        String    color = order.unlimited ? "§e"
                : pct >= 100 ? "§a" : pct > 0 ? "§e" : "§c";
        int       stash = orderSystem.getStashTotal(order);

        String typeLabel = order.unlimited
                ? "§eUNLIMITED ♾"
                : "§f" + order.amountNeeded;

        String progLine = order.unlimited
                ? " §7Budget: §a"
                    + plugin.getLobbySystem().formatCoins(order.priceRemaining())
                    + " §7left"
                : " " + buildProgressBar(pct, 18) + " §f" + pct + "%";

        List<String> lore = new ArrayList<>(List.of(
                "§8Order #" + order.id,
                "§7Type   : " + (order.unlimited ? "§eUnlimited ♾" : "§fFixed"),
                "§7Status : " + (order.completed ? "§a✔ Completed" : "§e⏳ Active"),
                "",
                "§7Amount : " + typeLabel,
                "§7Filled : §f" + order.amountFilled,
                "§7Pay/ea : §a" + plugin.getLobbySystem().formatCoins(order.pricePerItem),
                "",
                progLine
        ));

        if (stash > 0) {
            lore.add("");
            lore.add("§a§lSTASH: §f" + stash + " items ready to collect!");
        }

        lore.add("");
        lore.add("§e▶ Click to open stash");

        return item(base, color + "§l" + order.displayName, lore);
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY BUILDERS
    // ════════════════════════════════════════════════════════════════

    public ItemStack item(Material mat, String name, List<String> lore) {
        return item(new ItemStack(mat), name, lore);
    }

    public ItemStack item(ItemStack base, String name, List<String> lore) {
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;
        meta.setDisplayName(name);
        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    private ItemStack glass(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String buildProgressBar(int percent, int length) {
        int    filled = (int)((Math.min(percent, 100) / 100.0) * length);
        String color  = percent >= 100 ? "§a" : percent >= 50 ? "§e" : "§c";
        return color + "█".repeat(filled) + "§8" + "░".repeat(length - filled);
    }
}
