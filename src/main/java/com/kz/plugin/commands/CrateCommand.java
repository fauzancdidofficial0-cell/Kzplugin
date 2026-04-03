// ============================================================
// PATH: src/main/java/com/kz/plugin/commands/CrateCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.CrateSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final KZPlugin plugin;

    public CrateCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "gachacreate" -> handleCreate(player, args);
            case "gachadelete" -> handleDelete(player);
            case "gachalist" -> handleList(player);
            case "gachapreview" -> handlePreview(player, args);
            case "givekey" -> handleGiveKey(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cNo permission.");
            return;
        }

        if (args.length < 5) {
            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §cUsage:");
            player.sendMessage("  §e/gachacreate <title> <desc1> <desc2> <color> <keyName>");
            player.sendMessage("");
            player.sendMessage("  §7Example:");
            player.sendMessage("  §f/gachacreate Legendary_Crate Epic_loot! Good_luck! purple my_key");
            player.sendMessage("");
            player.sendMessage("  §7• Use §f_ §7for spaces");
            player.sendMessage("  §7• Hold §eany item §7as key template");
            player.sendMessage("  §7• Colors: white, orange, magenta, light_blue, yellow,");
            player.sendMessage("    §7lime, pink, gray, light_gray, cyan, purple, blue,");
            player.sendMessage("    §7brown, green, red, black");
            player.sendMessage("");
            return;
        }

        plugin.getCrateSystem().createCrate(player,
                args[0].replace("_", " "),
                args[1], args[2], args[3], args[4]);
    }

    private void handleDelete(Player player) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cNo permission.");
            return;
        }
        plugin.getCrateSystem().deleteCrate(player);
    }

    private void handleList(Player player) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cNo permission.");
            return;
        }
        plugin.getCrateSystem().listCrates(player);
    }

    private void handlePreview(Player player, String[] args) {
        CrateSystem sys = plugin.getCrateSystem();

        var block = player.getTargetBlockExact(5);
        if (block != null && sys.isCrate(block)) {
            CrateSystem.CrateData crate = sys.getCrateAt(block);
            if (crate != null) {
                sys.openPreviewGUI(player, crate);
                return;
            }
        }

        if (args.length >= 1) {
            CrateSystem.CrateData crate = sys.getCrate(args[0]);
            if (crate != null) {
                sys.openPreviewGUI(player, crate);
            } else {
                player.sendMessage("§c§lKZ §8» §cCrate not found: §f" + args[0]);
            }
            return;
        }

        player.sendMessage("§c§lKZ §8» §cLook at a crate or: §f/gachapreview <crateId>");
    }

    private void handleGiveKey(Player player, String[] args) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cNo permission.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §cUsage: §f/givekey <player> <crateId> [amount]");
            player.sendMessage("  §7Use §f/gachalist §7to see crate IDs.");
            player.sendMessage("");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c§lKZ §8» §cPlayer not online.");
            return;
        }

        String crateId = args[1];
        if (plugin.getCrateSystem().getCrate(crateId) == null) {
            player.sendMessage("§c§lKZ §8» §cCrate not found: §f" + crateId);
            return;
        }

        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1) amount = 1;
                if (amount > 64) amount = 64;
            } catch (NumberFormatException e) {
                player.sendMessage("§c§lKZ §8» §cInvalid amount.");
                return;
            }
        }

        plugin.getCrateSystem().giveKey(target, crateId, amount);
        player.sendMessage("§a§lKZ §8» §7Gave §e" + amount + " key(s) §7to §f" + target.getName() + "§7.");
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│       §f§lCRATE COMMANDS             §6§l│");
        player.sendMessage("§6§l└─────────────────────────────────┘");
        player.sendMessage("  §e/gachacreate  §8- §7Create a crate");
        player.sendMessage("  §e/gachadelete  §8- §7Delete nearest crate");
        player.sendMessage("  §e/gachalist    §8- §7List all crates");
        player.sendMessage("  §e/gachapreview §8- §7Preview rewards");
        player.sendMessage("  §e/givekey      §8- §7Give key to player");
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (cmd.getName().equalsIgnoreCase("givekey")) {
            return switch (args.length) {
                case 1 -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
                case 2 -> plugin.getCrateSystem().getAllCrates().keySet().stream()
                        .filter(id -> id.startsWith(args[1].toLowerCase())).toList();
                case 3 -> List.of("1", "5", "10", "32", "64");
                default -> List.of();
            };
        }

        if (cmd.getName().equalsIgnoreCase("gachacreate")) {
            return switch (args.length) {
                case 4 -> List.of("white", "orange", "magenta", "light_blue", "yellow",
                        "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
                        "brown", "green", "red", "black").stream()
                        .filter(c -> c.startsWith(args[3].toLowerCase())).toList();
                default -> List.of();
            };
        }

        return List.of();
    }
}
