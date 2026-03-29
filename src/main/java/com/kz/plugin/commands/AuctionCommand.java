package com.kz.plugin.commands;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionCommand implements CommandExecutor {

    private final KZPlugin plugin;

    // Auction data
    private static int auctionCounter = 0;
    private static final Map<Integer, AuctionItem> listings = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerPage = new HashMap<>();

    // Inbox for expired items
    private static final Map<UUID, List<InboxItem>> inbox = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 28;
    private static final int MAX_LISTINGS_PER_PLAYER = 5;

    private static final int[] CONTENT_SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34,
        37,38,39,40,41,42,43
    };

    public static class AuctionItem {
        public int id;
        public Material material;
        public int amount;
        public int price;
        public UUID seller;
        public String sellerName;
        public long expireTime;

        public AuctionItem(int id, Material material, int amount, int price,
                          UUID seller, String sellerName) {
            this.id = id;
            this.material = material;
            this.amount = amount;
            this.price = price;
            this.seller = seller;
            this.sellerName = sellerName;
            this.expireTime = System.currentTimeMillis() + 86400000L; // 24 hours
        }
    }

    public static class InboxItem {
        public Material material;
        public int amount;

        public InboxItem(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }
    }

    public AuctionCommand(KZPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command is for players only.");
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("inbox")) {
            openInbox(player);
            return true;
        }

        // /ah command
        if (args.length == 0) {
            int page = playerPage.getOrDefault(player.getUniqueId(), 1);
            openAuctionHouse(player, page);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell":
                handleSell(player, args);
                break;
            case "cancel":
                handleCancel(player, args);
                break;
            default:
                player.sendMessage("§b§lKZ §8» §7Auction House Commands:");
                player.sendMessage("§f  /ah §8→ §7Browse listings");
                player.sendMessage("§f  /ah sell <price> [amount] §8→ §7Sell held item");
                player.sendMessage("§f  /ah cancel <id> §8→ §7Cancel listing");
                player.sendMessage("§f  /inbox §8→ §7Collect expired items");
                break;
        }

        return true;
    }

    // ══════════════════════════════════════
    //  OPEN AUCTION HOUSE GUI
    // ══════════════════════════════════════

    public void openAuctionHouse(Player player, int page) {
        List<AuctionItem> validItems = new ArrayList<>(listings.values());
        int total = validItems.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));
        page = Math.max(1, Math.min(page, maxPage));
        playerPage.put(player.getUniqueId(), page);

        String title = "§3§lAuction House §8| §7" + page + "/" + maxPage;
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Render items
        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < CONTENT_SLOTS.length && (start + i) < total; i++) {
            AuctionItem ai = validItems.get(start + i);

            ItemStack item = new ItemStack(ai.material, Math.min(ai.amount, 64));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§b§l" + formatName(ai.material));
            meta.setLore(Arrays.asList(
                "§7━━━━━━━━━━━━━━━━",
                "§7Price   : §a$" + ai.price,
                "§7Seller  : §f" + ai.sellerName,
                "§7Amount  : §f" + ai.amount,
                "§7ID      : §3#" + ai.id,
                "§7━━━━━━━━━━━━━━━━",
                "",
                "§aClick to purchase."
            ));
            item.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[i], item);
        }

        // Navigation
        if (page > 1) {
            inv.setItem(45, createItem(Material.ARROW, "§e← Previous Page"));
        } else {
            inv.setItem(45, createItem(Material.BARRIER, "§8← Previous Page"));
        }

        inv.setItem(47, createItem(Material.CHEST, "§e§lMy Listings",
            "§7View your active listings."));

        inv.setItem(49, createItem(Material.NETHER_STAR, "§f§lAuction House",
            "§7Total listings: §b" + total,
            "§7Page: §f" + page + "/" + maxPage,
            "",
            "§eUse /ah sell <price> to list items."));

        inv.setItem(51, createItem(Material.EMERALD, "§a§lSell Item",
            "§7Hold an item and type:",
            "§f/ah sell <price> [amount]"));

        if (page < maxPage) {
            inv.setItem(53, createItem(Material.ARROW, "§a→ Next Page"));
        } else {
            inv.setItem(53, createItem(Material.BARRIER, "§8→ Next Page"));
        }

        player.openInventory(inv);
    }

    // ══════════════════════════════════════
    //  MY LISTINGS GUI
    // ══════════════════════════════════════

    public void openMyListings(Player player) {
        UUID uuid = player.getUniqueId();
        Inventory inv = Bukkit.createInventory(null, 54, "§e§lMy Listings");

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        int slotIndex = 0;
        for (AuctionItem ai : listings.values()) {
            if (!ai.seller.equals(uuid)) continue;
            if (slotIndex >= CONTENT_SLOTS.length) break;

            ItemStack item = new ItemStack(ai.material, Math.min(ai.amount, 64));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e§l" + formatName(ai.material) + " §7[YOURS]");
            meta.setLore(Arrays.asList(
                "§7━━━━━━━━━━━━━━━━",
                "§7Price  : §a$" + ai.price,
                "§7Amount : §f" + ai.amount,
                "§7ID     : §3#" + ai.id,
                "§7━━━━━━━━━━━━━━━━",
                "",
                "§cClick to REMOVE listing.",
                "§7Item will be returned to you."
            ));
            item.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[slotIndex], item);
            slotIndex++;
        }

        if (slotIndex == 0) {
            inv.setItem(22, createItem(Material.BARRIER, "§cNo Active Listings",
                "§7Use §f/ah sell <price> §7to list items."));
        }

        inv.setItem(49, createItem(Material.ARROW, "§c← Back to Auction House"));
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  SELL ITEM
    // ══════════════════════════════════════

    private void handleSell(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (args.length < 2) {
            player.sendMessage("§b§lKZ §8» §7Usage: §f/ah sell <price> [amount]");
            return;
        }

        int price;
        try {
            price = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§lKZ §8» §7Invalid price.");
            return;
        }

        if (price <= 0) {
            player.sendMessage("§c§lKZ §8» §7Price must be greater than 0.");
            return;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage("§c§lKZ §8» §7You must be holding an item.");
            return;
        }

        int amount = held.getAmount();
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§c§lKZ §8» §7Invalid amount.");
                return;
            }
        }

        if (amount > held.getAmount()) {
            player.sendMessage("§c§lKZ §8» §7You don't have that many items.");
            return;
        }

        // Check max listings
        long myCount = listings.values().stream()
            .filter(a -> a.seller.equals(uuid)).count();
        if (myCount >= MAX_LISTINGS_PER_PLAYER) {
            player.sendMessage("§c§lKZ §8» §7Maximum §f" + MAX_LISTINGS_PER_PLAYER + " §7listings at a time.");
            return;
        }

        // Remove from inventory
        if (amount >= held.getAmount()) {
            player.getInventory().setItemInMainHand(null);
        } else {
            held.setAmount(held.getAmount() - amount);
        }

        // Create listing
        auctionCounter++;
        AuctionItem ai = new AuctionItem(auctionCounter, held.getType(), amount,
            price, uuid, player.getName());
        listings.put(auctionCounter, ai);

        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  ITEM LISTED ON AUCTION");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Item   : §f" + formatName(held.getType()) + " x" + amount);
        player.sendMessage("§7  Price  : §a$" + price);
        player.sendMessage("§7  ID     : §3#" + auctionCounter);
        player.sendMessage("§7  Expires: §e24 hours");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  CANCEL LISTING
    // ══════════════════════════════════════

    private void handleCancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§b§lKZ §8» §7Usage: §f/ah cancel <id>");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§lKZ §8» §7Invalid ID.");
            return;
        }

        AuctionItem ai = listings.get(id);
        if (ai == null) {
            player.sendMessage("§c§lKZ §8» §7Listing not found.");
            return;
        }
        if (!ai.seller.equals(player.getUniqueId())) {
            player.sendMessage("§c§lKZ §8» §7That listing does not belong to you.");
            return;
        }

        listings.remove(id);
        player.getInventory().addItem(new ItemStack(ai.material, ai.amount));

        player.sendMessage("§a§lKZ §8» §f" + formatName(ai.material) + " x" + ai.amount + " §7returned to inventory.");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1f);
    }

    // ══════════════════════════════════════
    //  BUY FROM AH (Called from GUIListener)
    // ══════════════════════════════════════

    public void processPurchase(Player buyer, int listingId) {
        AuctionItem ai = listings.get(listingId);
        if (ai == null) {
            buyer.sendMessage("§c§lKZ §8» §7This listing is no longer available.");
            return;
        }

        if (ai.seller.equals(buyer.getUniqueId())) {
            buyer.sendMessage("§c§lKZ §8» §7You cannot purchase your own listing.");
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!plugin.getEconomyManager().removeBalance(buyer.getUniqueId(), ai.price)) {
            buyer.sendMessage("§c§lKZ §8» §7Insufficient balance. Required: §a$" + ai.price);
            buyer.playSound(buyer.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Give money to seller
        plugin.getEconomyManager().addBalance(ai.seller, ai.price);

        // Give item to buyer
        buyer.getInventory().addItem(new ItemStack(ai.material, ai.amount));
        listings.remove(listingId);

        buyer.closeInventory();

        buyer.sendMessage("");
        buyer.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        buyer.sendMessage("§f§l  PURCHASE SUCCESSFUL");
        buyer.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        buyer.sendMessage("§7  Item    : §f" + formatName(ai.material) + " x" + ai.amount);
        buyer.sendMessage("§7  Price   : §c-$" + ai.price);
        buyer.sendMessage("§7  Seller  : §f" + ai.sellerName);
        buyer.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(buyer)));
        buyer.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        buyer.sendMessage("");

        buyer.playSound(buyer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        // Notify seller
        Player seller = Bukkit.getPlayer(ai.seller);
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§a§lKZ §8» §f" + buyer.getName() + " §7purchased §f" +
                formatName(ai.material) + " x" + ai.amount + " §7for §a$" + ai.price + "§7.");
            seller.playSound(seller.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    // ══════════════════════════════════════
    //  REMOVE MY LISTING (from My Listings GUI)
    // ══════════════════════════════════════

    public void removeMyListing(Player player, int slotIndex) {
        UUID uuid = player.getUniqueId();
        List<AuctionItem> myItems = new ArrayList<>();

        for (AuctionItem ai : listings.values()) {
            if (ai.seller.equals(uuid)) myItems.add(ai);
        }

        if (slotIndex >= myItems.size()) return;

        AuctionItem ai = myItems.get(slotIndex);
        listings.remove(ai.id);

        player.getInventory().addItem(new ItemStack(ai.material, ai.amount));
        player.sendMessage("§a§lKZ §8» §f" + formatName(ai.material) + " x" + ai.amount + " §7returned.");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1f);

        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openMyListings(player), 1L);
    }

    // ══════════════════════════════════════
    //  INBOX
    // ══════════════════════════════════════

    public void openInbox(Player player) {
        UUID uuid = player.getUniqueId();
        List<InboxItem> items = inbox.getOrDefault(uuid, new ArrayList<>());

        Inventory inv = Bukkit.createInventory(null, 54, "§e§lInbox - Expired Items");

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        int slotIndex = 0;
        for (int i = 0; i < items.size() && slotIndex < 28; i++) {
            InboxItem ii = items.get(i);
            ItemStack item = new ItemStack(ii.material, Math.min(ii.amount, 64));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + formatName(ii.material) + " §7[EXPIRED]");
            meta.setLore(Arrays.asList(
                "§7Amount: §f" + ii.amount,
                "",
                "§aClick to collect."
            ));
            item.setItemMeta(meta);
            inv.setItem(CONTENT_SLOTS[slotIndex], item);
            slotIndex++;
        }

        if (slotIndex == 0) {
            inv.setItem(22, createItem(Material.BARRIER, "§7Inbox is empty.",
                "§7Expired auction items appear here."));
        }

        inv.setItem(49, createItem(Material.NETHER_STAR, "§e§lInbox",
            "§7Expired items from Auction House",
            "§7will be stored here."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    public void collectInboxItem(Player player, int slotIndex) {
        UUID uuid = player.getUniqueId();
        List<InboxItem> items = inbox.getOrDefault(uuid, new ArrayList<>());

        if (slotIndex >= items.size()) return;

        InboxItem ii = items.remove(slotIndex);
        player.getInventory().addItem(new ItemStack(ii.material, ii.amount));

        player.sendMessage("§a§lKZ §8» §f" + formatName(ii.material) + " x" + ii.amount + " §7collected.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> openInbox(player), 1L);
    }

    // ══════════════════════════════════════
    //  EXPIRE CHECKER (Called from scheduled task)
    // ══════════════════════════════════════

    public static void checkExpired(KZPlugin plugin) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, AuctionItem>> it = listings.entrySet().iterator();

        while (it.hasNext()) {
            AuctionItem ai = it.next().getValue();
            if (now > ai.expireTime) {
                // Move to inbox
                inbox.computeIfAbsent(ai.seller, k -> new ArrayList<>())
                    .add(new InboxItem(ai.material, ai.amount));

                it.remove();

                // Notify if online
                Player seller = Bukkit.getPlayer(ai.seller);
                if (seller != null && seller.isOnline()) {
                    seller.sendMessage("§e§lKZ §8» §7Your listing §f" +
                        formatName(ai.material) + " §7has expired. Check §f/inbox§7.");
                    seller.playSound(seller.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                }
            }
        }
    }

    // ══════════════════════════════════════
    //  STATIC GETTERS
    // ══════════════════════════════════════

    public static Map<Integer, AuctionItem> getListings() { return listings; }
    public static int getPlayerPageNum(UUID uuid) { return playerPage.getOrDefault(uuid, 1); }
    public static void setPlayerPage(UUID uuid, int page) { playerPage.put(uuid, page); }

    public static int getListingIdBySlot(int slot, int page) {
        List<AuctionItem> items = new ArrayList<>(listings.values());
        int start = (page - 1) * ITEMS_PER_PAGE;

        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) {
                int index = start + i;
                if (index < items.size()) return items.get(index).id;
            }
        }
        return -1;
    }

    public static boolean isContentSlot(int slot) {
        for (int s : CONTENT_SLOTS) if (s == slot) return true;
        return false;
    }

    public static int getContentSlotIndex(int slot) {
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    // ══════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════

    private static String formatName(Material material) {
        String name = material.name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (word.length() > 0) {
                result.append(word.substring(0, 1).toUpperCase())
                      .append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return result.toString().trim();
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}
