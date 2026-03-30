package com.kz.plugin.listeners;

import com.kz.plugin.systems.SpawnerPriceRegistry;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnerDropListener implements Listener {

    private final NamespacedKey spawnerMobKey;
    private final NamespacedKey spawnerDropKey;
    private final NamespacedKey spawnerSellPriceKey;

    public SpawnerDropListener(JavaPlugin plugin) {
        this.spawnerMobKey = new NamespacedKey(plugin, "spawner_mob");
        this.spawnerDropKey = new NamespacedKey(plugin, "spawner_drop");
        this.spawnerSellPriceKey = new NamespacedKey(plugin, "spawner_sell_price");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawnerSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity living = (LivingEntity) event.getEntity();
        living.getPersistentDataContainer().set(spawnerMobKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isSpawnerMob(entity)) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        List<ItemStack> customDrops = createSpawnerDrops(entity);
        for (ItemStack drop : customDrops) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), drop);
        }
    }

    private boolean isSpawnerMob(LivingEntity entity) {
        Byte value = entity.getPersistentDataContainer().get(spawnerMobKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private List<ItemStack> createSpawnerDrops(LivingEntity entity) {
        EntityType type = entity.getType();
        List<ItemStack> drops = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        switch (type) {
            case ZOMBIE:
                add(drops, Material.ROTTEN_FLESH, 1 + random.nextInt(3));
                break;
            case SKELETON:
                add(drops, Material.BONE, 1 + random.nextInt(3));
                add(drops, Material.ARROW, 1 + random.nextInt(3));
                break;
            case SPIDER:
                add(drops, Material.STRING, 1 + random.nextInt(3));
                if (random.nextDouble() < 0.5) add(drops, Material.SPIDER_EYE, 1);
                break;
            case CAVE_SPIDER:
                add(drops, Material.STRING, 1 + random.nextInt(2));
                if (random.nextDouble() < 0.7) add(drops, Material.SPIDER_EYE, 1);
                break;
            case SLIME:
                add(drops, Material.SLIME_BALL, 1 + random.nextInt(3));
                break;
            case BLAZE:
                add(drops, Material.BLAZE_ROD, 1 + random.nextInt(2));
                break;
            case MAGMA_CUBE:
                add(drops, Material.MAGMA_CREAM, 1 + random.nextInt(2));
                break;
            case CREEPER:
                add(drops, Material.GUNPOWDER, 1 + random.nextInt(3));
                break;
            case ENDERMAN:
                if (random.nextDouble() < 0.7) add(drops, Material.ENDER_PEARL, 1);
                break;
            case WITCH:
                if (random.nextDouble() < 0.5) add(drops, Material.GLOWSTONE_DUST, 1 + random.nextInt(3));
                if (random.nextDouble() < 0.5) add(drops, Material.GUNPOWDER, 1 + random.nextInt(2));
                if (random.nextDouble() < 0.3) add(drops, Material.SPIDER_EYE, 1);
                if (random.nextDouble() < 0.3) add(drops, Material.GLASS_BOTTLE, 1 + random.nextInt(2));
                break;
            case RABBIT:
                add(drops, Material.RABBIT_HIDE, 1);
                if (random.nextDouble() < 0.1) add(drops, Material.RABBIT_FOOT, 1);
                break;
            default:
                break;
        }

        return drops;
    }

    private void add(List<ItemStack> drops, Material material, int amount) {
        if (!SpawnerPriceRegistry.isSpawnerSellable(material)) return;
        drops.add(createItem(material, amount));
    }

    private ItemStack createItem(Material material, int amount) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String color = getItemColor(material);
        String name = formatName(material);

        meta.setDisplayName(ChatColor.RED + "Spawner Drops " + ChatColor.DARK_GRAY + "• " + color + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Obtained from a mob spawner.",
                ChatColor.GRAY + "Sell price differs from normal drops."
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(spawnerDropKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(spawnerSellPriceKey, PersistentDataType.INTEGER, SpawnerPriceRegistry.getSpawnerSellPrice(material));

        item.setItemMeta(meta);
        return item;
    }

    private String getItemColor(Material material) {
        if (material == Material.ROTTEN_FLESH) return ChatColor.DARK_GREEN.toString();
        if (material == Material.BONE) return ChatColor.WHITE.toString();
        if (material == Material.ARROW) return ChatColor.WHITE.toString();
        if (material == Material.FEATHER) return ChatColor.WHITE.toString();
        if (material == Material.STRING) return ChatColor.WHITE.toString();
        if (material == Material.SPIDER_EYE) return ChatColor.DARK_RED.toString();
        if (material == Material.GUNPOWDER) return ChatColor.GRAY.toString();
        if (material == Material.SLIME_BALL) return ChatColor.GREEN.toString();
        if (material == Material.MAGMA_CREAM) return ChatColor.GOLD.toString();
        if (material == Material.BLAZE_ROD) return ChatColor.GOLD.toString();
        if (material == Material.BLAZE_POWDER) return ChatColor.GOLD.toString();
        if (material == Material.GHAST_TEAR) return ChatColor.LIGHT_PURPLE.toString();
        if (material == Material.PHANTOM_MEMBRANE) return ChatColor.AQUA.toString();
        if (material == Material.INK_SAC) return ChatColor.DARK_GRAY.toString();
        if (material == Material.GLOW_INK_SAC) return ChatColor.AQUA.toString();
        if (material == Material.PRISMARINE_SHARD) return ChatColor.AQUA.toString();
        if (material == Material.PRISMARINE_CRYSTALS) return ChatColor.AQUA.toString();
        if (material == Material.RABBIT_HIDE) return ChatColor.YELLOW.toString();
        if (material == Material.RABBIT_FOOT) return ChatColor.LIGHT_PURPLE.toString();
        if (material == Material.SCUTE) return ChatColor.GREEN.toString();
        if (material == Material.ENDER_PEARL) return ChatColor.LIGHT_PURPLE.toString();
        if (material == Material.EXPERIENCE_BOTTLE) return ChatColor.GREEN.toString();
        if (material == Material.GLOWSTONE_DUST) return ChatColor.YELLOW.toString();
        if (material == Material.GLASS_BOTTLE) return ChatColor.WHITE.toString();
        return ChatColor.WHITE.toString();
    }

    private String formatName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            builder.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1).toLowerCase())
                    .append(" ");
        }
        return builder.toString().trim();
    }

    public boolean isSpawnerDrop(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(spawnerDropKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public int getSpawnerDropSellPrice(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer value = meta.getPersistentDataContainer().get(spawnerSellPriceKey, PersistentDataType.INTEGER);
        return value != null ? value : 0;
    }
}
