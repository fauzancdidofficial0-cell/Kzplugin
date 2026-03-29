package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class IslandCommand implements CommandExecutor {

    private final KZPlugin plugin;

    public IslandCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        Player player = (Player) sender;
        String command = cmd.getName().toLowerCase();

        switch (command) {
            case "createisland":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/createisland <mode>");
                    player.sendMessage("§7  Modes: §foneblock§7, §fskyblock§7, §facid§7, §fisland");
                    return true;
                }
                plugin.getIslandSystem().createIsland(player, args[0]);
                break;

            case "deleteisland":
                plugin.getIslandSystem().deleteIsland(player);
                break;

            case "home":
                plugin.getIslandSystem().teleportHome(player);
                break;

            case "upisland":
            case "upgradeisland":
                plugin.getIslandSystem().upgradeIsland(player);
                break;

            case "islandsetting":
            case "islandinfo":
                plugin.getIslandSystem().showInfo(player);
                break;

            case "nameisland":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/nameisland <name>");
                    return true;
                }
                plugin.getIslandSystem().setIslandName(player, String.join(" ", args));
                break;

            case "visit":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/visit <player>");
                    return true;
                }
                Player visitTarget = Bukkit.getPlayer(args[0]);
                if (visitTarget == null || !visitTarget.isOnline()) {
                    player.sendMessage("§c§lKZ §8» §7Player is not online.");
                    return true;
                }
                if (visitTarget.equals(player)) {
                    player.sendMessage("§c§lKZ §8» §7Cannot visit your own island. Use §f/home§7.");
                    return true;
                }
                plugin.getIslandSystem().visitIsland(player, visitTarget);
                break;

            case "topisland":
                showTopIslands(player);
                break;

            case "invite":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/invite <player>");
                    return true;
                }
                Player inviteTarget = Bukkit.getPlayer(args[0]);
                if (inviteTarget == null || !inviteTarget.isOnline()) {
                    player.sendMessage("§c§lKZ §8» §7Player is not online.");
                    return true;
                }
                if (inviteTarget.equals(player)) {
                    player.sendMessage("§c§lKZ §8» §7Cannot invite yourself.");
                    return true;
                }
                plugin.getIslandSystem().invitePlayer(player, inviteTarget);
                break;

            case "accept":
                plugin.getIslandSystem().acceptInvite(player);
                break;

            case "deny":
                plugin.getIslandSystem().denyInvite(player);
                break;

            case "trust":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/trust <player>");
                    return true;
                }
                Player trustTarget = Bukkit.getPlayer(args[0]);
                if (trustTarget == null || !trustTarget.isOnline()) {
                    player.sendMessage("§c§lKZ §8» §7Player is not online.");
                    return true;
                }
                if (trustTarget.equals(player)) {
                    player.sendMessage("§c§lKZ §8» §7Cannot trust yourself.");
                    return true;
                }
                plugin.getIslandSystem().trustPlayer(player, trustTarget);
                break;

            case "untrust":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/untrust <player>");
                    return true;
                }
                plugin.getIslandSystem().untrustPlayer(player, args[0]);
                break;
        }

        return true;
    }

    private void showTopIslands(Player player) {
        var top = plugin.getIslandSystem().getTopIslands(10);

        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  TOP 10 ISLANDS");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (top.isEmpty()) {
            player.sendMessage("§7  No islands found.");
        } else {
            int rank = 0;
            for (var entry : top) {
                rank++;
                var data = entry.getValue();
                String ownerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (ownerName == null) ownerName = "Unknown";

                String medal;
                switch (rank) {
                    case 1: medal = "§6🥇"; break;
                    case 2: medal = "§7🥈"; break;
                    case 3: medal = "§c🥉"; break;
                    default: medal = "§f#" + rank; break;
                }

                String islandName = data.name.equals("Unnamed Island") ?
                    "Island of " + ownerName : data.name;

                player.sendMessage("  " + medal + " §f" + islandName +
                    " §8| §7" + ownerName + " §8| §a" + data.blocksBroken + " blocks");
            }
        }

        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
