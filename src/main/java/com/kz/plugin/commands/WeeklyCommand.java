package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class WeeklyCommand implements CommandExecutor {
    private final KZPlugin plugin;
    public WeeklyCommand(KZPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
        plugin.getWeeklyRewardSystem().claim((Player) sender);
        return true;
    }
}
