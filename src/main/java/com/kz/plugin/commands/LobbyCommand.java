// ============================================================
// Path: src/main/java/com/kz/plugin/commands/LobbyCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.utils.ServerUtils;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LobbyCommand implements CommandExecutor {

    private final KZPlugin plugin;

    public LobbyCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        String command = cmd.getName().toLowerCase();

        switch (command) {
            case "lobby", "hub" -> handleLobby(player);
            case "spawn"        -> handleSpawn(player);
            case "help"         -> handleHelp(player);
            case "stats"        -> handleStats(player, args);
            case "rank"         -> handleRank(player);
            case "discord"      -> handleDiscord(player);
            case "website"      -> handleWebsite(player);
            case "rules"        -> handleRules(player);
        }

        return true;
    }

    // ══════════════════════════════════════
    //  LOBBY - Velocity Proxy Transfer
    // ══════════════════════════════════════

    private void handleLobby(Player player) {
        String currentServer = plugin.getConfig().getString("server-name", "lobby");

        if (currentServer.equalsIgnoreCase("lobby")) {
            Location lobby = plugin.getLobbySystem().getLobbySpawn();
            if (lobby == null) {
                player.sendMessage("§c§lKZ §8» §7Lobby spawn has not been set.");
                return;
            }
            player.teleport(lobby);
            player.sendMessage("§a§lKZ §8» §7Teleported to lobby spawn.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            return;
        }

        player.sendMessage("§a§lKZ §8» §7Sending you to §fLobby§7...");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        ServerUtils.sendToServer(plugin, player, "lobby");
    }

    // ══════════════════════════════════════
    //  SPAWN
    // ══════════════════════════════════════

    private void handleSpawn(Player player) {
        if (plugin.getIslandSystem().hasIsland(player.getUniqueId())) {
            plugin.getIslandSystem().teleportHome(player);
            return;
        }

        UUID ownerUUID = plugin.getIslandSystem().getOwnerOf(player.getUniqueId());
        if (ownerUUID != null && plugin.getIslandSystem().hasIsland(ownerUUID)) {
            plugin.getIslandSystem().teleportHome(player);
            return;
        }

        handleLobby(player);
    }

    // ══════════════════════════════════════
    //  HELP - Rank-Filtered
    // ══════════════════════════════════════

    private void handleHelp(Player player) {
        String rank = plugin.getLobbySystem().getRank(player.getUniqueId()).toLowerCase();
        boolean isStaff = player.hasPermission("kzplugin.admin");

        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│     §f§lKZ SERVER §b§l- §e§lCOMMAND LIST   §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");

        player.sendMessage("");
        player.sendMessage("  §e§l⛏ ISLAND");
        player.sendMessage("  §b/createisland §f<mode> §8- §7Create island");
        player.sendMessage("  §b/deleteisland §8- §7Delete island");
        player.sendMessage("  §b/home §8- §7Teleport to island");
        player.sendMessage("  §b/upisland §8- §7Upgrade island border");
        player.sendMessage("  §b/islandsetting §8- §7Island info");
        player.sendMessage("  §b/nameisland §f<name> §8- §7Rename island");
        player.sendMessage("  §b/visit §f<player> §8- §7Visit island");
        player.sendMessage("  §b/topisland §8- §7Top 10 islands");
        player.sendMessage("  §b/invite §f<player> §8- §7Invite to island");
        player.sendMessage("  §b/accept §8/ §b/deny §8- §7Accept/deny invite");
        player.sendMessage("  §b/trust §f<player> §8- §7Trust player");
        player.sendMessage("  §b/untrust §f<player> §8- §7Untrust player");

        player.sendMessage("");
        player.sendMessage("  §e§l💰 ECONOMY");
        player.sendMessage("  §b/bal §8- §7Check balance");
        player.sendMessage("  §b/pay §f<player> <amount> §8- §7Transfer money");
        player.sendMessage("  §b/baltop §8- §7Top 10 richest");
        player.sendMessage("  §b/cf §f<amount> §8- §7Coinflip gamble");
        player.sendMessage("  §b/shop §8- §7Open shop");
        player.sendMessage("  §b/sell §8- §7Sell items");
        player.sendMessage("  §b/ah §8- §7Auction house");
        player.sendMessage("  §b/inbox §8- §7Expired auction items");

        player.sendMessage("");
        player.sendMessage("  §e§l🏠 LAND");
        player.sendMessage("  §7Use §fGolden Shovel §7to claim land");
        player.sendMessage("  §b/cekcapasitas §8- §7Land info");
        player.sendMessage("  §b/landinvite §f<player> §8- §7Invite to land");
        player.sendMessage("  §b/landaccept §8/ §b/landdeny §8- §7Accept/deny");
        player.sendMessage("  §b/landrole §f<player> <role> §8- §7Set role");
        player.sendMessage("  §b/landkick §f<player> §8- §7Kick from land");
        player.sendMessage("  §b/trustland §f<player> §8- §7Trust on land");
        player.sendMessage("  §b/memberrule §8- §7Member permissions");
        player.sendMessage("  §b/trustrule §8- §7Trust permissions");
        player.sendMessage("  §b/setlandname §f<name> §8- §7Rename land");
        player.sendMessage("  §b/deleteland §8- §7Delete land");

        player.sendMessage("");
        player.sendMessage("  §e§l📦 GENERAL");
        player.sendMessage("  §b/job §f[miner|farmer|hunter] §8- §7Job system");
        player.sendMessage("  §b/daily §8- §7Daily reward");
        player.sendMessage("  §b/weekly §8- §7Weekly reward");
        player.sendMessage("  §b/tpa §f<player> §8- §7Teleport request");
        player.sendMessage("  §b/tpaccept §8/ §b/tpadeny §8- §7Accept/deny TPA");
        player.sendMessage("  §b/lobby §8- §7Back to lobby");
        player.sendMessage("  §b/stats §f[player] §8- §7Player stats");
        player.sendMessage("  §b/rank §8- §7Rank info");
        player.sendMessage("  §b/discord §8- §7Discord link");
        player.sendMessage("  §b/rules §8- §7Server rules");

        if (isStaff) {
            player.sendMessage("");
            player.sendMessage("  §c§l⚙ ADMIN");
            player.sendMessage("  §c/setlobby §8- §7Set lobby spawn");
            player.sendMessage("  §c/setrank §f<player> <rank> §8- §7Set rank");
            player.sendMessage("  §c/maintenance §8- §7Toggle maintenance");
            player.sendMessage("  §c/createnpc §f<mode> <name> §8- §7Create NPC");
            player.sendMessage("  §c/removenpc §8- §7Remove nearby NPC");
            player.sendMessage("  §c/listnpc §8- §7List all NPCs");
            player.sendMessage("  §c/eco §f<set|add|remove> <player> <amount> §8- §7Manage economy");
            player.sendMessage("  §c/clearlag §8- §7Force clear items");
        }

        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│  §7Your Rank: " + plugin.getLobbySystem().getRankDisplay(rank) + "          §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");
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
                player.sendMessage("§c§lKZ §8» §7That player is not online.");
                return;
            }
        }

        plugin.getLobbySystem().showStats(player, target);
    }

    // ══════════════════════════════════════
    //  RANK - Enhanced Display
    // ══════════════════════════════════════

    private void handleRank(Player player) {
        String rank = plugin.getLobbySystem().getRank(player.getUniqueId());
        int maxLand = plugin.getLobbySystem().getMaxLandSize(rank);
        int maxClaims = plugin.getLobbySystem().getMaxClaims(rank);
        int maxHomes = plugin.getLobbySystem().getMaxHomes(rank);

        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│        §f§lRANK SYSTEM              §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §7Your Rank: " + plugin.getLobbySystem().getRankDisplay(rank));
        player.sendMessage("  §7Max Land: §f" + maxLand + "x" + maxLand + " §8| §7Claims: §f" + maxClaims + " §8| §7Homes: §f" + maxHomes);
        player.sendMessage("");
        player.sendMessage("  §f§lAvailable Ranks:");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("member") + " §8→ §7Land §f25x25 §8| §7Claims §f1 §8| §7Homes §f1");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("iron") + " §8→ §7Land §f30x30 §8| §7Claims §f2 §8| §7Homes §f2");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("gold") + " §8→ §7Land §f35x35 §8| §7Claims §f3 §8| §7Homes §f2");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("diamond") + " §8→ §7Land §f40x40 §8| §7Claims §f4 §8| §7Homes §f3");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("emerald") + " §8→ §7Land §f50x50 §8| §7Claims §f5 §8| §7Homes §f3");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("obsidian") + " §8→ §7Land §f60x60 §8| §7Claims §f6 §8| §7Homes §f4");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("onyx") + " §8→ §7Land §f75x75 §8| §7Claims §f8 §8| §7Homes §f5");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("phantom") + " §8→ §7Land §f100x100 §8| §7Claims §f10 §8| §7Homes §f6");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("eclipse") + " §8→ §7Land §f150x150 §8| §7Claims §f15 §8| §7Homes §f8");
        player.sendMessage("  §7" + plugin.getLobbySystem().getRankDisplay("ethereal") + " §8→ §7Land §f200x200 §8| §7Claims §f20 §8| §7Homes §f10");
        player.sendMessage("");
        player.sendMessage("  §7Purchase ranks at: §b/website");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  DISCORD
    // ══════════════════════════════════════

    private void handleDiscord(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│     §f§lJOIN OUR DISCORD!          §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §bhttps://discord.gg/kzserver");
        player.sendMessage("");
        player.sendMessage("  §7Get updates, chat with players,");
        player.sendMessage("  §7and report bugs!");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  WEBSITE
    // ══════════════════════════════════════

    private void handleWebsite(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│    §f§lVISIT OUR STORE!            §6§l│");
        player.sendMessage("§6§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §ehttps://store.kzserver.com");
        player.sendMessage("");
        player.sendMessage("  §7Buy ranks, cosmetics, and more!");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  RULES
    // ══════════════════════════════════════

    private void handleRules(Player player) {
        player.sendMessage("");
        player.sendMessage("§c§l┌─────────────────────────────────┐");
        player.sendMessage("§c§l│      §f§lKZ SERVER RULES            §c§l│");
        player.sendMessage("§c§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §f1. §7No hacking or cheating.");
        player.sendMessage("  §f2. §7No abusive language or bullying.");
        player.sendMessage("  §f3. §7No spamming or advertising.");
        player.sendMessage("  §f4. §7No griefing outside your land.");
        player.sendMessage("  §f5. §7No exploiting bugs (report them).");
        player.sendMessage("  §f6. §7Respect all players and staff.");
        player.sendMessage("  §f7. §7No AFK farming or auto-clickers.");
        player.sendMessage("  §f8. §7No real-money trading (RMT).");
        player.sendMessage("  §f9. §7No inappropriate builds or names.");
        player.sendMessage("  §f10. §7Staff decisions are final.");
        player.sendMessage("");
        player.sendMessage("  §7Violations may result in §cmute§7, §ckick§7,");
        player.sendMessage("  §7or §cpermanent ban§7.");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
