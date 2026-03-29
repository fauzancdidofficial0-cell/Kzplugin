package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class DailyCommand implements CommandExecutor {
    private final KZPlugin plugin;
    public DailyCommand(KZPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        plugin.getDailyRewardSystem().claim((Player) sender);
        return true;
    }
}
