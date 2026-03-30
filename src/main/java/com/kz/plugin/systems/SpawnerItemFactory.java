package com.kz.plugin.systems;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class SpawnerItemFactory {

    private final NamespacedKey spawnerTypeKey;

    public SpawnerItemFactory(JavaPlugin plugin) {
        this.spawnerTypeKey = new NamespacedKey(plugin, "shop_spawner_type");
    }

    public ItemStack createSpawner(EntityType type) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (meta == null) return item;

        String displayName = getSpawnerColor(type) + formatType(type) + ChatColor.RED + " Spawner";

        meta.setDisplayName(displayName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place this spawner to spawn",
                ChatColor.GRAY + formatType(type) + ChatColor.GRAY + ".",
                ChatColor.DARK_GRAY + "Spawner Type: " + type.name()
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(spawnerTypeKey, PersistentDataType.STRING, type.name());

        if (meta.getBlockState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(type);
            meta.setBlockState(spawner);
        }

        item.setItemMeta(meta);
        return item;
    }

    public NamespacedKey getSpawnerTypeKey() {
        return spawnerTypeKey;
    }

    private String formatType(EntityType type) {
        String[] parts = type.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            builder.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
        }
        return builder.toString().trim();
    }

    private String getSpawnerColor(EntityType type) {
        return switch (type) {
            case SKELETON -> ChatColor.WHITE.toString();
            case ZOMBIE -> ChatColor.GREEN.toString();
            case SPIDER -> ChatColor.DARK_GRAY.toString();
            case CAVE_SPIDER -> ChatColor.DARK_AQUA.toString();
            case SLIME -> ChatColor.GREEN.toString();
            case BLAZE -> ChatColor.GOLD.toString();
            case MAGMA_CUBE -> ChatColor.RED.toString();
            case CREEPER -> ChatColor.DARK_GREEN.toString();
            case ENDERMAN -> ChatColor.DARK_PURPLE.toString();
            case WITCH -> ChatColor.LIGHT_PURPLE.toString();
            default -> ChatColor.WHITE.toString();
        };
    }
}
