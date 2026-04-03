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
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LobbySystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // ════════════════════════════════════════════════════════════════
    //  DATA STORAGE
    // ════════════════════════════════════════════════════════════════

    private Location lobbySpawn;
    private final Map<String, Location> modeSpawns = new HashMap<>();

    // Player data (cached from lobby.yml)
    private final Map<UUID, String> platforms = new HashMap<>();
    private final Map<UUID, String> ranks = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Integer> playtime = new HashMap<>();
    private final Map<UUID, Boolean> firstJoin = new HashMap<>();

    // NPC data - key = entityUUID string
    private final Map<String, Location> npcLocations = new HashMap<>();
    private final Map<String, String> npcModes = new HashMap<>();
    private final Map<String, String> npcNames = new HashMap<>();
    private final Map<String, String> npcServers = new HashMap<>();

    // Live entity tracking - key = entityUUID string from config, value = spawned entity UUID
    private final Map<String, UUID> npcEntityMap = new HashMap<>();

    private boolean maintenance = false;
    private int totalPlayers = 0;

    // Current server identity (from config.yml)
    private final String currentServer;

    // ════════════════════════════════════════════════════════════════
    //  RANK CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    public record RankData(
            String displayName,
            String color,
            String chatTag,
            int maxLandSize,
            int maxClaims,
            int maxHomes,
            int priority,
            List<String> permissions
    ) {}

    private final Map<String, RankData> rankConfig = new LinkedHashMap<>();

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public LobbySystem(KZPlugin plugin) {
        this.plugin = plugin;
        this.currentServer = plugin.getConfig().getString("server-name", "lobby");
        initRankConfig();
        loadData();

        // Respawn NPC entities setelah server start (delay 3 detik supaya world loaded)
        Bukkit.getScheduler().runTaskLater(plugin, this::respawnNPCEntities, 60L);
    }

    // ════════════════════════════════════════════════════════════════
    //  RANK INIT - Semua rank data di satu tempat
    // ════════════════════════════════════════════════════════════════

    private void initRankConfig() {
        rankConfig.put("member", new RankData(
                "Member", "§7", "§7[Member]",
                25, 1, 1, 0,
                List.of(
                        "kzplugin.cmd.help", "kzplugin.cmd.rules", "kzplugin.cmd.shop",
                        "kzplugin.cmd.sell", "kzplugin.cmd.bal", "kzplugin.cmd.pay",
                        "kzplugin.cmd.baltop", "kzplugin.cmd.daily", "kzplugin.cmd.weekly",
                        "kzplugin.cmd.tpa", "kzplugin.cmd.lobby", "kzplugin.cmd.spawn",
                        "kzplugin.cmd.stats", "kzplugin.cmd.rank", "kzplugin.cmd.discord",
                        "kzplugin.cmd.website",
                        "kzplugin.cmd.island", "kzplugin.cmd.land", "kzplugin.cmd.job",
                        "kzplugin.cmd.ah", "kzplugin.cmd.cf"
                )
        ));
        rankConfig.put("iron", new RankData(
                "Iron", "§f", "§f[Iron]",
                30, 2, 2, 1,
                List.of("kzplugin.rank.iron", "kzplugin.cmd.nick")
        ));
        rankConfig.put("gold", new RankData(
                "Gold", "§6", "§6[Gold]",
                35, 3, 2, 2,
                List.of("kzplugin.rank.gold", "kzplugin.cmd.nick", "kzplugin.cmd.hat")
        ));
        rankConfig.put("diamond", new RankData(
                "Diamond", "§b", "§b[Diamond]",
                40, 4, 3, 3,
                List.of("kzplugin.rank.diamond", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest")
        ));
        rankConfig.put("emerald", new RankData(
                "Emerald", "§a", "§a[Emerald]",
                50, 5, 3, 4,
                List.of("kzplugin.rank.emerald", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest", "kzplugin.cmd.craft")
        ));
        rankConfig.put("obsidian", new RankData(
                "Obsidian", "§5", "§5[Obsidian]",
                60, 6, 4, 5,
                List.of("kzplugin.rank.obsidian", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest", "kzplugin.cmd.craft", "kzplugin.cmd.fly")
        ));
        rankConfig.put("onyx", new RankData(
                "Onyx", "§8", "§8[§f§lOnyx§8]",
                75, 8, 5, 6,
                List.of("kzplugin.rank.onyx", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest", "kzplugin.cmd.craft", "kzplugin.cmd.fly",
                        "kzplugin.cmd.heal")
        ));
        rankConfig.put("phantom", new RankData(
                "Phantom", "§d", "§d[Phantom]",
                100, 10, 6, 7,
                List.of("kzplugin.rank.phantom", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest", "kzplugin.cmd.craft", "kzplugin.cmd.fly",
                        "kzplugin.cmd.heal", "kzplugin.cmd.feed")
        ));
        rankConfig.put("eclipse", new RankData(
                "Eclipse", "§e", "§e[§6§lEclipse§e]",
                150, 15, 8, 8,
                List.of("kzplugin.rank.eclipse", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest", "kzplugin.cmd.craft", "kzplugin.cmd.fly",
                        "kzplugin.cmd.heal", "kzplugin.cmd.feed", "kzplugin.cmd.back")
        ));
        rankConfig.put("ethereal", new RankData(
                "Ethereal", "§3", "§3[§b§l✦Ethereal✦§3]",
                200, 20, 10, 9,
                List.of("kzplugin.rank.ethereal", "kzplugin.cmd.nick", "kzplugin.cmd.hat",
                        "kzplugin.cmd.enderchest", "kzplugin.cmd.craft", "kzplugin.cmd.fly",
                        "kzplugin.cmd.heal", "kzplugin.cmd.feed", "kzplugin.cmd.back",
                        "kzplugin.cmd.god")
        ));

        plugin.getLogger().info("[Rank] Loaded " + rankConfig.size() + " ranks.");
    }

    // ════════════════════════════════════════════════════════════════
    //  LOAD / SAVE DATA (lobby.yml)
    // ════════════════════════════════════════════════════════════════

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "lobby.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("[Lobby] Failed to create lobby.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load lobby spawn
        if (dataConfig.contains("lobby")) {
            World w = Bukkit.getWorld(dataConfig.getString("lobby.world", "world"));
            if (w != null) {
                lobbySpawn = new Location(w,
                        dataConfig.getDouble("lobby.x"),
                        dataConfig.getDouble("lobby.y"),
                        dataConfig.getDouble("lobby.z"),
                        (float) dataConfig.getDouble("lobby.yaw"),
                        (float) dataConfig.getDouble("lobby.pitch"));
            }
        }

        // Load mode spawns
        if (dataConfig.contains("spawns")) {
            var section = dataConfig.getConfigurationSection("spawns");
            if (section != null) {
                for (String mode : section.getKeys(false)) {
                    World w = Bukkit.getWorld(dataConfig.getString("spawns." + mode + ".world", "world"));
                    if (w != null) {
                        modeSpawns.put(mode, new Location(w,
                                dataConfig.getDouble("spawns." + mode + ".x"),
                                dataConfig.getDouble("spawns." + mode + ".y"),
                                dataConfig.getDouble("spawns." + mode + ".z")));
                    }
                }
            }
        }

        // Load player stats
        if (dataConfig.contains("stats")) {
            var section = dataConfig.getConfigurationSection("stats");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        ranks.put(uuid, dataConfig.getString("stats." + key + ".rank", "Member"));
                        kills.put(uuid, dataConfig.getInt("stats." + key + ".kills", 0));
                        deaths.put(uuid, dataConfig.getInt("stats." + key + ".deaths", 0));
                        playtime.put(uuid, dataConfig.getInt("stats." + key + ".playtime", 0));
                        firstJoin.put(uuid, dataConfig.getBoolean("stats." + key + ".joined", false));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("[Lobby] Invalid UUID in stats: " + key);
                    }
                }
            }
        }

        // Load NPC data
        if (dataConfig.contains("npcs")) {
            var section = dataConfig.getConfigurationSection("npcs");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    String path = "npcs." + key;
                    npcModes.put(key, dataConfig.getString(path + ".mode", "lobby"));
                    npcNames.put(key, dataConfig.getString(path + ".name", "NPC"));
                    npcServers.put(key, dataConfig.getString(path + ".server", "lobby"));

                    // Load NPC location for respawning
                    if (dataConfig.contains(path + ".location")) {
                        String locPath = path + ".location";
                        World w = Bukkit.getWorld(dataConfig.getString(locPath + ".world", "world"));
                        if (w != null) {
                            Location loc = new Location(w,
                                    dataConfig.getDouble(locPath + ".x"),
                                    dataConfig.getDouble(locPath + ".y"),
                                    dataConfig.getDouble(locPath + ".z"),
                                    (float) dataConfig.getDouble(locPath + ".yaw", 0),
                                    (float) dataConfig.getDouble(locPath + ".pitch", 0));
                            npcLocations.put(key, loc);
                        }
                    }
                }
            }
        }

        maintenance = dataConfig.getBoolean("maintenance", false);
        totalPlayers = dataConfig.getInt("totalPlayers", 0);

        plugin.getLogger().info("[Lobby] Data loaded. NPCs: " + npcModes.size()
                + " | Stats: " + ranks.size() + " players");
    }

    public void saveData() {
        // Save lobby spawn
        if (lobbySpawn != null) {
            dataConfig.set("lobby.world", lobbySpawn.getWorld().getName());
            dataConfig.set("lobby.x", lobbySpawn.getX());
            dataConfig.set("lobby.y", lobbySpawn.getY());
            dataConfig.set("lobby.z", lobbySpawn.getZ());
            dataConfig.set("lobby.yaw", lobbySpawn.getYaw());
            dataConfig.set("lobby.pitch", lobbySpawn.getPitch());
        }

        // Save mode spawns
        for (Map.Entry<String, Location> entry : modeSpawns.entrySet()) {
            String path = "spawns." + entry.getKey();
            Location loc = entry.getValue();
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getX());
            dataConfig.set(path + ".y", loc.getY());
            dataConfig.set(path + ".z", loc.getZ());
        }

        // Save player stats
        for (UUID uuid : ranks.keySet()) {
            String path = "stats." + uuid.toString();
            dataConfig.set(path + ".rank", ranks.get(uuid));
            dataConfig.set(path + ".kills", kills.getOrDefault(uuid, 0));
            dataConfig.set(path + ".deaths", deaths.getOrDefault(uuid, 0));
            dataConfig.set(path + ".playtime", playtime.getOrDefault(uuid, 0));
            dataConfig.set(path + ".joined", firstJoin.getOrDefault(uuid, false));
        }

        // Save NPC data (clear old section first)
        dataConfig.set("npcs", null);
        for (Map.Entry<String, String> entry : npcModes.entrySet()) {
            String key = entry.getKey();
            String path = "npcs." + key;
            dataConfig.set(path + ".mode", entry.getValue());
            dataConfig.set(path + ".name", npcNames.get(key));
            dataConfig.set(path + ".server", npcServers.getOrDefault(key, "lobby"));

            // Save NPC location
            Location loc = npcLocations.get(key);
            if (loc != null) {
                String locPath = path + ".location";
                dataConfig.set(locPath + ".world", loc.getWorld().getName());
                dataConfig.set(locPath + ".x", loc.getX());
                dataConfig.set(locPath + ".y", loc.getY());
                dataConfig.set(locPath + ".z", loc.getZ());
                dataConfig.set(locPath + ".yaw", loc.getYaw());
                dataConfig.set(locPath + ".pitch", loc.getPitch());
            }
        }

        dataConfig.set("maintenance", maintenance);
        dataConfig.set("totalPlayers", totalPlayers);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Lobby] Failed to save lobby.yml: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  JOIN / QUIT HANDLER - Server-aware
    // ════════════════════════════════════════════════════════════════

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        // Teleport ke lobby spawn HANYA jika ini lobby server
        if (isLobbyServer() && lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        // Maintenance check
        if (maintenance && !player.hasPermission("kzplugin.admin")) {
            player.kickPlayer("§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later.");
            return;
        }

        // Detect platform - pakai FloodgateApi jika ada
        String platform = detectPlatform(player);
        platforms.put(uuid, platform);

        // Apply rank permissions
        applyRankPermissions(player);

        // First join vs returning
        if (!firstJoin.getOrDefault(uuid, false)) {
            handleFirstJoin(player);
        } else {
            handleReturningPlayer(player);
        }

        // Update visual (delay supaya player fully loaded)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            updateNametag(player);
            updateScoreboard(player);
        }, 5L);
    }

    /**
     * Detect player platform: Java atau Bedrock
     * Prioritas: FloodgateApi > fallback prefix check
     */
    private String detectPlatform(Player player) {
        // Coba pakai FloodgateApi dulu (paling akurat)
        try {
            Class<?> floodgateClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = floodgateClass.getMethod("getInstance").invoke(null);
            boolean isBedrock = (boolean) floodgateClass.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(api, player.getUniqueId());
            if (isBedrock) return "Bedrock";
        } catch (Exception ignored) {
            // FloodgateApi not available, fallback
        }

        // Fallback: cek prefix dari Floodgate config (default ".")
        String prefix = plugin.getConfig().getString("floodgate-prefix", ".");
        if (player.getName().startsWith(prefix)) {
            return "Bedrock";
        }

        return "Java";
    }

    /**
     * Check apakah player ini Bedrock client
     */
    public boolean isBedrockPlayer(Player player) {
        return "Bedrock".equals(platforms.getOrDefault(player.getUniqueId(), detectPlatform(player)));
    }

    private void handleFirstJoin(Player player) {
        UUID uuid = player.getUniqueId();

        firstJoin.put(uuid, true);
        ranks.put(uuid, "Member");
        kills.put(uuid, 0);
        deaths.put(uuid, 0);
        playtime.put(uuid, 0);
        totalPlayers++;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String rank = getRank(uuid);
            RankData rd = getRankData(rank);

            player.sendMessage("");
            player.sendMessage("§b§l┌─────────────────────────────────┐");
            player.sendMessage("§b§l│   §f§lWELCOME TO §b§lKZ SERVER         §b§l│");
            player.sendMessage("§b§l└─────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7Hello, §b" + player.getName() + "§7!");
            player.sendMessage("  §7Welcome to KZ Minecraft Network.");
            player.sendMessage("");
            player.sendMessage("  §f💰 Balance  : §a$1,000");
            player.sendMessage("  §f👑 Rank     : " + rd.chatTag() + " " + rd.displayName());
            player.sendMessage("  §f🎮 Platform : §f" + platforms.get(uuid));
            player.sendMessage("  §f🏠 Land     : §f" + rd.maxLandSize() + "x" + rd.maxLandSize());
            player.sendMessage("");
            player.sendMessage("  §fQuick Start:");

            // Bedrock player mendapat instruksi berbeda
            if (isBedrockPlayer(player)) {
                player.sendMessage("  §7▸ Type §b/menu §7to select a game mode");
            } else {
                player.sendMessage("  §7▸ Click an NPC to select a game mode");
            }
            player.sendMessage("  §7▸ Type §b/daily §7for daily rewards");
            player.sendMessage("  §7▸ Type §b/help §7for commands");
            player.sendMessage("");
            player.sendMessage("§b§l───────────────────────────────────");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            try {
                player.getWorld().spawn(player.getLocation(), Firework.class);
            } catch (Exception ignored) {}

            // Broadcast hanya ke server ini
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage("");
                online.sendMessage("  §a§l[+] §f" + player.getName() + " §7joined for the first time! §a🎉");
                online.sendMessage("");
            }
        }, 20L);
    }

    private void handleReturningPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String rank = getRank(uuid);
            RankData rd = getRankData(rank);

            // Safe economy access
            double bal = 0;
            String modeName = "§7Lobby";
            if (plugin.getEconomyManager() != null) {
                bal = plugin.getEconomyManager().getBalance(player);
                String mode = plugin.getEconomyManager().getPlayerMode(player);
                modeName = plugin.getEconomyManager().getModeName(mode);
            }

            player.sendMessage("");
            player.sendMessage("§b§l┌─────────────────────────────────┐");
            player.sendMessage("§b§l│   §f§lWELCOME BACK                  §b§l│");
            player.sendMessage("§b§l└─────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7Hey, §b" + player.getName() + "§7!");
            player.sendMessage("");
            player.sendMessage("  §f💰 Balance  : §a" + formatMoney(bal));
            player.sendMessage("  §f👑 Rank     : " + rd.chatTag());
            player.sendMessage("  §f🌍 Server   : §f" + capitalize(currentServer));
            player.sendMessage("  §f🎮 Platform : §f" + platforms.getOrDefault(uuid, "Java"));
            player.sendMessage("");
            player.sendMessage("  §7Don't forget to claim §b/daily §7rewards!");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage("  §a§l[+] §f" + player.getName() + " §7joined the server.");
            }
        }, 20L);
    }

    public void handleQuit(Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.sendMessage("  §c§l[-] §f" + player.getName() + " §7left the server.");
            }
        }
        saveData();
    }

    // ════════════════════════════════════════════════════════════════
    //  CHAT FORMAT - [Rank] PlayerName : message
    // ════════════════════════════════════════════════════════════════

    public void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String rank = getRank(uuid);
        RankData rd = getRankData(rank);

        String format = rd.chatTag() + " " + rd.color() + player.getName() + " §8: §f%2$s";
        event.setFormat(format);
    }

    // ════════════════════════════════════════════════════════════════
    //  NAMETAG - [Level] [Rank] PlayerName
    // ════════════════════════════════════════════════════════════════

    public void updateNametag(Player player) {
        UUID uuid = player.getUniqueId();
        String rank = getRank(uuid);
        RankData rd = getRankData(rank);
        int level = player.getLevel();

        // Safe economy access
        double bal = 0;
        if (plugin.getEconomyManager() != null) {
            bal = plugin.getEconomyManager().getBalance(player);
        }

        String prefix = "§7[§a" + level + "§7] " + rd.chatTag() + " ";

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        if (board == manager.getMainScoreboard()) {
            board = manager.getNewScoreboard();
        }

        String teamName = "r" + rd.priority() + "_" + player.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        team.setPrefix(prefix);
        team.setSuffix("\n" + rd.color() + formatMoney(bal));
        team.setColor(ChatColor.valueOf(getColorName(rd.color())));

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        player.setScoreboard(board);

        player.setPlayerListName(
                "§7[§a" + level + "§7] " + rd.chatTag() + " " + rd.color() + player.getName()
        );

        // Sync ke semua player lain
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            Scoreboard otherBoard = other.getScoreboard();

            Team otherTeam = otherBoard.getTeam(teamName);
            if (otherTeam == null) otherTeam = otherBoard.registerNewTeam(teamName);

            otherTeam.setPrefix(prefix);
            otherTeam.setColor(ChatColor.valueOf(getColorName(rd.color())));
            if (!otherTeam.hasEntry(player.getName())) {
                otherTeam.addEntry(player.getName());
            }
        }
    }

    public void updateAllNametags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateNametag(player);
        }
    }

    private String getColorName(String colorCode) {
        return switch (colorCode) {
            case "§0" -> "BLACK";
            case "§1" -> "DARK_BLUE";
            case "§2" -> "DARK_GREEN";
            case "§3" -> "DARK_AQUA";
            case "§4" -> "DARK_RED";
            case "§5" -> "DARK_PURPLE";
            case "§6" -> "GOLD";
            case "§7" -> "GRAY";
            case "§8" -> "DARK_GRAY";
            case "§9" -> "BLUE";
            case "§a" -> "GREEN";
            case "§b" -> "AQUA";
            case "§c" -> "RED";
            case "§d" -> "LIGHT_PURPLE";
            case "§e" -> "YELLOW";
            case "§f" -> "WHITE";
            default -> "WHITE";
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  SCOREBOARD - Server-aware, null-safe
    // ════════════════════════════════════════════════════════════════

    public void updateScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        String rank = getRank(uuid);
        RankData rd = getRankData(rank);
        int level = player.getLevel();

        // Safe economy access
        double bal = 0;
        String modeName = "§7" + capitalize(currentServer);
        if (plugin.getEconomyManager() != null) {
            bal = plugin.getEconomyManager().getBalance(player);
            String mode = plugin.getEconomyManager().getPlayerMode(player);
            modeName = plugin.getEconomyManager().getModeName(mode);
        }

        // Safe job access
        String jobDisplay = "§cNone";
        try {
            if (plugin.getJobSystem() != null) {
                String job = plugin.getJobSystem().getJob(uuid);
                if (job != null) jobDisplay = capitalize(job);
            }
        } catch (Exception ignored) {}

        int k = kills.getOrDefault(uuid, 0);
        int d = deaths.getOrDefault(uuid, 0);
        int pt = playtime.getOrDefault(uuid, 0);
        String ptDisplay = formatPlaytime(pt);
        int online = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = player.getScoreboard();
        Objective old = board.getObjective("kz_sb");
        if (old != null) old.unregister();

        Objective obj = board.registerNewObjective("kz_sb", Criteria.DUMMY, "§b§lKZ §f§lSERVER");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = 15;

        obj.getScore("§b§l─────────────────").setScore(score--);
        obj.getScore("§f Player: " + rd.color() + player.getName()).setScore(score--);
        obj.getScore("§f Rank: " + rd.chatTag()).setScore(score--);
        obj.getScore("§f Level: §a" + level).setScore(score--);
        obj.getScore("§6§l─────────────────").setScore(score--);
        obj.getScore("§f Server: §e" + capitalize(currentServer)).setScore(score--);
        obj.getScore("§f Mode: " + modeName).setScore(score--);
        obj.getScore("§f Balance: §a" + formatMoney(bal)).setScore(score--);
        obj.getScore("§e§l─────────────────").setScore(score--);
        obj.getScore("§f ⚔ K: §a" + k + " §8| §f💀 D: §c" + d).setScore(score--);
        obj.getScore("§f ⏱ Play: §7" + ptDisplay).setScore(score--);
        obj.getScore("§a§l─────────────────").setScore(score--);
        obj.getScore("§f Online: §a" + online + "§7/§f" + maxPlayers).setScore(score--);
        obj.getScore("§7 play.kzserver.com").setScore(score--);
        obj.getScore("§f§l─────────────────").setScore(score);
    }

    public void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }

    private String formatPlaytime(int minutes) {
        if (minutes < 60) return minutes + "m";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours < 24) return hours + "h " + mins + "m";
        int days = hours / 24;
        hours = hours % 24;
        return days + "d " + hours + "h";
    }

    // ════════════════════════════════════════════════════════════════
    //  NPC SYSTEM - Velocity cross-server + persistent respawn
    // ════════════════════════════════════════════════════════════════

    /**
     * Create NPC ArmorStand dengan target server
     * Usage: /createnpc <mode> <displayName> [targetServer]
     */
    public void createNPC(Player player, String mode, String displayName, String targetServer) {
        Location loc = player.getLocation();

        ArmorStand npc = loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setCustomName("§b§l" + displayName);
            stand.setCustomNameVisible(true);
            stand.setGravity(false);
            stand.setVisible(true);
            stand.setInvulnerable(true);
            stand.setCanPickupItems(false);
            stand.setCollidable(false);

            // Marker supaya tidak bisa di-push
            stand.setMarker(true);

            // Persistent tag supaya tidak despawn
            stand.setPersistent(true);
            stand.setRemoveWhenFarAway(false);
        });

        // Generate unique config key (bukan entity UUID karena bisa berubah saat respawn)
        String configKey = "npc_" + System.currentTimeMillis();

        npcModes.put(configKey, mode);
        npcNames.put(configKey, displayName);
        npcServers.put(configKey, targetServer);
        npcLocations.put(configKey, loc.clone());
        npcEntityMap.put(configKey, npc.getUniqueId());

        player.sendMessage("");
        player.sendMessage("§a§l┌─────────────────────────────────┐");
        player.sendMessage("§a§l│        §f§lNPC CREATED               §a§l│");
        player.sendMessage("§a§l└─────────────────────────────────┘");
        player.sendMessage("  §7Name   : §b" + displayName);
        player.sendMessage("  §7Mode   : §f" + mode);
        player.sendMessage("  §7Server : §e" + targetServer);
        player.sendMessage("  §7ID     : §8" + configKey);
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        saveData();
    }

    /**
     * Create NPC dengan auto-detect server berdasarkan mode
     */
    public void createNPC(Player player, String mode, String displayName) {
        String server = getServerForMode(mode);
        createNPC(player, mode, displayName, server);
    }

    /**
     * Mapping mode → server name (sesuai Velocity config)
     */
    public String getServerForMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival", "vanilla" -> "survival";
            case "oneblock", "skyblock" -> "void";
            case "island", "acid" -> "custom";
            case "lobby" -> "lobby";
            default -> "lobby";
        };
    }

    /**
     * Respawn semua NPC entities dari config saat server start
     * Dipanggil di constructor dengan delay
     */
    private void respawnNPCEntities() {
        int spawned = 0;
        int failed = 0;

        for (Map.Entry<String, Location> entry : npcLocations.entrySet()) {
            String configKey = entry.getKey();
            Location loc = entry.getValue();

            if (loc == null || loc.getWorld() == null) {
                failed++;
                continue;
            }

            // Hapus entity lama di lokasi yang sama (radius 1 block)
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1, 2, 1)) {
                if (entity instanceof ArmorStand && entity.isCustomNameVisible()) {
                    entity.remove();
                }
            }

            // Spawn baru
            String displayName = npcNames.getOrDefault(configKey, "NPC");

            try {
                ArmorStand npc = loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
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
                });

                npcEntityMap.put(configKey, npc.getUniqueId());
                spawned++;
            } catch (Exception e) {
                plugin.getLogger().warning("[NPC] Failed to spawn NPC '" + displayName + "': " + e.getMessage());
                failed++;
            }
        }

        if (spawned > 0 || failed > 0) {
            plugin.getLogger().info("[NPC] Respawned " + spawned + " NPCs. Failed: " + failed);
        }
    }

    /**
     * Remove NPC terdekat dari player (radius 3 block)
     */
    public void removeNearbyNPC(Player player) {
        int removed = 0;

        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (!(entity instanceof ArmorStand)) continue;

            UUID entityUUID = entity.getUniqueId();

            // Cari config key dari entity UUID
            String foundKey = null;
            for (Map.Entry<String, UUID> entry : npcEntityMap.entrySet()) {
                if (entry.getValue().equals(entityUUID)) {
                    foundKey = entry.getKey();
                    break;
                }
            }

            if (foundKey != null) {
                npcModes.remove(foundKey);
                npcNames.remove(foundKey);
                npcServers.remove(foundKey);
                npcLocations.remove(foundKey);
                npcEntityMap.remove(foundKey);
                entity.remove();
                removed++;
            }
        }

        if (removed > 0) {
            player.sendMessage("§a§lKZ §8» §7Removed §f" + removed + " §7NPC(s).");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        } else {
            player.sendMessage("§c§lKZ §8» §7No NPCs found within 3 blocks.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        saveData();
    }

    /**
     * HANDLE NPC CLICK - Core logic untuk server transfer
     *
     * Flow:
     * 1. Player klik NPC ArmorStand
     * 2. Cek apakah entity ini registered NPC
     * 3. Jika target server BEDA → kirim via Velocity (ServerUtils)
     * 4. Jika target server SAMA → cek island, teleport/create
     */
    public void handleNPCClick(Player player, Entity entity) {
        UUID entityUUID = entity.getUniqueId();

        // Cari config key dari entity UUID
        String configKey = null;
        for (Map.Entry<String, UUID> entry : npcEntityMap.entrySet()) {
            if (entry.getValue().equals(entityUUID)) {
                configKey = entry.getKey();
                break;
            }
        }

        // Bukan NPC kita
        if (configKey == null) return;

        String mode = npcModes.get(configKey);
        String targetServer = npcServers.getOrDefault(configKey, getServerForMode(mode));
        String npcName = npcNames.getOrDefault(configKey, "NPC");

        // Sound feedback
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // ─── CASE 1: Target server BERBEDA dari server saat ini ───
        if (!currentServer.equalsIgnoreCase(targetServer)) {
            player.sendMessage("");
            player.sendMessage("§a§lKZ §8» §7Connecting to §b" + capitalize(npcName) + " §7server...");
            player.sendMessage("§a§lKZ §8» §7Mode: §f" + capitalize(mode));
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            // Spawn particle di lokasi player
            player.getWorld().spawnParticle(
                    Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1
            );

            // Kirim ke server lain via Velocity BungeeCord channel
            ServerUtils.sendToServer(plugin, player, targetServer);
            return;
        }

        // ─── CASE 2: Target server SAMA - handle locally ───

        // Cek apakah ada mode spawn yang di-set
        Location modeSpawn = modeSpawns.get(mode);
        if (modeSpawn != null) {
            player.teleport(modeSpawn);
            player.sendMessage("§a§lKZ §8» §7Teleported to §f" + capitalize(mode) + "§7.");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);

            // Set player mode di economy
            if (plugin.getEconomyManager() != null) {
                plugin.getEconomyManager().setPlayerMode(player, mode);
            }
            return;
        }

        // Cek island system (null-safe)
        try {
            if (plugin.getIslandSystem() != null) {
                UUID uuid = player.getUniqueId();

                if (plugin.getIslandSystem().hasIsland(uuid)) {
                    // Player sudah punya island
                    var island = plugin.getIslandSystem().getIsland(uuid);
                    if (island != null && island.mode.equalsIgnoreCase(mode)) {
                        player.teleport(island.spawnPoint);
                        player.sendMessage("§a§lKZ §8» §7Teleported to your §f"
                                + capitalize(mode) + " §7island.");
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } else {
                        String existingMode = (island != null) ? island.mode : "unknown";
                        player.sendMessage("§6§lKZ §8» §7You already have a §f"
                                + capitalize(existingMode) + " §7island.");
                        player.sendMessage("  §7Delete it first: §c/deleteisland");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                } else {
                    // Buat island baru
                    plugin.getIslandSystem().createIsland(player, mode);
                }
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NPC] Island system error: " + e.getMessage());
        }

        // Fallback: tidak ada island system dan tidak ada mode spawn
        player.sendMessage("§c§lKZ §8» §7Mode §f" + capitalize(mode) + " §7is not yet configured.");
        player.sendMessage("  §7Ask an admin to set spawn: §e/setmodespawn " + mode);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    /**
     * Check apakah entity ini NPC kita
     */
    public boolean isNPC(Entity entity) {
        UUID entityUUID = entity.getUniqueId();
        for (UUID tracked : npcEntityMap.values()) {
            if (tracked.equals(entityUUID)) return true;
        }
        return false;
    }

    /**
     * List semua NPC yang terdaftar
     */
    public void listNPCs(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│         §f§lNPC LIST                 §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");

        if (npcModes.isEmpty()) {
            player.sendMessage("  §7No NPCs registered.");
        } else {
            int count = 0;
            for (Map.Entry<String, String> entry : npcModes.entrySet()) {
                count++;
                String key = entry.getKey();
                String name = npcNames.getOrDefault(key, "Unknown");
                String server = npcServers.getOrDefault(key, "?");
                String mode = entry.getValue();
                boolean alive = npcEntityMap.containsKey(key);

                player.sendMessage("  §7" + count + ". §b" + name
                        + " §8| §7Mode: §f" + mode
                        + " §8| §7Server: §e" + server
                        + " §8| " + (alive ? "§a✔" : "§c✘"));
            }
        }

        player.sendMessage("");
        player.sendMessage("  §7Total: §f" + npcModes.size() + " §7NPCs");
        player.sendMessage("");
    }

    /**
     * Get NPC mode dari entity
     */
    public String getNPCMode(Entity entity) {
        UUID entityUUID = entity.getUniqueId();
        for (Map.Entry<String, UUID> entry : npcEntityMap.entrySet()) {
            if (entry.getValue().equals(entityUUID)) {
                return npcModes.get(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Get NPC target server dari entity
     */
    public String getNPCTargetServer(Entity entity) {
        UUID entityUUID = entity.getUniqueId();
        for (Map.Entry<String, UUID> entry : npcEntityMap.entrySet()) {
            if (entry.getValue().equals(entityUUID)) {
                return npcServers.get(entry.getKey());
            }
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════
    //  RANK SYSTEM - Permission management
    // ════════════════════════════════════════════════════════════════

    public void applyRankPermissions(Player player) {
        UUID uuid = player.getUniqueId();
        String rank = getRank(uuid).toLowerCase();
        RankData rd = getRankData(rank);

        // Apply member permissions ke semua player
        RankData memberData = rankConfig.get("member");
        if (memberData != null) {
            for (String perm : memberData.permissions()) {
                if (!player.hasPermission(perm)) {
                    player.addAttachment(plugin, perm, true);
                }
            }
        }

        // Apply rank-specific permissions (kumulatif berdasarkan priority)
        if (!rank.equals("member")) {
            for (Map.Entry<String, RankData> entry : rankConfig.entrySet()) {
                if (entry.getValue().priority() <= rd.priority() && !entry.getKey().equals("member")) {
                    for (String perm : entry.getValue().permissions()) {
                        if (!player.hasPermission(perm)) {
                            player.addAttachment(plugin, perm, true);
                        }
                    }
                }
            }
        }
    }

    public boolean hasRankPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public RankData getRankData(String rank) {
        return rankConfig.getOrDefault(rank.toLowerCase(), rankConfig.get("member"));
    }

    public String getRankDisplay(String rank) {
        return getRankData(rank).chatTag();
    }

    public int getMaxLandSize(String rank) {
        return getRankData(rank).maxLandSize();
    }

    public int getMaxClaims(String rank) {
        return getRankData(rank).maxClaims();
    }

    public int getMaxHomes(String rank) {
        return getRankData(rank).maxHomes();
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYTIME / CLEARLAG / FIREWORKS / STATS / KILL-DEATH
    // ════════════════════════════════════════════════════════════════

    public void trackPlaytime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            playtime.merge(player.getUniqueId(), 1, Integer::sum);
        }
    }

    public void clearLag() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage("§c§lKZ §8» §7Ground items will be cleared in §f30 seconds§7...");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§c§lKZ §8» §c10 seconds! Pick up your items!");
            }
        }, 400L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int count = 0;
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Item) {
                        entity.remove();
                        count++;
                    }
                }
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage("§a§lKZ §8» §a" + count + " §7items cleared. ✨");
            }
        }, 600L);
    }

    public void spawnFireworks() {
        if (lobbySpawn == null) return;
        try {
            for (int i = 0; i < 4; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        lobbySpawn.getWorld().spawn(lobbySpawn, Firework.class), i * 10L);
            }
        } catch (Exception ignored) {}
    }

    public void addKill(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
    }

    public void addDeath(UUID uuid) {
        deaths.merge(uuid, 1, Integer::sum);
    }

    public void showStats(Player player, Player target) {
        UUID uuid = target.getUniqueId();

        String rank = getRank(uuid);
        RankData rd = getRankData(rank);
        int k = kills.getOrDefault(uuid, 0);
        int d = deaths.getOrDefault(uuid, 0);
        int pt = playtime.getOrDefault(uuid, 0);
        String platform = platforms.getOrDefault(uuid, "Unknown");
        double kd = d > 0 ? (double) k / d : k;

        // Safe access
        String jobDisplay = "None";
        double bal = 0;
        try {
            if (plugin.getJobSystem() != null) {
                String job = plugin.getJobSystem().getJob(uuid);
                if (job != null) jobDisplay = capitalize(job);
            }
        } catch (Exception ignored) {}

        try {
            if (plugin.getEconomyManager() != null) {
                bal = plugin.getEconomyManager().getBalance(target);
            }
        } catch (Exception ignored) {}

        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│      §f§lPLAYER STATISTICS          §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §f👤 Player   : " + rd.color() + target.getName());
        player.sendMessage("  §f👑 Rank     : " + rd.chatTag());
        player.sendMessage("  §f🎮 Platform : §f" + platform);
        player.sendMessage("");
        player.sendMessage("  §f💰 Balance  : §a" + formatMoney(bal));
        player.sendMessage("  §f💼 Job      : §f" + jobDisplay);
        player.sendMessage("  §f🏠 Max Land : §f" + rd.maxLandSize() + "x" + rd.maxLandSize());
        player.sendMessage("");
        player.sendMessage("  §f⚔ Kills    : §a" + k);
        player.sendMessage("  §f💀 Deaths   : §c" + d);
        player.sendMessage("  §f📊 K/D      : §e" + String.format("%.2f", kd));
        player.sendMessage("  §f⏱ Playtime : §7" + formatPlaytime(pt));
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ════════════════════════════════════════════════════════════════
    //  GETTERS & SETTERS
    // ════════════════════════════════════════════════════════════════

    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(Location loc) {
        lobbySpawn = loc;
        saveData();
    }

    public Location getModeSpawn(String mode) {
        return modeSpawns.get(mode.toLowerCase());
    }

    public void setModeSpawn(String mode, Location loc) {
        modeSpawns.put(mode.toLowerCase(), loc);
        saveData();
    }

    public String getRank(UUID uuid) {
        return ranks.getOrDefault(uuid, "Member");
    }

    public void setRank(UUID uuid, String rank) {
        ranks.put(uuid, capitalize(rank));
        saveData();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyRankPermissions(player);
            updateNametag(player);
            updateScoreboard(player);
        }
    }

    public String getPlatform(UUID uuid) {
        return platforms.getOrDefault(uuid, "Java");
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setMaintenance(boolean val) {
        maintenance = val;
        saveData();
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public boolean isLobbyServer() {
        return "lobby".equalsIgnoreCase(currentServer);
    }

    public Map<String, RankData> getAllRanks() {
        return Collections.unmodifiableMap(rankConfig);
    }

    // ════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ════════════════════════════════════════════════════════════════

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Format money display - null-safe, tidak tergantung EconomyManager
     */
    private String formatMoney(double amount) {
        if (amount >= 1_000_000_000) return String.format("$%.1fB", amount / 1_000_000_000);
        if (amount >= 1_000_000) return String.format("$%.1fM", amount / 1_000_000);
        if (amount >= 1_000) return String.format("$%.1fK", amount / 1_000);
        return String.format("$%.0f", amount);
    }
}
