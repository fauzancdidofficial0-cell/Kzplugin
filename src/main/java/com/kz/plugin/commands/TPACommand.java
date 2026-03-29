package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TPACommand implements CommandExecutor {
    private final KZPlugin plugin;
    public TPACommand(KZPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "tpa":
                if (args.length == 0) {
                    player.sendMessage("§c§lKZ §8» §7Usage: §f/tpa <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§c§lKZ §8» §7Player is not online.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§c§lKZ §8» §7Cannot send TPA to yourself.");
                    return true;
                }
                plugin.getTpaSystem().sendRequest(player, target);
                break;
            case "tpaccept":
                plugin.getTpaSystem().acceptRequest(player);
                break;
            case "tpadeny":
                plugin.getTpaSystem().denyRequest(player);
                break;
            case "tpcancel":
                plugin.getTpaSystem().cancelRequest(player);
                break;
        }
        return true;
    }
}
