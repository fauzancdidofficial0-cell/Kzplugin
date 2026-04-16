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
import org.bukkit.inventory.meta.FireworkMeta;
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

    private final Map<String, CrateData>       crates          = new LinkedHashMap<>();
    private final Map<String, String>           crateLocations  = new LinkedHashMap<>();
    private final Map<UUID, String>             hologramEntities= new HashMap<>();
    private final Set<UUID>                     animatingPlayers= new HashSet<>();
    private final Map<UUID, String>             editingPlayers  = new HashMap<>();
    private final Map<UUID, Inventory>          spinningGUIs    = new HashMap<>();
    private final Map<UUID, BukkitRunnable>     spinTasks       = new HashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  RARITY
    // ════════════════════════════════════════════════════════════════

    public enum Rarity {
        EASY     ("§a§lEasy",      "§a", Material.LIME_STAINED_GLASS_PANE,    50.0, 0),
        MID      ("§e§lMid",       "§e", Material.YELLOW_STAINED_GLASS_PANE,  25.0, 1),
        HARD     ("§6§lHard",      "§6", Material.ORANGE_STAINED_GLASS_PANE,  15.0, 2),
        HARDCORE ("§c§lHardcore",  "§c", Material.RED_STAINED_GLASS_PANE,      8.0, 3),
        EXCLUSIVE("§d§lExclusive", "§d", Material.MAGENTA_STAINED_GLASS_PANE,  2.0, 4);

        public final String   displayName;
        public final String   color;
        public final Material paneMaterial;
        public final double   weight;
        public final int      column;

        Rarity(String d, String c, Material m, double w, int col) {
            displayName = d; color = c; paneMaterial = m; weight = w; column = col;
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CRATE DATA
    // ════════════════════════════════════════════════════════════════

    public static class CrateData {
        public String    id, title, description1, description2, shulkerColor;
        public ItemStack keyItem;
        public Location  location;
        public List<UUID>                hologramIds = new ArrayList<>();
        public Map<Rarity, List<ItemStack>> rewards  = new EnumMap<>(Rarity.class);

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
            try   { return Material.valueOf(shulkerColor.toUpperCase() + "_SHULKER_BOX"); }
            catch (Exception e) { return Material.PURPLE_SHULKER_BOX; }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public CrateSystem(KZPlugin plugin) {
        this.plugin  = plugin;
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
    //  COLOR TRANSLATE
    // ════════════════════════════════════════════════════════════════

    private String translateColor(String input) {
        if (input == null) return "";
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

    private ItemStack buildKeyItem(CrateData crate, int amount) {
        ItemStack key  = crate.keyItem.clone();
        key.setAmount(amount);
        ItemMeta meta  = key.getItemMeta();
        if (meta != null) {
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

    public ItemStack createKeyItem(String crateId, String displayName, Material material) {
        CrateData crate = crates.get(crateId);
        if (crate == null) return null;
        ItemStack key  = new ItemStack(material);
        ItemMeta  meta = key.getItemMeta();
        if (meta != null) {
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
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        Material shulkerMat;
        try {
            shulkerMat = Material.valueOf(color.toUpperCase() + "_SHULKER_BOX");
        } catch (Exception e) {
            player.sendMessage("§c§lKZ §8» §cInvalid color: §f" + color);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        String    crateId = "crate_" + System.currentTimeMillis();
        CrateData crate   = new CrateData(crateId);
        crate.title        = translateColor(title);
        crate.description1 = translateColor(desc1.replace("_", " "));
        crate.description2 = translateColor(desc2.replace("_", " "));
        crate.shulkerColor = color.toUpperCase();

        crate.keyItem = handItem.clone();
        crate.keyItem.setAmount(1);
        ItemMeta keyMeta = crate.keyItem.getItemMeta();
        if (keyMeta != null) {
            keyMeta.setDisplayName(translateColor(keyName));
            crate.keyItem.setItemMeta(keyMeta);
        }
        stampKey(crate.keyItem, crateId);

        Location loc   = player.getLocation().getBlock().getLocation();
        Block    block = loc.getBlock();
        block.setType(shulkerMat);
        crate.location = block.getLocation();

        String locKey = locationToKey(block.getLocation());
        crateLocations.put(locKey, crateId);
        spawnHolograms(crate);
        crates.put(crateId, crate);
        saveData();

        ItemStack sampleKey = buildKeyItem(crate, 1);
        player.getInventory().addItem(sampleKey);

        player.sendMessage("");
        player.sendMessage("§a§l┌─────────────────────────────────┐");
        player.sendMessage("§a§l│      §f§lCRATE CREATED              §a§l│");
        player.sendMessage("§a§l└─────────────────────────────────┘");
        player.sendMessage("  §7Title : " + crate.title);
        player.sendMessage("  §7Desc  : §f" + crate.description1);
        player.sendMessage("  §7        §f" + crate.description2);
        player.sendMessage("  §7Color : §f" + color);
        player.sendMessage("  §7Key   : " + getItemDisplayName(crate.keyItem));
        player.sendMessage("  §7ID    : §8" + crateId);
        player.sendMessage("  §7🔑 1x sample key added!");
        player.sendMessage("  §7📦 Shift+Left-click crate to add rewards!");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE CRATE
    // ════════════════════════════════════════════════════════════════

    public void deleteCrate(Player player) {
        Location pLoc    = player.getLocation();
        String   foundId = null;
        double   closest = 5.0;

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

        Location base  = crate.location.clone().add(0.5, 2.5, 0.5);
        String[] lines = {
                "§b§l" + crate.title,
                "§7"   + crate.description1,
                "§7"   + crate.description2,
                "§e§oRight-click to open!"
        };

        crate.hologramIds.clear();
        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = base.clone().add(0, -i * 0.3, 0);
            String   line    = lines[i];
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
        String    crateId = crateLocations.get(locationToKey(block.getLocation()));
        if (crateId == null) return false;
        CrateData crate   = crates.get(crateId);
        if (crate == null)   return false;

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
        String    crateId = crateLocations.get(locationToKey(block.getLocation()));
        if (crateId == null) return false;
        CrateData crate   = crates.get(crateId);
        if (crate == null)   return false;
        if (!player.hasPermission("kzplugin.admin")) return false;
        openEditorGUI(player, crate);
        return true;
    }

    // ════════════════════════════════════════════════════════════════
    //  SPINNING GUI ANIMATION
    // ════════════════════════════════════════════════════════════════

    private static final int    SPIN_GUI_SIZE  = 27;
    private static final String SPIN_GUI_TITLE = "§8§l✦ §6§lGACHA §8§l✦";

    private void startOpenAnimation(Player player, CrateData crate) {
        UUID uuid = player.getUniqueId();
        animatingPlayers.add(uuid);

        // Tentukan reward sebelum animasi
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

        final ItemStack finalReward = rarityRewards
                .get(ThreadLocalRandom.current().nextInt(rarityRewards.size())).clone();
        final Rarity finalRarity = wonRarity;

        // Buka GUI spin
        Inventory spinGUI = Bukkit.createInventory(null, SPIN_GUI_SIZE,
                SPIN_GUI_TITLE + " §8| §7" + crate.title);
        buildSpinFrame(spinGUI, null, null);
        spinningGUIs.put(uuid, spinGUI);
        player.openInventory(spinGUI);

        // Jalankan animasi
        runSpinAnimation(player, spinGUI, crate, finalReward, finalRarity);
    }

    /**
     * Build frame GUI
     *
     * Layout (3 baris x 9):
     * Row 0 (slot  0- 8): border atas
     * Row 1 (slot  9-17): area spin
     * Row 2 (slot 18-26): border bawah
     *
     * Slot 13 (row 1 col 4) = tengah = hasil
     */
    private void buildSpinFrame(Inventory gui, ItemStack centerItem, Rarity rarity) {
        // Border atas & bawah
        ItemStack border = createGlassBorder(rarity);
        for (int i = 0;  i < 9;  i++) gui.setItem(i,      border.clone());
        for (int i = 18; i < 27; i++) gui.setItem(i,      border.clone());

        // Baris tengah
        for (int col = 0; col < 9; col++) {
            int slot = 9 + col;
            if (col == 4) {
                // Slot tengah
                gui.setItem(slot, centerItem != null ? centerItem : createQuestionMark());
            } else {
                gui.setItem(slot, createRandomColorGlass());
            }
        }
    }

    private ItemStack createGlassBorder(Rarity rarity) {
        Material mat = (rarity != null)
                ? rarity.paneMaterial
                : Material.WHITE_STAINED_GLASS_PANE;
        return createItem(mat, "§r");
    }

    private ItemStack createQuestionMark() {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l?  ?  ?");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRandomColorGlass() {
        Material[] glasses = {
                Material.RED_STAINED_GLASS_PANE,
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.LIME_STAINED_GLASS_PANE,
                Material.CYAN_STAINED_GLASS_PANE,
                Material.BLUE_STAINED_GLASS_PANE,
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.MAGENTA_STAINED_GLASS_PANE,
                Material.PINK_STAINED_GLASS_PANE,
                Material.WHITE_STAINED_GLASS_PANE,
        };
        Material mat = glasses[ThreadLocalRandom.current().nextInt(glasses.length)];
        return createItem(mat, "§r");
    }

    /**
     * Animasi spin:
     * Phase 0-2 : Cepat (interval 1t)  - glass random semua
     * Phase 3-4 : Mulai lambat (2-3t)  - masih random
     * Phase 5   : Lambat (4t)          - tengah mulai kedip reward
     * Phase 6-7 : Sangat lambat (5-6t) - reward fix di tengah
     * Reveal    : Border ganti rarity  - dekorasi muncul
     */
    private void runSpinAnimation(Player player, Inventory gui,
                                   CrateData crate,
                                   ItemStack finalReward, Rarity finalRarity) {
        UUID uuid = player.getUniqueId();

        // interval (tick antar update), phaseDuration (berapa update per phase)
        int[] interval      = {1, 1, 1, 2, 3, 4, 5, 6};
        int[] phaseDuration = {15,12,10, 8, 6, 5, 4, 4};

        BukkitRunnable task = new BukkitRunnable() {
            int phase     = 0;
            int stepCount = 0; // update ke-berapa di phase ini
            int waitTick  = 0; // counter buat interval

            @Override
            public void run() {
                if (!player.isOnline()
                        || !spinGUI(player).equals(gui)) {
                    cleanupSpin(uuid);
                    this.cancel();
                    return;
                }

                // Tunggu interval
                waitTick++;
                if (waitTick < interval[phase]) return;
                waitTick = 0;

                // Phase selesai → naik
                if (stepCount >= phaseDuration[phase]) {
                    phase++;
                    stepCount = 0;

                    if (phase >= interval.length) {
                        // Selesai → reveal
                        this.cancel();
                        revealResult(player, gui, crate, finalReward, finalRarity);
                        return;
                    }
                }

                // Update frame
                updateFrame(gui, phase, finalReward, finalRarity, stepCount);

                // Sound (makin lambat makin rendah pitch)
                float pitch = Math.max(0.5f, 2.0f - (phase * 0.2f));
                player.playSound(player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, pitch);

                stepCount++;
            }
        };

        task.runTaskTimer(plugin, 2L, 1L);
        spinTasks.put(uuid, task);
    }

    private void updateFrame(Inventory gui, int phase,
                              ItemStack finalReward, Rarity finalRarity,
                              int stepCount) {
        boolean showReward = (phase >= 6);
        boolean blinking   = (phase == 5);

        // Border: warna rarity kalau phase akhir
        Rarity borderRarity = (phase >= 5) ? finalRarity : null;
        ItemStack border = createGlassBorder(borderRarity);
        for (int i = 0;  i < 9;  i++) gui.setItem(i,  border.clone());
        for (int i = 18; i < 27; i++) gui.setItem(i,  border.clone());

        // Baris tengah
        for (int col = 0; col < 9; col++) {
            int slot = 9 + col;
            if (col == 4) {
                // Tengah
                if (showReward) {
                    gui.setItem(slot, finalReward.clone());
                } else if (blinking) {
                    // Kedip antara reward dan placeholder
                    gui.setItem(slot, (stepCount % 2 == 0)
                            ? finalReward.clone()
                            : createQuestionMark());
                } else {
                    gui.setItem(slot, createQuestionMark());
                }
            } else {
                gui.setItem(slot, createRandomColorGlass());
            }
        }
    }

    /**
     * Reveal final: dekorasi + efek + kasih reward
     */
    private void revealResult(Player player, Inventory gui,
                               CrateData crate,
                               ItemStack finalReward, Rarity finalRarity) {
        UUID uuid = player.getUniqueId();
        if (!player.isOnline()) { cleanupSpin(uuid); return; }

        // Update GUI: border rarity + dekorasi
        ItemStack border = createGlassBorder(finalRarity);
        for (int i = 0;  i < 9;  i++) gui.setItem(i,  border.clone());
        for (int i = 18; i < 27; i++) gui.setItem(i,  border.clone());

        // Tengah = reward
        gui.setItem(13, finalReward.clone());

        // Slot 9,10,11 → arrow kiri
        ItemStack left = createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l◀◀◀");
        gui.setItem(9,  left.clone());
        gui.setItem(10, left.clone());
        gui.setItem(11, left.clone());

        // Slot 15,16,17 → arrow kanan
        ItemStack right = createItem(Material.YELLOW_STAINED_GLASS_PANE, "§e§l▶▶▶");
        gui.setItem(15, right.clone());
        gui.setItem(16, right.clone());
        gui.setItem(17, right.clone());

        // Slot 12 → rarity badge
        ItemStack badge = new ItemStack(finalRarity.paneMaterial);
        ItemMeta  bMeta = badge.getItemMeta();
        if (bMeta != null) {
            bMeta.setDisplayName(finalRarity.displayName);
            bMeta.setLore(List.of("§7Chance: §f" + finalRarity.weight + "%"));
            badge.setItemMeta(bMeta);
        }
        gui.setItem(12, badge);

        // Slot 14 → info
        String rewardName = getItemDisplayName(finalReward);
        ItemStack info = createItemWithLore(Material.PAPER,
                "§f§l" + rewardName,
                List.of(
                    "§7Rarity : " + finalRarity.displayName,
                    "§7Amount : §f" + finalReward.getAmount(),
                    "",
                    "§a§lCongrats! 🎉"
                ));
        gui.setItem(14, info);

        // Sound & efek
        Location loc = player.getLocation();
        switch (finalRarity) {
            case EASY ->
                    player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            case MID ->
                    player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            case HARD -> {
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.8f);
                player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
            }
            case HARDCORE -> {
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                spawnFirework(loc);
            }
            case EXCLUSIVE -> {
                player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                player.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                spawnFirework(loc);
                spawnFirework(loc.clone().add( 1, 0,  0));
                spawnFirework(loc.clone().add(-1, 0,  0));
                spawnFirework(loc.clone().add( 0, 0,  1));
                spawnFirework(loc.clone().add( 0, 0, -1));
            }
        }

        // Tutup GUI & kasih reward 2 detik kemudian
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { cleanupSpin(uuid); return; }

            player.closeInventory();

            // Kasih reward
            HashMap<Integer, ItemStack> overflow =
                    player.getInventory().addItem(finalReward.clone());
            if (!overflow.isEmpty()) {
                for (ItemStack drop : overflow.values())
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                player.sendMessage("§e§lKZ §8» §eInventory full! Item dropped.");
            }

            // Chat
            sendRewardMessage(player, crate, finalReward, finalRarity);

            // Broadcast kalau rare
            if (finalRarity == Rarity.HARDCORE || finalRarity == Rarity.EXCLUSIVE) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage("  §6§l⭐ " + finalRarity.displayName
                            + " §6§lREWARD §8» §f" + player.getName()
                            + " §7won §f" + rewardName
                            + " §7from §b" + crate.title + "§7!");
                }
            }

            cleanupSpin(uuid);
        }, 40L);
    }

    private void sendRewardMessage(Player player, CrateData crate,
                                    ItemStack reward, Rarity rarity) {
        String name = getItemDisplayName(reward);
        player.sendMessage("");
        player.sendMessage("§6§l┌─────────────────────────────────┐");
        player.sendMessage("§6§l│       §f§l" + crate.title.toUpperCase());
        player.sendMessage("§6§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §7You received:");
        player.sendMessage("  " + rarity.displayName
                + " §8» §f" + reward.getAmount() + "x " + name);
        player.sendMessage("");
    }

    private void spawnFirework(Location loc) {
        try {
            Firework    fw   = loc.getWorld().spawn(loc, Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .withColor(Color.GOLD, Color.YELLOW)
                    .withFade(Color.WHITE)
                    .withFlicker()
                    .build());
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        } catch (Exception ignored) {}
    }

    private void cleanupSpin(UUID uuid) {
        animatingPlayers.remove(uuid);
        spinningGUIs.remove(uuid);
        BukkitRunnable task = spinTasks.remove(uuid);
        if (task != null) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
    }

    // Helper: ambil spin GUI player (null-safe)
    private Inventory spinGUI(Player player) {
        Inventory inv = spinningGUIs.get(player.getUniqueId());
        return inv != null ? inv : Bukkit.createInventory(null, 9, "dummy");
    }

    public boolean isSpinningGUI(Player player, Inventory inv) {
        Inventory spinGUI = spinningGUIs.get(player.getUniqueId());
        return spinGUI != null && spinGUI.equals(inv);
    }

    // ════════════════════════════════════════════════════════════════
    //  RARITY SELECTION
    // ════════════════════════════════════════════════════════════════

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
    //  EDITOR GUI
    // ════════════════════════════════════════════════════════════════

    private static final int    GUI_SIZE             = 54;
    private static final String GUI_TITLE_PREFIX     = "§8§lCrate Editor: §r§b";
    private static final int[]  RARITY_COLUMNS       = {0, 2, 4, 6, 8};
    private static final int    MAX_ITEMS_PER_RARITY = 4;

    private static final Set<Integer> LOCKED_SLOTS = new HashSet<>();
    static {
        LOCKED_SLOTS.addAll(List.of(0, 2, 4, 6, 8));
        for (int row = 0; row < 6; row++)
            for (int col : new int[]{1, 3, 5, 7})
                LOCKED_SLOTS.add(row * 9 + col);
        for (int i = 45; i <= 53; i++) LOCKED_SLOTS.add(i);
    }

    public void openEditorGUI(Player player, CrateData crate) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                GUI_TITLE_PREFIX + crate.title);

        // Header per rarity
        for (Rarity r : Rarity.values()) {
            int slot = RARITY_COLUMNS[r.column];
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta  meta   = header.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(r.displayName);
                meta.setLore(List.of(
                        "§7Drop chance: §f" + r.weight + "%",
                        "§7Items: §f" + crate.rewards.get(r).size()
                                + "/" + MAX_ITEMS_PER_RARITY,
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
            }
        }

        // Separator filler
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int row = 0; row < 5; row++)
            for (int col : new int[]{1, 3, 5, 7})
                gui.setItem(row * 9 + col, filler.clone());

        // Bottom bar
        gui.setItem(45, createItem(Material.BOOK,
                "§e§lInfo §7- Drag items to reward slots"));
        gui.setItem(49, createItem(Material.EMERALD_BLOCK, "§a§lSAVE & CLOSE"));
        gui.setItem(53, createItem(Material.BARRIER,       "§c§lCLOSE without saving"));
        for (int col : new int[]{46, 47, 48, 50, 51, 52})
            gui.setItem(col, filler.clone());

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
            returnRewardItemsToPlayer(player, inventory);
            player.closeInventory();
            player.sendMessage("§c§lKZ §8» §7Editor closed without saving.");
            return true;
        }

        return LOCKED_SLOTS.contains(slot);
    }

    private void returnRewardItemsToPlayer(Player player, Inventory inventory) {
        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            for (int row = 1; row <= MAX_ITEMS_PER_RARITY; row++) {
                int slot = row * 9 + col;
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR
                        && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    HashMap<Integer, ItemStack> overflow =
                            player.getInventory().addItem(item.clone());
                    if (!overflow.isEmpty())
                        for (ItemStack drop : overflow.values())
                            player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    inventory.setItem(slot, null);
                }
            }
        }
    }

    private void saveEditorContents(Player player, Inventory inventory) {
        String    crateId = editingPlayers.get(player.getUniqueId());
        if (crateId == null) return;
        CrateData crate   = crates.get(crateId);
        if (crate == null)   return;

        for (Rarity r : Rarity.values()) crate.rewards.get(r).clear();

        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            for (int row = 1; row <= MAX_ITEMS_PER_RARITY; row++) {
                int slot = row * 9 + col;
                ItemStack item = inventory.getItem(slot);
                if (item != null && item.getType() != Material.AIR
                        && item.getType() != Material.GRAY_STAINED_GLASS_PANE)
                    crate.rewards.get(r).add(item.clone());
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

    public boolean isLockedSlot(int slot) {
        return LOCKED_SLOTS.contains(slot);
    }

    // ════════════════════════════════════════════════════════════════
    //  PREVIEW GUI
    // ════════════════════════════════════════════════════════════════

    public void openPreviewGUI(Player player, CrateData crate) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                "§8Preview: §b" + crate.title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, "§8");

        for (Rarity r : Rarity.values()) {
            int col = RARITY_COLUMNS[r.column];
            ItemStack header = new ItemStack(r.paneMaterial);
            ItemMeta  meta   = header.getItemMeta();
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
        player.sendMessage("§6§l│        §f§lCRATE LIST             §6§l│");
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
    //  UTILITY
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
            String    path  = "crates." + crateId;
            CrateData crate = new CrateData(crateId);
            crate.title        = dataConfig.getString(path + ".title",  "Crate");
            crate.description1 = dataConfig.getString(path + ".desc1",  "");
            crate.description2 = dataConfig.getString(path + ".desc2",  "");
            crate.shulkerColor = dataConfig.getString(path + ".color",  "PURPLE");

            if (dataConfig.contains(path + ".key"))
                crate.keyItem = dataConfig.getItemStack(path + ".key");

            if (dataConfig.contains(path + ".location")) {
                String lp = path + ".location";
                World w = Bukkit.getWorld(
                        dataConfig.getString(lp + ".world", "world"));
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
                            if (obj instanceof ItemStack item)
                                crate.rewards.get(r).add(item);
                }
            }

            crates.put(crateId, crate);
        }
    }

    public void saveData() {
        dataConfig.set("crates", null);
        for (Map.Entry<String, CrateData> entry : crates.entrySet()) {
            CrateData crate = entry.getValue();
            String    path  = "crates." + entry.getKey();
            dataConfig.set(path + ".title",  crate.title);
            dataConfig.set(path + ".desc1",  crate.description1);
            dataConfig.set(path + ".desc2",  crate.description2);
            dataConfig.set(path + ".color",  crate.shulkerColor);
            if (crate.keyItem  != null) dataConfig.set(path + ".key", crate.keyItem);
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
        catch (IOException e) {
            plugin.getLogger().severe("[Crate] Failed to save crates.yml");
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════

    private String locationToKey(Location loc) {
        return loc.getWorld().getName() + ","
                + loc.getBlockX() + ","
                + loc.getBlockY() + ","
                + loc.getBlockZ();
    }

    private String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            return item.getItemMeta().getDisplayName();
        StringBuilder sb = new StringBuilder();
        for (String w : item.getType().name().replace("_", " ").split(" "))
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase()).append(" ");
        return sb.toString().trim();
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack createItemWithLore(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void shutdown() {
        saveData();
        animatingPlayers.clear();
        editingPlayers.clear();
        spinTasks.forEach((uuid, task) -> {
            try { task.cancel(); } catch (Exception ignored) {}
        });
        spinTasks.clear();
        spinningGUIs.clear();
        for (CrateData crate : crates.values()) removeHolograms(crate);
        plugin.getLogger().info("[Crate] System shutdown.");
    }
}
