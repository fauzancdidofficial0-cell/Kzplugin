// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/CrateSystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CrateSystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // ════════════════════════════════════════════════════════════════
    //  DATA STRUCTURES
    // ════════════════════════════════════════════════════════════════

    /**
     * Stores all crate data
     * Key = crate ID (e.g. "crate_1700000000")
     */
    private final Map<String, CrateData> crates = new LinkedHashMap<>();

    /**
     * Maps placed shulker block location → crate ID
     * Format key: "world,x,y,z"
     */
    private final Map<String, String> crateLocations = new LinkedHashMap<>();

    /**
     * Maps hologram ArmorStand UUID → crate ID
     */
    private final Map<UUID, String> hologramEntities = new HashMap<>();

    /**
     * Tracks players currently in animation (prevent double click)
     */
    private final Set<UUID> animatingPlayers = new HashSet<>();

    /**
     * Tracks players currently editing a crate (admin GUI open)
     */
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
        public final int column; // Column index in GUI (0-4)

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
        public String shulkerColor;    // e.g. "PURPLE", "BLUE", "RED"
        public ItemStack keyItem;      // The key item required to open
        public Location location;      // Block location of placed shulker
        public List<UUID> hologramIds; // ArmorStand holograms

        // Rewards per rarity: Map<Rarity, List<ItemStack>>
        public Map<Rarity, List<ItemStack>> rewards = new EnumMap<>(Rarity.class);

        public CrateData(String id) {
            this.id = id;
            this.hologramIds = new ArrayList<>();
            for (Rarity r : Rarity.values()) {
                rewards.put(r, new ArrayList<>());
            }
        }

        /**
         * Get total reward count across all rarities
         */
        public int getTotalRewards() {
            int total = 0;
            for (List<ItemStack> items : rewards.values()) {
                total += items.size();
            }
            return total;
        }

        /**
         * Get shulker Material from color string
         */
        public Material getShulkerMaterial() {
            try {
                String mat = shulkerColor.toUpperCase() + "_SHULKER_BOX";
                return Material.valueOf(mat);
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
        loadData();

        // Respawn holograms setelah server start
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnAllHolograms, 80L);

        plugin.getLogger().info("[Crate] System initialized. Loaded " + crates.size() + " crates.");
    }

    // ════════════════════════════════════════════════════════════════
    //  CREATE CRATE - /gachacreate command
    // ════════════════════════════════════════════════════════════════

    /**
     * Create a new crate at the player's location
     *
     * @param player    Admin yang menjalankan command
     * @param title     Judul crate (e.g. "Legendary Crate")
     * @param desc1     Deskripsi baris 1
     * @param desc2     Deskripsi baris 2
     * @param color     Warna shulker (e.g. "purple", "blue", "red")
     * @param keyName   Nama key yang dipakai (untuk referensi, key item = item di tangan)
     */
    public void createCrate(Player player, String title, String desc1, String desc2,
                            String color, String keyName) {

        // Validasi item di tangan sebagai key
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            player.sendMessage("§c§lKZ §8» §cHold the key item in your main hand!");
            player.sendMessage("  §7The item you're holding will be used as the crate key.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Validasi warna
        Material shulkerMat;
        try {
            shulkerMat = Material.valueOf(color.toUpperCase() + "_SHULKER_BOX");
        } catch (Exception e) {
            player.sendMessage("§c§lKZ §8» §cInvalid color: §f" + color);
            player.sendMessage("  §7Valid colors: §fwhite, orange, magenta, light_blue, yellow,");
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
        crate.keyItem = handItem.clone();
        crate.keyItem.setAmount(1); // Key selalu 1

        // Place shulker block di lokasi player (1 block di bawah pandangan)
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
        player.sendMessage("  §aNext: §fLeft-click the crate to add rewards!");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE CRATE
    // ════════════════════════════════════════════════════════════════

    /**
     * Delete crate terdekat dari player (radius 5 block)
     */
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

        // Remove shulker block
        if (crate.location != null && crate.location.getBlock() != null) {
            crate.location.getBlock().setType(Material.AIR);
        }

        // Remove holograms
        removeHolograms(crate);

        // Remove from maps
        String locKey = locationToKey(crate.location);
        crateLocations.remove(locKey);
        crates.remove(foundId);

        saveData();

        player.sendMessage("§a§lKZ §8» §7Crate §f" + crate.title + " §7has been deleted.");
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  HOLOGRAM SYSTEM - ArmorStand text above shulker
    // ════════════════════════════════════════════════════════════════

    /**
     * Spawn hologram ArmorStands di atas crate
     * Line 1: Title (paling atas)
     * Line 2: Description 1
     * Line 3: Description 2
     * Line 4: "Right-click to open"
     */
    private void spawnHolograms(CrateData crate) {
        if (crate.location == null || crate.location.getWorld() == null) return;

        // Remove old holograms dulu
        removeHolograms(crate);

        Location base = crate.location.clone().add(0.5, 2.5, 0.5); // Di atas shulker
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

    /**
     * Remove semua hologram ArmorStand milik crate ini
     */
    private void removeHolograms(CrateData crate) {
        if (crate.location == null || crate.location.getWorld() == null) return;

        // Remove by tracked UUIDs
        for (UUID holoId : crate.hologramIds) {
            hologramEntities.remove(holoId);
            Entity entity = Bukkit.getEntity(holoId);
            if (entity != null) {
                entity.remove();
            }
        }

        // Cleanup: remove any invisible ArmorStands near the crate location
        for (Entity entity : crate.location.getWorld().getNearbyEntities(
                crate.location.clone().add(0.5, 1.5, 0.5), 1, 2, 1)) {
            if (entity instanceof ArmorStand stand && !stand.isVisible() && stand.isMarker()) {
                stand.remove();
            }
        }

        crate.hologramIds.clear();
    }

    /**
     * Respawn semua holograms saat server start
     */
    private void respawnAllHolograms() {
        int count = 0;
        for (CrateData crate : crates.values()) {
            if (crate.location != null && crate.location.getWorld() != null) {
                // Verify shulker block masih ada
                Block block = crate.location.getBlock();
                if (!block.getType().name().contains("SHULKER_BOX")) {
                    // Re-place shulker
                    block.setType(crate.getShulkerMaterial());
                }

                spawnHolograms(crate);
                count++;
            }
        }
        plugin.getLogger().info("[Crate] Respawned holograms for " + count + " crates.");
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER INTERACTION - Click handler
    // ════════════════════════════════════════════════════════════════

    /**
     * Handle player right-click on a crate block
     * Called from PlayerEventListener
     *
     * @return true if the click was handled (is a crate)
     */
    public boolean handleCrateRightClick(Player player, Block block) {
        String locKey = locationToKey(block.getLocation());
        String crateId = crateLocations.get(locKey);

        if (crateId == null) return false;

        CrateData crate = crates.get(crateId);
        if (crate == null) return false;

        // Prevent double click
        if (animatingPlayers.contains(player.getUniqueId())) {
            player.sendMessage("§c§lKZ §8» §cPlease wait for the current animation to finish!");
            return true;
        }

        // Check if crate has rewards
        if (crate.getTotalRewards() == 0) {
            player.sendMessage("§c§lKZ §8» §cThis crate has no rewards yet!");
            if (player.hasPermission("kzplugin.admin")) {
                player.sendMessage("  §7Left-click the crate to add rewards.");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Check if player has the key
        ItemStack key = crate.keyItem;
        if (!hasKey(player, key)) {
            player.sendMessage("");
            player.sendMessage("§c§lKZ §8» §cYou need a key to open this crate!");
            player.sendMessage("  §7Required: §e" + getItemDisplayName(key));
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            return true;
        }

        // Consume key
        removeOneKey(player, key);

        // Start opening animation
        startOpenAnimation(player, crate);

        return true;
    }

    /**
     * Handle player left-click on a crate block (ADMIN ONLY - open editor)
     *
     * @return true if the click was handled (is a crate)
     */
    public boolean handleCrateLeftClick(Player player, Block block) {
        String locKey = locationToKey(block.getLocation());
        String crateId = crateLocations.get(locKey);

        if (crateId == null) return false;

        CrateData crate = crates.get(crateId);
        if (crate == null) return false;

        // Admin only
        if (!player.hasPermission("kzplugin.admin")) {
            return false; // Let normal left-click through
        }

        // Open editor GUI
        openEditorGUI(player, crate);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  KEY SYSTEM - Check & consume key items
    // ════════════════════════════════════════════════════════════════

    /**
     * Check apakah player punya key item di inventory
     * Matching by: Material + DisplayName + CustomModelData (if any)
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
     * Remove 1 key dari inventory player
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
     * Compare two items to check if they're the same key
     * Matches: Material, DisplayName, Lore
     */
    private boolean isMatchingKey(ItemStack item, ItemStack key) {
        if (item.getType() != key.getType()) return false;

        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta keyMeta = key.getItemMeta();

        // Both no meta = match
        if (itemMeta == null && keyMeta == null) return true;
        if (itemMeta == null || keyMeta == null) return false;

        // Compare display name
        String itemName = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : "";
        String keyName = keyMeta.hasDisplayName() ? keyMeta.getDisplayName() : "";
        if (!itemName.equals(keyName)) return false;

        // Compare lore
        List<String> itemLore = itemMeta.hasLore() ? itemMeta.getLore() : List.of();
        List<String> keyLore = keyMeta.hasLore() ? keyMeta.getLore() : List.of();
        if (!itemLore.equals(keyLore)) return false;

        // Compare custom model data (if present)
        if (itemMeta.hasCustomModelData() != keyMeta.hasCustomModelData()) return false;
        if (itemMeta.hasCustomModelData() && keyMeta.hasCustomModelData()) {
            if (itemMeta.getCustomModelData() != keyMeta.getCustomModelData()) return false;
        }

        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  OPEN ANIMATION - Particles, sounds, rolling reward
    // ════════════════════════════════════════════════════════════════

    /**
     * Start the crate opening animation
     * 1. Particle swirl (1.5 seconds)
     * 2. Rolling sound (rapid clicks)
     * 3. Reward selection
     * 4. Reward announcement
     */
    private void startOpenAnimation(Player player, CrateData crate) {
        UUID uuid = player.getUniqueId();
        animatingPlayers.add(uuid);

        Location crateCenter = crate.location.clone().add(0.5, 1.0, 0.5);

        // Select reward FIRST (so animation leads to it)
        Rarity wonRarity = selectRandomRarity();
        List<ItemStack> rarityRewards = crate.rewards.get(wonRarity);

        // Fallback jika rarity kosong, cari rarity lain yang punya reward
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

        // ── Phase 1: Particle Swirl (0-30 ticks = 1.5 sec) ──
        new BukkitRunnable() {
            int tick = 0;
            final int totalTicks = 30;

            @Override
            public void run() {
                if (tick >= totalTicks || !player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Swirling particles
                double angle = (tick * 24) * Math.PI / 180;
                double radius = 1.0 - (tick * 0.02);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location particleLoc = crateCenter.clone().add(x, 0.5 + (tick * 0.03), z);

                player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 2, 0, 0, 0, 0.01);
                player.getWorld().spawnParticle(Particle.ENCHANT, crateCenter, 5, 0.5, 0.5, 0.5, 1);

                // Accelerating click sound
                if (tick % Math.max(1, 5 - tick / 7) == 0) {
                    float pitch = 0.5f + (tick * 0.05f);
                    player.playSound(crateCenter, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, pitch);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // ── Phase 2: Reveal reward (after animation) ──
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                animatingPlayers.remove(uuid);
                return;
            }

            // Big reveal particles
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, crateCenter,
                    100, 0.5, 1.0, 0.5, 0.5);
            player.getWorld().spawnParticle(Particle.FLASH, crateCenter, 1, 0, 0, 0, 0);

            // Reveal sound based on rarity
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
                    // Extra firework for exclusive
                    try {
                        player.getWorld().spawn(crateCenter, Firework.class);
                    } catch (Exception ignored) {}
                }
            }

            // Give reward to player
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(finalReward);
            if (!overflow.isEmpty()) {
                // Drop di lokasi player jika inventory penuh
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage("§e§lKZ §8» §eInventory full! Item dropped on the ground.");
            }

            // Reward message to player
            String rewardName = getItemDisplayName(finalReward);
            player.sendMessage("");
            player.sendMessage("§6§l┌─────────────────────────────────┐");
            player.sendMessage("§6§l│       §f§l" + crate.title.toUpperCase()
                    + " §6§l                   │");
            player.sendMessage("§6§l└─────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7You received:");
            player.sendMessage("  " + finalRarity.displayName + " §8» §f"
                    + finalReward.getAmount() + "x " + rewardName);
            player.sendMessage("");

            // Broadcast for rare+ rewards
            if (finalRarity == Rarity.HARDCORE || finalRarity == Rarity.EXCLUSIVE) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage("");
                    online.sendMessage("  §6§l⭐ " + finalRarity.displayName
                            + " §6§lREWARD §8» §f" + player.getName()
                            + " §7won §f" + rewardName
                            + " §7from §b" + crate.title + "§7!");
                    online.sendMessage("");
                }
            }

            animatingPlayers.remove(uuid);

        }, 35L); // After particle animation
    }

    /**
     * Select random rarity based on weights
     */
    private Rarity selectRandomRarity() {
        double totalWeight = 0;
        for (Rarity r : Rarity.values()) {
            totalWeight += r.weight;
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0;

        for (Rarity r : Rarity.values()) {
            cumulative += r.weight;
            if (roll < cumulative) {
                return r;
            }
        }

        return Rarity.EASY;
    }

    /**
     * Find any rarity that has rewards (fallback)
     */
    private Rarity findFallbackRarity(CrateData crate) {
        // Try from common to rare
        for (Rarity r : Rarity.values()) {
            List<ItemStack> items = crate.rewards.get(r);
            if (items != null && !items.isEmpty()) {
                return r;
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════
    //  EDITOR GUI - Admin adds rewards by dragging items
    // ════════════════════════════════════════════════════════════════

    /*
     * GUI Layout (54 slots = 6 rows x 9 columns):
     *
     * Row 0: [Easy] [Fill] [Mid] [Fill] [Hard] [Fill] [Hardcore] [Fill] [Exclusive]
     * Row 1: [E-1]  [Fill] [M-1] [Fill] [H-1]  [Fill] [HC-1]    [Fill] [EX-1]
     * Row 2: [E-2]  [Fill] [M-2] [Fill] [H-2]  [Fill] [HC-2]    [Fill] [EX-2]
     * Row 3: [E-3]  [Fill] [M-3] [Fill] [H-3]  [Fill] [HC-3]    [Fill] [EX-3]
     * Row 4: [E-4]  [Fill] [M-4] [Fill] [H-4]  [Fill] [HC-4]    [Fill] [EX-4]
     * Row 5: [Info] [Fill] [Fill] [Fill] [SAVE] [Fill] [Fill]    [Fill] [Close]
     *
     * Column mapping:
     *   Easy=0, Mid=2, Hard=4, Hardcore=6, Exclusive=8
     *   Fill columns=1,3,5,7
     */

    private static final int GUI_SIZE = 54;
    private static final String GUI_TITLE_PREFIX = "§8§lCrate Editor: §r§b";

    // Rarity → GUI column slot
    private static final int[] RARITY_COLUMNS = {0, 2, 4, 6, 8};

    // Max items per rarity in GUI
    private static final int MAX_ITEMS_PER_RARITY = 4;

    /**
     * Open the crate editor GUI for an admin
     */
    public void openEditorGUI(Player player, CrateData crate) {
        String guiTitle = GUI_TITLE_PREFIX + crate.title;
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, guiTitle);

        // ── Row 0: Rarity headers ──
        for (Rarity r : Rarity.values()) {
            int slot = RARITY_COLUMNS[r.column];
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta meta = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(r.displayName);
                List<String> lore = new ArrayList<>();
                lore.add("§7Drop chance: §f" + r.weight + "%");
                lore.add("§7Items: §f" + crate.rewards.get(r).size() + "/" + MAX_ITEMS_PER_RARITY);
                lore.add("");
                lore.add("§eDrag items below to add rewards");
                meta.setLore(lore);
                header.setItemMeta(meta);
            }
            gui.setItem(slot, header);
        }

        // ── Row 1-4: Existing reward items ──
        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            List<ItemStack> items = crate.rewards.get(r);
            for (int row = 0; row < MAX_ITEMS_PER_RARITY; row++) {
                int slot = (row + 1) * 9 + col;
                if (row < items.size()) {
                    gui.setItem(slot, items.get(row).clone());
                }
                // Empty slots stay empty (player can place items there)
            }
        }

        // ── Fill columns (separator) ──
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName("§8");
            filler.setItemMeta(fillerMeta);
        }

        int[] fillerCols = {1, 3, 5, 7};
        for (int row = 0; row < 6; row++) {
            for (int col : fillerCols) {
                gui.setItem(row * 9 + col, filler.clone());
            }
        }

        // ── Row 5: Action buttons ──

        // Info button (slot 45)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§lCrate Info");
            List<String> lore = new ArrayList<>();
            lore.add("§7Title: §b" + crate.title);
            lore.add("§7Color: §f" + crate.shulkerColor);
            lore.add("§7Key: §e" + getItemDisplayName(crate.keyItem));
            lore.add("§7Total Rewards: §f" + crate.getTotalRewards());
            lore.add("");
            lore.add("§7Drag items into the columns");
            lore.add("§7above to add rewards.");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(45, info);

        // Save button (slot 49)
        ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName("§a§lSAVE & CLOSE");
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to save all rewards");
            lore.add("§7and close the editor.");
            saveMeta.setLore(lore);
            save.setItemMeta(saveMeta);
        }
        gui.setItem(49, save);

        // Close button (slot 53)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c§lCLOSE (No Save)");
            closeMeta.setLore(List.of("§7Close without saving changes."));
            close.setItemMeta(closeMeta);
        }
        gui.setItem(53, close);

        // Fill remaining row 5 slots
        for (int col : new int[]{46, 47, 48, 50, 51, 52}) {
            gui.setItem(col, filler.clone());
        }

        // Track that this player is editing
        editingPlayers.put(player.getUniqueId(), crate.id);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    /**
     * Handle click inside editor GUI
     * Called from InventoryClickListener
     *
     * @return true if click was handled
     */
    public boolean handleEditorClick(Player player, Inventory inventory, int slot, boolean isShiftClick) {
        if (!editingPlayers.containsKey(player.getUniqueId())) return false;

        String title = player.getOpenInventory().getTitle();
        if (!title.startsWith(GUI_TITLE_PREFIX)) return false;

        // ── SAVE button ──
        if (slot == 49) {
            saveEditorContents(player, inventory);
            player.closeInventory();
            player.sendMessage("§a§lKZ §8» §aCrate rewards saved successfully!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            return true;
        }

        // ── CLOSE button ──
        if (slot == 53) {
            player.closeInventory();
            player.sendMessage("§c§lKZ §8» §7Editor closed without saving.");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return true;
        }

        // ── Filler slots - block interaction ──
        int[] fillerCols = {1, 3, 5, 7};
        int col = slot % 9;
        for (int fc : fillerCols) {
            if (col == fc) return true; // Block click on fillers
        }

        // ── Row 0 (headers) - block interaction ──
        if (slot < 9) return true;

        // ── Row 5 (bottom) - block all except save/close ──
        if (slot >= 45) return true;

        // ── Reward slots (row 1-4, columns 0,2,4,6,8) - ALLOW interaction ──
        // Player can place/take items in these slots
        return false; // Don't cancel = allow normal inventory interaction
    }

    /**
     * Save contents dari editor GUI ke CrateData
     */
    private void saveEditorContents(Player player, Inventory inventory) {
        String crateId = editingPlayers.get(player.getUniqueId());
        if (crateId == null) return;

        CrateData crate = crates.get(crateId);
        if (crate == null) return;

        // Clear existing rewards
        for (Rarity r : Rarity.values()) {
            crate.rewards.get(r).clear();
        }

        // Read items from GUI slots
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

        // Update hologram dengan reward count
        spawnHolograms(crate);

        saveData();
        editingPlayers.remove(player.getUniqueId());
    }

    /**
     * Handle editor GUI close (cleanup)
     */
    public void handleEditorClose(Player player) {
        editingPlayers.remove(player.getUniqueId());
    }

    /**
     * Check if player is currently editing a crate
     */
    public boolean isEditing(Player player) {
        return editingPlayers.containsKey(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  CRATE PREVIEW - Player can see rewards without key
    // ════════════════════════════════════════════════════════════════

    /**
     * Open preview GUI showing all possible rewards
     * Triggered by: /gachapreview or Bedrock form
     */
    public void openPreviewGUI(Player player, CrateData crate) {
        String title = "§8Preview: §b" + crate.title;
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, title);

        // Same layout as editor, but items are display-only
        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];

            // Header
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta meta = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(r.displayName);
                meta.setLore(List.of(
                        "§7Drop chance: §f" + r.weight + "%",
                        "§7Items: §f" + crate.rewards.get(r).size()
                ));
                header.setItemMeta(meta);
            }
            gui.setItem(col, header);

            // Items
            List<ItemStack> items = crate.rewards.get(r);
            for (int row = 0; row < Math.min(items.size(), MAX_ITEMS_PER_RARITY); row++) {
                gui.setItem((row + 1) * 9 + col, items.get(row).clone());
            }
        }

        // Fillers
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName("§8");
            filler.setItemMeta(fm);
        }
        for (int row = 0; row < 6; row++) {
            for (int col : new int[]{1, 3, 5, 7}) {
                gui.setItem(row * 9 + col, filler.clone());
            }
        }

        // Bottom row info
        ItemStack keyInfo = new ItemStack(crate.keyItem.getType());
        ItemMeta keyMeta = keyInfo.getItemMeta();
        if (keyMeta != null) {
            keyMeta.setDisplayName("§e§lRequired Key");
            keyMeta.setLore(List.of(
                    "§7You need: §e" + getItemDisplayName(crate.keyItem),
                    "",
                    "§7Right-click the crate with",
                    "§7this key to open!"
            ));
            keyInfo.setItemMeta(keyMeta);
        }
        gui.setItem(49, keyInfo);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  LIST CRATES
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
                String loc = crate.location != null
                        ? "§7(" + crate.location.getBlockX() + ", "
                        + crate.location.getBlockY() + ", "
                        + crate.location.getBlockZ() + ")"
                        : "§cNo location";
                player.sendMessage("  §7" + count + ". §b" + crate.title
                        + " §8| §7Rewards: §f" + crate.getTotalRewards()
                        + " §8| " + loc);
            }
        }

        player.sendMessage("");
        player.sendMessage("  §7Total: §f" + crates.size() + " §7crates");
        player.sendMessage("");
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY - Check if block/entity is crate-related
    // ════════════════════════════════════════════════════════════════

    /**
     * Check if a block is a registered crate
     */
    public boolean isCrate(Block block) {
        return crateLocations.containsKey(locationToKey(block.getLocation()));
    }

    /**
     * Check if an entity is a crate hologram
     */
    public boolean isHologram(Entity entity) {
        return hologramEntities.containsKey(entity.getUniqueId());
    }

    /**
     * Get CrateData from block location
     */
    public CrateData getCrateAt(Block block) {
        String crateId = crateLocations.get(locationToKey(block.getLocation()));
        if (crateId == null) return null;
        return crates.get(crateId);
    }

    /**
     * Get CrateData by ID
     */
    public CrateData getCrate(String crateId) {
        return crates.get(crateId);
    }

    /**
     * Get all crates
     */
    public Map<String, CrateData> getAllCrates() {
        return Collections.unmodifiableMap(crates);
    }

    /**
     * Check if player is in animation
     */
    public boolean isAnimating(Player player) {
        return animatingPlayers.contains(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  LOAD / SAVE DATA (crates.yml)
    // ════════════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "crates.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[Crate] Failed to create crates.yml: " + e.getMessage());
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

            // Load key item
            if (dataConfig.contains(path + ".key")) {
                crate.keyItem = dataConfig.getItemStack(path + ".key");
            }

            // Load location
            if (dataConfig.contains(path + ".location")) {
                String locPath = path + ".location";
                World w = Bukkit.getWorld(dataConfig.getString(locPath + ".world", "world"));
                if (w != null) {
                    crate.location = new Location(w,
                            dataConfig.getDouble(locPath + ".x"),
                            dataConfig.getDouble(locPath + ".y"),
                            dataConfig.getDouble(locPath + ".z"));

                    // Register location mapping
                    crateLocations.put(locationToKey(crate.location), crateId);
                }
            }

            // Load rewards per rarity
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

        plugin.getLogger().info("[Crate] Loaded " + crates.size() + " crates from crates.yml");
    }

    public void saveData() {
        // Clear old data
        dataConfig.set("crates", null);

        for (Map.Entry<String, CrateData> entry : crates.entrySet()) {
            String crateId = entry.getKey();
            CrateData crate = entry.getValue();
            String path = "crates." + crateId;

            dataConfig.set(path + ".title", crate.title);
            dataConfig.set(path + ".desc1", crate.description1);
            dataConfig.set(path + ".desc2", crate.description2);
            dataConfig.set(path + ".color", crate.shulkerColor);

            // Save key item
            if (crate.keyItem != null) {
                dataConfig.set(path + ".key", crate.keyItem);
            }

            // Save location
            if (crate.location != null) {
                String locPath = path + ".location";
                dataConfig.set(locPath + ".world", crate.location.getWorld().getName());
                dataConfig.set(locPath + ".x", crate.location.getX());
                dataConfig.set(locPath + ".y", crate.location.getY());
                dataConfig.set(locPath + ".z", crate.location.getZ());
            }

            // Save rewards per rarity
            for (Rarity r : Rarity.values()) {
                List<ItemStack> items = crate.rewards.get(r);
                dataConfig.set(path + ".rewards." + r.name(), items);
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Crate] Failed to save crates.yml: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════════════════════════════

    /**
     * Convert Location to string key for HashMap
     */
    private String locationToKey(Location loc) {
        return loc.getWorld().getName() + ","
                + loc.getBlockX() + ","
                + loc.getBlockY() + ","
                + loc.getBlockZ();
    }

    /**
     * Get display name of an item (with fallback to material name)
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        // Convert DIAMOND_SWORD → Diamond Sword
        String name = item.getType().name().replace("_", " ");
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Shutdown cleanup
     */
    public void shutdown() {
        saveData();
        animatingPlayers.clear();
        editingPlayers.clear();

        // Remove all holograms
        for (CrateData crate : crates.values()) {
            removeHolograms(crate);
        }

        plugin.getLogger().info("[Crate] System shutdown complete.");
    }
}
