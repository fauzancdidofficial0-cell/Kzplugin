// ============================================================
// PATH: src/main/java/com/kz/plugin/commands/EconomyCommand.java
// ============================================================
package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.systems.EconomyManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyCommand implements CommandExecutor, Listener {

    private final KZPlugin plugin;

    // ════════════════════════════════════════════════════════════════
    //  COINFLIP SYSTEM DATA
    // ════════════════════════════════════════════════════════════════

    /** Active coinflip bets: UUID → bet amount */
    private final Map<UUID, Double> activeBets = new ConcurrentHashMap<>();

    /** Players currently in coinflip animation */
    private final Set<UUID> inAnimation = ConcurrentHashMap.newKeySet();

    /** GUI title constants */
    private static final String CF_LIST_TITLE = "§8§lCoinflip §7- Active Bets";
    private static final String CF_CONFIRM_PREFIX = "§8§lAccept Bet: §e$";
    private static final String CF_RESULT_TITLE = "§8§lCoinflip Result";

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public EconomyCommand(KZPlugin plugin) {
        this.plugin = plugin;

        // Register as listener for GUI clicks
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ════════════════════════════════════════════════════════════════
    //  COMMAND HANDLER
    // ════════════════════════════════════════════════════════════════

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        switch (cmd.getName().toLowerCase()) {
            case "bal", "balance", "money" -> handleBalance(player, args);
            case "pay" -> handlePay(player, args);
            case "baltop" -> handleBaltop(player);
            case "cf", "coinflip" -> handleCoinflip(player, args);
        }

        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  /bal [player] - View balance
    // ════════════════════════════════════════════════════════════════

    private void handleBalance(Player player, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();

        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§c§lKZ §8» §7Player is not online.");
                return;
            }
            double bal = eco.getBalance(target);
            player.sendMessage("§b§lKZ §8» §f" + target.getName() + "§7's balance: §a"
                    + eco.formatBalance(bal));
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Double> allBal = eco.getAllBalances(uuid);

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  YOUR BALANCE");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (allBal.isEmpty()) {
            player.sendMessage("§7  No balance data found.");
        } else {
            for (Map.Entry<String, Double> entry : allBal.entrySet()) {
                String modeName = eco.getModeName(entry.getKey());
                player.sendMessage("§7  " + modeName + " §8: §a"
                        + eco.formatBalance(entry.getValue()));
            }
        }

        String currentMode = eco.getPlayerMode(player);
        player.sendMessage("");
        player.sendMessage("§7  Active Mode: " + eco.getModeName(currentMode));
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  /pay <player> <amount> - Transfer money
    // ════════════════════════════════════════════════════════════════

    private void handlePay(Player player, String[] args) {
        EconomyManager eco = plugin.getEconomyManager();

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
        String senderMode = eco.getPlayerMode(player);
        String targetMode = eco.getPlayerMode(target);

        if (!senderMode.equals(targetMode)) {
            player.sendMessage("§c§lKZ §8» §7Transfer failed. Both players must be in the same game mode.");
            player.sendMessage("§7  Your mode: " + eco.getModeName(senderMode));
            player.sendMessage("§7  Their mode: " + eco.getModeName(targetMode));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        EconomyManager.TransferResult result = eco.transfer(
                player.getUniqueId(), target.getUniqueId(), amount);

        if (result != EconomyManager.TransferResult.SUCCESS) {
            String errorMsg = switch (result) {
                case INSUFFICIENT_FUNDS -> "Insufficient balance.";
                case INVALID_AMOUNT -> "Invalid amount.";
                case SENDER_NOT_LOADED -> "Your data is not loaded yet. Please wait.";
                case RECEIVER_NOT_LOADED -> "Target player's data is not loaded yet.";
                case DIFFERENT_MODE -> "Both players must be in the same game mode.";
                default -> "Transfer failed.";
            };
            player.sendMessage("§c§lKZ §8» §7" + errorMsg);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  TRANSFER SUCCESSFUL");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  To      : §f" + target.getName());
        player.sendMessage("§7  Amount  : §c-" + eco.formatBalance(amount));
        player.sendMessage("§7  Balance : §a" + eco.formatBalance(eco.getBalance(player)));
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        target.sendMessage("§a§lKZ §8» §f" + player.getName() + " §7transferred §a"
                + eco.formatBalance(amount) + " §7to you.");
        target.sendMessage("§7  Balance: §a" + eco.formatBalance(eco.getBalance(target)));

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  /baltop - Top 10 richest
    // ════════════════════════════════════════════════════════════════

    private void handleBaltop(Player player) {
        EconomyManager eco = plugin.getEconomyManager();
        String mode = eco.getPlayerMode(player);

        player.sendMessage("§b§lKZ §8» §7Loading top balances...");

        eco.getTopBalances(mode, 10).thenAccept(top -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("");
                player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage("§f§l  TOP 10 RICHEST §7(" + eco.getModeName(mode) + "§7)");
                player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                if (top.isEmpty()) {
                    player.sendMessage("§7  No data available.");
                } else {
                    int rank = 0;
                    for (Map.Entry<String, Double> entry : top) {
                        rank++;
                        String medal = switch (rank) {
                            case 1 -> "§6🥇";
                            case 2 -> "§7🥈";
                            case 3 -> "§c🥉";
                            default -> "§f#" + rank;
                        };
                        player.sendMessage("  " + medal + " §f" + entry.getKey()
                                + " §8- §a" + eco.formatBalance(entry.getValue()));
                    }
                }

                player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage("§c§lKZ §8» §7Failed to load top balances."));
            return null;
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  COINFLIP SYSTEM
    //
    //  /cf          → Open GUI showing all active bets
    //  /cf <amount> → Create a new bet (deducts money immediately)
    //  /coinflip    → Same as /cf
    //
    //  Flow:
    //  1. Player A: /cf 5000 → Creates bet, money deducted, appears in GUI
    //  2. Player B: /cf → Opens GUI, sees Player A's bet
    //  3. Player B clicks Player A's bet → money deducted from B
    //  4. Coinflip animation plays for both players
    //  5. Winner gets 2x the bet
    // ════════════════════════════════════════════════════════════════

    private void handleCoinflip(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        // Block if in animation
        if (inAnimation.contains(uuid)) {
            player.sendMessage("§c§lKZ §8» §7Please wait for your current coinflip to finish.");
            return;
        }

        if (args.length == 0) {
            // /cf → Open bet list GUI
            openCoinflipListGUI(player);
        } else {
            // /cf <amount> → Create new bet
            createCoinflipBet(player, args[0]);
        }
    }

    // ═══════════════════════════════
    //  CREATE BET
    // ═══════════════════════════════

    private void createCoinflipBet(Player player, String amountStr) {
        EconomyManager eco = plugin.getEconomyManager();
        UUID uuid = player.getUniqueId();

        // Check if player already has active bet
        if (activeBets.containsKey(uuid)) {
            player.sendMessage("§c§lKZ §8» §7You already have an active bet!");
            player.sendMessage("  §7Cancel it first by closing the coinflip GUI");
            player.sendMessage("  §7or wait for someone to accept it.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§lKZ §8» §7Invalid amount. Usage: §f/cf <amount>");
            return;
        }

        if (amount < 100) {
            player.sendMessage("§c§lKZ §8» §7Minimum bet is §f$100§7.");
            return;
        }

        if (!eco.hasEnough(player, amount)) {
            player.sendMessage("§c§lKZ §8» §7Insufficient balance.");
            player.sendMessage("  §7Your balance: §a" + eco.formatBalance(eco.getBalance(player)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Deduct money immediately
        eco.removeBalance(player, amount);

        // Register bet
        activeBets.put(uuid, amount);

        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  🪙 COINFLIP BET CREATED");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Amount  : §e" + eco.formatBalance(amount));
        player.sendMessage("§7  Balance : §a" + eco.formatBalance(eco.getBalance(player)));
        player.sendMessage("");
        player.sendMessage("§7  Waiting for an opponent...");
        player.sendMessage("§7  Type §c/cf cancel §7to cancel your bet.");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

        // Broadcast to server
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.sendMessage("§6§l🪙 §f" + player.getName() + " §7placed a coinflip bet of §e"
                        + eco.formatBalance(amount) + "§7! Type §f/cf §7to accept.");
            }
        }
    }

    /**
     * Cancel a player's active bet and refund
     */
    public void cancelBet(Player player) {
        UUID uuid = player.getUniqueId();
        Double amount = activeBets.remove(uuid);

        if (amount == null) {
            player.sendMessage("§c§lKZ §8» §7You don't have an active bet.");
            return;
        }

        // Refund
        plugin.getEconomyManager().addBalance(player, amount);

        player.sendMessage("§a§lKZ §8» §7Your coinflip bet of §e"
                + plugin.getEconomyManager().formatBalance(amount)
                + " §7has been cancelled and refunded.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
    }

    // ═══════════════════════════════
    //  BET LIST GUI
    // ═══════════════════════════════

    /**
     * Open GUI showing all active coinflip bets
     *
     * Layout (54 slots):
     * Row 0: [Info] [Fill] [Fill] [Fill] [Fill] [Fill] [Fill] [Fill] [Create]
     * Row 1-4: Player head bets (up to 32 bets)
     * Row 5: [Fill] [Fill] [Fill] [Fill] [Refresh] [Fill] [Fill] [Fill] [Cancel]
     */
    private void openCoinflipListGUI(Player player) {
        EconomyManager eco = plugin.getEconomyManager();
        Inventory gui = Bukkit.createInventory(null, 54, CF_LIST_TITLE);

        // ── Filler ──
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8", null);
        for (int i = 0; i < 9; i++) gui.setItem(i, filler.clone());
        for (int i = 45; i < 54; i++) gui.setItem(i, filler.clone());

        // ── Info button (slot 0) ──
        gui.setItem(0, createItem(Material.BOOK, "§e§lCoinflip Info", List.of(
                "§7Bet your money on a coin toss!",
                "§7Winner takes §aall§7!",
                "",
                "§7Active bets: §f" + activeBets.size(),
                "",
                "§eClick a bet to accept it!"
        )));

        // ── Create bet button (slot 8) ──
        gui.setItem(8, createItem(Material.GOLD_INGOT, "§6§lCreate New Bet", List.of(
                "§7Use §f/cf <amount> §7to create",
                "§7a new coinflip bet.",
                "",
                "§7Minimum bet: §f$100",
                "§7No maximum!",
                "",
                "§eClose this menu and type /cf <amount>"
        )));

        // ── Refresh button (slot 49) ──
        gui.setItem(49, createItem(Material.COMPASS, "§a§lRefresh", List.of(
                "§7Click to refresh the bet list."
        )));

        // ── Cancel my bet button (slot 53) ──
        if (activeBets.containsKey(player.getUniqueId())) {
            Double myBet = activeBets.get(player.getUniqueId());
            gui.setItem(53, createItem(Material.BARRIER, "§c§lCancel My Bet", List.of(
                    "§7Your bet: §e" + eco.formatBalance(myBet),
                    "",
                    "§cClick to cancel and refund."
            )));
        }

        // ── Player head bets (slots 9-44) ──
        int slot = 9;
        for (Map.Entry<UUID, Double> entry : activeBets.entrySet()) {
            if (slot > 44) break; // Max 36 bets visible

            UUID betterUUID = entry.getKey();
            double betAmount = entry.getValue();

            // Don't show player's own bet as clickable
            Player better = Bukkit.getPlayer(betterUUID);
            String betterName = better != null ? better.getName() : "Unknown";

            // Create player head
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                if (better != null) {
                    skullMeta.setOwningPlayer(better);
                }
                skullMeta.setDisplayName("§e§l" + betterName + "'s Bet");

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§7Bet Amount: §e§l" + eco.formatBalance(betAmount));
                lore.add("§7Player: §f" + betterName);
                lore.add("");

                if (betterUUID.equals(player.getUniqueId())) {
                    lore.add("§c§oThis is your own bet.");
                    lore.add("§cClick §fCancel My Bet §cto cancel.");
                } else {
                    lore.add("§7You need §e" + eco.formatBalance(betAmount) + " §7to accept.");
                    lore.add("§7Your balance: §a" + eco.formatBalance(eco.getBalance(player)));
                    lore.add("");
                    if (eco.hasEnough(player, betAmount)) {
                        lore.add("§a§l▶ Click to accept this bet!");
                    } else {
                        lore.add("§c§oInsufficient balance.");
                    }
                }

                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }

            gui.setItem(slot, head);
            slot++;
        }

        // Fill empty bet slots
        ItemStack emptySlot = createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                "§7Empty Slot", List.of("§7No bet here yet."));
        while (slot <= 44) {
            gui.setItem(slot, emptySlot.clone());
            slot++;
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    // ═══════════════════════════════
    //  ACCEPT BET & ANIMATION
    // ═══════════════════════════════

    private void acceptBet(Player acceptor, UUID betterUUID) {
        EconomyManager eco = plugin.getEconomyManager();

        Double betAmount = activeBets.get(betterUUID);
        if (betAmount == null) {
            acceptor.sendMessage("§c§lKZ §8» §7This bet is no longer available.");
            acceptor.playSound(acceptor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        Player better = Bukkit.getPlayer(betterUUID);
        if (better == null || !better.isOnline()) {
            // Refund and remove
            activeBets.remove(betterUUID);
            acceptor.sendMessage("§c§lKZ §8» §7The bet creator is no longer online. Bet removed.");
            return;
        }

        if (betterUUID.equals(acceptor.getUniqueId())) {
            acceptor.sendMessage("§c§lKZ §8» §7You cannot accept your own bet!");
            return;
        }

        // Check if acceptor has enough money
        if (!eco.hasEnough(acceptor, betAmount)) {
            acceptor.sendMessage("§c§lKZ §8» §7Insufficient balance.");
            acceptor.sendMessage("  §7Required: §e" + eco.formatBalance(betAmount));
            acceptor.sendMessage("  §7Your balance: §a" + eco.formatBalance(eco.getBalance(acceptor)));
            acceptor.playSound(acceptor.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Deduct from acceptor
        eco.removeBalance(acceptor, betAmount);

        // Remove bet from active list
        activeBets.remove(betterUUID);

        // Close GUIs
        acceptor.closeInventory();

        // Mark both in animation
        inAnimation.add(acceptor.getUniqueId());
        inAnimation.add(betterUUID);

        // Start animation
        startCoinflipAnimation(better, acceptor, betAmount);
    }

    /**
     * Coinflip animation with GUI
     * Shows alternating heads of both players
     * Then reveals winner
     */
    private void startCoinflipAnimation(Player player1, Player player2, double betAmount) {
        EconomyManager eco = plugin.getEconomyManager();
        double totalPot = betAmount * 2;

        // Notify both players
        String msg = "§6§l🪙 COINFLIP §8» §f" + player1.getName() + " §7vs §f" + player2.getName()
                + " §8| §ePot: " + eco.formatBalance(totalPot);

        player1.sendMessage(msg);
        player2.sendMessage(msg);

        // Broadcast
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player1) && !online.equals(player2)) {
                online.sendMessage(msg);
            }
        }

        // Create animation GUI for both players
        Inventory animGUI = Bukkit.createInventory(null, 27, CF_RESULT_TITLE);

        // Fill with filler
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, "§8", null);
        for (int i = 0; i < 27; i++) animGUI.setItem(i, filler.clone());

        // Center slot (13) will show the spinning coin
        ItemStack p1Head = createPlayerHead(player1, "§e" + player1.getName());
        ItemStack p2Head = createPlayerHead(player2, "§e" + player2.getName());

        // Info items
        animGUI.setItem(4, createItem(Material.GOLD_BLOCK, "§6§lCOINFLIP", List.of(
                "§7" + player1.getName() + " §7vs §7" + player2.getName(),
                "§7Pot: §e" + eco.formatBalance(totalPot)
        )));

        // Player heads on sides
        animGUI.setItem(10, createPlayerHead(player1, "§a" + player1.getName()));
        animGUI.setItem(16, createPlayerHead(player2, "§c" + player2.getName()));

        player1.openInventory(animGUI);

        // Clone for player2
        Inventory animGUI2 = Bukkit.createInventory(null, 27, CF_RESULT_TITLE);
        for (int i = 0; i < 27; i++) {
            ItemStack item = animGUI.getItem(i);
            animGUI2.setItem(i, item != null ? item.clone() : null);
        }
        player2.openInventory(animGUI2);

        // Determine winner NOW (but reveal later)
        boolean player1Wins = new Random().nextBoolean();
        Player winner = player1Wins ? player1 : player2;
        Player loser = player1Wins ? player2 : player1;

        // Animation: alternate heads in center slot
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 15; // ~3 seconds at 4 ticks each

            @Override
            public void run() {
                if (!player1.isOnline() || !player2.isOnline()) {
                    handleDisconnect(player1, player2, betAmount);
                    cancel();
                    return;
                }

                if (tick >= totalTicks) {
                    // ── REVEAL WINNER ──
                    cancel();
                    revealWinner(player1, player2, winner, loser, betAmount, totalPot);
                    return;
                }

                // Alternate between player heads
                ItemStack currentHead = (tick % 2 == 0) ? p1Head.clone() : p2Head.clone();

                // Update center slot in both GUIs
                Inventory inv1 = player1.getOpenInventory().getTopInventory();
                Inventory inv2 = player2.getOpenInventory().getTopInventory();

                if (inv1 != null && inv1.getSize() == 27) inv1.setItem(13, currentHead);
                if (inv2 != null && inv2.getSize() == 27) inv2.setItem(13, currentHead);

                // Sound
                float pitch = 0.5f + (tick * 0.1f);
                player1.playSound(player1.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
                player2.playSound(player2.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);

                tick++;
            }
        }.runTaskTimer(plugin, 5L, 4L);
    }

    /**
     * Reveal the winner after animation
     */
    private void revealWinner(Player player1, Player player2, Player winner, Player loser,
                               double betAmount, double totalPot) {
        EconomyManager eco = plugin.getEconomyManager();

        // Give winnings
        eco.addBalance(winner, totalPot);

        // Close animation GUIs
        player1.closeInventory();
        player2.closeInventory();

        // Remove from animation set
        inAnimation.remove(player1.getUniqueId());
        inAnimation.remove(player2.getUniqueId());

        // ── Winner message ──
        winner.sendMessage("");
        winner.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("§a§l  🪙 YOU WON THE COINFLIP!");
        winner.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("§7  Opponent : §f" + loser.getName());
        winner.sendMessage("§7  Pot      : §e" + eco.formatBalance(totalPot));
        winner.sendMessage("§7  Won      : §a+" + eco.formatBalance(totalPot));
        winner.sendMessage("§7  Balance  : §a" + eco.formatBalance(eco.getBalance(winner)));
        winner.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        winner.sendMessage("");
        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // Firework for winner
        try {
            winner.getWorld().spawn(winner.getLocation(), org.bukkit.entity.Firework.class);
        } catch (Exception ignored) {}

        // ── Loser message ──
        loser.sendMessage("");
        loser.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        loser.sendMessage("§c§l  🪙 YOU LOST THE COINFLIP...");
        loser.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        loser.sendMessage("§7  Opponent : §f" + winner.getName());
        loser.sendMessage("§7  Pot      : §e" + eco.formatBalance(totalPot));
        loser.sendMessage("§7  Lost     : §c-" + eco.formatBalance(betAmount));
        loser.sendMessage("§7  Balance  : §a" + eco.formatBalance(eco.getBalance(loser)));
        loser.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        loser.sendMessage("");
        loser.playSound(loser.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);

        // ── Broadcast result ──
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(winner) && !online.equals(loser)) {
                online.sendMessage("§6§l🪙 §f" + winner.getName() + " §7won §e"
                        + eco.formatBalance(totalPot) + " §7in a coinflip vs §f"
                        + loser.getName() + "§7!");
            }
        }
    }

    /**
     * Handle disconnect during animation - refund both
     */
    private void handleDisconnect(Player player1, Player player2, double betAmount) {
        EconomyManager eco = plugin.getEconomyManager();

        // Refund both players
        if (player1.isOnline()) {
            eco.addBalance(player1, betAmount);
            player1.closeInventory();
            player1.sendMessage("§c§lKZ §8» §7Coinflip cancelled. Opponent disconnected. Refunded.");
        }
        if (player2.isOnline()) {
            eco.addBalance(player2, betAmount);
            player2.closeInventory();
            player2.sendMessage("§c§lKZ §8» §7Coinflip cancelled. Opponent disconnected. Refunded.");
        }

        inAnimation.remove(player1.getUniqueId());
        inAnimation.remove(player2.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  GUI EVENT HANDLERS
    // ════════════════════════════════════════════════════════════════

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = player.getOpenInventory().getTitle();

        // ── Coinflip List GUI ──
        if (title.equals(CF_LIST_TITLE)) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            if (slot < 0 || slot >= 54) return;

            // Refresh button
            if (slot == 49) {
                openCoinflipListGUI(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                return;
            }

            // Cancel my bet button
            if (slot == 53) {
                cancelBet(player);
                openCoinflipListGUI(player); // Refresh
                return;
            }

            // Bet slots (9-44) - player heads
            if (slot >= 9 && slot <= 44) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta == null || meta.getOwningPlayer() == null) return;

                UUID betterUUID = meta.getOwningPlayer().getUniqueId();
                acceptBet(player, betterUUID);
            }

            return;
        }

        // ── Coinflip Result GUI - block all clicks ──
        if (title.equals(CF_RESULT_TITLE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Nothing special needed - bets stay active even when GUI closed
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Refund if player has active bet
        Double bet = activeBets.remove(uuid);
        if (bet != null) {
            plugin.getEconomyManager().addBalance(uuid, bet);
            plugin.getLogger().info("[CF] Refunded " + event.getPlayer().getName()
                    + "'s bet of $" + bet + " on quit.");
        }

        // Clean up animation
        inAnimation.remove(uuid);
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY - Item builders
    // ════════════════════════════════════════════════════════════════

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(Player player, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(displayName);
            head.setItemMeta(meta);
        }
        return head;
    }
}
