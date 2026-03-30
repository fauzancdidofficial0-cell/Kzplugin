package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.LandSystem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class PlayerEventListener implements Listener {

    private final KZPlugin plugin;

    public PlayerEventListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================
    //  LOGIN - Maintenance check
    // ========================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.getLobbySystem().isMaintenance()
            && !event.getPlayer().hasPermission("kz.admin")) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                "§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later.");
        }
    }

    // ========================
    //  JOIN
    // ========================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null); // LobbySystem handle broadcast
        plugin.getLobbySystem().handleJoin(player);
    }

    // ========================
    //  QUIT
    // ========================
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null); // LobbySystem handle broadcast
        plugin.getLobbySystem().handleQuit(player);

        // Cleanup TPA requests
        plugin.getTpaSystem().removeRequests(player.getUniqueId());
    }

    // ========================
    //  NPC CLICK
    // ========================
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Check if it's a NPC (ArmorStand registered in LobbySystem)
        if (!(entity instanceof ArmorStand)) return;
        if (!plugin.getLobbySystem().isNPC(entity)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.getLobbySystem().handleNPCClick(player, entity);
    }

    // ========================
    //  MOVE - Land border detection
    // ========================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Optimasi: skip kalau gak pindah block
        if (from.getBlockX() == to.getBlockX()
            && from.getBlockZ() == to.getBlockZ()
            && from.getBlockY() == to.getBlockY()) return;

        Player player = event.getPlayer();

        // Land border detection
        boolean wasInClaim = plugin.getLandSystem().getLandAt(from) != null;
        boolean nowInClaim = plugin.getLandSystem().getLandAt(to) != null;

        if (!wasInClaim && nowInClaim) {
            // Masuk ke claimed land
            LandSystem.LandData land = plugin.getLandSystem().getLandAt(to);
            if (land != null) {
                if (land.owner.equals(player.getUniqueId())) {
                    sendActionBar(player, "§a🏠 §7Entering §ayour land §8- §f" + land.name);
                } else {
                    String ownerName = Bukkit.getOfflinePlayer(land.owner).getName();
                    sendActionBar(player, "§e🔒 §7Entering §f" + land.name
                        + " §8(§7owned by §f" + ownerName + "§8)");
                }
                player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1.5f);
            }
        } else if (wasInClaim && !nowInClaim) {
            sendActionBar(player, "§7🌍 Leaving claimed land");
        }
    }

    // ========================
    //  RESPAWN
    // ========================
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Respawn di island jika punya
        if (plugin.getIslandSystem().hasIsland(player.getUniqueId())) {
            Location spawn = plugin.getIslandSystem()
                .getIsland(player.getUniqueId()).spawnPoint;
            if (spawn != null) {
                event.setRespawnLocation(spawn);
                return;
            }
        }

        // Respawn di lobby
        Location lobby = plugin.getLobbySystem().getLobbySpawn();
        if (lobby != null) event.setRespawnLocation(lobby);
    }

    // ========================
    //  UTILITY
    // ========================
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
        );
    }
}
