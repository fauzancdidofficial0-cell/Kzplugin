// ============================================================
// PATH: src/main/java/com/kz/plugin/listeners/MenuListener.java
// ============================================================
package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    private static final String MENU_TITLE = "§8TPI Menu";
    private final KZPlugin plugin;

    public MenuListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().equals(MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Material type = event.getCurrentItem().getType();

        if (type == Material.GRASS_BLOCK) {
            teleportToPasar(player);
            return;
        }

        if (type == Material.OBSIDIAN) {
            player.sendMessage("§7Fitur ini §8(Coming Soon) §7belum tersedia.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
        }
    }

    private void teleportToPasar(Player player) {
        FileConfiguration config = plugin.getConfig();

        if (!config.contains("teleport.pasar.world")) {
            player.sendMessage("§cLokasi Pasar belum di-set admin. Gunakan /settppasar");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String worldName = config.getString("teleport.pasar.world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            player.sendMessage("§cWorld untuk Pasar tidak ditemukan.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        double x = config.getDouble("teleport.pasar.x");
        double y = config.getDouble("teleport.pasar.y");
        double z = config.getDouble("teleport.pasar.z");
        float yaw = (float) config.getDouble("teleport.pasar.yaw");
        float pitch = (float) config.getDouble("teleport.pasar.pitch");

        Location loc = new Location(world, x, y, z, yaw, pitch);
        player.teleport(loc);
        player.closeInventory();
        player.sendMessage("§aKamu diteleport ke §ePasar§a.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }
}
