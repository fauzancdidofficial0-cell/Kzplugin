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

    // ════════════════════════════════════════════════════════════════
    //  NAMESPACED KEYS - Anti-forge hidden tags
    // ════════════════════════════════════════════════════════════════

    private final NamespacedKey KEY_TAG;
    private final NamespacedKey KEY_CRATE_ID;

    // ════════════════════════════════════════════════════════════════
    //  DATA STRUCTURES
    // ════════════════════════════════════════════════════════════════

    private final Map<String, CrateData> crates = new LinkedHashMap<>();
    private final Map<String, String> crateLocations = new LinkedHashMap<>();
    private final Map<UUID, String> hologramEntities = new HashMap<>();
    private final Set<UUID> animatingPlayers = new HashSet<>();
    private final Map<UUID, String> editingPlayers = new HashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  RARITY SYSTEM
    // ════════════════════════════════════════════════════════════════

    public enum Rarity {
        EASY("§a§lEasy", "§a", Material.LIME_STAINED_GLASS_PANE, 50.0, 0),
        MID("§e§lMid", "§e", Material.YELLOW_STAINED_GLASS_PANE, 25.0, 1),
        HARD("§6§lHard", "§6", Material.ORANGE_STAINED_GLASS_PANE, 15.0, 2),
        HARDCORE("§c§lHardcore", "§c", Material.RED_STAINED_GLASS_PANE, 8.0, 3),
        EXCLUSIVE("§d§lExclusive", "§d", Material.MAGENTA_STAINED_GLASS_PANE, 2.0, 4);

        public final String displayName;
        public final String color;
        public final Material paneMaterial;
        public final double weight;
        public final int column;

        Rarity(String displayName, String color, Material paneMaterial, double weight, int column) {
            this.displayName = displayName;
            this.color = color;
            this.paneMaterial = paneMaterial;
            this.weight = weight;
            this.column = column;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CRATE DATA CLASS
    // ════════════════════════════════════════════════════════════════

    public static class CrateData {
        public String id;
        public String title;
        public String description1;
        public String description2;
        public String shulkerColor;
        public ItemStack keyItem;
        public Location location;
        public List<UUID> hologramIds;
        public Map<Rarity, List<ItemStack>> rewards = new EnumMap<>(Rarity.class);

        public CrateData(String id) {
            this.id = id;
            this.hologramIds = new ArrayList<>();
            for (Rarity r : Rarity.values()) {
                rewards.put(r, new ArrayList<>());
            }
        }

        public int getTotalRewards() {
            int total = 0;
            for (List<ItemStack> items : rewards.values()) {
                total += items.size();
            }
            return total;
        }

        public Material getShulkerMaterial() {
            try {
                return Material.valueOf(shulkerColor.toUpperCase() + "_SHULKER_BOX");
            } catch (Exception e) {
                return Material.PURPLE_SHULKER_BOX;
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public CrateSystem(KZPlugin plugin) {
        this.plugin = plugin;

        // Initialize NamespacedKeys
        KEY_TAG = new NamespacedKey(plugin, "crate_key");
        KEY_CRATE_ID = new NamespacedKey(plugin, "crate_id");

        loadData();

        Bukkit.getScheduler().runTaskLater(plugin, this::respawnAllHolograms, 80L);

        plugin.getLogger().info("[Crate] System initialized. Loaded " + crates.size() + " crates.");
    }

    // ════════════════════════════════════════════════════════════════
    //  KEY SYSTEM - Anti-forge with PersistentDataContainer
    // ════════════════════════════════════════════════════════════════

    /**
     * Stamp a key item with hidden anti-forge tags.
     * These tags CANNOT be replicated via anvil, crafting, or any vanilla method.
     *
     * @param item    The key ItemStack to stamp
     * @param crateId The crate ID this key belongs to
     * @return The stamped ItemStack
     */
    public ItemStack stampKey(ItemStack item, String crateId) {
        if (item == null || item.getType() == Material.AIR) return item;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_TAG, PersistentDataType.BOOLEAN, true);
        pdc.set(KEY_CRATE_ID, PersistentDataType.STRING, crateId);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Check if an item is a VALID stamped crate key (not anvil-forged).
     *
     * Validation:
     * 1. Material must match
     * 2. Must have hidden PersistentData tag (kz_crate_key = true)
     * 3. Display name must match
     *
     * Anvil-renamed items will FAIL step 2 because PersistentData
     * is NOT copied when renaming in anvil.
     */
    private boolean isMatchingKey(ItemStack item, ItemStack key) {
        if (item == null || key == null) return false;
        if (item.getType() != key.getType()) return false;

        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta keyMeta = key.getItemMeta();
        if (itemMeta == null || keyMeta == null) return false;

        // ── ANTI-FORGE CHECK: Must have hidden PersistentData tag ──
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();

        if (!pdc.has(KEY_TAG, PersistentDataType.BOOLEAN)) {
            return false; // No hidden tag = anvil fake → REJECT
        }

        Boolean isKey = pdc.get(KEY_TAG, PersistentDataType.BOOLEAN);
        if (isKey == null || !isKey) {
            return false; // Tag exists but false → REJECT
        }

        // ── Display name check ──
        String itemName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : "";
        String keyName = keyMeta.hasDisplayName() ? keyMeta.getDisplayName() : "";
        if (!itemName.equals(keyName)) return false;

        return true;
    }

    /**
     * Check if an item is ANY valid crate key (for anvil/craft blocking)
     */
    public boolean isAnyCrateKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(KEY_TAG, PersistentDataType.BOOLEAN);
    }

    /**
     * Check if player has a valid key in inventory
     */
    private boolean hasKey(Player player, ItemStack key) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isMatchingKey(item, key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove 1 valid key from player inventory
     */
    private void removeOneKey(Player player, ItemStack key) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && isMatchingKey(item, key)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return;
            }
        }
    }

    /**
     * Give stamped key items to a player.
     * Called by: /givekey command
     */
    public void giveKey(Player player, String crateId, int amount) {
        CrateData crate = crates.get(crateId);
        if (crate == null || crate.keyItem == null) {
            player.sendMessage("§c§lKZ §8» §cCrate not found or has no key template.");
            return;
        }

        ItemStack key = crate.keyItem.clone();
        key.setAmount(amount);
        stampKey(key, crateId);

        // Add lore hint to key
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§8§o[Crate Key]");
            lore.add("§7Use on: §b" + crate.title);
            lore.add("§7Right-click a crate to use!");
            meta.setLore(lore);
            key.setItemMeta(meta);
        }

        // Re-stamp after lore change (meta was replaced)
        stampKey(key, crateId);

        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(key);
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage("§e§lKZ §8» §eInventory full! Key dropped on the ground.");
        }

        player.sendMessage("§a§lKZ §8» §7You received §e" + amount + "x §f"
                + getItemDisplayName(crate.keyItem) + "§7!");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
    }

    /**
     * Create a key item from scratch (admin command alternative)
     * When admin doesn't have a specific item in hand
     */
    public ItemStack createKeyItem(String crateId, String displayName, Material material) {
        CrateData crate = crates.get(crateId);
        if (crate == null) return null;

        ItemStack key = new ItemStack(material);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
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
    //  CREATE CRATE - /gachacreate command
    // ════════════════════════════════════════════════════════════════

    public void createCrate(Player player, String title, String desc1, String desc2,
                            String color, String keyName) {

        // Validate item in hand as key
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            player.sendMessage("§c§lKZ §8» §cHold the key item in your main hand!");
            player.sendMessage("  §7The item you're holding will be used as the crate key.");
            player.sendMessage("  §7Tip: Use any item! The plugin adds color & hidden tags.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Validate color
        Material shulkerMat;
        try {
            shulkerMat = Material.valueOf(color.toUpperCase() + "_SHULKER_BOX");
        } catch (Exception e) {
            player.sendMessage("§c§lKZ §8» §cInvalid color: §f" + color);
            player.sendMessage("  §7Valid: §fwhite, orange, magenta, light_blue, yellow,");
            player.sendMessage("  §flime, pink, gray, light_gray, cyan, purple, blue,");
            player.sendMessage("  §fbrown, green, red, black");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Generate crate ID
        String crateId = "crate_" + System.currentTimeMillis();

        // Create crate data
        CrateData crate = new CrateData(crateId);
        crate.title = title;
        crate.description1 = desc1.replace("_", " ");
        crate.description2 = desc2.replace("_", " ");
        crate.shulkerColor = color.toUpperCase();

        // Clone hand item as key template + stamp with hidden tags
        crate.keyItem = handItem.clone();
        crate.keyItem.setAmount(1);
        stampKey(crate.keyItem, crateId);

        // Place shulker block
        Location loc = player.getLocation().getBlock().getLocation();
        Block block = loc.getBlock();
        block.setType(shulkerMat);
        crate.location = block.getLocation();

        // Register location
        String locKey = locationToKey(block.getLocation());
        crateLocations.put(locKey, crateId);

        // Spawn holograms
        spawnHolograms(crate);

        // Save
        crates.put(crateId, crate);
        saveData();

        // Give admin 1 stamped key as sample
        ItemStack sampleKey = crate.keyItem.clone();
        sampleKey.setAmount(1);

        ItemMeta sampleMeta = sampleKey.getItemMeta();
        if (sampleMeta != null) {
            List<String> lore = sampleMeta.hasLore() ? new ArrayList<>(sampleMeta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§8§o[Crate Key]");
            lore.add("§7Use on: §b" + crate.title);
            lore.add("§7Right-click a crate to use!");
            sampleMeta.setLore(lore);
            sampleKey.setItemMeta(sampleMeta);
        }
        stampKey(sampleKey, crateId);
        player.getInventory().addItem(sampleKey);

        // Feedback
        player.sendMessage("");
        player.sendMessage("§a§l┌─────────────────────────────────┐");
        player.sendMessage("§a§l│      §f§lCRATE CREATED              §a§l│");
        player.sendMessage("§a§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §7Title   : §b" + title);
        player.sendMessage("  §7Desc    : §f" + crate.description1);
        player.sendMessage("  §7          §f" + crate.description2);
        player.sendMessage("  §7Color   : §f" + color);
        player.sendMessage("  §7Key     : §e" + getItemDisplayName(crate.keyItem));
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
                if (dist < closest) {
                    closest = dist;
                    foundId = entry.getKey();
                }
            }
        }

        if (foundId == null) {
            player.sendMessage("§c§lKZ §8» §cNo crate found within 5 blocks.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        CrateData crate = crates.get(foundId);

        if (crate.location != null) {
            crate.location.getBlock().setType(Material.AIR);
        }

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
        double lineSpacing = 0.3;

        String[] lines = {
                "§b§l" + crate.title,
                "§7" + crate.description1,
                "§7" + crate.description2,
                "§e§oRight-click to open!"
        };

        crate.hologramIds.clear();

        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = base.clone().add(0, -i * lineSpacing, 0);
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

        for (UUID holoId : crate.hologramIds) {
            hologramEntities.remove(holoId);
            Entity entity = Bukkit.getEntity(holoId);
            if (entity != null) entity.remove();
        }

        for (Entity entity : crate.location.getWorld().getNearbyEntities(
                crate.location.clone().add(0.5, 1.5, 0.5), 1, 2, 1)) {
            if (entity instanceof ArmorStand stand && !stand.isVisible() && stand.isMarker()) {
                stand.remove();
            }
        }

        crate.hologramIds.clear();
    }

    private void respawnAllHolograms() {
        int count = 0;
        for (CrateData crate : crates.values()) {
            if (crate.location != null && crate.location.getWorld() != null) {
                Block block = crate.location.getBlock();
                if (!block.getType().name().contains("SHULKER_BOX")) {
                    block.setType(crate.getShulkerMaterial());
                }
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
        String locKey = locationToKey(block.getLocation());
        String crateId = crateLocations.get(locKey);
        if (crateId == null) return false;

        CrateData crate = crates.get(crateId);
        if (crate == null) return false;

        if (animatingPlayers.contains(player.getUniqueId())) {
            player.sendMessage("§c§lKZ §8» §cPlease wait for the current animation!");
            return true;
        }

        if (crate.getTotalRewards() == 0) {
            player.sendMessage("§c§lKZ §8» §cThis crate has no rewards yet!");
            if (player.hasPermission("kzplugin.admin")) {
                player.sendMessage("  §7Shift+Left-click to add rewards.");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (!hasKey(player, crate.keyItem)) {
            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §cYou need a key to open this crate!");
            player.sendMessage("  §7Required: §e" + getItemDisplayName(crate.keyItem));
            player.sendMessage("");
            player.sendMessage("  §8§oKeys can only be obtained from the server.");
            player.sendMessage("  §8§oAnvil-renamed items will NOT work.");
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return true;
        }

        removeOneKey(player, crate.keyItem);
        startOpenAnimation(player, crate);
        return true;
    }

    public boolean handleCrateLeftClick(Player player, Block block) {
        String locKey = locationToKey(block.getLocation());
        String crateId = crateLocations.get(locKey);
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
                ThreadLocalRandom.current().nextInt(rarityRewards.size())
        ).clone();

        final Rarity finalRarity = wonRarity;
        final ItemStack finalReward = reward;

        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 30;

            @Override
            public void run() {
                if (tick >= totalTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                double angle = (tick * 24) * Math.PI / 180;
                double radius = 1.0 - (tick * 0.02);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = crateCenter.clone().add(x, 0.5 + (tick * 0.03), z);

                player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0, 0, 0, 0.01);
                player.getWorld().spawnParticle(Particle.ENCHANT, crateCenter, 5, 0.5, 0.5, 0.5, 1);

                if (tick % Math.max(1, 5 - tick / 7) == 0) {
                    float pitch = 0.5f + (tick * 0.05f);
                    player.playSound(crateCenter, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                animatingPlayers.remove(uuid);
                return;
            }

            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, crateCenter,
                    100, 0.5, 1.0, 0.5, 0.5);

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
                    try { player.getWorld().spawn(crateCenter, Firework.class); } catch (Exception ignored) {}
                }
            }

            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(finalReward);
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
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
        double totalWeight = 0;
        for (Rarity r : Rarity.values()) totalWeight += r.weight;

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0;

        for (Rarity r : Rarity.values()) {
            cumulative += r.weight;
            if (roll < cumulative) return r;
        }
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
    //  EDITOR GUI
    // ════════════════════════════════════════════════════════════════

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE_PREFIX = "§8§lCrate Editor: §r§b";
    private static final int[] RARITY_COLUMNS = {0, 2, 4, 6, 8};
    private static final int MAX_ITEMS_PER_RARITY = 4;

    public void openEditorGUI(Player player, CrateData crate) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE_PREFIX + crate.title);

        for (Rarity r : Rarity.values()) {
            int slot = RARITY_COLUMNS[r.column];
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta meta = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(r.displayName);
                meta.setLore(List.of(
                        "§7Drop chance: §f" + r.weight + "%",
                        "§7Items: §f" + crate.rewards.get(r).size() + "/" + MAX_ITEMS_PER_RARITY,
                        "", "§eDrag items below to add rewards"
                ));
                header.setItemMeta(meta);
            }
            gui.setItem(slot, header);
        }

        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            List<ItemStack> items = crate.rewards.get(r);
            for (int row = 0; row < MAX_ITEMS_PER_RARITY; row++) {
                int slot = (row + 1) * 9 + col;
                if (row < items.size()) gui.setItem(slot, items.get(row).clone());
            }
        }

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int row = 0; row < 6; row++) {
            for (int col : new int[]{1, 3, 5, 7}) {
                gui.setItem(row * 9 + col, filler.clone());
            }
        }

        gui.setItem(45, createItem(Material.BOOK, "§e§lInfo"));
        gui.setItem(49, createItem(Material.EMERALD_BLOCK, "§a§lSAVE & CLOSE"));
        gui.setItem(53, createItem(Material.BARRIER, "§c§lCLOSE"));
        for (int col : new int[]{46, 47, 48, 50, 51, 52}) {
            gui.setItem(col, filler.clone());
        }

        editingPlayers.put(player.getUniqueId(), crate.id);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    public boolean handleEditorClick(Player player, Inventory inventory, int slot) {
        if (!editingPlayers.containsKey(player.getUniqueId())) return false;

        if (slot == 49) {
            saveEditorContents(player, inventory);
            player.closeInventory();
            player.sendMessage("§a§lKZ §8» §aCrate rewards saved!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            return true;
        }

        if (slot == 53) {
            player.closeInventory();
            player.sendMessage("§c§lKZ §8» §7Editor closed without saving.");
            return true;
        }

        int col = slot % 9;
        for (int fc : new int[]{1, 3, 5, 7}) {
            if (col == fc) return true;
        }

        if (slot < 9) return true;
        if (slot >= 45) return true;

        return false;
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
        editingPlayers.remove(player.getUniqueId());
    }

    public boolean isEditing(Player player) {
        return editingPlayers.containsKey(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  PREVIEW GUI
    // ════════════════════════════════════════════════════════════════

    public void openPreviewGUI(Player player, CrateData crate) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, "§8Preview: §b" + crate.title);

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
            for (int row = 0; row < Math.min(items.size(), MAX_ITEMS_PER_RARITY); row++) {
                gui.setItem((row + 1) * 9 + col, items.get(row).clone());
            }
        }

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int row = 0; row < 6; row++) {
            for (int col : new int[]{1, 3, 5, 7}) {
                gui.setItem(row * 9 + col, filler.clone());
            }
        }

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  LIST / UTILITY
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
            for (Map.Entry<String, CrateData> entry : crates.entrySet()) {
                count++;
                CrateData crate = entry.getValue();
                player.sendMessage("  §7" + count + ". §b" + crate.title
                        + " §8| §7ID: §f" + entry.getKey()
                        + " §8| §7Rewards: §f" + crate.getTotalRewards());
            }
        }

        player.sendMessage("");
        player.sendMessage("  §7Use §f/givekey <player> <crateId> <amount> §7to give keys");
        player.sendMessage("");
    }

    public boolean isCrate(Block block) {
        return crateLocations.containsKey(locationToKey(block.getLocation()));
    }

    public boolean isHologram(Entity entity) {
        return hologramEntities.containsKey(entity.getUniqueId());
    }

    public CrateData getCrateAt(Block block) {
        String crateId = crateLocations.get(locationToKey(block.getLocation()));
        return crateId != null ? crates.get(crateId) : null;
    }

    public CrateData getCrate(String crateId) {
        return crates.get(crateId);
    }

    public Map<String, CrateData> getAllCrates() {
        return Collections.unmodifiableMap(crates);
    }

    public boolean isAnimating(Player player) {
        return animatingPlayers.contains(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  LOAD / SAVE DATA
    // ════════════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "crates.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[Crate] Failed to create crates.yml");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (!dataConfig.contains("crates")) return;

        var section = dataConfig.getConfigurationSection("crates");
        if (section == null) return;

        for (String crateId : section.getKeys(false)) {
            String path = "crates." + crateId;
            CrateData crate = new CrateData(crateId);
            crate.title = dataConfig.getString(path + ".title", "Crate");
            crate.description1 = dataConfig.getString(path + ".desc1", "");
            crate.description2 = dataConfig.getString(path + ".desc2", "");
            crate.shulkerColor = dataConfig.getString(path + ".color", "PURPLE");

            if (dataConfig.contains(path + ".key")) {
                crate.keyItem = dataConfig.getItemStack(path + ".key");
            }

            if (dataConfig.contains(path + ".location")) {
                String locPath = path + ".location";
                World w = Bukkit.getWorld(dataConfig.getString(locPath + ".world", "world"));
                if (w != null) {
                    crate.location = new Location(w,
                            dataConfig.getDouble(locPath + ".x"),
                            dataConfig.getDouble(locPath + ".y"),
                            dataConfig.getDouble(locPath + ".z"));
                    crateLocations.put(locationToKey(crate.location), crateId);
                }
            }

            for (Rarity r : Rarity.values()) {
                String rPath = path + ".rewards." + r.name();
                if (dataConfig.contains(rPath)) {
                    List<?> items = dataConfig.getList(rPath);
                    if (items != null) {
                        for (Object obj : items) {
                            if (obj instanceof ItemStack item) {
                                crate.rewards.get(r).add(item);
                            }
                        }
                    }
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

            dataConfig.set(path + ".title", crate.title);
            dataConfig.set(path + ".desc1", crate.description1);
            dataConfig.set(path + ".desc2", crate.description2);
            dataConfig.set(path + ".color", crate.shulkerColor);

            if (crate.keyItem != null) dataConfig.set(path + ".key", crate.keyItem);

            if (crate.location != null) {
                String locPath = path + ".location";
                dataConfig.set(locPath + ".world", crate.location.getWorld().getName());
                dataConfig.set(locPath + ".x", crate.location.getX());
                dataConfig.set(locPath + ".y", crate.location.getY());
                dataConfig.set(locPath + ".z", crate.location.getZ());
            }

            for (Rarity r : Rarity.values()) {
                dataConfig.set(path + ".rewards." + r.name(), crate.rewards.get(r));
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Crate] Failed to save crates.yml");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private String locationToKey(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" ")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)))
                    .append(w.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
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
