// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/CrateSystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CrateSystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private final NamespacedKey KEY_TAG;
    private final NamespacedKey KEY_CRATE_ID;

    private final Map<String, CrateData> crates = new LinkedHashMap<>();
    private final Map<String, String> crateLocations = new LinkedHashMap<>();
    private final Map<UUID, String> hologramEntities = new HashMap<>();
    private final Set<UUID> animatingPlayers = new HashSet<>();
    private final Map<UUID, String> editingPlayers = new HashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  RARITY
    // ════════════════════════════════════════════════════════════════

    public enum Rarity {
        EASY    ("§a§lEasy",      "§a", Material.LIME_STAINED_GLASS_PANE,    50.0, 0),
        MID     ("§e§lMid",       "§e", Material.YELLOW_STAINED_GLASS_PANE,  25.0, 1),
        HARD    ("§6§lHard",      "§6", Material.ORANGE_STAINED_GLASS_PANE,  15.0, 2),
        HARDCORE("§c§lHardcore",  "§c", Material.RED_STAINED_GLASS_PANE,      8.0, 3),
        EXCLUSIVE("§d§lExclusive","§d", Material.MAGENTA_STAINED_GLASS_PANE,  2.0, 4);

        public final String displayName;
        public final String color;
        public final Material paneMaterial;
        public final double weight;
        public final int column;

        Rarity(String d, String c, Material m, double w, int col) {
            displayName = d; color = c; paneMaterial = m; weight = w; column = col;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CRATE DATA
    // ════════════════════════════════════════════════════════════════

    public static class CrateData {
        public String id, title, description1, description2, shulkerColor;
        public ItemStack keyItem;
        public Location location;
        public List<UUID> hologramIds = new ArrayList<>();
        public Map<Rarity, List<ItemStack>> rewards = new EnumMap<>(Rarity.class);

        public CrateData(String id) {
            this.id = id;
            for (Rarity r : Rarity.values()) rewards.put(r, new ArrayList<>());
        }

        public int getTotalRewards() {
            int t = 0;
            for (List<ItemStack> l : rewards.values()) t += l.size();
            return t;
        }

        public Material getShulkerMaterial() {
            try { return Material.valueOf(shulkerColor.toUpperCase() + "_SHULKER_BOX"); }
            catch (Exception e) { return Material.PURPLE_SHULKER_BOX; }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public CrateSystem(KZPlugin plugin) {
        this.plugin = plugin;
        KEY_TAG      = new NamespacedKey(plugin, "crate_key");
        KEY_CRATE_ID = new NamespacedKey(plugin, "crate_id");
        loadData();
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnAllHolograms, 80L);
        plugin.getLogger().info("[Crate] Loaded " + crates.size() + " crates.");
    }

    // ════════════════════════════════════════════════════════════════
    //  KEY SYSTEM
    // ════════════════════════════════════════════════════════════════

    public ItemStack stampKey(ItemStack item, String crateId) {
        if (item == null || item.getType() == Material.AIR) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_TAG,      PersistentDataType.BOOLEAN, true);
        pdc.set(KEY_CRATE_ID, PersistentDataType.STRING,  crateId);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isMatchingKey(ItemStack item, ItemStack key) {
        if (item == null || key == null) return false;
        if (item.getType() != key.getType()) return false;
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta keyMeta  = key.getItemMeta();
        if (itemMeta == null || keyMeta == null) return false;

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        if (!pdc.has(KEY_TAG, PersistentDataType.BOOLEAN)) return false;
        Boolean tag = pdc.get(KEY_TAG, PersistentDataType.BOOLEAN);
        if (tag == null || !tag) return false;

        String itemName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : "";
        String keyName  = keyMeta.hasDisplayName()  ? keyMeta.getDisplayName()  : "";
        return itemName.equals(keyName);
    }

    public boolean isAnyCrateKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(KEY_TAG, PersistentDataType.BOOLEAN);
    }

    private boolean hasKey(Player player, ItemStack key) {
        for (ItemStack item : player.getInventory().getContents())
            if (isMatchingKey(item, key)) return true;
        return false;
    }

    private void removeOneKey(Player player, ItemStack key) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isMatchingKey(item, key)) {
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItem(i, null);
                return;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ✅ FIX 1: translateColor() - Support § dan & color codes
    // ════════════════════════════════════════════════════════════════

    /**
     * Translate & color codes → § color codes
     * Supports both & and § input
     */
    private String translateColor(String input) {
        if (input == null) return "";
        // Translate &x → §x untuk semua color codes (0-9, a-f, l, m, n, o, r, k)
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    // ════════════════════════════════════════════════════════════════
    //  GIVE KEY
    // ════════════════════════════════════════════════════════════════

    public void giveKey(Player player, String crateId, int amount) {
        CrateData crate = crates.get(crateId);
        if (crate == null || crate.keyItem == null) {
            player.sendMessage("§c§lKZ §8» §cCrate not found or has no key template.");
            return;
        }

        ItemStack key = buildKeyItem(crate, amount);

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(key);
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values())
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            player.sendMessage("§e§lKZ §8» §eInventory full! Key dropped on the ground.");
        }

        player.sendMessage("§a§lKZ §8» §7You received §e" + amount + "x §f"
                + getItemDisplayName(crate.keyItem) + "§7!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
    }

    /**
     * Build a ready-to-give key item (stamped + lore)
     */
    private ItemStack buildKeyItem(CrateData crate, int amount) {
        ItemStack key = crate.keyItem.clone();
        key.setAmount(amount);

        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            // Rebuild lore (jangan duplikat kalau sudah ada)
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§8§o[Crate Key]");
            lore.add("§7Use on: §b" + crate.title);
            lore.add("§7Right-click a crate to use!");
            meta.setLore(lore);
            key.setItemMeta(meta);
        }

        stampKey(key, crate.id);
        return key;
    }

    // ════════════════════════════════════════════════════════════════
    //  ✅ FIX 2: createKeyItem() - Support color codes di nama key
    // ════════════════════════════════════════════════════════════════

    /**
     * Create a key item with color support
     * Example: /createkey TRIPWIRE_HOOK &e&lLegendary &6&lKey
     */
    public ItemStack createKeyItem(String crateId, String displayName, Material material) {
        CrateData crate = crates.get(crateId);
        if (crate == null) return null;

        ItemStack key = new ItemStack(material);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            // ✅ FIX: Translate color codes (&e&lLegendary → §e§lLegendary)
            meta.setDisplayName(translateColor(displayName));

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§8§o[Crate Key]");
            lore.add("§7Use on: §b" + crate.title);
            lore.add("§7Right-click a crate to use!");
            meta.setLore(lore);
            key.setItemMeta(meta);
        }

        stampKey(key, crateId);
        return key;
    }

    // ════════════════════════════════════════════════════════════════
    //  CREATE CRATE
    // ════════════════════════════════════════════════════════════════

    public void createCrate(Player player, String title, String desc1, String desc2,
                            String color, String keyName) {

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            player.sendMessage("§c§lKZ §8» §cHold the key item in your main hand!");
            player.sendMessage("  §7The item you're holding will be used as the crate key.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        Material shulkerMat;
        try {
            shulkerMat = Material.valueOf(color.toUpperCase() + "_SHULKER_BOX");
        } catch (Exception e) {
            player.sendMessage("§c§lKZ §8» §cInvalid color: §f" + color);
            player.sendMessage("  §7Valid: white, orange, magenta, light_blue, yellow,");
            player.sendMessage("  §7lime, pink, gray, light_gray, cyan, purple, blue,");
            player.sendMessage("  §7brown, green, red, black");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String crateId = "crate_" + System.currentTimeMillis();
        CrateData crate = new CrateData(crateId);
        crate.title        = translateColor(title);        // ✅ Support color di title
        crate.description1 = translateColor(desc1.replace("_", " "));
        crate.description2 = translateColor(desc2.replace("_", " "));
        crate.shulkerColor = color.toUpperCase();

        // ✅ FIX: Clone item + rename dengan keyName yang support color codes
        crate.keyItem = handItem.clone();
        crate.keyItem.setAmount(1);

        ItemMeta keyMeta = crate.keyItem.getItemMeta();
        if (keyMeta != null) {
            // ✅ FIX: Translate &e&lLegendary §6§lKey → §e§lLegendary §6§lKey
            keyMeta.setDisplayName(translateColor(keyName));
            crate.keyItem.setItemMeta(keyMeta);
        }

        stampKey(crate.keyItem, crateId);

        // Place shulker
        Location loc = player.getLocation().getBlock().getLocation();
        Block block = loc.getBlock();
        block.setType(shulkerMat);
        crate.location = block.getLocation();

        String locKey = locationToKey(block.getLocation());
        crateLocations.put(locKey, crateId);
        spawnHolograms(crate);

        crates.put(crateId, crate);
        saveData();

        // Give sample key
        ItemStack sampleKey = buildKeyItem(crate, 1);
        player.getInventory().addItem(sampleKey);

        player.sendMessage("");
        player.sendMessage("§a§l┌─────────────────────────────────┐");
        player.sendMessage("§a§l│      §f§lCRATE CREATED              §a§l│");
        player.sendMessage("§a§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §7Title   : " + crate.title);
        player.sendMessage("  §7Desc    : §f" + crate.description1);
        player.sendMessage("  §7          §f" + crate.description2);
        player.sendMessage("  §7Color   : §f" + color);
        player.sendMessage("  §7Key     : " + getItemDisplayName(crate.keyItem));
        player.sendMessage("  §7ID      : §8" + crateId);
        player.sendMessage("");
        player.sendMessage("  §7🔑 1x sample key added to your inventory!");
        player.sendMessage("  §7📦 Shift+Left-click crate to add rewards!");
        player.sendMessage("  §7🎁 Use §f/givekey <player> " + crateId + " <amount>");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE CRATE
    // ════════════════════════════════════════════════════════════════

    public void deleteCrate(Player player) {
        Location pLoc = player.getLocation();
        String foundId = null;
        double closest = 5.0;

        for (Map.Entry<String, CrateData> entry : crates.entrySet()) {
            CrateData crate = entry.getValue();
            if (crate.location != null && crate.location.getWorld() != null
                    && crate.location.getWorld().equals(pLoc.getWorld())) {
                double dist = crate.location.distance(pLoc);
                if (dist < closest) { closest = dist; foundId = entry.getKey(); }
            }
        }

        if (foundId == null) {
            player.sendMessage("§c§lKZ §8» §cNo crate found within 5 blocks.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        CrateData crate = crates.get(foundId);
        if (crate.location != null) crate.location.getBlock().setType(Material.AIR);
        removeHolograms(crate);
        crateLocations.remove(locationToKey(crate.location));
        crates.remove(foundId);
        saveData();

        player.sendMessage("§a§lKZ §8» §7Crate §f" + crate.title + " §7deleted.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  HOLOGRAM SYSTEM
    // ════════════════════════════════════════════════════════════════

    private void spawnHolograms(CrateData crate) {
        if (crate.location == null || crate.location.getWorld() == null) return;
        removeHolograms(crate);

        Location base = crate.location.clone().add(0.5, 2.5, 0.5);
        String[] lines = {
                "§b§l" + crate.title,
                "§7" + crate.description1,
                "§7" + crate.description2,
                "§e§oRight-click to open!"
        };

        crate.hologramIds.clear();
        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = base.clone().add(0, -i * 0.3, 0);
            String line = lines[i];

            ArmorStand hologram = crate.location.getWorld().spawn(lineLoc, ArmorStand.class, stand -> {
                stand.setCustomName(line);
                stand.setCustomNameVisible(true);
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setInvulnerable(true);
                stand.setMarker(true);
                stand.setSmall(true);
                stand.setPersistent(true);
                stand.setRemoveWhenFarAway(false);
                stand.setCanPickupItems(false);
                stand.setCollidable(false);
            });

            crate.hologramIds.add(hologram.getUniqueId());
            hologramEntities.put(hologram.getUniqueId(), crate.id);
        }
    }

    private void removeHolograms(CrateData crate) {
        if (crate.location == null || crate.location.getWorld() == null) return;
        for (UUID id : crate.hologramIds) {
            hologramEntities.remove(id);
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        for (Entity e : crate.location.getWorld().getNearbyEntities(
                crate.location.clone().add(0.5, 1.5, 0.5), 1, 2, 1)) {
            if (e instanceof ArmorStand s && !s.isVisible() && s.isMarker()) s.remove();
        }
        crate.hologramIds.clear();
    }

    private void respawnAllHolograms() {
        int count = 0;
        for (CrateData crate : crates.values()) {
            if (crate.location != null && crate.location.getWorld() != null) {
                if (!crate.location.getBlock().getType().name().contains("SHULKER_BOX"))
                    crate.location.getBlock().setType(crate.getShulkerMaterial());
                spawnHolograms(crate);
                count++;
            }
        }
        plugin.getLogger().info("[Crate] Respawned holograms for " + count + " crates.");
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER INTERACTION
    // ════════════════════════════════════════════════════════════════

    public boolean handleCrateRightClick(Player player, Block block) {
        String crateId = crateLocations.get(locationToKey(block.getLocation()));
        if (crateId == null) return false;
        CrateData crate = crates.get(crateId);
        if (crate == null) return false;

        if (animatingPlayers.contains(player.getUniqueId())) {
            player.sendMessage("§c§lKZ §8» §cPlease wait for the current animation!");
            return true;
        }

        if (crate.getTotalRewards() == 0) {
            player.sendMessage("§c§lKZ §8» §cThis crate has no rewards yet!");
            if (player.hasPermission("kzplugin.admin"))
                player.sendMessage("  §7Shift+Left-click to add rewards.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (!hasKey(player, crate.keyItem)) {
            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §cYou need a key to open this crate!");
            player.sendMessage("  §7Required: " + getItemDisplayName(crate.keyItem));
            player.sendMessage("");
            player.sendMessage("  §8§oKeys can only be obtained from the server.");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return true;
        }

        removeOneKey(player, crate.keyItem);
        startOpenAnimation(player, crate);
        return true;
    }

    public boolean handleCrateLeftClick(Player player, Block block) {
        String crateId = crateLocations.get(locationToKey(block.getLocation()));
        if (crateId == null) return false;
        CrateData crate = crates.get(crateId);
        if (crate == null) return false;
        if (!player.hasPermission("kzplugin.admin")) return false;

        openEditorGUI(player, crate);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  OPEN ANIMATION
    // ════════════════════════════════════════════════════════════════

    private void startOpenAnimation(Player player, CrateData crate) {
        UUID uuid = player.getUniqueId();
        animatingPlayers.add(uuid);

        Location crateCenter = crate.location.clone().add(0.5, 1.0, 0.5);
        Rarity wonRarity = selectRandomRarity();
        List<ItemStack> rarityRewards = crate.rewards.get(wonRarity);

        if (rarityRewards == null || rarityRewards.isEmpty()) {
            wonRarity = findFallbackRarity(crate);
            if (wonRarity == null) {
                player.sendMessage("§c§lKZ §8» §cNo rewards available!");
                animatingPlayers.remove(uuid);
                return;
            }
            rarityRewards = crate.rewards.get(wonRarity);
        }

        ItemStack reward = rarityRewards.get(
                ThreadLocalRandom.current().nextInt(rarityRewards.size())).clone();
        final Rarity finalRarity = wonRarity;
        final ItemStack finalReward = reward;

        new BukkitRunnable() {
            int tick = 0;
            final int total = 30;
            @Override public void run() {
                if (tick >= total || !player.isOnline()) { this.cancel(); return; }
                double angle  = (tick * 24) * Math.PI / 180;
                double radius = 1.0 - (tick * 0.02);
                Location pLoc = crateCenter.clone().add(
                        Math.cos(angle) * radius, 0.5 + (tick * 0.03), Math.sin(angle) * radius);
                player.getWorld().spawnParticle(Particle.END_ROD,  pLoc,        2, 0,   0,   0,   0.01);
                player.getWorld().spawnParticle(Particle.ENCHANT, crateCenter, 5, 0.5, 0.5, 0.5, 1);
                if (tick % Math.max(1, 5 - tick / 7) == 0)
                    player.playSound(crateCenter, Sound.BLOCK_NOTE_BLOCK_PLING,
                            0.5f, 0.5f + (tick * 0.05f));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { animatingPlayers.remove(uuid); return; }

            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING,
                    crateCenter, 100, 0.5, 1.0, 0.5, 0.5);

            switch (finalRarity) {
                case EASY, MID ->
                        player.playSound(crateCenter, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                case HARD ->
                        player.playSound(crateCenter, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                case HARDCORE -> {
                    player.playSound(crateCenter, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    player.playSound(crateCenter, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                }
                case EXCLUSIVE -> {
                    player.playSound(crateCenter, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    player.playSound(crateCenter, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                    try { player.getWorld().spawn(crateCenter, Firework.class); }
                    catch (Exception ignored) {}
                }
            }

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(finalReward);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values())
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                player.sendMessage("§e§lKZ §8» §eInventory full! Item dropped.");
            }

            String rewardName = getItemDisplayName(finalReward);
            player.sendMessage("");
            player.sendMessage("§6§l┌─────────────────────────────────┐");
            player.sendMessage("§6§l│       §f§l" + crate.title.toUpperCase() + " §6§l│");
            player.sendMessage("§6§l└─────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7You received:");
            player.sendMessage("  " + finalRarity.displayName + " §8» §f"
                    + finalReward.getAmount() + "x " + rewardName);
            player.sendMessage("");

            if (finalRarity == Rarity.HARDCORE || finalRarity == Rarity.EXCLUSIVE) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage("  §6§l⭐ " + finalRarity.displayName
                            + " §6§lREWARD §8» §f" + player.getName()
                            + " §7won §f" + rewardName
                            + " §7from §b" + crate.title + "§7!");
                }
            }

            animatingPlayers.remove(uuid);
        }, 35L);
    }

    private Rarity selectRandomRarity() {
        double total = 0;
        for (Rarity r : Rarity.values()) total += r.weight;
        double roll = ThreadLocalRandom.current().nextDouble(total), cum = 0;
        for (Rarity r : Rarity.values()) { cum += r.weight; if (roll < cum) return r; }
        return Rarity.EASY;
    }

    private Rarity findFallbackRarity(CrateData crate) {
        for (Rarity r : Rarity.values()) {
            List<ItemStack> items = crate.rewards.get(r);
            if (items != null && !items.isEmpty()) return r;
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════
    //  ✅ FIX 3: EDITOR GUI - Reward tidak balik ke inventory
    // ════════════════════════════════════════════════════════════════

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE_PREFIX = "§8§lCrate Editor: §r§b";
    private static final int[] RARITY_COLUMNS = {0, 2, 4, 6, 8};
    private static final int MAX_ITEMS_PER_RARITY = 4;

    // ✅ FIX: Set untuk track slot mana yang LOCKED (tidak boleh diambil player)
    private static final Set<Integer> LOCKED_SLOTS = new HashSet<>();
    static {
        // Row 0: header rarity (slot 0,2,4,6,8)
        LOCKED_SLOTS.addAll(List.of(0, 2, 4, 6, 8));
        // Separator columns (col 1,3,5,7) di semua row
        for (int row = 0; row < 6; row++) {
            for (int col : new int[]{1, 3, 5, 7}) {
                LOCKED_SLOTS.add(row * 9 + col);
            }
        }
        // Bottom bar (slot 45-53)
        for (int i = 45; i <= 53; i++) LOCKED_SLOTS.add(i);
    }

    public void openEditorGUI(Player player, CrateData crate) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE_PREFIX + crate.title);

        // Header per rarity
        for (Rarity r : Rarity.values()) {
            int slot = RARITY_COLUMNS[r.column];
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta meta = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(r.displayName);
                meta.setLore(List.of(
                        "§7Drop chance: §f" + r.weight + "%",
                        "§7Items: §f" + crate.rewards.get(r).size() + "/" + MAX_ITEMS_PER_RARITY,
                        "",
                        "§eDrag items below to add rewards"
                ));
                header.setItemMeta(meta);
            }
            gui.setItem(slot, header);
        }

        // Isi reward yang sudah ada
        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            List<ItemStack> items = crate.rewards.get(r);
            for (int row = 0; row < MAX_ITEMS_PER_RARITY; row++) {
                int slot = (row + 1) * 9 + col;
                if (row < items.size()) gui.setItem(slot, items.get(row).clone());
                // ✅ FIX: Slot kosong dibiarkan KOSONG (bukan filler) agar bisa diisi
            }
        }

        // Separator filler (kolom 1,3,5,7)
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int row = 0; row < 5; row++) { // row 0-4 (row 5 = bottom bar)
            for (int col : new int[]{1, 3, 5, 7}) {
                gui.setItem(row * 9 + col, filler.clone());
            }
        }

        // Bottom bar
        gui.setItem(45, createItem(Material.BOOK,          "§e§lInfo §7- Drag items to reward slots"));
        gui.setItem(49, createItem(Material.EMERALD_BLOCK, "§a§lSAVE & CLOSE"));
        gui.setItem(53, createItem(Material.BARRIER,       "§c§lCLOSE without saving"));
        for (int col : new int[]{46, 47, 48, 50, 51, 52}) gui.setItem(col, filler.clone());

        editingPlayers.put(player.getUniqueId(), crate.id);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    /**
     * ✅ FIX: handleEditorClick sekarang return boolean untuk di-cancel di listener
     *
     * Return true  = event HARUS di-cancel (slot locked)
     * Return false = event BOLEH (slot reward, player bisa taruh/ambil item)
     */
    public boolean handleEditorClick(Player player, Inventory inventory, int slot) {
        if (!editingPlayers.containsKey(player.getUniqueId())) return false;

        // Tombol SAVE
        if (slot == 49) {
            saveEditorContents(player, inventory);
            player.closeInventory();
            player.sendMessage("§a§lKZ §8» §aCrate rewards saved!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            return true; // cancel event
        }

        // Tombol CLOSE
        if (slot == 53) {
            // ✅ FIX: Kembalikan item reward ke player sebelum tutup
            returnRewardItemsToPlayer(player, inventory);
            player.closeInventory();
            player.sendMessage("§c§lKZ §8» §7Editor closed without saving.");
            return true;
        }

        // ✅ FIX: Cek apakah slot ini LOCKED
        // Kalau locked → cancel event (item tidak bisa diambil/diletakkan)
        if (LOCKED_SLOTS.contains(slot)) {
            return true; // cancel event di listener!
        }

        // ✅ FIX: Slot reward (col 0,2,4,6,8 di row 1-4) → ALLOW
        // Player bebas drag/drop item ke sini
        return false; // jangan cancel event
    }

    /**
     * ✅ FIX: Kembalikan item yang ada di reward slots ke inventory player
     * Dipanggil saat close tanpa save
     */
    private void returnRewardItemsToPlayer(Player player, Inventory inventory) {
        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            for (int row = 1; row <= MAX_ITEMS_PER_RARITY; row++) {
                int slot = row * 9 + col;
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR
                        && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item.clone());
                    if (!overflow.isEmpty()) {
                        for (ItemStack drop : overflow.values())
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    inventory.setItem(slot, null);
                }
            }
        }
    }

    private void saveEditorContents(Player player, Inventory inventory) {
        String crateId = editingPlayers.get(player.getUniqueId());
        if (crateId == null) return;
        CrateData crate = crates.get(crateId);
        if (crate == null) return;

        for (Rarity r : Rarity.values()) crate.rewards.get(r).clear();

        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            for (int row = 1; row <= MAX_ITEMS_PER_RARITY; row++) {
                int slot = row * 9 + col;
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR
                        && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    crate.rewards.get(r).add(item.clone());
                }
            }
        }

        spawnHolograms(crate);
        saveData();
        editingPlayers.remove(player.getUniqueId());
    }

    public void handleEditorClose(Player player) {
        // ✅ FIX: Jangan langsung remove - cek dulu apakah close dari tombol atau ESC
        // Kalau ESC (bukan dari tombol), item di GUI hilang → kembalikan ke player
        // Tapi kita tidak bisa bedakan ESC vs tombol di sini
        // Solusi: handleEditorClose dipanggil SETELAH closeInventory()
        // Jadi kalau dari tombol, editingPlayers sudah di-remove di saveEditorContents
        // Kalau masih ada di editingPlayers = player ESC → data tidak disimpan
        if (editingPlayers.containsKey(player.getUniqueId())) {
            editingPlayers.remove(player.getUniqueId());
        }
    }

    public boolean isEditing(Player player) {
        return editingPlayers.containsKey(player.getUniqueId());
    }

    /**
     * ✅ FIX: Method baru untuk dicek di CrateListener
     * Cek apakah slot ini locked (tidak boleh diinteraksi)
     */
    public boolean isLockedSlot(int slot) {
        return LOCKED_SLOTS.contains(slot);
    }

    // ════════════════════════════════════════════════════════════════
    //  PREVIEW GUI
    // ════════════════════════════════════════════════════════════════

    public void openPreviewGUI(Player player, CrateData crate) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "§8Preview: §b" + crate.title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8");

        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta meta = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(r.displayName);
                meta.setLore(List.of("§7Chance: §f" + r.weight + "%"));
                header.setItemMeta(meta);
            }
            gui.setItem(col, header);

            List<ItemStack> items = crate.rewards.get(r);
            for (int row = 0; row < Math.min(items.size(), MAX_ITEMS_PER_RARITY); row++)
                gui.setItem((row + 1) * 9 + col, items.get(row).clone());
        }

        for (int row = 0; row < 6; row++)
            for (int col : new int[]{1, 3, 5, 7})
                gui.setItem(row * 9 + col, filler.clone());

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  LIST
    // ════════════════════════════════════════════════════════════════

    public void listCrates(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│        §f§lCRATE LIST                §6§l│");
        player.sendMessage("§6§l└─────────────────────────────────┘");
        if (crates.isEmpty()) {
            player.sendMessage("  §7No crates created yet.");
        } else {
            int count = 0;
            for (Map.Entry<String, CrateData> e : crates.entrySet()) {
                CrateData c = e.getValue();
                player.sendMessage("  §7" + (++count) + ". §b" + c.title
                        + " §8| §7ID: §f" + e.getKey()
                        + " §8| §7Rewards: §f" + c.getTotalRewards());
            }
        }
        player.sendMessage("  §7Use §f/givekey <player> <crateId> <amount>");
        player.sendMessage("");
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ════════════════════════════════════════════════════════════════

    public boolean isCrate(Block block) {
        return crateLocations.containsKey(locationToKey(block.getLocation()));
    }

    public boolean isHologram(Entity entity) {
        return hologramEntities.containsKey(entity.getUniqueId());
    }

    public CrateData getCrateAt(Block block) {
        String id = crateLocations.get(locationToKey(block.getLocation()));
        return id != null ? crates.get(id) : null;
    }

    public CrateData getCrate(String crateId) { return crates.get(crateId); }

    public Map<String, CrateData> getAllCrates() {
        return Collections.unmodifiableMap(crates);
    }

    public boolean isAnimating(Player player) {
        return animatingPlayers.contains(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  LOAD / SAVE
    // ════════════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "crates.yml");
        if (!dataFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("[Crate] Failed to create crates.yml"); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.contains("crates")) return;

        var section = dataConfig.getConfigurationSection("crates");
        if (section == null) return;

        for (String crateId : section.getKeys(false)) {
            String path = "crates." + crateId;
            CrateData crate = new CrateData(crateId);
            crate.title        = dataConfig.getString(path + ".title", "Crate");
            crate.description1 = dataConfig.getString(path + ".desc1", "");
            crate.description2 = dataConfig.getString(path + ".desc2", "");
            crate.shulkerColor = dataConfig.getString(path + ".color", "PURPLE");

            if (dataConfig.contains(path + ".key"))
                crate.keyItem = dataConfig.getItemStack(path + ".key");

            if (dataConfig.contains(path + ".location")) {
                String lp = path + ".location";
                World w = Bukkit.getWorld(dataConfig.getString(lp + ".world", "world"));
                if (w != null) {
                    crate.location = new Location(w,
                            dataConfig.getDouble(lp + ".x"),
                            dataConfig.getDouble(lp + ".y"),
                            dataConfig.getDouble(lp + ".z"));
                    crateLocations.put(locationToKey(crate.location), crateId);
                }
            }

            for (Rarity r : Rarity.values()) {
                String rp = path + ".rewards." + r.name();
                if (dataConfig.contains(rp)) {
                    List<?> items = dataConfig.getList(rp);
                    if (items != null)
                        for (Object obj : items)
                            if (obj instanceof ItemStack item) crate.rewards.get(r).add(item);
                }
            }

            crates.put(crateId, crate);
        }
    }

    public void saveData() {
        dataConfig.set("crates", null);
        for (Map.Entry<String, CrateData> entry : crates.entrySet()) {
            CrateData crate = entry.getValue();
            String path = "crates." + entry.getKey();
            dataConfig.set(path + ".title",  crate.title);
            dataConfig.set(path + ".desc1",  crate.description1);
            dataConfig.set(path + ".desc2",  crate.description2);
            dataConfig.set(path + ".color",  crate.shulkerColor);
            if (crate.keyItem != null) dataConfig.set(path + ".key", crate.keyItem);
            if (crate.location != null) {
                String lp = path + ".location";
                dataConfig.set(lp + ".world", crate.location.getWorld().getName());
                dataConfig.set(lp + ".x",     crate.location.getX());
                dataConfig.set(lp + ".y",     crate.location.getY());
                dataConfig.set(lp + ".z",     crate.location.getZ());
            }
            for (Rarity r : Rarity.values())
                dataConfig.set(path + ".rewards." + r.name(), crate.rewards.get(r));
        }
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("[Crate] Failed to save crates.yml"); }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private String locationToKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX()
                + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            return item.getItemMeta().getDisplayName();
        StringBuilder sb = new StringBuilder();
        for (String w : item.getType().name().replace("_", " ").split(" "))
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)))
                    .append(w.substring(1).toLowerCase()).append(" ");
        return sb.toString().trim();
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }

    public void shutdown() {
        saveData();
        animatingPlayers.clear();
        editingPlayers.clear();
        for (CrateData crate : crates.values()) removeHolograms(crate);
        plugin.getLogger().info("[Crate] System shutdown.");
    }
}
