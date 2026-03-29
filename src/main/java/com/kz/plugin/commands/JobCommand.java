package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class JobCommand implements CommandExecutor {
    private final KZPlugin plugin;
    public JobCommand(KZPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getJobSystem().showJobInfo(player);
        } else {
            plugin.getJobSystem().setJob(player, args[0].toLowerCase());
        }
        return true;
    }
}
