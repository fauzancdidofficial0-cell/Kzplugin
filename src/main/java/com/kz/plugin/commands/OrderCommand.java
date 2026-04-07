// ============================================================
// Path: src/main/java/com/kz/plugin/commands/OrderCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.AdvancedOrderSystem;
import com.kz.plugin.systems.OrderGUI;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class OrderCommand implements CommandExecutor {

    private final KZPlugin            plugin;
    private final AdvancedOrderSystem orderSystem;
    private final OrderGUI            gui;

    public OrderCommand(KZPlugin plugin, AdvancedOrderSystem orderSystem, OrderGUI gui) {
        this.plugin      = plugin;
        this.orderSystem = orderSystem;
        this.gui         = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayer only.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "myorders" -> gui.openMyOrders(player);
            default         -> gui.openMainMenu(player, 0);
        }

        return true;
    }
}
