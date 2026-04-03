// ============================================================
// PATH: src/main/java/com/kz/plugin/commands/CrateCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.CrateSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.DyeColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final KZPlugin plugin;

    public CrateCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "gachacreate" -> handleCreate(player, args);
            case "gachadelete" -> handleDelete(player);
            case "gachalist" -> handleList(player);
            case "gachapreview" -> handlePreview(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  /gachacreate <title> <desc1> <desc2> <color> <keyName>
    // ════════════════════════════════════════════════════════════════

    private void handleCreate(Player player, String[] args) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cYou don't have permission to do this.");
            return;
        }

        if (args.length < 5) {
            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §cUsage:");
            player.sendMessage("  §e/gachacreate <title> <desc1> <desc2> <color> <keyName>");
            player.sendMessage("");
            player.sendMessage("  §7Example:");
            player.sendMessage("  §f/gachacreate Legendary_Crate Open_for_epic_loot! Good_luck! purple legendary_key");
            player.sendMessage("");
            player.sendMessage("  §7Notes:");
            player.sendMessage("  §7• Use §f_ §7for spaces in title/desc");
            player.sendMessage("  §7• Hold the §ekey item §7in your main hand");
            player.sendMessage("  §7• Crate spawns at your current location");
            player.sendMessage("");
            player.sendMessage("  §7Valid colors:");
            player.sendMessage("  §fwhite, orange, magenta, light_blue, yellow,");
            player.sendMessage("  §flime, pink, gray, light_gray, cyan, purple,");
            player.sendMessage("  §fblue, brown, green, red, black");
            player.sendMessage("");
            return;
        }

        String title = args[0].replace("_", " ");
        String desc1 = args[1].replace("_", " ");
        String desc2 = args[2].replace("_", " ");
        String color = args[3];
        String keyName = args[4].replace("_", " ");

        plugin.getCrateSystem().createCrate(player, title, desc1, desc2, color, keyName);
    }

    // ════════════════════════════════════════════════════════════════
    //  /gachadelete - Delete nearest crate
    // ════════════════════════════════════════════════════════════════

    private void handleDelete(Player player) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cYou don't have permission to do this.");
            return;
        }

        plugin.getCrateSystem().deleteCrate(player);
    }

    // ════════════════════════════════════════════════════════════════
    //  /gachalist - List all crates
    // ════════════════════════════════════════════════════════════════

    private void handleList(Player player) {
        if (!player.hasPermission("kzplugin.admin")) {
            player.sendMessage("§c§lKZ §8» §cYou don't have permission to do this.");
            return;
        }

        plugin.getCrateSystem().listCrates(player);
    }

    // ════════════════════════════════════════════════════════════════
    //  /gachapreview [crateId] - Preview crate rewards
    // ════════════════════════════════════════════════════════════════

    private void handlePreview(Player player, String[] args) {
        CrateSystem crateSystem = plugin.getCrateSystem();

        // If player is looking at a crate block, preview that one
        var targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null && crateSystem.isCrate(targetBlock)) {
            CrateSystem.CrateData crate = crateSystem.getCrateAt(targetBlock);
            if (crate != null) {
                crateSystem.openPreviewGUI(player, crate);
                return;
            }
        }

        // If crate ID provided
        if (args.length >= 1) {
            String crateId = args[0];
            CrateSystem.CrateData crate = crateSystem.getCrate(crateId);
            if (crate != null) {
                crateSystem.openPreviewGUI(player, crate);
            } else {
                player.sendMessage("§c§lKZ §8» §cCrate not found: §f" + crateId);
            }
            return;
        }

        // No target
        player.sendMessage("§c§lKZ §8» §cLook at a crate or use §f/gachapreview <crateId>");
    }

    // ════════════════════════════════════════════════════════════════
    //  HELP
    // ════════════════════════════════════════════════════════════════

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│       §f§lCRATE COMMANDS             §6§l│");
        player.sendMessage("§6§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §e/gachacreate §8- §7Create a crate");
        player.sendMessage("  §e/gachadelete §8- §7Delete nearest crate");
        player.sendMessage("  §e/gachalist   §8- §7List all crates");
        player.sendMessage("  §e/gachapreview§8- §7Preview crate rewards");
        player.sendMessage("");
    }

    // ════════════════════════════════════════════════════════════════
    //  TAB COMPLETE
    // ════════════════════════════════════════════════════════════════

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("gachacreate")) {
            return switch (args.length) {
                case 1 -> List.of("<title>", "Legendary_Crate", "Starter_Crate", "Event_Crate");
                case 2 -> List.of("<description_1>", "Open_for_rewards!", "Rare_items_inside!");
                case 3 -> List.of("<description_2>", "Good_luck!", "Limited_edition!");
                case 4 -> {
                    List<String> colors = new ArrayList<>();
                    colors.addAll(Arrays.asList(
                            "white", "orange", "magenta", "light_blue", "yellow",
                            "lime", "pink", "gray", "light_gray", "cyan",
                            "purple", "blue", "brown", "green", "red", "black"
                    ));
                    yield colors.stream()
                            .filter(c -> c.startsWith(args[3].toLowerCase()))
                            .toList();
                }
                case 5 -> List.of("<key_name>", "crate_key", "legendary_key");
                default -> List.of();
            };
        }

        if (cmd.equals("gachapreview") && args.length == 1) {
            return plugin.getCrateSystem().getAllCrates().keySet().stream()
                    .filter(id -> id.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return List.of();
    }
}
