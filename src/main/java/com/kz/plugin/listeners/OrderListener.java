// ============================================================
// Path: src/main/java/com/kz/plugin/listeners/OrderListener.java
// ============================================================
package com.kz.plugin.listeners;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.AdvancedOrderSystem;
import com.kz.plugin.systems.AdvancedOrderSystem.*;
import com.kz.plugin.systems.OrderGUI;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

import java.util.*;

public class OrderListener implements Listener {

    private final KZPlugin            plugin;
    private final AdvancedOrderSystem orderSystem;
    private final OrderGUI            gui;

    // ════════════════════════════════════════════════════════════════
    //  STATE TRACKING
    // ════════════════════════════════════════════════════════════════

    // Player yang sedang menunggu input chat untuk search
    private final Set<UUID> awaitingSearch = new HashSet<>();

    // Player yang sedang dalam flow buat order
    // step: "material" | "amount" | "priceperitem" | "budget"
    private final Map<UUID, OrderCreationState> creationState = new HashMap<>();

    private record OrderCreationState(
            String   step,
            Material material,
            int      amount,       // 0 = unlimited
            double   pricePerItem
    ) {}

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public OrderListener(KZPlugin plugin, AdvancedOrderSystem orderSystem, OrderGUI gui) {
        this.plugin      = plugin;
        this.orderSystem = orderSystem;
        this.gui         = gui;
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY CLICK
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();

        if (title.equals("§8§l[ §b§lORDER MARKET §8§l]")) {
            event.setCancelled(true);
            handleMainMenu(player, event);
            return;
        }

        if (title.equals("§8§l[ §b§lMY ORDERS §8§l]")) {
            event.setCancelled(true);
            handleMyOrders(player, event);
            return;
        }

        if (title.startsWith("§8Supply §b#")) {
            handleSupply(player, event, title);
            return;
        }

        if (title.startsWith("§8Stash §b#")) {
            event.setCancelled(true);
            handleStash(player, event, title);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MAIN MENU HANDLER
    // ════════════════════════════════════════════════════════════════

    private void handleMainMenu(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Slot 0-44: Klik order → buka Supply GUI
        if (slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null
                    || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            List<BuyOrder> orders = orderSystem.getFilteredOrders(player.getUniqueId());
            int page  = orderSystem.getPage(player.getUniqueId());
            int index = page * 45 + slot;
            if (index >= orders.size()) return;

            player.closeInventory();
            gui.openSupplyGUI(player, orders.get(index).id);
            return;
        }

        switch (slot) {
            // Slot 46: Filter kategori
            case 46 -> {
                Category next = cycleEnum(
                        Category.values(),
                        orderSystem.getCategory(player.getUniqueId()));
                orderSystem.setCategory(player.getUniqueId(), next);
                playClick(player);
                gui.openMainMenu(player, orderSystem.getPage(player.getUniqueId()));
            }
            // Slot 47: Sort
            case 47 -> {
                SortType next = cycleEnum(
                        SortType.values(),
                        orderSystem.getSort(player.getUniqueId()));
                orderSystem.setSort(player.getUniqueId(), next);
                playClick(player);
                gui.openMainMenu(player, orderSystem.getPage(player.getUniqueId()));
            }
            // Slot 49: Search
            case 49 -> {
                player.closeInventory();
                awaitingSearch.add(player.getUniqueId());
                player.sendMessage("");
                player.sendMessage("§b§lORDER §8» §7Type item name to search.");
                player.sendMessage("§b§lORDER §8» §8Type §ccancel §8to abort.");
                player.sendMessage("");
                player.playSound(player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            }
            // Slot 50: Clear search
            case 50 -> {
                orderSystem.setSearch(player.getUniqueId(), "");
                playClick(player);
                gui.openMainMenu(player, 0);
            }
            // Slot 51: Prev page
            case 51 -> {
                int p = orderSystem.getPage(player.getUniqueId());
                if (p > 0) { playClick(player); gui.openMainMenu(player, p - 1); }
            }
            // Slot 52: My Orders
            case 52 -> {
                playClick(player);
                gui.openMyOrders(player);
            }
            // Slot 53: Next page
            case 53 -> {
                int p = orderSystem.getPage(player.getUniqueId());
                playClick(player);
                gui.openMainMenu(player, p + 1);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MY ORDERS HANDLER
    // ════════════════════════════════════════════════════════════════

    private void handleMyOrders(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int size = event.getInventory().getSize();
        if (slot < 0 || slot >= size) return;

        // Back button (last slot)
        if (slot == size - 1) {
            playClick(player);
            gui.openMainMenu(player, 0);
            return;
        }

        // Slot 0: Create order
        if (slot == 0) {
            player.closeInventory();
            startOrderCreation(player);
            return;
        }

        // Slot 1+: Order → stash GUI
        List<BuyOrder> myOrders = orderSystem.getMyOrders(player.getUniqueId());
        int index = slot - 1;
        if (index < 0 || index >= myOrders.size()) return;

        player.closeInventory();
        gui.openStashGUI(player, myOrders.get(index).id);
    }

    // ════════════════════════════════════════════════════════════════
    //  SUPPLY HANDLER
    // ════════════════════════════════════════════════════════════════

    private void handleSupply(Player player, InventoryClickEvent event, String title) {
        int slot = event.getRawSlot();

        // Slot 7: Info → cancel
        if (slot == 7) {
            event.setCancelled(true);
            return;
        }

        // Slot 8: Confirm
        if (slot == 8) {
            event.setCancelled(true);
            String orderId = extractId(title, "§8Supply §b#", " §8[");
            if (orderId == null) return;

            // Kumpulkan item dari slot 0-6
            Inventory inv = event.getInventory();
            List<ItemStack> supplied = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                ItemStack it = inv.getItem(i);
                if (it != null
                        && it.getType() != Material.AIR
                        && it.getType() != Material.WHITE_STAINED_GLASS_PANE) {
                    supplied.add(it.clone());
                    inv.setItem(i, null);
                }
            }

            if (supplied.isEmpty()) {
                player.sendMessage("§c§lORDER §8» §cNo items placed in slots!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            player.closeInventory();
            String result = orderSystem.supplyOrder(player, orderId, supplied);
            sendMultiline(player, result);
            playResultSound(player, result);
            return;
        }

        // Slot 0-6: Validasi material yang dimasukkan
        if (slot >= 0 && slot < 7) {
            String orderId = extractId(title, "§8Supply §b#", " §8[");
            if (orderId == null) { event.setCancelled(true); return; }

            BuyOrder order = orderSystem.getOrder(orderId);
            if (order == null) { event.setCancelled(true); return; }

            ItemStack cursor = event.getCursor();
            if (cursor != null
                    && cursor.getType() != Material.AIR
                    && cursor.getType() != order.material) {
                event.setCancelled(true);
                player.sendMessage("§c§lORDER §8» §cOnly §f"
                        + orderSystem.formatMaterial(order.material)
                        + " §cis accepted here!");
                player.playSound(player.getLocation(),
                        Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  STASH HANDLER
    // ════════════════════════════════════════════════════════════════

    private void handleStash(Player player, InventoryClickEvent event, String title) {
        int slot = event.getRawSlot();

        // Slot 22: Collect All
        if (slot == 22) {
            String orderId = extractId(title, "§8Stash §b#", " §8[");
            if (orderId == null) return;

            player.closeInventory();
            String result = orderSystem.collectStash(player, orderId);
            sendMultiline(player, result);
            playResultSound(player, result);

            // Refresh stash GUI setelah collect
            if (result.startsWith("§a")) {
                Bukkit.getScheduler().runTaskLater(plugin,
                        () -> gui.openStashGUI(player, orderId), 2L);
            }
            return;
        }

        // Slot 24: Cancel Order (right click)
        if (slot == 24 && event.getClick() == ClickType.RIGHT) {
            String orderId = extractId(title, "§8Stash §b#", " §8[");
            if (orderId == null) return;

            player.closeInventory();
            String result = orderSystem.cancelOrder(player, orderId);
            player.sendMessage(result);
            playResultSound(player, result);
            return;
        }

        // Slot 26: Back
        if (slot == 26) {
            playClick(player);
            gui.openMyOrders(player);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  INVENTORY CLOSE - Kembalikan item supply yang belum diconfirm
    // ════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith("§8Supply §b#")) return;

        Inventory inv = event.getInventory();
        for (int i = 0; i < 7; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null
                    || it.getType() == Material.AIR
                    || it.getType() == Material.WHITE_STAINED_GLASS_PANE) continue;

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(it.clone());
            for (ItemStack left : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
            inv.setItem(i, null);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ORDER CREATION FLOW
    // ════════════════════════════════════════════════════════════════

    private void startOrderCreation(Player player) {
        creationState.put(player.getUniqueId(),
                new OrderCreationState("material", null, 0, 0));

        player.sendMessage("");
        player.sendMessage("§b§l╔══════════════════════════════╗");
        player.sendMessage("§b§l║   §f§lCREATE BUY ORDER         §b§l║");
        player.sendMessage("§b§l╚══════════════════════════════╝");
        player.sendMessage("");
        player.sendMessage("  §f§lStep 1§8/§f4: §7Item Name");
        player.sendMessage("  §7Type the item name. Example:");
        player.sendMessage("  §b  DIAMOND §8| §bIRON_INGOT §8| §bOAK_LOG");
        player.sendMessage("");
        player.sendMessage("  §8Type §ccancel §8to abort.");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
    }

    // ════════════════════════════════════════════════════════════════
    //  CHAT HANDLER
    // ════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();
        String msg    = event.getMessage().trim();

        // ── Search flow ───────────────────────────────────────────
        if (awaitingSearch.contains(uuid)) {
            event.setCancelled(true);
            awaitingSearch.remove(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    orderSystem.setSearch(uuid, "");
                    player.sendMessage("§b§lORDER §8» §7Search cleared.");
                } else {
                    orderSystem.setSearch(uuid, msg);
                    player.sendMessage("§b§lORDER §8» §7Searching: §f" + msg);
                }
                playClick(player);
                gui.openMainMenu(player, 0);
            });
            return;
        }

        // ── Order creation flow ───────────────────────────────────
        if (!creationState.containsKey(uuid)) return;
        event.setCancelled(true);

        OrderCreationState state = creationState.get(uuid);

        Bukkit.getScheduler().runTask(plugin, () -> {

            // Cancel kapanpun
            if (msg.equalsIgnoreCase("cancel")) {
                creationState.remove(uuid);
                player.sendMessage("§b§lORDER §8» §7Order creation cancelled.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                gui.openMyOrders(player);
                return;
            }

            switch (state.step()) {

                // ── Step 1: Material ──────────────────────────────
                case "material" -> {
                    Material mat = parseMaterial(msg);
                    if (mat == null || mat.isAir()) {
                        player.sendMessage(
                                "§c§lORDER §8» §cUnknown item: §f" + msg);
                        player.sendMessage(
                                "  §7Try: §bDIAMOND §8| §bIRON_INGOT §8| §bCOAL");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    creationState.put(uuid,
                            new OrderCreationState("amount", mat, 0, 0));

                    player.sendMessage("");
                    player.sendMessage("  §a✔ §7Item: §f"
                            + orderSystem.formatMaterial(mat));
                    player.sendMessage("");
                    player.sendMessage("  §f§lStep 2§8/§f4: §7Amount");
                    player.sendMessage("  §7How many items do you want?");
                    player.sendMessage(
                            "  §7Type §b0 §7for §eUNLIMITED §8(buy until budget runs out)");
                    player.sendMessage("");
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                }

                // ── Step 2: Amount (0 = unlimited) ────────────────
                case "amount" -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(
                                msg.replace(",", "").replace(".", ""));
                    } catch (NumberFormatException e) {
                        player.sendMessage(
                                "§c§lORDER §8» §cInvalid number: §f" + msg);
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    if (amount < 0) {
                        player.sendMessage(
                                "§c§lORDER §8» §cAmount cannot be negative.");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    boolean isUnlimited = (amount == 0);
                    creationState.put(uuid,
                            new OrderCreationState("priceperitem",
                                    state.material(), amount, 0));

                    player.sendMessage("");
                    player.sendMessage("  §a✔ §7Amount: §f"
                            + (isUnlimited ? "§eUNLIMITED ♾" : String.valueOf(amount)));
                    player.sendMessage("");
                    player.sendMessage("  §f§lStep 3§8/§f4: §7Price Per Item");
                    player.sendMessage(
                            "  §7How much will you pay for §f1 item§7?");
                    player.sendMessage("  §7Your balance: §a"
                            + plugin.getLobbySystem().formatCoins(
                            plugin.getEconomyManager().getBalance(player)));
                    player.sendMessage("");
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                }

                // ── Step 3: Price per item ────────────────────────
                case "priceperitem" -> {
                    double pricePerItem;
                    try {
                        pricePerItem = Double.parseDouble(msg.replace(",", ""));
                    } catch (NumberFormatException e) {
                        player.sendMessage(
                                "§c§lORDER §8» §cInvalid number: §f" + msg);
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    if (pricePerItem <= 0) {
                        player.sendMessage(
                                "§c§lORDER §8» §cPrice per item must be > 0.");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    boolean isUnlimited = (state.amount() == 0);

                    if (!isUnlimited) {
                        // ── Fixed amount: langsung buat order ─────
                        creationState.remove(uuid);

                        double totalEscrow = state.amount() * pricePerItem;

                        player.sendMessage("");
                        player.sendMessage(
                                "§b§l╔══════════════════════════════╗");
                        player.sendMessage(
                                "§b§l║   §f§lORDER SUMMARY (FIXED)    §b§l║");
                        player.sendMessage(
                                "§b§l╚══════════════════════════════╝");
                        player.sendMessage("");
                        player.sendMessage("  §7Item    : §f"
                                + orderSystem.formatMaterial(state.material()));
                        player.sendMessage("  §7Amount  : §f" + state.amount());
                        player.sendMessage("  §7Price/ea: §a"
                                + plugin.getLobbySystem().formatCoins(pricePerItem));
                        player.sendMessage("  §7Escrow  : §a"
                                + plugin.getLobbySystem().formatCoins(totalEscrow));
                        player.sendMessage("  §7Balance : §a"
                                + plugin.getLobbySystem().formatCoins(
                                plugin.getEconomyManager().getBalance(player)));
                        player.sendMessage("");

                        String error = orderSystem.createOrder(
                                player, state.material(),
                                state.amount(), 0, pricePerItem);

                        if (error != null) {
                            player.sendMessage(error);
                            player.playSound(player.getLocation(),
                                    Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        } else {
                            player.sendMessage("  §a§l✔ Order created!");
                            player.sendMessage("  §7Escrow: §a"
                                    + plugin.getLobbySystem().formatCoins(totalEscrow)
                                    + " §7deducted.");
                            player.sendMessage("");
                            player.playSound(player.getLocation(),
                                    Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                            Bukkit.getScheduler().runTaskLater(plugin,
                                    () -> gui.openMyOrders(player), 20L);
                        }

                    } else {
                        // ── Unlimited: tanya budget dulu ──────────
                        creationState.put(uuid,
                                new OrderCreationState("budget",
                                        state.material(), 0, pricePerItem));

                        player.sendMessage("");
                        player.sendMessage("  §a✔ §7Price/ea: §a"
                                + plugin.getLobbySystem().formatCoins(pricePerItem));
                        player.sendMessage("");
                        player.sendMessage("  §f§lStep 4§8/§f4: §7Budget (Escrow)");
                        player.sendMessage(
                                "  §7Set your §fmaximum spending limit§7.");
                        player.sendMessage(
                                "  §7Suppliers get paid until budget runs out.");
                        player.sendMessage("  §7Your balance: §a"
                                + plugin.getLobbySystem().formatCoins(
                                plugin.getEconomyManager().getBalance(player)));
                        player.sendMessage("");
                        player.playSound(player.getLocation(),
                                Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                    }
                }

                // ── Step 4: Budget (unlimited only) ──────────────
                case "budget" -> {
                    double budget;
                    try {
                        budget = Double.parseDouble(msg.replace(",", ""));
                    } catch (NumberFormatException e) {
                        player.sendMessage(
                                "§c§lORDER §8» §cInvalid number: §f" + msg);
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    if (budget <= 0) {
                        player.sendMessage(
                                "§c§lORDER §8» §cBudget must be > 0.");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    if (budget < state.pricePerItem()) {
                        player.sendMessage(
                                "§c§lORDER §8» §cBudget must be at least §f"
                                + plugin.getLobbySystem().formatCoins(state.pricePerItem())
                                + " §c(1 item worth).");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    long estimatedItems =
                            (long) Math.floor(budget / state.pricePerItem());
                    creationState.remove(uuid);

                    player.sendMessage("");
                    player.sendMessage(
                            "§b§l╔══════════════════════════════╗");
                    player.sendMessage(
                            "§b§l║  §f§lORDER SUMMARY (UNLIMITED) §b§l║");
                    player.sendMessage(
                            "§b§l╚══════════════════════════════╝");
                    player.sendMessage("");
                    player.sendMessage("  §7Item    : §f"
                            + orderSystem.formatMaterial(state.material()));
                    player.sendMessage("  §7Amount  : §eUNLIMITED ♾");
                    player.sendMessage("  §7Price/ea: §a"
                            + plugin.getLobbySystem().formatCoins(state.pricePerItem()));
                    player.sendMessage("  §7Budget  : §a"
                            + plugin.getLobbySystem().formatCoins(budget));
                    player.sendMessage("  §7Est.    : §7~§f" + estimatedItems
                            + " §7items max");
                    player.sendMessage("  §7Balance : §a"
                            + plugin.getLobbySystem().formatCoins(
                            plugin.getEconomyManager().getBalance(player)));
                    player.sendMessage("");

                    String error = orderSystem.createOrder(
                            player, state.material(), 0, budget, state.pricePerItem());

                    if (error != null) {
                        player.sendMessage(error);
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } else {
                        player.sendMessage("  §a§l✔ Unlimited order created!");
                        player.sendMessage("  §7Budget §a"
                                + plugin.getLobbySystem().formatCoins(budget)
                                + " §7in escrow.");
                        player.sendMessage("");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        Bukkit.getScheduler().runTaskLater(plugin,
                                () -> gui.openMyOrders(player), 20L);
                    }
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private <T extends Enum<T>> T cycleEnum(T[] values, T current) {
        return values[(current.ordinal() + 1) % values.length];
    }

    private Material parseMaterial(String input) {
        // Exact match
        try {
            return Material.valueOf(input.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException ignored) {}

        // Partial match
        String lower = input.toLowerCase().replace(" ", "_");
        for (Material mat : Material.values()) {
            if (!mat.isAir() && mat.name().toLowerCase().contains(lower)) return mat;
        }
        return null;
    }

    private String extractId(String title, String prefix, String suffix) {
        try {
            int s = title.indexOf(prefix) + prefix.length();
            int e = title.indexOf(suffix, s);
            if (s < prefix.length() || e < 0) return null;
            return title.substring(s, e);
        } catch (Exception ex) { return null; }
    }

    private void sendMultiline(Player player, String msg) {
        for (String line : msg.split("\n")) player.sendMessage(line);
    }

    private void playClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    private void playResultSound(Player player, String result) {
        if (result.startsWith("§a")) {
            player.playSound(player.getLocation(),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        } else {
            player.playSound(player.getLocation(),
                    Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
}
