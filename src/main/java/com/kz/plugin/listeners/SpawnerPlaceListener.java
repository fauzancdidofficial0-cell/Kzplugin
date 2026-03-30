package com.kz.plugin.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SpawnerPlaceListener implements Listener {

    private final NamespacedKey spawnerTypeKey;

    public SpawnerPlaceListener(JavaPlugin plugin) {
        this.spawnerTypeKey = new NamespacedKey(plugin, "shop_spawner_type");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.SPAWNER) return;
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String typeName = meta.getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
        if (typeName == null || typeName.isEmpty()) return;

        EntityType type;
        try {
            type = EntityType.valueOf(typeName);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof CreatureSpawner spawner)) return;

        spawner.setSpawnedType(type);
        spawner.setDelay(200);
        spawner.setMinSpawnDelay(200);
        spawner.setMaxSpawnDelay(200);
        spawner.setSpawnCount(1 + (int) (Math.random() * 3));
        spawner.setMaxNearbyEntities(10);
        spawner.setRequiredPlayerRange(16);
        spawner.setSpawnRange(4);
        spawner.update(true);
    }
}
