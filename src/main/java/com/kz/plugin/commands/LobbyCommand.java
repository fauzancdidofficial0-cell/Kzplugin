package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class LobbyCommand implements CommandExecutor {

    private final KZPlugin plugin;

    public LobbyCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        Player player = (Player) sender;
        String command = cmd.getName().toLowerCase();

        switch (command) {
            case "lobby":
                handleLobby(player);
                break;
            case "spawn":
                handleSpawn(player);
                break;
            case "help":
                handleHelp(player);
                break;
            case "stats":
                handleStats(player, args);
                break;
            case "rank":
                handleRank(player);
                break;
            case "discord":
                handleDiscord(player);
                break;
            case "website":
                handleWebsite(player);
                break;
            case "rules":
                handleRules(player);
                break;
        }

        return true;
    }

    // ══════════════════════════════════════
    //  LOBBY
    // ══════════════════════════════════════

    private void handleLobby(Player player) {
        Location lobby = plugin.getLobbySystem().getLobbySpawn();
        if (lobby == null) {
            player.sendMessage("§c§lKZ §8» §7Lobby spawn has not been set.");
            return;
        }
        player.teleport(lobby);
        player.sendMessage("§a§lKZ §8» §7Teleported to lobby.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  SPAWN
    // ══════════════════════════════════════

    private void handleSpawn(Player player) {
        // Kalau punya island, teleport ke island
        if (plugin.getIslandSystem().hasIsland(player.getUniqueId())) {
            plugin.getIslandSystem().teleportHome(player);
            return;
        }

        // Kalau member di island orang
        java.util.UUID ownerUUID = plugin.getIslandSystem().getOwnerOf(player.getUniqueId());
        if (ownerUUID != null && plugin.getIslandSystem().hasIsland(ownerUUID)) {
            plugin.getIslandSystem().teleportHome(player);
            return;
        }

        // Fallback ke lobby
        handleLobby(player);
    }

    // ══════════════════════════════════════
    //  HELP
    // ══════════════════════════════════════

    private void handleHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  KZ SERVER - COMMAND LIST");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("§e§l  ⛏ ISLAND");
        player.sendMessage("§f  /createisland <mode> §8- §7Create island");
        player.sendMessage("§f  /deleteisland §8- §7Delete island");
        player.sendMessage("§f  /home §8- §7Teleport to island");
        player.sendMessage("§f  /upisland §8- §7Upgrade island border");
        player.sendMessage("§f  /islandsetting §8- §7Island info");
        player.sendMessage("§f  /nameisland <name> §8- §7Rename island");
        player.sendMessage("§f  /visit <player> §8- §7Visit island");
        player.sendMessage("§f  /topisland §8- §7Top 10 islands");
        player.sendMessage("§f  /invite <player> §8- §7Invite to island");
        player.sendMessage("§f  /accept / /deny §8- §7Accept/deny invite");
        player.sendMessage("§f  /trust <player> §8- §7Trust player");
        player.sendMessage("§f  /untrust <player> §8- §7Untrust player");
        player.sendMessage("");
        player.sendMessage("§e§l  💰 ECONOMY");
        player.sendMessage("§f  /bal §8- §7Check balance");
        player.sendMessage("§f  /pay <player> <amount> §8- §7Transfer money");
        player.sendMessage("§f  /baltop §8- §7Top 10 richest");
        player.sendMessage("§f  /cf <amount> §8- §7Coinflip gamble");
        player.sendMessage("§f  /shop §8- §7Open shop");
        player.sendMessage("§f  /sell §8- §7Sell items");
        player.sendMessage("§f  /ah §8- §7Auction house");
        player.sendMessage("§f  /inbox §8- §7Expired auction items");
        player.sendMessage("");
        player.sendMessage("§e§l  🏠 LAND");
        player.sendMessage("§f  §7Use §fGolden Shovel §7to claim land");
        player.sendMessage("§f  /cekcapasitas §8- §7Land info");
        player.sendMessage("§f  /landinvite <player> §8- §7Invite to land");
        player.sendMessage("§f  /landaccept / /landdeny §8- §7Accept/deny");
        player.sendMessage("§f  /landrole <player> <role> §8- §7Set role");
        player.sendMessage("§f  /landkick <player> §8- §7Kick from land");
        player.sendMessage("§f  /trustland <player> §8- §7Trust on land");
        player.sendMessage("§f  /memberrule §8- §7Member permissions");
        player.sendMessage("§f  /trustrule §8- §7Trust permissions");
        player.sendMessage("§f  /setlandname <name> §8- §7Rename land");
        player.sendMessage("§f  /deleteland §8- §7Delete land");
        player.sendMessage("");
        player.sendMessage("§e§l  📦 OTHER");
        player.sendMessage("§f  /job [miner|farmer|hunter] §8- §7Job system");
        player.sendMessage("§f  /daily §8- §7Daily reward");
        player.sendMessage("§f  /weekly §8- §7Weekly reward");
        player.sendMessage("§f  /tpa <player> §8- §7Teleport request");
        player.sendMessage("§f  /tpaccept / /tpadeny §8- §7Accept/deny TPA");
        player.sendMessage("§f  /lobby §8- §7Back to lobby");
        player.sendMessage("§f  /stats [player] §8- §7Player stats");
        player.sendMessage("§f  /rank §8- §7Rank info");
        player.sendMessage("§f  /discord §8- §7Discord link");
        player.sendMessage("§f  /rules §8- §7Server rules");
        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════

    private void handleStats(Player player, String[] args) {
        Player target = player;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c§lKZ §8» §7Player is not online.");
                return;
            }
        }

        plugin.getLobbySystem().showStats(player, target);
    }

    // ══════════════════════════════════════
    //  RANK
    // ══════════════════════════════════════

    private void handleRank(Player player) {
        String rank = plugin.getLobbySystem().getRank(player.getUniqueId());

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  RANK SYSTEM");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("§7  Your Rank: §b" + rank);
        player.sendMessage("");
        player.sendMessage("§7  Available Ranks:");
        player.sendMessage("§f    Member §8→ §7Default rank");
        player.sendMessage("§f    Iron §8→ §7Land 30x30");
        player.sendMessage("§f    Gold §8→ §7Land 35x35");
        player.sendMessage("§f    Diamond §8→ §7Land 40x40");
        player.sendMessage("§f    Emerald §8→ §7Land 50x50");
        player.sendMessage("§f    Obsidian §8→ §7Land 60x60");
        player.sendMessage("§f    Onyx §8→ §7Land 75x75");
        player.sendMessage("§f    Phantom §8→ §7Land 100x100");
        player.sendMessage("§f    Eclipse §8→ §7Land 150x150");
        player.sendMessage("§f    Ethereal §8→ §7Land 200x200");
        player.sendMessage("");
        player.sendMessage("§7  Visit our store: §b/website");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  DISCORD
    // ══════════════════════════════════════

    private void handleDiscord(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  JOIN OUR DISCORD!");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("§7  §bhttps://discord.gg/kzserver");
        player.sendMessage("");
        player.sendMessage("§7  Get updates, chat with players,");
        player.sendMessage("§7  and report bugs!");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  WEBSITE
    // ══════════════════════════════════════

    private void handleWebsite(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  VISIT OUR WEBSITE!");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("§7  §ehttps://store.kzserver.com");
        player.sendMessage("");
        player.sendMessage("§7  Buy ranks, cosmetics, and more!");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  RULES
    // ══════════════════════════════════════

    private void handleRules(Player player) {
        player.sendMessage("");
        player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  KZ SERVER RULES");
        player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("§f  1. §7No hacking or cheating.");
        player.sendMessage("§f  2. §7No abusive language or bullying.");
        player.sendMessage("§f  3. §7No spamming or advertising.");
        player.sendMessage("§f  4. §7No griefing outside your land.");
        player.sendMessage("§f  5. §7No exploiting bugs (report them).");
        player.sendMessage("§f  6. §7Respect all players and staff.");
        player.sendMessage("§f  7. §7No AFK farming or auto-clickers.");
        player.sendMessage("§f  8. §7No real-money trading (RMT).");
        player.sendMessage("§f  9. §7No inappropriate builds or names.");
        player.sendMessage("§f  10. §7Staff decisions are final.");
        player.sendMessage("");
        player.sendMessage("§7  Violations may result in §cmute§7, §ckick§7,");
        player.sendMessage("§7  or §cpermanent ban§7.");
        player.sendMessage("");
        player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
