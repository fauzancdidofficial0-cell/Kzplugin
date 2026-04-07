// ============================================================
// Path: src/main/java/com/kz/plugin/commands/LobbyCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.LobbySystem;
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

        switch (cmd.getName().toLowerCase()) {
            case "lobby", "hub" -> handleLobby(player);
            case "spawn"        -> handleSpawn(player);
            case "help"         -> handleHelp(player);
            case "stats"        -> handleStats(player, args);
            case "rank"         -> handleRank(player);
            case "discord"      -> handleDiscord(player);
            case "website"      -> handleWebsite(player);
            case "rules"        -> handleRules(player);
            case "fly"          -> handleFly(player);
            case "vanish"       -> handleVanish(player);
        }

        return true;
    }

    // ══════════════════════════════════════
    //  LOBBY
    // ══════════════════════════════════════

    private void handleLobby(Player player) {
        String currentServer = plugin.getConfig().getString("server-name", "lobby");

        if (currentServer.equalsIgnoreCase("lobby")) {
            Location lobby = plugin.getLobbySystem().getLobbySpawn();
            if (lobby == null) {
                sendMsg(player, "§cLobby spawn has not been set.");
                return;
            }
            player.teleport(lobby);
            sendMsg(player, "§7Teleported to §bLobby§7.");
            playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
            return;
        }

        sendMsg(player, "§7Sending you to §bLobby§7...");
        playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
        ServerUtils.sendToServer(plugin, player, "lobby");
    }

    // ══════════════════════════════════════
    //  SPAWN
    // ══════════════════════════════════════

    private void handleSpawn(Player player) {
        if (plugin.getIslandSystem() != null) {
            if (plugin.getIslandSystem().hasIsland(player.getUniqueId())) {
                plugin.getIslandSystem().teleportHome(player);
                return;
            }
            UUID ownerUUID = plugin.getIslandSystem().getOwnerOf(player.getUniqueId());
            if (ownerUUID != null && plugin.getIslandSystem().hasIsland(ownerUUID)) {
                plugin.getIslandSystem().teleportHome(player);
                return;
            }
        }
        handleLobby(player);
    }

    // ══════════════════════════════════════
    //  FLY
    // ══════════════════════════════════════

    private void handleFly(Player player) {
        if (!player.hasPermission("kzplugin.cmd.fly")
                && !player.hasPermission("kzplugin.admin")) {
            sendMsg(player, "§cYou need §eValiant§c+ rank to use §b/fly§c.");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        boolean nowFlying = !player.getAllowFlight();
        player.setAllowFlight(nowFlying);

        if (nowFlying) {
            sendMsg(player, "§a✈ Flight §oenabled§a.");
            playSound(player, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH);
        } else {
            player.setFlying(false);
            sendMsg(player, "§c✈ Flight §odisabled§c.");
            playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS);
        }
    }

    // ══════════════════════════════════════
    //  VANISH
    // ══════════════════════════════════════

    private void handleVanish(Player player) {
        if (!player.hasPermission("kzplugin.admin")) {
            sendMsg(player, "§cNo permission.");
            playSound(player, Sound.ENTITY_VILLAGER_NO);
            return;
        }

        boolean nowVanished = plugin.getLobbySystem().toggleVanish(player);

        if (nowVanished) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.hasPermission("kzplugin.admin")) {
                    other.hidePlayer(plugin, player);
                }
            }
            sendMsg(player, "§8👻 You are now §ovanished§8. Only admins can see you.");
            playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);
        } else {
            for (Player other : Bukkit.getOnlinePlayers()) {
                other.showPlayer(plugin, player);
            }
            sendMsg(player, "§a👁 You are now §ovisible§a.");
            playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        }
    }

    // ══════════════════════════════════════
    //  HELP
    // ══════════════════════════════════════

    private void handleHelp(Player player) {
        boolean isAdmin = player.hasPermission("kzplugin.admin");

        send(player, "");
        send(player, "§b§l┌──────────────────────────────────┐");
        send(player, "§b§l│   §f§lKZ SERVER §b§l- §e§lCOMMAND LIST    §b§l│");
        send(player, "§b§l└──────────────────────────────────┘");
        send(player, "");
        send(player, "  §e§l⛏ ISLAND");
        send(player, "  §b/createisland §f<mode>  §8│ §7Create island");
        send(player, "  §b/deleteisland          §8│ §7Delete island");
        send(player, "  §b/home                 §8│ §7Go to island");
        send(player, "  §b/upisland              §8│ §7Upgrade border");
        send(player, "  §b/nameisland §f<name>    §8│ §7Rename island");
        send(player, "  §b/visit §f<player>       §8│ §7Visit island");
        send(player, "  §b/topisland             §8│ §7Top 10 islands");
        send(player, "  §b/invite §8/ §b/accept §8/ §b/deny");
        send(player, "  §b/trust §8/ §b/untrust §f<player>");
        send(player, "");
        send(player, "  §e§l💰 ECONOMY");
        send(player, "  §b/bal §f[player]          §8│ §7Balance");
        send(player, "  §b/pay §f<player> <amount> §8│ §7Transfer");
        send(player, "  §b/baltop               §8│ §7Top richest");
        send(player, "  §b/cf §f<amount>          §8│ §7Coinflip");
        send(player, "  §b/shop                 §8│ §7Shop GUI");
        send(player, "  §b/sell                 §8│ §7Sell items");
        send(player, "  §b/ah                   §8│ §7Auction house");
        send(player, "");
        send(player, "  §e§l🏠 LAND");
        send(player, "  §b/cekcapasitas          §8│ §7Land info");
        send(player, "  §b/landinvite §f<player>  §8│ §7Invite");
        send(player, "  §b/landrole §f<player> <role> §8│ §7Set role");
        send(player, "  §b/memberrule §8/ §b/trustrule");
        send(player, "  §b/setlandname §f<name>   §8│ §7Rename land");
        send(player, "  §b/deleteland            §8│ §7Delete land");
        send(player, "");
        send(player, "  §e§l📦 GENERAL");
        send(player, "  §b/job §f[miner|farmer|hunter]");
        send(player, "  §b/daily §8/ §b/weekly      §8│ §7Rewards");
        send(player, "  §b/tpa §f<player>          §8│ §7Teleport req");
        send(player, "  §b/stats §f[player]        §8│ §7Statistics");
        send(player, "  §b/rank                  §8│ §7Rank info");
        send(player, "  §b/discord §8/ §b/rules §8/ §b/website");

        if (player.hasPermission("kzplugin.cmd.fly")) {
            send(player, "  §b/fly                  §8│ §7Toggle flight");
        }

        if (isAdmin) {
            send(player, "");
            send(player, "  §c§l⚙ ADMIN");
            send(player, "  §c/setrank §f<player> <rank>  §8│ §7Set rank");
            send(player, "  §c/maintenance             §8│ §7Toggle maint");
            send(player, "  §c/createnpc §f<mode> <name> §8│ §7Create NPC");
            send(player, "  §c/vanish                  §8│ §7Toggle vanish");
            send(player, "  §c/clearlag                §8│ §7Clear items");
            send(player, "  §c/givebal §f<player> <amt>  §8│ §7Give money");
            send(player, "  §c/announce §f<msg>          §8│ §7Broadcast");
        }

        send(player, "");
        send(player, "§b§l└──────────────────────────────────┘");
        send(player, "");
        playSound(player, Sound.UI_BUTTON_CLICK);
    }

    // ══════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════

    private void handleStats(Player player, String[] args) {
        Player target = player;

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                sendMsg(player, "§cPlayer §f" + args[0] + " §cis not online.");
                playSound(player, Sound.ENTITY_VILLAGER_NO);
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
        LobbySystem.RankData rd = plugin.getLobbySystem().getRankData(rank);

        send(player, "");
        send(player, "§b§l┌──────────────────────────────────┐");
        send(player, "§b§l│         §f§lRANK SYSTEM              §b§l│");
        send(player, "§b§l└──────────────────────────────────┘");
        send(player, "");
        send(player, "  §7Your Rank : " + rd.chatTag());
        send(player, "  §7Max Land  : §f" + rd.maxLandSize() + "x" + rd.maxLandSize());
        send(player, "  §7Claims    : §f" + rd.maxClaims());
        send(player, "  §7Homes     : §f" + rd.maxHomes());
        send(player, "");
        send(player, "  §f§lAll Ranks:");

        for (var entry : plugin.getLobbySystem().getAllRanks().entrySet()) {
            LobbySystem.RankData r = entry.getValue();
            String arrow = entry.getKey().equalsIgnoreCase(rank) ? " §a◄ YOU" : "";
            send(player, "  " + r.chatTag()
                    + " §8│ §7Land §f" + r.maxLandSize() + "x" + r.maxLandSize()
                    + " §8│ §7Claims §f" + r.maxClaims()
                    + " §8│ §7Homes §f" + r.maxHomes()
                    + arrow);
        }

        send(player, "");
        send(player, "  §7Buy rank: §b/website");
        send(player, "");
        playSound(player, Sound.UI_BUTTON_CLICK);
    }

    // ══════════════════════════════════════
    //  DISCORD
    // ══════════════════════════════════════

    private void handleDiscord(Player player) {
        send(player, "");
        send(player, "§9§l┌──────────────────────────────────┐");
        send(player, "§9§l│      §f§lJOIN OUR DISCORD!          §9§l│");
        send(player, "§9§l└──────────────────────────────────┘");
        send(player, "");
        send(player, "  §bhttps://discord.gg/kzserver");
        send(player, "");
        send(player, "  §7Updates · Events · Bug Reports");
        send(player, "");
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    // ══════════════════════════════════════
    //  WEBSITE
    // ══════════════════════════════════════

    private void handleWebsite(Player player) {
        send(player, "");
        send(player, "§6§l┌──────────────────────────────────┐");
        send(player, "§6§l│      §f§lVISIT OUR STORE!           §6§l│");
        send(player, "§6§l└──────────────────────────────────┘");
        send(player, "");
        send(player, "  §ehttps://store.kzserver.com");
        send(player, "");
        send(player, "  §7Ranks · Cosmetics · Keys");
        send(player, "");
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
    }

    // ══════════════════════════════════════
    //  RULES
    // ══════════════════════════════════════

    private void handleRules(Player player) {
        send(player, "");
        send(player, "§c§l┌──────────────────────────────────┐");
        send(player, "§c§l│       §f§lKZ SERVER RULES           §c§l│");
        send(player, "§c§l└──────────────────────────────────┘");
        send(player, "");
        send(player, "  §f1. §7No hacking or cheating.");
        send(player, "  §f2. §7No abusive language or bullying.");
        send(player, "  §f3. §7No spamming or advertising.");
        send(player, "  §f4. §7No griefing outside your land.");
        send(player, "  §f5. §7No exploiting bugs — report them!");
        send(player, "  §f6. §7Respect all players and staff.");
        send(player, "  §f7. §7No AFK farming or auto-clickers.");
        send(player, "  §f8. §7No real-money trading (RMT).");
        send(player, "  §f9. §7No inappropriate builds or names.");
        send(player, "  §f10. §7Staff decisions are final.");
        send(player, "");
        send(player, "  §7Breaking rules → §cmute §8/ §ckick §8/ §cban");
        send(player, "");
        playSound(player, Sound.UI_BUTTON_CLICK);
    }

    // ══════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════

    private void send(Player player, String msg) {
        player.sendMessage(msg);
    }

    private void sendMsg(Player player, String msg) {
        player.sendMessage("§b§lKZ §8» " + msg);
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }
}
