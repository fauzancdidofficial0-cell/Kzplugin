// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/LobbySystem.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.utils.ServerUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LobbySystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // ════════════════════════════════════════════════════════════════
    //  DATA STORAGE
    // ════════════════════════════════════════════════════════════════

    private Location lobbySpawn;
    private final Map<String, Location> modeSpawns   = new HashMap<>();

    private final Map<UUID, String>  platforms  = new HashMap<>();
    private final Map<UUID, String>  ranks      = new HashMap<>();
    private final Map<UUID, Integer> kills      = new HashMap<>();
    private final Map<UUID, Integer> deaths     = new HashMap<>();
    private final Map<UUID, Integer> playtime   = new HashMap<>();
    private final Map<UUID, Boolean> firstJoin  = new HashMap<>();
    private final Set<UUID>          vanished   = new HashSet<>();

    // NPC data
    private final Map<String, Location> npcLocations  = new HashMap<>();
    private final Map<String, String>   npcModes      = new HashMap<>();
    private final Map<String, String>   npcNames      = new HashMap<>();
    private final Map<String, String>   npcServers    = new HashMap<>();
    private final Map<String, String>   npcSkinOwners = new HashMap<>();
    private final Map<String, UUID>     npcEntityMap  = new HashMap<>();

    // Skin cache: username → [texture, signature]
    private final Map<String, String[]> skinCache = new ConcurrentHashMap<>();

    private boolean maintenance     = false;
    private int     totalPlayers    = 0;
    private boolean clearlagRunning = false;

    private final String currentServer;

    // ════════════════════════════════════════════════════════════════
    //  RANK CONFIG
    // ════════════════════════════════════════════════════════════════

    public record RankData(
            String displayName,
            String color,
            String chatTag,
            int    maxLandSize,
            int    maxClaims,
            int    maxHomes,
            int    priority
    ) {}

    private final Map<String, RankData> rankConfig = new LinkedHashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public LobbySystem(KZPlugin plugin) {
        this.plugin        = plugin;
        this.currentServer = plugin.getConfig().getString("server-name", "lobby");
        initRankConfig();
        loadData();

        // Respawn NPC 3 detik setelah server start
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnNPCEntities, 60L);

        // Start clearlag scheduler
        startClearlagScheduler();
    }

    // ════════════════════════════════════════════════════════════════
    //  RANK INIT
    // ════════════════════════════════════════════════════════════════

    private void initRankConfig() {
        rankConfig.put("initiate", new RankData(
                "Initiate", "§7", "§8[§7Initiate§8]", 20, 1, 1, 0));
        rankConfig.put("citizen", new RankData(
                "Citizen", "§f", "§7[§fCitizen§7]", 25, 1, 1, 1));
        rankConfig.put("resident", new RankData(
                "Resident", "§a", "§a[Resident]", 30, 2, 2, 2));
        rankConfig.put("valiant", new RankData(
                "Valiant", "§e", "§e[Valiant]", 40, 3, 2, 3));
        rankConfig.put("sovereign", new RankData(
                "Sovereign", "§6", "§6[§lSovereign§6]", 55, 5, 3, 4));
        rankConfig.put("ethereal", new RankData(
                "Ethereal", "§b", "§b[§lEthereal§b]", 75, 8, 5, 5));
        rankConfig.put("luminescent", new RankData(
                "Luminescent", "§d", "§d[§l✦Luminescent§d]", 100, 12, 7, 6));
        rankConfig.put("celestial", new RankData(
                "Celestial", "§5", "§5[§l★Celestial★§5]", 150, 18, 9, 7));
        rankConfig.put("ascended", new RankData(
                "Ascended", "§c", "§c[§4§l⚡ASCENDED⚡§c]", 200, 25, 12, 8));

        plugin.getLogger().info("[Rank] Loaded " + rankConfig.size() + " ranks.");
    }

    // ════════════════════════════════════════════════════════════════
    //  CLEARLAG SCHEDULER
    // ════════════════════════════════════════════════════════════════

    private void startClearlagScheduler() {
        // Setiap 5 menit = 6000 ticks
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (clearlagRunning) return;
            clearlagRunning = true;
            runClearlagCountdown();
        }, 6000L, 6000L);
    }

    private void runClearlagCountdown() {
        for (int sec = 5; sec >= 1; sec--) {
            final int countdown = sec;
            long delayTicks = (long)(5 - countdown) * 20L;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Sound sound;
                String color;

                switch (countdown) {
                    case 5, 4 -> { sound = Sound.BLOCK_NOTE_BLOCK_PLING; color = "§e"; }
                    case 3, 2 -> { sound = Sound.BLOCK_NOTE_BLOCK_PLING; color = "§6"; }
                    default   -> { sound = Sound.BLOCK_NOTE_BLOCK_BASS;  color = "§c"; }
                }

                String msg = "§8[§c§lCLEARLAG§8] " + color + "§lGround items clearing in §f§l"
                        + countdown + color + "§l second"
                        + (countdown > 1 ? "s" : "") + "§8!";

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(msg);
                    p.playSound(p.getLocation(), sound, 1f, countdown == 1 ? 0.5f : 1f);
                }
            }, delayTicks);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            executeClearlag();
            clearlagRunning = false;
        }, 100L);
    }

    private void executeClearlag() {
        int itemCount = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item itemEntity) {
                    itemCount += itemEntity.getItemStack().getAmount();
                    entity.remove();
                }
            }
        }

        final int total = itemCount;
        String msg = "§8[§a§lCLEARLAG§8] §a§lDone! §f§l" + total
                + " §7item" + (total == 1 ? "" : "s")
                + " cleared. §8(Server optimized ✨)";

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        }

        plugin.getLogger().info("[ClearLag] Cleared " + total + " items.");
    }

    public void clearLag() {
        if (clearlagRunning) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§8[§c§lCLEARLAG§8] §cAlready in progress!");
            }
            return;
        }
        clearlagRunning = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§8[§c§lCLEARLAG§8] §eManual clearlag by admin!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
        }

        runClearlagCountdown();
    }

    // ════════════════════════════════════════════════════════════════
    //  NPC SKIN SYSTEM - Pure Reflection (No authlib import needed)
    // ════════════════════════════════════════════════════════════════

    /**
     * Fetch skin dari Mojang API secara async.
     * Hasilnya di-cache supaya tidak hit API berkali-kali.
     */
    private void fetchSkin(String username,
                           java.util.function.Consumer<String[]> callback) {
        // Cek cache dulu
        String lower = username.toLowerCase();
        if (skinCache.containsKey(lower)) {
            callback.accept(skinCache.get(lower));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Step 1: Dapat UUID dari username
                URL uuidUrl = new URL(
                        "https://api.mojang.com/users/profiles/minecraft/" + username);
                HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
                uuidConn.setConnectTimeout(5000);
                uuidConn.setReadTimeout(5000);
                uuidConn.setRequestProperty("User-Agent", "KZPlugin/2.0");

                if (uuidConn.getResponseCode() != 200) {
                    plugin.getLogger().warning("[NPC] Player not found: " + username);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                    return;
                }

                String uuidJson = new String(
                        uuidConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                // Parse UUID dari JSON tanpa library
                // Format: {"id":"<uuid>","name":"<name>"}
                String rawUUID = uuidJson.split("\"id\":\"")[1].split("\"")[0];
                String formattedUUID = rawUUID.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                );

                // Step 2: Dapat texture dari profile
                URL profileUrl = new URL(
                        "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + formattedUUID + "?unsigned=false");
                HttpURLConnection profileConn =
                        (HttpURLConnection) profileUrl.openConnection();
                profileConn.setConnectTimeout(5000);
                profileConn.setReadTimeout(5000);
                profileConn.setRequestProperty("User-Agent", "KZPlugin/2.0");

                if (profileConn.getResponseCode() != 200) {
                    plugin.getLogger().warning("[NPC] Profile fetch failed: " + username);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                    return;
                }

                String profileJson = new String(
                        profileConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                // Parse texture & signature
                String texture   = profileJson.split("\"value\":\"")[1].split("\"")[0];
                String signature = profileJson.split("\"signature\":\"")[1].split("\"")[0];

                String[] skinData = {texture, signature};
                skinCache.put(lower, skinData);

                plugin.getLogger().info("[NPC] Skin fetched for: " + username);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(skinData));

            } catch (Exception e) {
                plugin.getLogger().warning("[NPC] Skin fetch failed for "
                        + username + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    /**
     * Buat PlayerHead dengan skin menggunakan pure reflection.
     * Tidak butuh import com.mojang.authlib sama sekali.
     */
    private ItemStack createSkinHead(String texture, String signature, String displayName) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            // Buat GameProfile via reflection
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass    = Class.forName("com.mojang.authlib.properties.Property");

            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), displayName);

            // Buat Property dengan texture + signature
            Object property = propertyClass
                    .getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", texture, signature);

            // Masukkan property ke profile
            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object properties    = getProperties.invoke(profile);

            // PropertyMap extends Multimap, pakai method put(String, Object)
            properties.getClass()
                    .getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            // Inject profile ke SkullMeta via reflection
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "[NPC] Skin apply failed (reflection): " + e.getMessage());
            // Gagal apply skin tapi NPC tetap spawn (tanpa skin)
        }

        meta.setDisplayName("§b§l" + displayName);
        head.setItemMeta(meta);
        return head;
    }

    // ════════════════════════════════════════════════════════════════
    //  NPC CREATE
    // ════════════════════════════════════════════════════════════════

    public void createNPC(Player player, String mode, String displayName,
                          String targetServer, String skinOwner) {
        Location loc       = player.getLocation();
        String   configKey = "npc_" + System.currentTimeMillis();

        ArmorStand npc = spawnNPCArmorStand(loc, displayName);

        npcModes.put(configKey,     mode);
        npcNames.put(configKey,     displayName);
        npcServers.put(configKey,   targetServer);
        npcLocations.put(configKey, loc.clone());
        npcEntityMap.put(configKey, npc.getUniqueId());

        if (skinOwner != null && !skinOwner.isBlank()) {
            npcSkinOwners.put(configKey, skinOwner);
            player.sendMessage("§7Fetching skin for §b" + skinOwner + "§7...");

            fetchSkin(skinOwner, skinData -> {
                if (skinData == null) {
                    player.sendMessage("§c§lKZ §8» §cSkin fetch failed for §f"
                            + skinOwner + "§c. NPC created without skin.");
                    return;
                }
                Entity entity = Bukkit.getEntity(npc.getUniqueId());
                if (entity instanceof ArmorStand stand) {
                    stand.getEquipment().setHelmet(
                            createSkinHead(skinData[0], skinData[1], displayName));
                    player.sendMessage("§a§lKZ §8» §7Skin §b"
                            + skinOwner + " §7applied!");
                }
            });
        }

        player.sendMessage("");
        player.sendMessage("§a§l[NPC Created]");
        player.sendMessage("  §7Name   : §b" + displayName);
        player.sendMessage("  §7Mode   : §f" + mode);
        player.sendMessage("  §7Server : §e" + targetServer);
        player.sendMessage("  §7Skin   : §f"
                + (skinOwner != null && !skinOwner.isBlank() ? skinOwner : "none"));
        player.sendMessage("  §7ID     : §8" + configKey);
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        saveData();
    }

    public void createNPC(Player player, String mode,
                          String displayName, String targetServer) {
        createNPC(player, mode, displayName, targetServer, null);
    }

    public void createNPC(Player player, String mode, String displayName) {
        createNPC(player, mode, displayName, getServerForMode(mode), null);
    }

    private ArmorStand spawnNPCArmorStand(Location loc, String displayName) {
        return loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setCustomName("§b§l" + displayName);
            stand.setCustomNameVisible(true);
            stand.setGravity(false);
            stand.setVisible(true);
            stand.setInvulnerable(true);
            stand.setCanPickupItems(false);
            stand.setCollidable(false);
            stand.setMarker(true);
            stand.setPersistent(true);
            stand.setRemoveWhenFarAway(false);
            stand.setArms(false);
            stand.setBasePlate(false);
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  NPC RESPAWN
    // ════════════════════════════════════════════════════════════════

    private void respawnNPCEntities() {
        int ok = 0, fail = 0;

        for (var e : npcLocations.entrySet()) {
            String   key = e.getKey();
            Location loc = e.getValue();

            if (loc == null || loc.getWorld() == null) { fail++; continue; }

            // Hapus ArmorStand lama
            loc.getWorld().getNearbyEntities(loc, 1, 2, 1).stream()
                    .filter(en -> en instanceof ArmorStand && en.isCustomNameVisible())
                    .forEach(Entity::remove);

            String name = npcNames.getOrDefault(key, "NPC");
            try {
                ArmorStand npc = spawnNPCArmorStand(loc, name);
                npcEntityMap.put(key, npc.getUniqueId());

                // Restore skin jika ada
                String skinOwner = npcSkinOwners.get(key);
                if (skinOwner != null) {
                    fetchSkin(skinOwner, skinData -> {
                        if (skinData != null) {
                            Entity entity = Bukkit.getEntity(npc.getUniqueId());
                            if (entity instanceof ArmorStand stand) {
                                stand.getEquipment().setHelmet(
                                        createSkinHead(skinData[0], skinData[1], name));
                            }
                        }
                    });
                }
                ok++;
            } catch (Exception ex) {
                plugin.getLogger().warning(
                        "[NPC] Failed to spawn '" + name + "': " + ex.getMessage());
                fail++;
            }
        }

        if (ok > 0 || fail > 0)
            plugin.getLogger().info("[NPC] Respawned " + ok + " | Failed " + fail);
    }

    // ════════════════════════════════════════════════════════════════
    //  LOAD / SAVE (lobby.yml)
    // ════════════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "lobby.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe(
                        "[Lobby] Failed to create lobby.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Lobby spawn
        if (dataConfig.contains("lobby")) {
            World w = Bukkit.getWorld(
                    dataConfig.getString("lobby.world", "world"));
            if (w != null) {
                lobbySpawn = new Location(w,
                        dataConfig.getDouble("lobby.x"),
                        dataConfig.getDouble("lobby.y"),
                        dataConfig.getDouble("lobby.z"),
                        (float) dataConfig.getDouble("lobby.yaw"),
                        (float) dataConfig.getDouble("lobby.pitch"));
            }
        }

        // Mode spawns
        if (dataConfig.contains("spawns")) {
            var sec = dataConfig.getConfigurationSection("spawns");
            if (sec != null) {
                for (String mode : sec.getKeys(false)) {
                    World w = Bukkit.getWorld(
                            dataConfig.getString("spawns." + mode + ".world", "world"));
                    if (w != null) {
                        modeSpawns.put(mode, new Location(w,
                                dataConfig.getDouble("spawns." + mode + ".x"),
                                dataConfig.getDouble("spawns." + mode + ".y"),
                                dataConfig.getDouble("spawns." + mode + ".z")));
                    }
                }
            }
        }

        // Player stats
        if (dataConfig.contains("stats")) {
            var sec = dataConfig.getConfigurationSection("stats");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        ranks.put(uuid,
                                dataConfig.getString("stats." + key + ".rank", "initiate"));
                        kills.put(uuid,
                                dataConfig.getInt("stats." + key + ".kills", 0));
                        deaths.put(uuid,
                                dataConfig.getInt("stats." + key + ".deaths", 0));
                        playtime.put(uuid,
                                dataConfig.getInt("stats." + key + ".playtime", 0));
                        firstJoin.put(uuid,
                                dataConfig.getBoolean("stats." + key + ".joined", false));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Lobby] Invalid UUID: " + key);
                    }
                }
            }
        }

        // NPCs
        if (dataConfig.contains("npcs")) {
            var sec = dataConfig.getConfigurationSection("npcs");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    String path = "npcs." + key;
                    npcModes.put(key,
                            dataConfig.getString(path + ".mode",   "lobby"));
                    npcNames.put(key,
                            dataConfig.getString(path + ".name",   "NPC"));
                    npcServers.put(key,
                            dataConfig.getString(path + ".server", "lobby"));

                    String skin = dataConfig.getString(path + ".skin", "");
                    if (skin != null && !skin.isBlank()) {
                        npcSkinOwners.put(key, skin);
                    }

                    if (dataConfig.contains(path + ".location")) {
                        String lp = path + ".location";
                        World w   = Bukkit.getWorld(
                                dataConfig.getString(lp + ".world", "world"));
                        if (w != null) {
                            npcLocations.put(key, new Location(w,
                                    dataConfig.getDouble(lp + ".x"),
                                    dataConfig.getDouble(lp + ".y"),
                                    dataConfig.getDouble(lp + ".z"),
                                    (float) dataConfig.getDouble(lp + ".yaw",   0),
                                    (float) dataConfig.getDouble(lp + ".pitch", 0)));
                        }
                    }
                }
            }
        }

        maintenance  = dataConfig.getBoolean("maintenance",  false);
        totalPlayers = dataConfig.getInt("totalPlayers",     0);

        plugin.getLogger().info("[Lobby] Data loaded. NPCs: " + npcModes.size()
                + " | Stats: " + ranks.size() + " players");
    }

    public void saveData() {
        if (lobbySpawn != null) {
            dataConfig.set("lobby.world", lobbySpawn.getWorld().getName());
            dataConfig.set("lobby.x",     lobbySpawn.getX());
            dataConfig.set("lobby.y",     lobbySpawn.getY());
            dataConfig.set("lobby.z",     lobbySpawn.getZ());
            dataConfig.set("lobby.yaw",   lobbySpawn.getYaw());
            dataConfig.set("lobby.pitch", lobbySpawn.getPitch());
        }

        for (var e : modeSpawns.entrySet()) {
            String path = "spawns." + e.getKey();
            dataConfig.set(path + ".world", e.getValue().getWorld().getName());
            dataConfig.set(path + ".x",     e.getValue().getX());
            dataConfig.set(path + ".y",     e.getValue().getY());
            dataConfig.set(path + ".z",     e.getValue().getZ());
        }

        for (UUID uuid : ranks.keySet()) {
            String path = "stats." + uuid;
            dataConfig.set(path + ".rank",     ranks.get(uuid));
            dataConfig.set(path + ".kills",    kills.getOrDefault(uuid,    0));
            dataConfig.set(path + ".deaths",   deaths.getOrDefault(uuid,   0));
            dataConfig.set(path + ".playtime", playtime.getOrDefault(uuid, 0));
            dataConfig.set(path + ".joined",   firstJoin.getOrDefault(uuid, false));
        }

        dataConfig.set("npcs", null);
        for (var e : npcModes.entrySet()) {
            String key  = e.getKey();
            String path = "npcs." + key;
            dataConfig.set(path + ".mode",   e.getValue());
            dataConfig.set(path + ".name",   npcNames.get(key));
            dataConfig.set(path + ".server", npcServers.getOrDefault(key, "lobby"));
            dataConfig.set(path + ".skin",   npcSkinOwners.getOrDefault(key, ""));

            Location loc = npcLocations.get(key);
            if (loc != null) {
                String lp = path + ".location";
                dataConfig.set(lp + ".world", loc.getWorld().getName());
                dataConfig.set(lp + ".x",     loc.getX());
                dataConfig.set(lp + ".y",     loc.getY());
                dataConfig.set(lp + ".z",     loc.getZ());
                dataConfig.set(lp + ".yaw",   loc.getYaw());
                dataConfig.set(lp + ".pitch", loc.getPitch());
            }
        }

        dataConfig.set("maintenance",  maintenance);
        dataConfig.set("totalPlayers", totalPlayers);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Lobby] Failed to save: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  JOIN / QUIT
    // ════════════════════════════════════════════════════════════════

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (isLobbyServer() && lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        if (maintenance && !player.hasPermission("kzplugin.admin")) {
            player.kickPlayer(
                    "§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Try again later.");
            return;
        }

        platforms.put(uuid, detectPlatform(player));

        // Hide vanished admins dari player baru
        for (UUID vid : vanished) {
            Player vp = Bukkit.getPlayer(vid);
            if (vp != null && !player.hasPermission("kzplugin.admin")) {
                player.hidePlayer(plugin, vp);
            }
        }

        if (!firstJoin.getOrDefault(uuid, false)) {
            handleFirstJoin(player);
        } else {
            handleReturningPlayer(player);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateNametag(player);
            updateScoreboard(player);
        }, 5L);
    }

    private String detectPlatform(Player player) {
        try {
            Class<?> cls = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object   api = cls.getMethod("getInstance").invoke(null);
            boolean  isBedrock = (boolean) cls
                    .getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
            if (isBedrock) return "Bedrock";
        } catch (Exception ignored) {}
        String prefix = plugin.getConfig().getString("floodgate-prefix", ".");
        return player.getName().startsWith(prefix) ? "Bedrock" : "Java";
    }

    public boolean isBedrockPlayer(Player player) {
        return "Bedrock".equals(platforms.getOrDefault(player.getUniqueId(), "Java"));
    }

    private void handleFirstJoin(Player player) {
        UUID uuid = player.getUniqueId();
        firstJoin.put(uuid, true);
        ranks.put(uuid,    "initiate");
        kills.put(uuid,    0);
        deaths.put(uuid,   0);
        playtime.put(uuid, 0);
        totalPlayers++;
        saveData();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RankData rd = getRankData("initiate");

            send(player, "");
            send(player, "§b§l┌──────────────────────────────────┐");
            send(player, "§b§l│    §f§lWELCOME TO §b§lKZ SERVER!       §b§l│");
            send(player, "§b§l└──────────────────────────────────┘");
            send(player, "");
            send(player, "  §7Hey §b" + player.getName() + "§7, welcome!");
            send(player, "");
            send(player, "  §f💰 Balance  : §a" + formatCoins(1000));
            send(player, "  §f👑 Rank     : " + rd.chatTag());
            send(player, "  §f🎮 Platform : §f" + platforms.get(uuid));
            send(player, "  §f🏠 Max Land : §f" + rd.maxLandSize() + "x" + rd.maxLandSize());
            send(player, "");
            if (isBedrockPlayer(player)) {
                send(player, "  §7▸ Type §b/menu §7to pick a game mode");
            } else {
                send(player, "  §7▸ §fClick an NPC §7to pick a game mode");
            }
            send(player, "  §7▸ Type §b/daily §7for free daily rewards");
            send(player, "  §7▸ Type §b/help §7for all commands");
            send(player, "");

            player.playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            try {
                player.getWorld().spawn(player.getLocation(), Firework.class);
            } catch (Exception ignored) {}

            String bc = "  §a§l[+] §f" + player.getName()
                    + " §7joined for the first time! §a🎉";
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(bc);
                p.playSound(p.getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            }
        }, 20L);
    }

    private void handleReturningPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            RankData rd  = getRankData(getRank(uuid));
            double   bal = getBalance(player);

            send(player, "");
            send(player, "§b§l┌──────────────────────────────────┐");
            send(player, "§b§l│       §f§lWELCOME BACK!             §b§l│");
            send(player, "§b§l└──────────────────────────────────┘");
            send(player, "");
            send(player, "  §7Hey §b" + player.getName() + "§7!");
            send(player, "  §f💰 Balance  : §a" + formatCoins(bal));
            send(player, "  §f👑 Rank     : " + rd.chatTag());
            send(player, "  §f🌍 Server   : §f" + cap(currentServer));
            send(player, "  §f🎮 Platform : §f" + platforms.getOrDefault(uuid, "Java"));
            send(player, "");
            send(player, "  §7Claim your §b/daily §7rewards!");
            send(player, "");

            player.playSound(player.getLocation(),
                    Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

            String bc = "  §a§l[+] §f" + player.getName() + " §7joined.";
            for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(bc);
        }, 20L);
    }

    public void handleQuit(Player player) {
        vanished.remove(player.getUniqueId());
        String bc = "  §c§l[-] §f" + player.getName() + " §7left.";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(player)) p.sendMessage(bc);
        }
        saveData();
    }

    // ════════════════════════════════════════════════════════════════
    //  VANISH
    // ════════════════════════════════════════════════════════════════

    public boolean toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        if (vanished.contains(uuid)) {
            vanished.remove(uuid);
            return false;
        }
        vanished.add(uuid);
        return true;
    }

    public boolean isVanished(UUID uuid) { return vanished.contains(uuid); }

    // ════════════════════════════════════════════════════════════════
    //  CHAT
    // ════════════════════════════════════════════════════════════════

    public void handleChat(AsyncPlayerChatEvent event) {
        Player   player = event.getPlayer();
        RankData rd     = getRankData(getRank(player.getUniqueId()));
        event.setFormat(rd.chatTag() + " " + rd.color()
                + player.getName() + " §8: §f%2$s");
    }

    // ════════════════════════════════════════════════════════════════
    //  NAMETAG
    // ════════════════════════════════════════════════════════════════

    public void updateNametag(Player player) {
        RankData rd    = getRankData(getRank(player.getUniqueId()));
        int      level = player.getLevel();
        double   bal   = getBalance(player);

        String prefix = "§7[§a" + level + "§7] " + rd.chatTag() + " ";
        String suffix = " §8[§a" + formatCoins(bal) + "§8]";
        if (suffix.length() > 40) suffix = "";

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }

        String teamName = ("rk" + rd.priority() + player.getName());
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        team.setPrefix(prefix);
        team.setSuffix(suffix);
        try {
            team.setColor(ChatColor.valueOf(colorName(rd.color())));
        } catch (Exception ignored) {}
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());

        player.setPlayerListName("§7[§a" + level + "§7] " + rd.chatTag()
                + " " + rd.color() + player.getName());

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            Scoreboard ob = other.getScoreboard();
            Team ot = ob.getTeam(teamName);
            if (ot == null) ot = ob.registerNewTeam(teamName);
            ot.setPrefix(prefix);
            try {
                ot.setColor(ChatColor.valueOf(colorName(rd.color())));
            } catch (Exception ignored) {}
            if (!ot.hasEntry(player.getName())) ot.addEntry(player.getName());
        }
    }

    public void updateAllNametags() {
        for (Player p : Bukkit.getOnlinePlayers()) updateNametag(p);
    }

    private String colorName(String code) {
        return switch (code) {
            case "§0" -> "BLACK";      case "§1" -> "DARK_BLUE";
            case "§2" -> "DARK_GREEN"; case "§3" -> "DARK_AQUA";
            case "§4" -> "DARK_RED";   case "§5" -> "DARK_PURPLE";
            case "§6" -> "GOLD";       case "§7" -> "GRAY";
            case "§8" -> "DARK_GRAY";  case "§9" -> "BLUE";
            case "§a" -> "GREEN";      case "§b" -> "AQUA";
            case "§c" -> "RED";        case "§d" -> "LIGHT_PURPLE";
            case "§e" -> "YELLOW";     default   -> "WHITE";
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  SCOREBOARD
    // ════════════════════════════════════════════════════════════════

    public void updateScoreboard(Player player) {
        UUID     uuid   = player.getUniqueId();
        RankData rd     = getRankData(getRank(uuid));
        int      level  = player.getLevel();
        double   bal    = getBalance(player);
        int      k      = kills.getOrDefault(uuid,    0);
        int      d      = deaths.getOrDefault(uuid,   0);
        int      pt     = playtime.getOrDefault(uuid, 0);
        int      online = Bukkit.getOnlinePlayers().size();
        int      max    = Bukkit.getMaxPlayers();

        String modeName = cap(currentServer);
        try {
            if (plugin.getEconomyManager() != null) {
                modeName = plugin.getEconomyManager().getModeName(
                        plugin.getEconomyManager().getPlayerMode(player));
            }
        } catch (Exception ignored) {}

        String job = "§cNone";
        try {
            if (plugin.getJobSystem() != null) {
                String j = plugin.getJobSystem().getJob(uuid);
                if (j != null) job = cap(j);
            }
        } catch (Exception ignored) {}

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective old = board.getObjective("kz_sb");
        if (old != null) old.unregister();

        Objective obj = board.registerNewObjective(
                "kz_sb", Criteria.DUMMY, "§b§l✦ §f§lKZ SERVER §b§l✦");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int s = 15;
        obj.getScore("§r§b──────────────────").setScore(s--);
        obj.getScore(" §f" + rd.color() + player.getName()).setScore(s--);
        obj.getScore(" " + rd.chatTag()).setScore(s--);
        obj.getScore(" §7Lv §a" + level).setScore(s--);
        obj.getScore("§r§6──────────────────").setScore(s--);
        obj.getScore(" §7Server  §f" + cap(currentServer)).setScore(s--);
        obj.getScore(" §7Mode    " + modeName).setScore(s--);
        obj.getScore(" §7Balance §a" + formatCoins(bal)).setScore(s--);
        obj.getScore(" §7Job     §f" + job).setScore(s--);
        obj.getScore("§r§e──────────────────").setScore(s--);
        obj.getScore(" §7⚔ §a" + k + " §8| §7💀 §c" + d + " §8| §7KD §e"
                + String.format("%.1f", d > 0 ? (double) k / d : k)).setScore(s--);
        obj.getScore(" §7⏱ §f" + fmtPlaytime(pt)).setScore(s--);
        obj.getScore("§r§a──────────────────").setScore(s--);
        obj.getScore(" §7Online §a" + online + "§8/§7" + max).setScore(s--);
        obj.getScore(" §8play.kzserver.com").setScore(s);
    }

    public void updateAllScoreboards() {
        for (Player p : Bukkit.getOnlinePlayers()) updateScoreboard(p);
    }

    // ════════════════════════════════════════════════════════════════
    //  NPC - REMOVE / CLICK / LIST
    // ════════════════════════════════════════════════════════════════

    public void removeNearbyNPC(Player player) {
        int removed = 0;

        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (!(entity instanceof ArmorStand)) continue;
            UUID   eu  = entity.getUniqueId();
            String key = null;
            for (var e : npcEntityMap.entrySet()) {
                if (e.getValue().equals(eu)) { key = e.getKey(); break; }
            }
            if (key != null) {
                npcModes.remove(key);
                npcNames.remove(key);
                npcServers.remove(key);
                npcLocations.remove(key);
                npcEntityMap.remove(key);
                npcSkinOwners.remove(key);
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            player.sendMessage(
                    "§a§lKZ §8» §7Removed §f" + removed + " §7NPC(s).");
            player.playSound(player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            player.sendMessage("§c§lKZ §8» §7No NPCs within 3 blocks.");
            player.playSound(player.getLocation(),
                    Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        saveData();
    }

    public void handleNPCClick(Player player, Entity entity) {
        UUID   eu  = entity.getUniqueId();
        String key = null;
        for (var e : npcEntityMap.entrySet()) {
            if (e.getValue().equals(eu)) { key = e.getKey(); break; }
        }
        if (key == null) return;

        String mode   = npcModes.get(key);
        String target = npcServers.getOrDefault(key, getServerForMode(mode));
        String name   = npcNames.getOrDefault(key, "NPC");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // Cross-server transfer
        if (!currentServer.equalsIgnoreCase(target)) {
            player.sendMessage(
                    "§a§lKZ §8» §7Connecting to §b" + cap(name) + "§7...");
            player.playSound(player.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            player.getWorld().spawnParticle(Particle.PORTAL,
                    player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
            ServerUtils.sendToServer(plugin, player, target);
            return;
        }

        // Same server - coba mode spawn
        Location spawn = modeSpawns.get(mode);
        if (spawn != null) {
            player.teleport(spawn);
            player.sendMessage(
                    "§a§lKZ §8» §7Teleported to §f" + cap(mode) + "§7.");
            player.playSound(player.getLocation(),
                    Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            if (plugin.getEconomyManager() != null)
                plugin.getEconomyManager().setPlayerMode(player, mode);
            return;
        }

        // Coba island system
        try {
            if (plugin.getIslandSystem() != null) {
                UUID uuid = player.getUniqueId();
                if (plugin.getIslandSystem().hasIsland(uuid)) {
                    var island = plugin.getIslandSystem().getIsland(uuid);
                    if (island != null && island.mode.equalsIgnoreCase(mode)) {
                        player.teleport(island.spawnPoint);
                        player.sendMessage("§a§lKZ §8» §7Teleported to your §f"
                                + cap(mode) + " §7island.");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } else {
                        String em = island != null ? island.mode : "unknown";
                        player.sendMessage("§6§lKZ §8» §7You already have a §f"
                                + cap(em) + " §7island. Delete: §c/deleteisland");
                        player.playSound(player.getLocation(),
                                Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                } else {
                    plugin.getIslandSystem().createIsland(player, mode);
                }
                return;
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("[NPC] Island error: " + ex.getMessage());
        }

        player.sendMessage(
                "§c§lKZ §8» §7Mode §f" + cap(mode) + " §7not configured.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    public boolean isNPC(Entity entity) {
        return npcEntityMap.containsValue(entity.getUniqueId());
    }

    public String getNPCMode(Entity entity) {
        return npcEntityMap.entrySet().stream()
                .filter(e -> e.getValue().equals(entity.getUniqueId()))
                .map(e -> npcModes.get(e.getKey()))
                .findFirst().orElse(null);
    }

    public String getNPCTargetServer(Entity entity) {
        return npcEntityMap.entrySet().stream()
                .filter(e -> e.getValue().equals(entity.getUniqueId()))
                .map(e -> npcServers.get(e.getKey()))
                .findFirst().orElse(null);
    }

    public String getServerForMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival", "vanilla" -> "survival";
            case "oneblock", "skyblock" -> "void";
            case "island", "acid"      -> "custom";
            default                    -> "lobby";
        };
    }

    public void listNPCs(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l┌──────────────────────────────────┐");
        player.sendMessage("§b§l│          §f§lNPC LIST               §b§l│");
        player.sendMessage("§b§l└──────────────────────────────────┘");
        if (npcModes.isEmpty()) {
            player.sendMessage("  §7No NPCs registered.");
        } else {
            int i = 0;
            for (var e : npcModes.entrySet()) {
                i++;
                String k    = e.getKey();
                String skin = npcSkinOwners.getOrDefault(k, "§8none");
                player.sendMessage("  §7" + i + ". §b"
                        + npcNames.getOrDefault(k, "?")
                        + " §8│ §7" + e.getValue()
                        + " §8│ §e" + npcServers.getOrDefault(k, "?")
                        + " §8│ 🎭§7" + skin
                        + " §8│ " + (npcEntityMap.containsKey(k) ? "§a✔" : "§c✘"));
            }
        }
        player.sendMessage("  §7Total: §f" + npcModes.size());
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  MISC
    // ════════════════════════════════════════════════════════════════

    public void trackPlaytime() {
        for (Player p : Bukkit.getOnlinePlayers())
            playtime.merge(p.getUniqueId(), 1, Integer::sum);
    }

    public void spawnFireworks() {
        if (lobbySpawn == null) return;
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> lobbySpawn.getWorld().spawn(lobbySpawn, Firework.class),
                    i * 10L);
        }
    }

    public void addKill(UUID uuid)  { kills.merge(uuid,  1, Integer::sum); }
    public void addDeath(UUID uuid) { deaths.merge(uuid, 1, Integer::sum); }

    public void showStats(Player viewer, Player target) {
        UUID     uuid = target.getUniqueId();
        RankData rd   = getRankData(getRank(uuid));
        int k  = kills.getOrDefault(uuid,    0);
        int d  = deaths.getOrDefault(uuid,   0);
        int pt = playtime.getOrDefault(uuid, 0);
        double kd  = d > 0 ? (double) k / d : k;
        double bal = getBalance(target);

        String job = "None";
        try {
            if (plugin.getJobSystem() != null) {
                String j = plugin.getJobSystem().getJob(uuid);
                if (j != null) job = cap(j);
            }
        } catch (Exception ignored) {}

        viewer.sendMessage("");
        viewer.sendMessage("§b§l┌──────────────────────────────────┐");
        viewer.sendMessage("§b§l│       §f§lPLAYER STATS              §b§l│");
        viewer.sendMessage("§b§l└──────────────────────────────────┘");
        viewer.sendMessage("");
        viewer.sendMessage("  §f👤 §7Player   : " + rd.color() + target.getName());
        viewer.sendMessage("  §f👑 §7Rank     : " + rd.chatTag());
        viewer.sendMessage("  §f🎮 §7Platform : §f"
                + platforms.getOrDefault(uuid, "Java"));
        viewer.sendMessage("");
        viewer.sendMessage("  §f💰 §7Balance  : §a" + formatCoins(bal));
        viewer.sendMessage("  §f💼 §7Job      : §f" + job);
        viewer.sendMessage("  §f🏠 §7Max Land : §f"
                + rd.maxLandSize() + "x" + rd.maxLandSize());
        viewer.sendMessage("");
        viewer.sendMessage("  §f⚔  §7Kills   : §a" + k);
        viewer.sendMessage("  §f💀 §7Deaths   : §c" + d);
        viewer.sendMessage("  §f📊 §7K/D      : §e" + String.format("%.2f", kd));
        viewer.sendMessage("  §f⏱  §7Playtime : §7" + fmtPlaytime(pt));
        viewer.sendMessage("");

        viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  GETTERS / SETTERS
    // ════════════════════════════════════════════════════════════════

    public Location getLobbySpawn()           { return lobbySpawn; }
    public void     setLobbySpawn(Location l) { lobbySpawn = l; saveData(); }

    public Location getModeSpawn(String mode) {
        return modeSpawns.get(mode.toLowerCase());
    }
    public void setModeSpawn(String mode, Location l) {
        modeSpawns.put(mode.toLowerCase(), l); saveData();
    }

    public String getRank(UUID uuid) {
        return ranks.getOrDefault(uuid, "initiate");
    }

    public void setRank(UUID uuid, String rank) {
        String r = rank.toLowerCase();
        if (!rankConfig.containsKey(r)) {
            plugin.getLogger().warning("[Rank] Unknown rank: " + rank);
            return;
        }
        ranks.put(uuid, r);
        saveData();

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            updateNametag(p);
            updateScoreboard(p);
            p.sendMessage("§b§lKZ §8» §7Rank updated → "
                    + getRankData(r).chatTag() + "§7!");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    public RankData getRankData(String rank) {
        return rankConfig.getOrDefault(rank.toLowerCase(),
                rankConfig.get("initiate"));
    }

    public String  getRankDisplay(String rank) { return getRankData(rank).chatTag(); }
    public int     getMaxLandSize(String rank) { return getRankData(rank).maxLandSize(); }
    public int     getMaxClaims(String rank)   { return getRankData(rank).maxClaims(); }
    public int     getMaxHomes(String rank)    { return getRankData(rank).maxHomes(); }
    public String  getPlatform(UUID uuid)      { return platforms.getOrDefault(uuid, "Java"); }
    public boolean isMaintenance()             { return maintenance; }
    public int     getTotalPlayers()           { return totalPlayers; }
    public String  getCurrentServer()          { return currentServer; }
    public boolean isLobbyServer()             { return "lobby".equalsIgnoreCase(currentServer); }

    public void setMaintenance(boolean val) { maintenance = val; saveData(); }

    public Map<String, RankData> getAllRanks() {
        return Collections.unmodifiableMap(rankConfig);
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════════

    private double getBalance(Player player) {
        try {
            if (plugin.getEconomyManager() != null)
                return plugin.getEconomyManager().getBalance(player);
        } catch (Exception ignored) {}
        return 0;
    }

    private void send(Player player, String msg) { player.sendMessage(msg); }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String fmtPlaytime(int minutes) {
        if (minutes < 60) return minutes + "m";
        int h = minutes / 60, m = minutes % 60;
        if (h < 24) return h + "h " + m + "m";
        int day = h / 24; h = h % 24;
        return day + "d " + h + "h";
    }

    /**
     * Format koin:
     *   500   → "500"
     *   1000  → "1K"
     *   1200  → "1.2K"
     *   1500000 → "1.5M"
     */
    public String formatCoins(double amount) {
        if (amount < 1_000) return String.format("%.0f", amount);

        record Tier(double div, String suffix) {}
        List<Tier> tiers = List.of(
                new Tier(1_000_000_000_000.0, "T"),
                new Tier(1_000_000_000.0,     "B"),
                new Tier(1_000_000.0,          "M"),
                new Tier(1_000.0,              "K")
        );

        for (Tier tier : tiers) {
            if (amount >= tier.div()) {
                double val = amount / tier.div();
                if (val == Math.floor(val) && !Double.isInfinite(val))
                    return String.format("%.0f%s", val, tier.suffix());
                return String.format("%.1f%s", val, tier.suffix());
            }
        }

        return String.format("%.0f", amount);
    }
}
