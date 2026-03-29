package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final KZPlugin plugin;

    public AdminCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String cmdName = cmd.getName().toLowerCase();

        if (!sender.hasPermission("kz.admin")) {
            sender.sendMessage("§c§lKZ §8» §7No permission.");
            return true;
        }

        switch (cmdName) {
            case "setlobby":     return handleSetLobby(sender);
            case "setspawn":     return handleSetSpawn(sender);
            case "createnpc":    return handleCreateNPC(sender, args);
            case "removenpc":    return handleRemoveNPC(sender);
            case "listnpc":      return handleListNPC(sender);
            case "givebal":      return handleGiveBal(sender, args);
            case "removebal":    return handleRemoveBal(sender, args);
            case "setrank":      return handleSetRank(sender, args);
            case "maintenance":  return handleMaintenance(sender);
            case "announce":     return handleAnnounce(sender, args);
        }
        return true;
    }

    // ========================
    //  SETLOBBY
    // ========================
    private boolean handleSetLobby(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayer only!");
            return true;
        }
        Player p = (Player) sender;
        plugin.getLobbySystem().setLobbySpawn(p.getLocation());
        p.sendMessage("§a§lKZ §8» §7Lobby spawn set!");
        return true;
    }

    // ========================
    //  SETSPAWN
    // ========================
    private boolean handleSetSpawn(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayer only!");
            return true;
        }
        Player p = (Player) sender;
        p.getWorld().setSpawnLocation(p.getLocation());
        plugin.getConfig().set("spawn.world", p.getWorld().getName());
        plugin.getConfig().set("spawn.x", p.getLocation().getX());
        plugin.getConfig().set("spawn.y", p.getLocation().getY());
        plugin.getConfig().set("spawn.z", p.getLocation().getZ());
        plugin.saveConfig();
        p.sendMessage("§a§lKZ §8» §7World spawn set!");
        return true;
    }

    // ========================
    //  CREATE NPC
    // ========================
    private boolean handleCreateNPC(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayer only!");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /createnpc <mode> [displayname]");
            sender.sendMessage("§7Modes: oneblock, skyblock, acid, island");
            return true;
        }

        Player p = (Player) sender;
        String mode = args[0];
        String displayName = args.length >= 2
            ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
            : capitalize(mode);

        plugin.getLobbySystem().createNPC(p, mode, displayName);
        return true;
    }

    // ========================
    //  REMOVE NPC
    // ========================
    private boolean handleRemoveNPC(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cPlayer only!");
            return true;
        }
        Player p = (Player) sender;
        plugin.getLobbySystem().removeNearbyNPC(p);
        return true;
    }

    // ========================
    //  LIST NPC
    // ========================
    private boolean handleListNPC(CommandSender sender) {
        plugin.getLobbySystem().listNPCs(
            sender instanceof Player ? (Player) sender : null
        );
        if (!(sender instanceof Player)) {
            sender.sendMessage("§7(Use in-game for formatted view)");
        }
        return true;
    }

    // ========================
    //  GIVE BALANCE
    // ========================
    private boolean handleGiveBal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /givebal <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c§lKZ §8» §7Player not found.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount!");
            return true;
        }

        plugin.getEconomyManager().addBalance(target.getUniqueId(), amount);
        sender.sendMessage("§a§lKZ §8» §7Gave §e$" + formatMoney(amount) + " §7to §f" + target.getName());
        target.sendMessage("§a§lKZ §8» §7You received §e$" + formatMoney(amount) + " §7from Admin!");
        return true;
    }

    // ========================
    //  REMOVE BALANCE
    // ========================
    private boolean handleRemoveBal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /removebal <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c§lKZ §8» §7Player not found.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid amount!");
            return true;
        }

        boolean success = plugin.getEconomyManager().removeBalance(target.getUniqueId(), amount);
        if (!success) {
            sender.sendMessage("§c§lKZ §8» §7Player doesn't have enough balance.");
            return true;
        }

        sender.sendMessage("§a§lKZ §8» §7Removed §e$" + formatMoney(amount) + " §7from §f" + target.getName());
        target.sendMessage("§c§lKZ §8» §7Admin removed §e$" + formatMoney(amount) + " §7from your balance.");
        return true;
    }

    // ========================
    //  SET RANK
    // ========================
    private boolean handleSetRank(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /setrank <player> <rank>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c§lKZ §8» §7Player not found.");
            return true;
        }

        String rank = args[1];
        plugin.getLobbySystem().setRank(target.getUniqueId(), rank);

        sender.sendMessage("§a§lKZ §8» §7Set §f" + target.getName() + "§7's rank to §b" + rank);
        target.sendMessage("§a§lKZ §8» §7Your rank has been set to §b" + rank);
        return true;
    }

    // ========================
    //  MAINTENANCE
    // ========================
    private boolean handleMaintenance(CommandSender sender) {
        boolean current = plugin.getLobbySystem().isMaintenance();
        plugin.getLobbySystem().setMaintenance(!current);

        if (!current) {
            // Turning ON
            Bukkit.broadcastMessage("§c§lKZ §8» §7Server is now under §cmaintenance§7.");
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("kz.admin")) {
                    online.kickPlayer(
                        "§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later."
                    );
                }
            }
            sender.sendMessage("§a§lKZ §8» §cMaintenance ENABLED");
        } else {
            // Turning OFF
            Bukkit.broadcastMessage("§a§lKZ §8» §7Server maintenance is §aover§7. Welcome back!");
            sender.sendMessage("§a§lKZ §8» §aMaintenance DISABLED");
        }
        return true;
    }

    // ========================
    //  ANNOUNCE
    // ========================
    private boolean handleAnnounce(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /announce <message>");
            return true;
        }

        String msg = ChatColor.translateAlternateColorCodes('&',
            String.join(" ", args));

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("§e§l  📢 ANNOUNCEMENT");
        Bukkit.broadcastMessage("§f  " + msg);
        Bukkit.broadcastMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.broadcastMessage("");

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
        sender.sendMessage("§a§lKZ §8» §7Announcement sent!");
        return true;
    }

    // ========================
    //  TAB COMPLETE
    // ========================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd,
                                       String alias, String[] args) {
        if (!sender.hasPermission("kz.admin")) return Collections.emptyList();

        String cmdName = cmd.getName().toLowerCase();

        if (cmdName.equals("createnpc") && args.length == 1) {
            return Arrays.asList("oneblock", "skyblock", "acid", "island")
                .stream().filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if ((cmdName.equals("givebal") || cmdName.equals("removebal")
            || cmdName.equals("setrank")) && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (cmdName.equals("setrank") && args.length == 2) {
            return Arrays.asList("Member", "Iron", "Gold", "Diamond",
                "Emerald", "Obsidian", "Onyx", "Phantom", "Eclipse",
                "Ethereal", "Owner")
                .stream().filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    private String formatMoney(double amount) {
        return String.format("%,.0f", amount);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
