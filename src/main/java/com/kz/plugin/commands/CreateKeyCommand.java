// ============================================================
// PATH: src/main/java/com/kz/plugin/commands/CreateKeyCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * CreateKeyCommand - Buat custom key item dengan warna + lore
 *
 * Usage: /createkey <material> <colored_name>
 *
 * Contoh:
 * /createkey NAME_TAG &l&bCrate &l&eKey
 * /createkey TRIPWIRE_HOOK &6&lLegendary &e&lKey
 * /createkey PAPER &c&lEvent &f&lTicket
 *
 * Pakai & sebagai color code (bukan §)
 * Setelah item dibuat, pegang item → /gachacreate ...
 */
public class CreateKeyCommand implements CommandExecutor, TabCompleter {

    private final KZPlugin plugin;

    public CreateKeyCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cNo permission.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("");
            player.sendMessage("§6§l┌─────────────────────────────────┐");
            player.sendMessage("§6§l│     §f§lCREATE KEY ITEM              §6§l│");
            player.sendMessage("§6§l└─────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §eUsage: §f/createkey <material> <name>");
            player.sendMessage("");
            player.sendMessage("  §7Use §f& §7for color codes:");
            player.sendMessage("  §7&a=§agreen §7&b=§baqua §7&c=§cred §7&d=§dpink");
            player.sendMessage("  §7&e=§eyellow §7&f=§fwhite §7&6=§6gold §7&5=§5purple");
            player.sendMessage("  §7&l=§l§fbold §r§7&o=§o§fitalic §r§7&n=§n§funderline");
            player.sendMessage("");
            player.sendMessage("  §7Add §f--lore:text §7for lore lines:");
            player.sendMessage("  §7Add §f--glow §7for enchant glow");
            player.sendMessage("");
            player.sendMessage("  §fExamples:");
            player.sendMessage("  §e/createkey NAME_TAG &l&bCrate &l&eKey");
            player.sendMessage("  §e/createkey TRIPWIRE_HOOK &6&lLegendary &e&lKey --glow");
            player.sendMessage("  §e/createkey PAPER &c&lEvent &f&lTicket --lore:&7Limited_Edition!");
            player.sendMessage("  §e/createkey NETHER_STAR &d&lMythic &5&lKey --glow --lore:&7Ultra_Rare!");
            player.sendMessage("");
            player.sendMessage("  §7After creating, hold it and use §f/gachacreate");
            player.sendMessage("");
            return true;
        }

        // Parse material
        Material material;
        try {
            material = Material.valueOf(args[0].toUpperCase());
        } catch (Exception e) {
            player.sendMessage("§c§lKZ §8» §cInvalid material: §f" + args[0]);
            player.sendMessage("  §7Examples: NAME_TAG, TRIPWIRE_HOOK, PAPER, NETHER_STAR, STICK");
            return true;
        }

        // Parse name and flags
        StringBuilder nameBuilder = new StringBuilder();
        List<String> loreLines = new ArrayList<>();
        boolean glow = false;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];

            if (arg.startsWith("--lore:")) {
                // Lore line
                String loreLine = arg.substring(7).replace("_", " ");
                loreLines.add(ChatColor.translateAlternateColorCodes('&', loreLine));
            } else if (arg.equalsIgnoreCase("--glow")) {
                glow = true;
            } else {
                // Part of the name
                if (nameBuilder.length() > 0) nameBuilder.append(" ");
                nameBuilder.append(arg);
            }
        }

        String rawName = nameBuilder.toString().replace("_", " ");
        String coloredName = ChatColor.translateAlternateColorCodes('&', rawName);

        // Create the item
        ItemStack key = new ItemStack(material);
        ItemMeta meta = key.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(coloredName);

            // Add default lore
            List<String> finalLore = new ArrayList<>();
            finalLore.add("");
            finalLore.add("§8§o[Crate Key]");

            // Add custom lore
            if (!loreLines.isEmpty()) {
                finalLore.add("");
                finalLore.addAll(loreLines);
            }

            finalLore.add("");
            finalLore.add("§7Right-click a crate to use!");

            meta.setLore(finalLore);

            // Add enchant glow
            if (glow) {
                meta.addEnchant(
                        org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            key.setItemMeta(meta);
        }

        // Give to player
        player.getInventory().addItem(key);

        player.sendMessage("");
        player.sendMessage("§a§l┌─────────────────────────────────┐");
        player.sendMessage("§a§l│     §f§lKEY ITEM CREATED             §a§l│");
        player.sendMessage("§a§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §7Material : §f" + material.name());
        player.sendMessage("  §7Name     : " + coloredName);
        if (glow) {
            player.sendMessage("  §7Glow     : §a✔ Enchant glow");
        }
        if (!loreLines.isEmpty()) {
            player.sendMessage("  §7Lore     :");
            for (String line : loreLines) {
                player.sendMessage("    §f" + line);
            }
        }
        player.sendMessage("");
        player.sendMessage("  §aItem added to your inventory!");
        player.sendMessage("  §7Now hold it and run §f/gachacreate §7to make a crate.");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.5f);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("kzplugin.admin")) return List.of();

        if (args.length == 1) {
            List<String> materials = List.of(
                    "NAME_TAG", "TRIPWIRE_HOOK", "PAPER", "NETHER_STAR",
                    "STICK", "BLAZE_ROD", "BREEZE_ROD", "ECHO_SHARD",
                    "AMETHYST_SHARD", "PRISMARINE_SHARD", "HEART_OF_THE_SEA",
                    "GOLD_INGOT", "DIAMOND", "EMERALD", "IRON_NUGGET",
                    "FEATHER", "BONE", "ENDER_EYE", "ENDER_PEARL",
                    "SUNFLOWER", "CLOCK", "COMPASS"
            );
            return materials.stream()
                    .filter(m -> m.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length >= 2) {
            String last = args[args.length - 1].toLowerCase();
            if (last.startsWith("--")) {
                return List.of("--glow", "--lore:&7Your_text_here").stream()
                        .filter(f -> f.toLowerCase().startsWith(last))
                        .toList();
            }
            return List.of("&l&bCrate", "&l&eKey", "&6&lLegendary", "&c&lEvent", "--glow", "--lore:");
        }

        return List.of();
    }
}
