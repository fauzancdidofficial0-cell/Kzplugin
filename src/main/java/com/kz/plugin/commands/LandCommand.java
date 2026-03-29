package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LandCommand implements CommandExecutor {

    private final KZPlugin plugin;

    public LandCommand(KZPlugin plugin) {
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
            case "landinvite": {
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/landinvite <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§c§lKZ §8» §7Player is not online.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§c§lKZ §8» §7Cannot invite yourself.");
                    return true;
                }
                plugin.getLandSystem().inviteToLand(player, target);
                break;
            }

            case "landaccept":
                plugin.getLandSystem().acceptLandInvite(player);
                break;

            case "landdeny":
                plugin.getLandSystem().denyLandInvite(player);
                break;

            case "landrole":
                if (args.length < 2) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/landrole <player> <admin|staff|member>");
                    return true;
                }
                plugin.getLandSystem().setRole(player, args[0], args[1].toLowerCase());
                break;

            case "landkick":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/landkick <player>");
                    return true;
                }
                plugin.getLandSystem().kickFromLand(player, args[0]);
                break;

            case "trustland": {
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/trustland <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§c§lKZ §8» §7Player is not online.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§c§lKZ §8» §7Cannot trust yourself.");
                    return true;
                }
                plugin.getLandSystem().trustLand(player, target);
                break;
            }

            case "memberrule":
                plugin.getLandSystem().openMemberRulesGUI(player);
                break;

            case "trustrule":
                plugin.getLandSystem().openTrustRulesGUI(player);
                break;

            case "setlandname":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/setlandname <name>");
                    return true;
                }
                plugin.getLandSystem().setLandName(player, String.join(" ", args));
                break;

            case "deleteland":
                plugin.getLandSystem().deleteLand(player);
                break;

            case "cekcapasitas":
                plugin.getLandSystem().showLandInfo(player);
                break;
        }

        return true;
    }
}
