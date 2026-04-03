// ============================================================
// PATH: src/main/java/com/kz/plugin/commands/MenuCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class MenuCommand implements CommandExecutor {

    private static final String MENU_TITLE = "§8TPI Menu";
    private final KZPlugin plugin;

    public MenuCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommand ini hanya bisa dipakai player.");
            return true;
        }

        if (cmdName.equals("menu")) {
            openMenu(player);
            return true;
        }

        if (cmdName.equals("settppasar")) {
            if (!player.hasPermission("kz.admin")) {
                player.sendMessage("§cKamu tidak punya permission.");
                return true;
            }

            Location loc = player.getLocation();
            plugin.getConfig().set("teleport.pasar.world", loc.getWorld().getName());
            plugin.getConfig().set("teleport.pasar.x", loc.getX());
            plugin.getConfig().set("teleport.pasar.y", loc.getY());
            plugin.getConfig().set("teleport.pasar.z", loc.getZ());
            plugin.getConfig().set("teleport.pasar.yaw", loc.getYaw());
            plugin.getConfig().set("teleport.pasar.pitch", loc.getPitch());
            plugin.saveConfig();

            player.sendMessage("§aLokasi teleport §ePasar §aberhasil diset.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            return true;
        }

        return true;
    }

    private void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        ItemStack pasar = createItem(
                Material.GRASS_BLOCK,
                "§a§lPasar",
                "§7Klik untuk teleport ke pasar"
        );

        ItemStack comingSoon = createItem(
                Material.OBSIDIAN,
                "§8§lComing Soon",
                "§7Fitur ini belum tersedia"
        );

        inv.setItem(11, pasar);
        inv.setItem(15, comingSoon);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(loreLines));
            item.setItemMeta(meta);
        }
        return item;
    }
}
