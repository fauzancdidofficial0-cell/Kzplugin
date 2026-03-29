package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SellCommand implements CommandExecutor {

    private final KZPlugin plugin;
    public static final String SELL_TITLE = "§a§lSell Items";
    private static final Set<UUID> sellingSessions = new HashSet<>();

    public SellCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        Player player = (Player) sender;
        openSellGUI(player);
        return true;
    }

    public void openSellGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, SELL_TITLE);

        // Fill bottom row with controls
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);

        // Info
        inv.setItem(49, createItem(Material.PAPER, "§e§lINSTRUCTIONS",
            "§7Place items in the empty slots above.",
            "§7Click §aSELL ALL §7to sell everything.",
            "§7Click §cCANCEL §7to return items.",
            "",
            "§7Items without sell value will be",
            "§7returned to your inventory."));

        // Sell button
        inv.setItem(47, createItem(Material.LIME_WOOL, "§a§lSELL ALL",
            "§7Click to sell all items above.",
            "§7You will receive payment based on",
            "§7each item's sell value."));

        // Cancel button
        inv.setItem(51, createItem(Material.RED_WOOL, "§c§lCANCEL",
            "§7Click to cancel and return all items."));

        // Preview total
        inv.setItem(45, createItem(Material.GOLD_INGOT, "§6§lESTIMATED TOTAL",
            "§7Place items above to see estimate.",
            "§7Current: §a$0"));

        sellingSessions.add(player.getUniqueId());
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  PROCESS SELL ALL
    // ══════════════════════════════════════

    public void processSellAll(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        int totalEarned = 0;
        int itemsSold = 0;

        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            int sellPrice = plugin.getItemDatabase().getSellPrice(item.getType());

            if (sellPrice > 0) {
                int earned = sellPrice * item.getAmount();
                totalEarned += earned;
                itemsSold += item.getAmount();
                inv.setItem(i, null);
            } else {
                // Return unsellable items
                player.getInventory().addItem(item);
                inv.setItem(i, null);
            }
        }

        sellingSessions.remove(uuid);
        player.closeInventory();

        if (totalEarned > 0) {
            plugin.getEconomyManager().addBalance(uuid, totalEarned);

            player.sendMessage("");
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§f§l  ITEMS SOLD SUCCESSFULLY");
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§7  Items Sold : §f" + itemsSold);
            player.sendMessage("§7  Earned     : §a+$" + plugin.getEconomyManager().formatBalance(totalEarned));
            player.sendMessage("§7  Balance    : §a" + plugin.getEconomyManager().formatBalance(
                plugin.getEconomyManager().getBalance(uuid)));
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            player.sendMessage("§c§lKZ §8» §7No sellable items were found.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // ══════════════════════════════════════
    //  CANCEL SELL
    // ══════════════════════════════════════

    public void cancelSell(Player player, Inventory inv) {
        returnItems(player, inv);
        sellingSessions.remove(player.getUniqueId());
        player.closeInventory();
        player.sendMessage("§c§lKZ §8» §7Sale cancelled. Items returned.");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1f);
    }

    public void returnItems(Player player, Inventory inv) {
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item);
                inv.setItem(i, null);
            }
        }
    }

    // ══════════════════════════════════════
    //  CALCULATE ESTIMATE
    // ══════════════════════════════════════

    public int calculateTotal(Inventory inv) {
        int total = 0;
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            int price = plugin.getItemDatabase().getSellPrice(item.getType());
            if (price > 0) total += price * item.getAmount();
        }
        return total;
    }

    public static boolean isInSellSession(UUID uuid) {
        return sellingSessions.contains(uuid);
    }

    public static void removeSellSession(UUID uuid) {
        sellingSessions.remove(uuid);
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
