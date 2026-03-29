package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.IslandSystem;
import com.kz.plugin.systems.LandSystem;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BlockEventListener implements Listener {

    private final KZPlugin plugin;

    public BlockEventListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ====================================
    //  BLOCK BREAK
    // ====================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // === 1. Lobby protection ===
        if (plugin.getLobbySystem().getLobbySpawn() != null) {
            Location lobby = plugin.getLobbySystem().getLobbySpawn();
            if (loc.getWorld().equals(lobby.getWorld())
                && loc.distance(lobby) <= 50
                && !player.hasPermission("kz.admin")) {
                event.setCancelled(true);
                return;
            }
        }

        // === 2. Island - OneBlock check ===
        IslandSystem.IslandData island = plugin.getIslandSystem().getIslandAt(loc);
        if (island != null) {
            // OneBlock mode
            if (island.mode.equalsIgnoreCase("oneblock")
                && loc.getBlockX() == island.center.getBlockX()
                && loc.getBlockY() == island.center.getBlockY()
                && loc.getBlockZ() == island.center.getBlockZ()) {

                // Only owner/team can break the oneblock
                if (!island.owner.equals(player.getUniqueId())
                    && !island.team.contains(player.getUniqueId())) {
                    event.setCancelled(true);
                    player.sendMessage("§c§lKZ §8» §7You cannot break this block.");
                    return;
                }

                // Process oneblock break
                island.blocksBroken++;
                plugin.getOneBlockSystem().processBreak(player, island.center, island.blocksBroken);
                plugin.getIslandSystem().saveAll();
                return;
            }

            // Not the block owner/team
            if (!plugin.getIslandSystem().canInteract(player.getUniqueId(), loc)) {
                event.setCancelled(true);
                player.sendMessage("§c§lKZ §8» §7You cannot break blocks here.");
                return;
            }
        }

        // === 3. Land protection ===
        LandSystem.LandData land = plugin.getLandSystem().getLandAt(loc);
        if (land != null) {
            String cid = land.id;
            if (!plugin.getLandSystem().checkPermission(cid, player.getUniqueId(), "break")) {
                event.setCancelled(true);
                player.sendMessage("§c§lKZ §8» §7You don't have permission to break here.");
                return;
            }
        }

        // === 4. Job processing ===
        String job = plugin.getJobSystem().getJob(player.getUniqueId());
        if (job != null) {
            switch (job) {
                case "miner":
                    plugin.getJobSystem().processMining(player, block.getType());
                    break;
                case "farmer":
                    plugin.getJobSystem().processFarming(player, block.getType());
                    break;
            }
        }
    }

    // ====================================
    //  BLOCK PLACE
    // ====================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // === 1. Lobby protection ===
        if (plugin.getLobbySystem().getLobbySpawn() != null) {
            Location lobby = plugin.getLobbySystem().getLobbySpawn();
            if (loc.getWorld().equals(lobby.getWorld())
                && loc.distance(lobby) <= 50
                && !player.hasPermission("kz.admin")) {
                event.setCancelled(true);
                return;
            }
        }

        // === 2. Island protection ===
        IslandSystem.IslandData island = plugin.getIslandSystem().getIslandAt(loc);
        if (island != null) {
            if (!plugin.getIslandSystem().canInteract(player.getUniqueId(), loc)) {
                event.setCancelled(true);
                player.sendMessage("§c§lKZ §8» §7You cannot place blocks here.");
                return;
            }
        }

        // === 3. Land protection ===
        LandSystem.LandData land = plugin.getLandSystem().getLandAt(loc);
        if (land != null) {
            if (!plugin.getLandSystem().checkPermission(land.id, player.getUniqueId(), "build")) {
                event.setCancelled(true);
                player.sendMessage("§c§lKZ §8» §7You don't have permission to build here.");
                return;
            }
        }
    }

    // ====================================
    //  GOLDEN SHOVEL → Land Claiming
    // ====================================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GOLDEN_SHOVEL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        event.setCancelled(true);

        // Delegate ke LandSystem
        plugin.getLandSystem().handleGoldenShovelClick(player, block.getLocation());
    }

    // ====================================
    //  BLOCK EXPLODE → Land/Island protection
    // ====================================
    @EventHandler
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            Location loc = block.getLocation();
            // Protect claimed land
            if (plugin.getLandSystem().getLandAt(loc) != null) return true;
            // Protect islands
            if (plugin.getIslandSystem().getIslandAt(loc) != null) return true;
            return false;
        });
    }
}
