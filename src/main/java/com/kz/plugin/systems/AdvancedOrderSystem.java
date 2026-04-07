// ============================================================
// Path: src/main/java/com/kz/plugin/systems/AdvancedOrderSystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AdvancedOrderSystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // ════════════════════════════════════════════════════════════════
    //  DATA MODEL
    // ════════════════════════════════════════════════════════════════

    public static class BuyOrder {
        public String   id;
        public UUID     buyerUUID;
        public String   buyerName;
        public Material material;
        public String   displayName;
        public int      amountNeeded; // 0 = unlimited
        public int      amountFilled;
        public double   totalPrice;   // 0 = unlimited budget
        public double   pricePerItem;
        public double   paidOut;
        public long     createdAt;
        public String   category;
        public boolean  completed;
        public boolean  unlimited;    // true = tidak ada batas amount
        public List<ItemStack> stash = new ArrayList<>();

        public int amountRemaining() {
            if (unlimited) return Integer.MAX_VALUE;
            return amountNeeded - amountFilled;
        }

        public double priceRemaining() {
            return totalPrice - paidOut;
        }

        public int fillPercent() {
            if (unlimited) return 0; // Unlimited tidak ada persentase
            if (amountNeeded == 0) return 100;
            return (int)((amountFilled * 100.0) / amountNeeded);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ENUMS
    // ════════════════════════════════════════════════════════════════

    public enum Category { ALL, BLOCKS, TOOLS, NATURE, COMBAT, OTHERS }
    public enum SortType  { RECENTLY_LISTED, LOWER_PAID, HIGHER_PAID }

    // ════════════════════════════════════════════════════════════════
    //  STORAGE
    // ════════════════════════════════════════════════════════════════

    private final Map<String, BuyOrder> allOrders      = new LinkedHashMap<>();
    private final Map<UUID, Category>   playerCategory = new HashMap<>();
    private final Map<UUID, SortType>   playerSort     = new HashMap<>();
    private final Map<UUID, String>     playerSearch   = new HashMap<>();
    private final Map<UUID, Integer>    playerPage     = new HashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public AdvancedOrderSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadData();
        plugin.getLogger().info("[Order] System initialized. Orders: " + allOrders.size());
    }

    // ════════════════════════════════════════════════════════════════
    //  CORE LOGIC
    // ════════════════════════════════════════════════════════════════

    /**
     * Buat order baru dengan amount tertentu.
     * Amount = 0 → unlimited (terima barang selama budget masih ada).
     *
     * @param buyer      Player yang buat order
     * @param material   Item yang diminta
     * @param amount     Jumlah yang diminta (0 = unlimited)
     * @param totalPrice Total budget escrow (0 = unlimited, bayar per item saat ada)
     * @param pricePerItem Harga per 1 item
     * @return null = sukses, String = pesan error
     */
    public String createOrder(Player buyer, Material material,
                              int amount, double totalPrice, double pricePerItem) {

        // Validasi harga per item wajib ada
        if (pricePerItem <= 0) return "§cPrice per item must be greater than 0.";

        // Validasi amount (0 = unlimited, boleh)
        if (amount < 0) return "§cAmount cannot be negative.";

        // Kalau amount ada, hitung totalPrice otomatis
        double escrow = totalPrice;
        boolean isUnlimited = (amount == 0);

        if (!isUnlimited) {
            // Fixed amount → escrow = amount × pricePerItem
            escrow = amount * pricePerItem;
        } else {
            // Unlimited amount → escrow = totalPrice yang diinput
            if (totalPrice <= 0) return "§cFor unlimited orders, you must set a budget.";
        }

        // Cek balance
        double balance = plugin.getEconomyManager().getBalance(buyer);
        if (balance < escrow) {
            return "§cInsufficient funds. Need §f"
                    + plugin.getLobbySystem().formatCoins(escrow)
                    + " §cbut you have §f"
                    + plugin.getLobbySystem().formatCoins(balance) + "§c.";
        }

        // Cek duplikat order aktif (material sama, belum selesai)
        for (BuyOrder o : allOrders.values()) {
            if (o.buyerUUID.equals(buyer.getUniqueId())
                    && o.material == material
                    && !o.completed) {
                return "§cYou already have an active order for §f"
                        + formatMaterial(material) + "§c. Cancel it first.";
            }
        }

        // Potong uang (escrow)
        plugin.getEconomyManager().removeBalance(buyer, escrow);

        // Buat order
        BuyOrder order      = new BuyOrder();
        order.id            = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.buyerUUID     = buyer.getUniqueId();
        order.buyerName     = buyer.getName();
        order.material      = material;
        order.displayName   = formatMaterial(material);
        order.amountNeeded  = amount;       // 0 jika unlimited
        order.amountFilled  = 0;
        order.totalPrice    = escrow;
        order.pricePerItem  = pricePerItem;
        order.paidOut       = 0;
        order.createdAt     = System.currentTimeMillis();
        order.category      = detectCategory(material);
        order.completed     = false;
        order.unlimited     = isUnlimited;
        order.stash         = new ArrayList<>();

        allOrders.put(order.id, order);
        saveData();

        plugin.getLogger().info("[Order] Created: #" + order.id
                + " | " + buyer.getName()
                + " | " + (isUnlimited ? "UNLIMITED" : amount + "x") + " " + material.name()
                + " | $" + pricePerItem + "/ea"
                + " | Escrow: $" + escrow);

        return null;
    }

    /**
     * Overload: createOrder dengan amount tertentu (non-unlimited).
     * totalPrice dihitung otomatis dari amount × pricePerItem.
     */
    public String createOrder(Player buyer, Material material,
                              int amount, double pricePerItem) {
        return createOrder(buyer, material, amount, 0, pricePerItem);
    }

    /**
     * Supplier setor item ke order.
     * Mendukung partial delivery.
     * Untuk unlimited order: terima barang selama budget (priceRemaining) masih ada.
     *
     * @return pesan hasil (multiline dengan \n)
     */
    public String supplyOrder(Player supplier, String orderId, List<ItemStack> items) {
        BuyOrder order = allOrders.get(orderId);
        if (order == null)    return "§cOrder not found.";
        if (order.completed)  return "§cThis order is already completed.";
        if (order.buyerUUID.equals(supplier.getUniqueId()))
            return "§cYou cannot supply your own order.";

        // Hitung total item valid yang dibawa supplier
        int totalSupplied = 0;
        for (ItemStack item : items) {
            if (item != null && item.getType() == order.material) {
                totalSupplied += item.getAmount();
            }
        }

        if (totalSupplied <= 0) {
            return "§cNo valid items. Order needs: §f" + formatMaterial(order.material);
        }

        // Hitung berapa yang bisa diterima
        int canAccept;
        if (order.unlimited) {
            // Unlimited: dibatasi oleh budget yang tersisa
            double budgetLeft = order.priceRemaining();
            if (budgetLeft <= 0) {
                // Budget habis → tandai completed
                order.completed = true;
                saveData();
                return "§cThis order's budget is exhausted.";
            }
            // Maksimal item yang bisa dibayar dengan budget tersisa
            canAccept = (int) Math.floor(budgetLeft / order.pricePerItem);
            if (canAccept <= 0) {
                return "§cNot enough budget remaining for even 1 item.";
            }
        } else {
            canAccept = order.amountRemaining();
        }

        int actualSupply = Math.min(totalSupplied, canAccept);
        int excess       = totalSupplied - actualSupply;

        // Hitung bayaran
        double payment = actualSupply * order.pricePerItem;

        // Pastikan tidak over-pay (floating point safety)
        if (payment > order.priceRemaining()) {
            payment = order.priceRemaining();
            actualSupply = (int) Math.floor(payment / order.pricePerItem);
            excess = totalSupplied - actualSupply;
            payment = actualSupply * order.pricePerItem;
        }

        if (actualSupply <= 0) {
            return "§cCannot accept items. Budget exhausted.";
        }

        // Bayar supplier
        plugin.getEconomyManager().addBalance(supplier, payment);

        // Masukkan item ke stash buyer
        addToStash(order, order.material, actualSupply);

        // Hapus item dari inventory supplier
        removeItemsFromInventory(supplier, order.material, actualSupply);

        // Update order
        order.amountFilled += actualSupply;
        order.paidOut      += payment;

        // Cek apakah selesai
        if (order.unlimited) {
            // Unlimited selesai jika budget habis
            order.completed = order.priceRemaining() < order.pricePerItem;
        } else {
            order.completed = order.amountFilled >= order.amountNeeded;
        }

        // Kembalikan item berlebih ke supplier
        if (excess > 0) {
            ItemStack excessItem = new ItemStack(order.material, excess);
            Map<Integer, ItemStack> leftover = supplier.getInventory().addItem(excessItem);
            for (ItemStack left : leftover.values()) {
                supplier.getWorld().dropItemNaturally(supplier.getLocation(), left);
            }
        }

        saveData();

        // Notifikasi buyer jika online
        Player buyer = Bukkit.getPlayer(order.buyerUUID);
        if (buyer != null && buyer.isOnline()) {
            buyer.sendMessage("§a§lORDER §8» §7Order §b#" + order.id
                    + " §7received §a" + actualSupply + "x "
                    + order.displayName + " §7from §f" + supplier.getName() + "§7!");
            buyer.playSound(buyer.getLocation(),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

            if (order.completed) {
                buyer.sendMessage("§a§lORDER §8» §a✔ Order §b#" + order.id
                        + (order.unlimited
                        ? " §a(budget exhausted)."
                        : " §acompleted!")
                        + " Open §b/myorders §ato collect.");
                buyer.playSound(buyer.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
        }

        // Build result
        StringBuilder result = new StringBuilder();
        result.append("§a§lORDER §8» §7Supplied §a").append(actualSupply)
                .append("x ").append(order.displayName)
                .append("§7. Earned: §a")
                .append(plugin.getLobbySystem().formatCoins(payment));

        if (excess > 0)
            result.append("\n§e§lORDER §8» §7").append(excess)
                    .append(" §7excess items returned.");
        if (order.completed) {
            result.append(order.unlimited
                    ? "\n§e§lORDER §8» §eBudget exhausted. Order closed."
                    : "\n§a§lORDER §8» §a✔ Order fully fulfilled!");
        } else if (order.unlimited) {
            result.append("\n§7Budget left: §a")
                    .append(plugin.getLobbySystem().formatCoins(order.priceRemaining()));
        }

        return result.toString();
    }

    /**
     * Buyer ambil semua item dari stash ke inventory.
     */
    public String collectStash(Player buyer, String orderId) {
        BuyOrder order = allOrders.get(orderId);
        if (order == null)
            return "§cOrder not found.";
        if (!order.buyerUUID.equals(buyer.getUniqueId()))
            return "§cThis is not your order.";
        if (order.stash.isEmpty())
            return "§cStash is empty. Wait for suppliers.";

        int collected = 0;
        int dropped   = 0;

        List<ItemStack> toRemove = new ArrayList<>(order.stash);
        order.stash.clear();

        for (ItemStack item : toRemove) {
            if (item == null) continue;
            Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(item.clone());

            if (leftover.isEmpty()) {
                collected += item.getAmount();
            } else {
                int leftoverTotal = leftover.values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
                collected += item.getAmount() - leftoverTotal;
                for (ItemStack left : leftover.values()) {
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), left);
                    dropped += left.getAmount();
                }
            }
        }

        saveData();

        StringBuilder result = new StringBuilder();
        result.append("§a§lORDER §8» §7Collected §a").append(collected)
                .append("x ").append(order.displayName).append(" §7to inventory.");
        if (dropped > 0)
            result.append("\n§e§lORDER §8» §e").append(dropped)
                    .append(" §7items dropped (inventory full).");

        return result.toString();
    }

    /**
     * Cancel order, refund escrow sisa ke buyer.
     */
    public String cancelOrder(Player buyer, String orderId) {
        BuyOrder order = allOrders.get(orderId);
        if (order == null)
            return "§cOrder not found.";
        if (!order.buyerUUID.equals(buyer.getUniqueId()))
            return "§cThis is not your order.";
        if (order.completed && order.stash.isEmpty())
            return "§cCompleted orders with empty stash cannot be cancelled.";

        // Refund escrow sisa
        double refund = order.priceRemaining();
        if (refund > 0) {
            plugin.getEconomyManager().addBalance(buyer, refund);
        }

        // Kembalikan item di stash
        for (ItemStack item : order.stash) {
            if (item == null) continue;
            Map<Integer, ItemStack> leftover = buyer.getInventory().addItem(item.clone());
            for (ItemStack left : leftover.values()) {
                buyer.getWorld().dropItemNaturally(buyer.getLocation(), left);
            }
        }

        allOrders.remove(orderId);
        saveData();

        return "§a§lORDER §8» §7Order §b#" + orderId
                + " §7cancelled. Refunded: §a"
                + plugin.getLobbySystem().formatCoins(refund);
    }

    // ════════════════════════════════════════════════════════════════
    //  FILTER & SORT
    // ════════════════════════════════════════════════════════════════

    public List<BuyOrder> getFilteredOrders(UUID playerUUID) {
        Category cat    = playerCategory.getOrDefault(playerUUID, Category.ALL);
        SortType sort   = playerSort.getOrDefault(playerUUID, SortType.RECENTLY_LISTED);
        String   search = playerSearch.getOrDefault(playerUUID, "").toLowerCase().trim();

        return allOrders.values().stream()
                // Tampilkan: belum selesai ATAU masih ada stash
                .filter(o -> !o.completed || !o.stash.isEmpty())
                .filter(o -> cat == Category.ALL
                        || o.category.equalsIgnoreCase(cat.name()))
                .filter(o -> search.isBlank()
                        || o.displayName.toLowerCase().contains(search)
                        || o.material.name().toLowerCase().contains(search))
                .sorted((a, b) -> switch (sort) {
                    case RECENTLY_LISTED -> Long.compare(b.createdAt, a.createdAt);
                    case LOWER_PAID      -> Double.compare(a.pricePerItem, b.pricePerItem);
                    case HIGHER_PAID     -> Double.compare(b.pricePerItem, a.pricePerItem);
                })
                .collect(Collectors.toList());
    }

    public List<BuyOrder> getMyOrders(UUID playerUUID) {
        return allOrders.values().stream()
                .filter(o -> o.buyerUUID.equals(playerUUID))
                .sorted((a, b) -> Long.compare(b.createdAt, a.createdAt))
                .collect(Collectors.toList());
    }

    public BuyOrder getOrder(String orderId) {
        return allOrders.get(orderId);
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER STATE
    // ════════════════════════════════════════════════════════════════

    public void    setCategory(UUID uuid, Category cat)  { playerCategory.put(uuid, cat); }
    public void    setSort(UUID uuid, SortType sort)     { playerSort.put(uuid, sort); }
    public void    setSearch(UUID uuid, String search)   { playerSearch.put(uuid, search); }
    public void    setPage(UUID uuid, int page)          { playerPage.put(uuid, page); }

    public Category getCategory(UUID uuid) { return playerCategory.getOrDefault(uuid, Category.ALL); }
    public SortType getSort(UUID uuid)     { return playerSort.getOrDefault(uuid, SortType.RECENTLY_LISTED); }
    public String   getSearch(UUID uuid)   { return playerSearch.getOrDefault(uuid, ""); }
    public int      getPage(UUID uuid)     { return playerPage.getOrDefault(uuid, 0); }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private void addToStash(BuyOrder order, Material material, int amount) {
        int remaining = amount;

        // Stack ke ItemStack yang sudah ada di stash
        for (ItemStack existing : order.stash) {
            if (existing == null || existing.getType() != material) continue;
            int canAdd = existing.getMaxStackSize() - existing.getAmount();
            if (canAdd <= 0) continue;
            int add = Math.min(canAdd, remaining);
            existing.setAmount(existing.getAmount() + add);
            remaining -= add;
            if (remaining <= 0) return;
        }

        // Buat stack baru
        while (remaining > 0) {
            int stackSize = Math.min(remaining, material.getMaxStackSize());
            order.stash.add(new ItemStack(material, stackSize));
            remaining -= stackSize;
        }
    }

    private void removeItemsFromInventory(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                item.setAmount(0);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
            if (remaining <= 0) break;
        }
    }

    private String detectCategory(Material material) {
        String name = material.name();
        if (material.isBlock()) return "BLOCKS";
        if (name.contains("SWORD")  || name.contains("BOW")
                || name.contains("ARROW")  || name.contains("SHIELD")
                || name.contains("HELMET") || name.contains("CHESTPLATE")
                || name.contains("LEGGINGS") || name.contains("BOOTS")
                || name.contains("CROSSBOW") || name.contains("TRIDENT"))
            return "COMBAT";
        if (name.contains("PICKAXE") || name.contains("SHOVEL")
                || name.contains("HOE")    || name.contains("AXE")
                || name.contains("SHEARS") || name.contains("BUCKET")
                || name.contains("FISHING_ROD") || name.contains("FLINT_AND_STEEL"))
            return "TOOLS";
        if (material.isEdible()        || name.contains("SEED")
                || name.contains("LOG")    || name.contains("LEAVES")
                || name.contains("FLOWER") || name.contains("GRASS")
                || name.contains("SAPLING")|| name.contains("WHEAT")
                || name.contains("CARROT") || name.contains("POTATO")
                || name.contains("MELON")  || name.contains("PUMPKIN"))
            return "NATURE";
        return "OTHERS";
    }

    public String formatMaterial(Material material) {
        return Arrays.stream(material.name().split("_"))
                .map(w -> w.isEmpty() ? w
                        : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public int getStashTotal(BuyOrder order) {
        return order.stash.stream()
                .filter(Objects::nonNull)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    // ════════════════════════════════════════════════════════════════
    //  EXTRA METHODS (dipanggil KZPlugin)
    // ════════════════════════════════════════════════════════════════

    public int getTotalOrders() {
        return allOrders.size();
    }

    /**
     * Save SYNCHRONOUS - dipanggil saat onDisable()
     */
    public void saveDataSync() {
        doSave();
        plugin.getLogger().info("[Order] Saved " + allOrders.size() + " orders (sync).");
    }

    // ════════════════════════════════════════════════════════════════
    //  SAVE / LOAD (orders.yml)
    // ════════════════════════════════════════════════════════════════

    public void saveData() {
        // Async save (normal operation)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::doSave);
    }

    /**
     * Core save logic - dipanggil baik sync maupun async
     */
    private void doSave() {
        dataConfig.set("orders", null);

        for (BuyOrder order : allOrders.values()) {
            String path = "orders." + order.id;
            dataConfig.set(path + ".buyerUUID",    order.buyerUUID.toString());
            dataConfig.set(path + ".buyerName",    order.buyerName);
            dataConfig.set(path + ".material",     order.material.name());
            dataConfig.set(path + ".displayName",  order.displayName);
            dataConfig.set(path + ".amountNeeded", order.amountNeeded);
            dataConfig.set(path + ".amountFilled", order.amountFilled);
            dataConfig.set(path + ".totalPrice",   order.totalPrice);
            dataConfig.set(path + ".pricePerItem", order.pricePerItem);
            dataConfig.set(path + ".paidOut",      order.paidOut);
            dataConfig.set(path + ".createdAt",    order.createdAt);
            dataConfig.set(path + ".category",     order.category);
            dataConfig.set(path + ".completed",    order.completed);
            dataConfig.set(path + ".unlimited",    order.unlimited);

            // Serialize stash
            List<Map<String, Object>> stashData = new ArrayList<>();
            for (ItemStack item : order.stash) {
                if (item != null) stashData.add(item.serialize());
            }
            dataConfig.set(path + ".stash", stashData);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Order] Save failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "orders.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[Order] Failed to create orders.yml: " + e.getMessage());
                return;
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        var section = dataConfig.getConfigurationSection("orders");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            try {
                String   path  = "orders." + id;
                BuyOrder order = new BuyOrder();

                order.id           = id;
                order.buyerUUID    = UUID.fromString(
                        dataConfig.getString(path + ".buyerUUID",
                                UUID.randomUUID().toString()));
                order.buyerName    = dataConfig.getString(path + ".buyerName",   "Unknown");
                order.material     = Material.valueOf(
                        dataConfig.getString(path + ".material",  "STONE"));
                order.displayName  = dataConfig.getString(path + ".displayName", "Unknown");
                order.amountNeeded = dataConfig.getInt(path + ".amountNeeded",   0);
                order.amountFilled = dataConfig.getInt(path + ".amountFilled",   0);
                order.totalPrice   = dataConfig.getDouble(path + ".totalPrice",  0);
                order.pricePerItem = dataConfig.getDouble(path + ".pricePerItem",0);
                order.paidOut      = dataConfig.getDouble(path + ".paidOut",     0);
                order.createdAt    = dataConfig.getLong(path + ".createdAt",
                        System.currentTimeMillis());
                order.category     = dataConfig.getString(path + ".category",    "OTHERS");
                order.completed    = dataConfig.getBoolean(path + ".completed",  false);
                order.unlimited    = dataConfig.getBoolean(path + ".unlimited",  false);

                // Deserialize stash
                List<?> stashRaw = dataConfig.getList(path + ".stash", new ArrayList<>());
                for (Object obj : stashRaw) {
                    if (obj instanceof Map) {
                        try {
                            order.stash.add(
                                    ItemStack.deserialize((Map<String, Object>) obj));
                        } catch (Exception ignored) {}
                    }
                }

                allOrders.put(id, order);

            } catch (Exception e) {
                plugin.getLogger().warning("[Order] Failed to load order "
                        + id + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("[Order] Loaded " + allOrders.size() + " orders.");
    }
}
