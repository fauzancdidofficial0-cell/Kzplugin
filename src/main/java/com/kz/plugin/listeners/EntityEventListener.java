package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.IslandSystem;
import com.kz.plugin.systems.LandSystem;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;

public class EntityEventListener implements Listener {

    private final KZPlugin plugin;

    public EntityEventListener(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ====================================
    //  MOB KILL → Hunter Job
    // ====================================
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;
        if (entity instanceof Player) return;

        // Job reward
        plugin.getJobSystem().processKill(killer, entity.getType());

        // Kill stat
        plugin.getLobbySystem().addKill(killer.getUniqueId());
    }

    // ====================================
    //  PLAYER DEATH → death stat
    // ====================================
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getLobbySystem().addDeath(event.getEntity().getUniqueId());
    }

    // ====================================
    //  PvP & DAMAGE PROTECTION
    // ====================================
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // === NPC Protection (ArmorStand) ===
        if (event.getEntity() instanceof ArmorStand) {
            ArmorStand as = (ArmorStand) event.getEntity();
            if (plugin.getLobbySystem().isNPC(as)) {
                event.setCancelled(true);
                return;
            }
        }

        // === PvP Check ===
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = getAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;

        Location victimLoc = victim.getLocation();

        // No PvP di lobby area
        Location lobby = plugin.getLobbySystem().getLobbySpawn();
        if (lobby != null && victimLoc.getWorld().equals(lobby.getWorld())
            && victimLoc.distance(lobby) <= 50) {
            event.setCancelled(true);
            attacker.sendMessage("§c§lKZ §8» §7PvP is not allowed in the lobby!");
            return;
        }

        // No PvP di island (kecuali owner toggle)
        IslandSystem.IslandData island =
            plugin.getIslandSystem().getIslandAt(victimLoc);
        if (island != null) {
            event.setCancelled(true);
            attacker.sendMessage("§c§lKZ §8» §7PvP is not allowed on islands!");
            return;
        }

        // No PvP di claimed land (kecuali rule pvp aktif)
        LandSystem.LandData land = plugin.getLandSystem().getLandAt(victimLoc);
        if (land != null) {
            boolean pvpAllowed = land.memberRules.getOrDefault("pvp", false);
            if (!pvpAllowed) {
                event.setCancelled(true);
                attacker.sendMessage("§c§lKZ §8» §7PvP is not allowed in this land!");
                return;
            }
        }
    }

    // ====================================
    //  FALL DAMAGE & LOBBY DAMAGE
    // ====================================
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        Location loc = player.getLocation();
        Location lobby = plugin.getLobbySystem().getLobbySpawn();

        if (lobby != null && loc.getWorld().equals(lobby.getWorld())
            && loc.distance(lobby) <= 50) {
            event.setCancelled(true);
            return;
        }

        // No fall damage di island spawn area
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            IslandSystem.IslandData island =
                plugin.getIslandSystem().getIslandAt(loc);
            if (island != null && island.spawnPoint != null
                && loc.distance(island.spawnPoint) <= 10) {
                event.setCancelled(true);
            }
        }
    }

    // ====================================
    //  ENTITY INTERACT → Land rules
    // ====================================
    @EventHandler
    public void onPlayerInteractEntity(
        org.bukkit.event.player.PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        // Skip NPC (handled by PlayerEventListener)
        if (entity instanceof ArmorStand
            && plugin.getLobbySystem().isNPC(entity)) return;

        Location loc = entity.getLocation();
        LandSystem.LandData land = plugin.getLandSystem().getLandAt(loc);
        if (land == null) return;

        // Check relevant rules
        String rule = null;
        if (entity instanceof Animals) rule = "feedanimal";
        else if (entity instanceof Minecart) rule = "minecart";
        else if (entity instanceof Boat) rule = "boat";

        if (rule != null) {
            if (!plugin.getLandSystem().checkPermission(
                land.id, player.getUniqueId(), rule)) {
                event.setCancelled(true);
                player.sendMessage("§c§lKZ §8» §7You don't have permission.");
            }
        }
    }

    // ====================================
    //  UTILITY
    // ====================================
    private Player getAttacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player)
                return (Player) proj.getShooter();
        }
        if (damager instanceof Tameable) {
            Tameable tamed = (Tameable) damager;
            if (tamed.getOwner() instanceof Player)
                return (Player) tamed.getOwner();
        }
        return null;
    }
}
