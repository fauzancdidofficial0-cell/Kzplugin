// ============================================================
// Path: src/main/java/com/kz/plugin/listeners/PlayerEventListener.java
// ============================================================
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

    // ════════════════════════════════════════════════════════════════
    //  LOGIN - Maintenance gate
    // ════════════════════════════════════════════════════════════════
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.getLobbySystem().isMaintenance()
                && !event.getPlayer().hasPermission("kzplugin.admin")) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                "§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later.");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  JOIN - Load economy + scoreboard + nametag
    // ════════════════════════════════════════════════════════════════
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.setJoinMessage(null);

        plugin.getEconomyManager().loadPlayer(
            player.getUniqueId(),
            player.getName()
        ).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLobbySystem().handleJoin(player);
            });
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  QUIT - Save economy + unload + cleanup
    // ════════════════════════════════════════════════════════════════
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null);

        plugin.getLobbySystem().handleQuit(player);
        plugin.getTpaSystem().removeRequests(player.getUniqueId());
        plugin.getEconomyManager().unloadPlayer(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  CHAT - Rank-based format: [Rank] PlayerName : message
    // ════════════════════════════════════════════════════════════════
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        plugin.getLobbySystem().handleChat(event);
    }

    // ════════════════════════════════════════════════════════════════
    //  NPC CLICK - ArmorStand interaction
    // ════════════════════════════════════════════════════════════════
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();

        if (!(entity instanceof ArmorStand)) return;
        if (!plugin.getLobbySystem().isNPC(entity)) return;

        event.setCancelled(true);
        plugin.getLobbySystem().handleNPCClick(event.getPlayer(), entity);
    }

    // ════════════════════════════════════════════════════════════════
    //  WORLD CHANGE - Refresh scoreboard + nametag + mode display
    // ════════════════════════════════════════════════════════════════
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            plugin.getLobbySystem().updateScoreboard(player);
            plugin.getLobbySystem().updateNametag(player);

            String mode     = plugin.getEconomyManager().getPlayerMode(player);
            String modeName = plugin.getEconomyManager().getModeName(mode);
            double bal      = plugin.getEconomyManager().getBalance(player);

            player.sendMessage("");
            player.sendMessage("§b§lKZ §8» §7World changed! Now in: " + modeName);
            player.sendMessage("§b§lKZ §8» §7Balance here: §a"
                    + plugin.getEconomyManager().formatBalance(bal));
            player.sendMessage("");
        }, 5L);
    }

    // ════════════════════════════════════════════════════════════════
    //  MOVE - Land border detection
    // ════════════════════════════════════════════════════════════════
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to   = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockZ() == to.getBlockZ()
                && from.getBlockY() == to.getBlockY()) return;

        Player player = event.getPlayer();

        LandSystem.LandData fromLand = plugin.getLandSystem().getLandAt(from);
        LandSystem.LandData toLand   = plugin.getLandSystem().getLandAt(to);

        boolean wasInClaim = fromLand != null;
        boolean nowInClaim = toLand   != null;

        if (!wasInClaim && nowInClaim) {
            if (toLand.owner.equals(player.getUniqueId())) {
                sendActionBar(player,
                        "§a🏠 §7Entering §ayour land §8- §f" + toLand.name);
            } else {
                String ownerName = Bukkit.getOfflinePlayer(toLand.owner).getName();
                sendActionBar(player,
                        "§e🔒 §7Entering §f" + toLand.name
                        + " §8(§7owned by §f" + ownerName + "§8)");
            }
            player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, 1.5f);

        } else if (wasInClaim && !nowInClaim) {
            sendActionBar(player, "§7🌍 Leaving claimed land");

        } else if (wasInClaim && nowInClaim) {
            if (fromLand != toLand && !fromLand.name.equals(toLand.name)) {
                if (toLand.owner.equals(player.getUniqueId())) {
                    sendActionBar(player,
                            "§a🏠 §7Now in §ayour land §8- §f" + toLand.name);
                } else {
                    String ownerName = Bukkit.getOfflinePlayer(toLand.owner).getName();
                    sendActionBar(player,
                            "§e🔒 §7Now in §f" + toLand.name
                            + " §8(§7" + ownerName + "§8)");
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  RESPAWN - Island spawn → Lobby fallback
    // ════════════════════════════════════════════════════════════════
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (plugin.getIslandSystem().hasIsland(player.getUniqueId())) {
            Location spawn = plugin.getIslandSystem()
                    .getIsland(player.getUniqueId()).spawnPoint;
            if (spawn != null) {
                event.setRespawnLocation(spawn);
                return;
            }
        }

        Location lobby = plugin.getLobbySystem().getLobbySpawn();
        if (lobby != null) event.setRespawnLocation(lobby);
    }

    // ════════════════════════════════════════════════════════════════
    //  COMMAND PERMISSION - Deny + custom message if no permission
    // ════════════════════════════════════════════════════════════════
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("kzplugin.admin")) return;

        String rawCmd      = event.getMessage().split(" ")[0].toLowerCase().replace("/", "");
        String requiredPerm = getPermissionForCommand(rawCmd);

        if (requiredPerm == null) return;

        if (!player.hasPermission(requiredPerm)) {
            event.setCancelled(true);
            String rank = plugin.getLobbySystem().getRank(player.getUniqueId());

            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §7You don't have permission to use this command.");
            player.sendMessage("§c§lKZ §8» §7Your rank: "
                    + plugin.getLobbySystem().getRankDisplay(rank));
            player.sendMessage("§c§lKZ §8» §7Purchase ranks at: §b/website");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private String getPermissionForCommand(String cmd) {
        return switch (cmd) {
            // Lobby & Info
            case "help"             -> "kzplugin.cmd.help";
            case "rules"            -> "kzplugin.cmd.rules";
            case "lobby", "hub"     -> "kzplugin.cmd.lobby";
            case "spawn"            -> "kzplugin.cmd.spawn";
            case "home"             -> "kzplugin.cmd.island";
            case "stats"            -> "kzplugin.cmd.stats";
            case "rank"             -> "kzplugin.cmd.rank";
            case "discord"          -> "kzplugin.cmd.discord";
            case "website"          -> "kzplugin.cmd.website";

            // Economy
            case "shop"             -> "kzplugin.cmd.shop";
            case "sell"             -> "kzplugin.cmd.sell";
            case "bal", "balance"   -> "kzplugin.cmd.bal";
            case "pay"              -> "kzplugin.cmd.pay";
            case "baltop"           -> "kzplugin.cmd.baltop";
            case "ah", "inbox"      -> "kzplugin.cmd.ah";
            case "cf"               -> "kzplugin.cmd.cf";

            // Island
            case "createisland", "deleteisland", "upisland",
                 "islandsetting", "nameisland", "visit",
                 "topisland", "invite", "accept", "deny",
                 "trust", "untrust"  -> "kzplugin.cmd.island";

            // TPA
            case "tpa", "tpaccept",
                 "tpadeny", "tpcancel" -> "kzplugin.cmd.tpa";

            // Land
            case "cekcapasitas", "landinvite", "landaccept",
                 "landdeny", "landrole", "landkick",
                 "trustland", "memberrule", "trustrule",
                 "setlandname", "deleteland" -> "kzplugin.cmd.land";

            // Job & Rewards
            case "job"              -> "kzplugin.cmd.job";
            case "daily"            -> "kzplugin.cmd.daily";
            case "weekly"           -> "kzplugin.cmd.weekly";

            // Rank-locked commands
            case "nick"             -> "kzplugin.cmd.nick";
            case "hat"              -> "kzplugin.cmd.hat";
            case "enderchest", "ec" -> "kzplugin.cmd.enderchest";
            case "craft", "wb",
                 "workbench"        -> "kzplugin.cmd.craft";
            case "fly"              -> "kzplugin.cmd.fly";
            case "heal"             -> "kzplugin.cmd.heal";
            case "feed"             -> "kzplugin.cmd.feed";
            case "back"             -> "kzplugin.cmd.back";
            case "god"              -> "kzplugin.cmd.god";

            // Admin commands — skip check
            case "setlobby", "setspawn", "createnpc", "removenpc",
                 "listnpc", "givebal", "removebal", "setrank",
                 "maintenance", "announce" -> null;

            // Unknown — don't block
            default -> null;
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY
    // ════════════════════════════════════════════════════════════════
    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(
            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
            net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
        );
    }
}
