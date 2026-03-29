package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EconomyCommand implements CommandExecutor {

    private final KZPlugin plugin;
    private final Set<UUID> coinflipCooldown = new HashSet<>();

    public EconomyCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        Player player = (Player) sender;

        switch (cmd.getName().toLowerCase()) {
            case "bal":
            case "balance":
            case "money":
                handleBalance(player, args);
                break;
            case "pay":
                handlePay(player, args);
                break;
            case "baltop":
                handleBaltop(player);
                break;
            case "cf":
            case "coinflip":
                handleCoinflip(player, args);
                break;
        }

        return true;
    }

    private void handleBalance(Player player, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c§lKZ §8» §7Player is not online.");
                return;
            }
            double bal = plugin.getEconomyManager().getBalance(target);
            player.sendMessage("§b§lKZ §8» §f" + target.getName() + "§7's balance: §a" +
                plugin.getEconomyManager().formatBalance(bal));
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Double> allBal = plugin.getEconomyManager().getAllBalances(uuid);

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  YOUR BALANCE");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (allBal.isEmpty()) {
            player.sendMessage("§7  No balance data found.");
        } else {
            for (Map.Entry<String, Double> entry : allBal.entrySet()) {
                String modeName = plugin.getEconomyManager().getModeName(entry.getKey());
                player.sendMessage("§7  " + modeName + " §8: §a" +
                    plugin.getEconomyManager().formatBalance(entry.getValue()));
            }
        }

        String currentMode = plugin.getEconomyManager().getPlayerMode(player);
        player.sendMessage("");
        player.sendMessage("§7  Active Mode: " + plugin.getEconomyManager().getModeName(currentMode));
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void handlePay(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c§lKZ §8» §7Usage: §f/pay <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c§lKZ §8» §7Player is not online.");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§c§lKZ §8» §7Cannot transfer to yourself.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§lKZ §8» §7Invalid amount.");
            return;
        }

        if (amount <= 0) {
            player.sendMessage("§c§lKZ §8» §7Amount must be greater than 0.");
            return;
        }

        // Check same mode
        String senderMode = plugin.getEconomyManager().getPlayerMode(player);
        String targetMode = plugin.getEconomyManager().getPlayerMode(target);

        if (!senderMode.equals(targetMode)) {
            player.sendMessage("§c§lKZ §8» §7Transfer failed. Both players must be in the same game mode.");
            player.sendMessage("§7  Your mode: " + plugin.getEconomyManager().getModeName(senderMode));
            player.sendMessage("§7  Their mode: " + plugin.getEconomyManager().getModeName(targetMode));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!plugin.getEconomyManager().transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage("§c§lKZ §8» §7Insufficient balance.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  TRANSFER SUCCESSFUL");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  To      : §f" + target.getName());
        player.sendMessage("§7  Amount  : §c-$" + plugin.getEconomyManager().formatBalance(amount));
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(player)));
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        target.sendMessage("§a§lKZ §8» §f" + player.getName() + " §7transferred §a$" +
            plugin.getEconomyManager().formatBalance(amount) + " §7to you.");
        target.sendMessage("§7  Balance: §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(target)));

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private void handleBaltop(Player player) {
        String mode = plugin.getEconomyManager().getPlayerMode(player);
        var top = plugin.getEconomyManager().getTopBalances(mode, 10);

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  TOP 10 RICHEST §7(" + plugin.getEconomyManager().getModeName(mode) + "§7)");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (top.isEmpty()) {
            player.sendMessage("§7  No data available.");
        } else {
            int rank = 0;
            for (var entry : top) {
                rank++;
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                if (name == null) name = "Unknown";

                String medal;
                switch (rank) {
                    case 1: medal = "§6🥇"; break;
                    case 2: medal = "§7🥈"; break;
                    case 3: medal = "§c🥉"; break;
                    default: medal = "§f#" + rank; break;
                }

                player.sendMessage("  " + medal + " §f" + name + " §8- §a" +
                    plugin.getEconomyManager().formatBalance(entry.getValue()));
            }
        }

        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void handleCoinflip(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            player.sendMessage("§c§lKZ §8» §7Usage: §f/cf <amount>");
            return;
        }

        if (coinflipCooldown.contains(uuid)) {
            player.sendMessage("§c§lKZ §8» §7Please wait for the current coinflip to finish.");
            return;
        }

        double bet;
        try {
            bet = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§lKZ §8» §7Invalid amount.");
            return;
        }

        if (bet <= 0 || bet > 50000) {
            player.sendMessage("§c§lKZ §8» §7Bet must be between §f$1 §7and §f$50,000§7.");
            return;
        }

        if (!plugin.getEconomyManager().removeBalance(uuid, bet)) {
            player.sendMessage("§c§lKZ §8» §7Insufficient balance.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        coinflipCooldown.add(uuid);

        player.sendMessage("§b§lKZ §8» §7Flipping coin... §f🪙");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                tick++;
                if (tick <= 3) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    return;
                }

                coinflipCooldown.remove(uuid);
                cancel();

                boolean won = new Random().nextBoolean();

                if (won) {
                    double winnings = bet * 2;
                    plugin.getEconomyManager().addBalance(uuid, winnings);

                    player.sendMessage("");
                    player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§a§l  ✔ YOU WON! 🪙");
                    player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§7  Bet     : §f$" + plugin.getEconomyManager().formatBalance(bet));
                    player.sendMessage("§7  Won     : §a+$" + plugin.getEconomyManager().formatBalance(winnings));
                    player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
                        plugin.getEconomyManager().getBalance(uuid)));
                    player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                } else {
                    player.sendMessage("");
                    player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§c§l  ✘ YOU LOST! 💀");
                    player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("§7  Bet     : §f$" + plugin.getEconomyManager().formatBalance(bet));
                    player.sendMessage("§7  Lost    : §c-$" + plugin.getEconomyManager().formatBalance(bet));
                    player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
                        plugin.getEconomyManager().getBalance(uuid)));
                    player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    player.sendMessage("");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
