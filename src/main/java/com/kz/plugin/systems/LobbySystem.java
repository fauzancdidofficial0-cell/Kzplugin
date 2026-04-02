// ============================================================
// Path: src/main/java/com/kz/plugin/systems/LobbySystem.java
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

    private Location lobbySpawn;
    private final Map<String, Location> modeSpawns = new HashMap<>();
    private final Map<UUID, String> platforms = new HashMap<>();
    private final Map<UUID, String> ranks = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Integer> playtime = new HashMap<>();
    private final Map<UUID, Boolean> firstJoin = new HashMap<>();
    private final Map<String, String> npcModes = new HashMap<>();
    private final Map<String, String> npcNames = new HashMap<>();
    private final Map<String, String> npcServers = new HashMap<>();

    private boolean maintenance = false;
    private int totalPlayers = 0;

    // ════════════════════════════════════════════════════════════════
    //  RANK CONFIGURATION - All rank data in one place
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
        initRankConfig();
        loadData();
    }

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
                "kzplugin.cmd.rules", "kzplugin.cmd.website",
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
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

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

        if (dataConfig.contains("spawns")) {
            for (String mode : Objects.requireNonNull(
                    dataConfig.getConfigurationSection("spawns")).getKeys(false)) {
                World w = Bukkit.getWorld(dataConfig.getString("spawns." + mode + ".world", "world"));
                if (w != null) {
                    modeSpawns.put(mode, new Location(w,
                        dataConfig.getDouble("spawns." + mode + ".x"),
                        dataConfig.getDouble("spawns." + mode + ".y"),
                        dataConfig.getDouble("spawns." + mode + ".z")));
                }
            }
        }

        if (dataConfig.contains("stats")) {
            for (String key : Objects.requireNonNull(
                    dataConfig.getConfigurationSection("stats")).getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                ranks.put(uuid, dataConfig.getString("stats." + key + ".rank", "Member"));
                kills.put(uuid, dataConfig.getInt("stats." + key + ".kills", 0));
                deaths.put(uuid, dataConfig.getInt("stats." + key + ".deaths", 0));
                playtime.put(uuid, dataConfig.getInt("stats." + key + ".playtime", 0));
                firstJoin.put(uuid, dataConfig.getBoolean("stats." + key + ".joined", false));
            }
        }

        if (dataConfig.contains("npcs")) {
            for (String key : Objects.requireNonNull(
                    dataConfig.getConfigurationSection("npcs")).getKeys(false)) {
                npcModes.put(key, dataConfig.getString("npcs." + key + ".mode"));
                npcNames.put(key, dataConfig.getString("npcs." + key + ".name"));
                npcServers.put(key, dataConfig.getString("npcs." + key + ".server", "lobby"));
            }
        }

        maintenance = dataConfig.getBoolean("maintenance", false);
        totalPlayers = dataConfig.getInt("totalPlayers", 0);
    }

    public void saveData() {
        if (lobbySpawn != null) {
            dataConfig.set("lobby.world", lobbySpawn.getWorld().getName());
            dataConfig.set("lobby.x", lobbySpawn.getX());
            dataConfig.set("lobby.y", lobbySpawn.getY());
            dataConfig.set("lobby.z", lobbySpawn.getZ());
            dataConfig.set("lobby.yaw", lobbySpawn.getYaw());
            dataConfig.set("lobby.pitch", lobbySpawn.getPitch());
        }

        for (Map.Entry<String, Location> entry : modeSpawns.entrySet()) {
            String path = "spawns." + entry.getKey();
            dataConfig.set(path + ".world", entry.getValue().getWorld().getName());
            dataConfig.set(path + ".x", entry.getValue().getX());
            dataConfig.set(path + ".y", entry.getValue().getY());
            dataConfig.set(path + ".z", entry.getValue().getZ());
        }

        for (UUID uuid : ranks.keySet()) {
            String path = "stats." + uuid.toString();
            dataConfig.set(path + ".rank", ranks.get(uuid));
            dataConfig.set(path + ".kills", kills.getOrDefault(uuid, 0));
            dataConfig.set(path + ".deaths", deaths.getOrDefault(uuid, 0));
            dataConfig.set(path + ".playtime", playtime.getOrDefault(uuid, 0));
            dataConfig.set(path + ".joined", firstJoin.getOrDefault(uuid, false));
        }

        for (Map.Entry<String, String> entry : npcModes.entrySet()) {
            dataConfig.set("npcs." + entry.getKey() + ".mode", entry.getValue());
            dataConfig.set("npcs." + entry.getKey() + ".name", npcNames.get(entry.getKey()));
            dataConfig.set("npcs." + entry.getKey() + ".server", npcServers.getOrDefault(entry.getKey(), "lobby"));
        }

        dataConfig.set("maintenance", maintenance);
        dataConfig.set("totalPlayers", totalPlayers);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  JOIN / QUIT HANDLER
    // ════════════════════════════════════════════════════════════════

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        if (maintenance && !player.hasPermission("kzplugin.admin")) {
            player.kickPlayer("§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later.");
            return;
        }

        String platform = player.getName().startsWith(".") ? "Bedrock" : "Java";
        platforms.put(uuid, platform);

        applyRankPermissions(player);

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
            player.sendMessage("  §7▸ Click an NPC to select a game mode");
            player.sendMessage("  §7▸ Type §b/daily §7for daily rewards");
            player.sendMessage("  §7▸ Type §b/help §7for commands");
            player.sendMessage("");
            player.sendMessage("§b§l─────────────────────────────────── ");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            try {
                player.getWorld().spawn(player.getLocation(), Firework.class);
            } catch (Exception ignored) {}

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("  §a§l[+] §f" + player.getName() + " §7joined for the first time! §a🎉");
            Bukkit.broadcastMessage("");
        }, 20L);
    }

    private void handleReturningPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String rank = getRank(uuid);
            RankData rd = getRankData(rank);
            double bal = plugin.getEconomyManager().getBalance(player);
            String mode = plugin.getEconomyManager().getPlayerMode(player);

            player.sendMessage("");
            player.sendMessage("§b§l┌─────────────────────────────────┐");
            player.sendMessage("§b§l│   §f§lWELCOME BACK                  §b§l│");
            player.sendMessage("§b§l└─────────────────────────────────┘");
            player.sendMessage("");
            player.sendMessage("  §7Hey, §b" + player.getName() + "§7!");
            player.sendMessage("");
            player.sendMessage("  §f💰 Balance  : §a" + plugin.getEconomyManager().formatBalance(bal));
            player.sendMessage("  §f👑 Rank     : " + rd.chatTag());
            player.sendMessage("  §f🌍 Mode     : " + plugin.getEconomyManager().getModeName(mode));
            player.sendMessage("  §f🎮 Platform : §f" + platforms.getOrDefault(uuid, "Java"));
            player.sendMessage("");
            player.sendMessage("  §7Don't forget to claim §b/daily §7rewards!");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

            Bukkit.broadcastMessage("  §a§l[+] §f" + player.getName() + " §7joined the server.");
        }, 20L);
    }

    public void handleQuit(Player player) {
        Bukkit.broadcastMessage("  §c§l[-] §f" + player.getName() + " §7left the server.");
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
    //                    [Money]
    // ════════════════════════════════════════════════════════════════

    public void updateNametag(Player player) {
        UUID uuid = player.getUniqueId();
        String rank = getRank(uuid);
        RankData rd = getRankData(rank);
        int level = player.getLevel();
        double bal = plugin.getEconomyManager().getBalance(player);

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
        team.setSuffix("\n" + rd.color() + plugin.getEconomyManager().formatBalance(bal));
        team.setColor(ChatColor.valueOf(getColorName(rd.color())));

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        player.setScoreboard(board);

        player.setPlayerListName(
            "§7[§a" + level + "§7] " + rd.chatTag() + " " + rd.color() + player.getName()
        );

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
            default   -> "WHITE";
        };
    }


    // ════════════════════════════════════════════════════════════════
    //  SCOREBOARD - Enhanced
    // ════════════════════════════════════════════════════════════════

    public void updateScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        String rank = getRank(uuid);
        RankData rd = getRankData(rank);
        double bal = plugin.getEconomyManager().getBalance(player);
        String mode = plugin.getEconomyManager().getPlayerMode(player);
        String modeName = plugin.getEconomyManager().getModeName(mode);
        int level = player.getLevel();
        String job = plugin.getJobSystem().getJob(uuid);
        String jobDisplay = job != null ? capitalize(job) : "§cNone";
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

        Objective obj = board.registerNewObjective("kz_sb", "dummy", "§b§lKZ §f§lSERVER");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = 15;

        obj.getScore("§b§l─────────────────").setScore(score--);
        obj.getScore("§f Player: " + rd.color() + player.getName()).setScore(score--);
        obj.getScore("§f Rank: " + rd.chatTag()).setScore(score--);
        obj.getScore("§f Level: §a" + level).setScore(score--);
        obj.getScore("§6§l─────────────────").setScore(score--);
        obj.getScore("§f Mode: " + modeName).setScore(score--);
        obj.getScore("§f Balance: §a" + plugin.getEconomyManager().formatBalance(bal)).setScore(score--);
        obj.getScore("§f Job: §e" + jobDisplay).setScore(score--);
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
    //  NPC SYSTEM - Now with Velocity server transfer
    // ════════════════════════════════════════════════════════════════

    public void createNPC(Player player, String mode, String displayName, String targetServer) {
        ArmorStand npc = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        npc.setCustomName("§b§l" + displayName);
        npc.setCustomNameVisible(true);
        npc.setGravity(false);
        npc.setVisible(true);
        npc.setInvulnerable(true);

        String entityId = npc.getUniqueId().toString();
        npcModes.put(entityId, mode);
        npcNames.put(entityId, displayName);
        npcServers.put(entityId, targetServer);

        player.sendMessage("");
        player.sendMessage("§a§l┌─────────────────────────────────┐");
        player.sendMessage("§a§l│        §f§lNPC CREATED               §a§l│");
        player.sendMessage("§a§l└─────────────────────────────────┘");
        player.sendMessage("  §7Name   : §b" + displayName);
        player.sendMessage("  §7Mode   : §f" + mode);
        player.sendMessage("  §7Server : §e" + targetServer);
        player.sendMessage("  §7UUID   : §8" + entityId);
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        saveData();
    }

    public void createNPC(Player player, String mode, String displayName) {
        String server = getServerForMode(mode);
        createNPC(player, mode, displayName, server);
    }

    public String getServerForMode(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival", "vanilla" -> "survival";
            case "oneblock", "skyblock" -> "void";
            case "island", "acid" -> "custom";
            default -> "lobby";
        };
    }

    public void removeNearbyNPC(Player player) {
        int removed = 0;
        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof ArmorStand) {
                String entityId = entity.getUniqueId().toString();
                if (npcModes.containsKey(entityId)) {
                    npcModes.remove(entityId);
                    npcNames.remove(entityId);
                    npcServers.remove(entityId);
                    entity.remove();
                    removed++;
                }
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

    public void handleNPCClick(Player player, Entity entity) {
        String entityId = entity.getUniqueId().toString();
        if (!npcModes.containsKey(entityId)) return;

        String mode = npcModes.get(entityId);
        String targetServer = npcServers.getOrDefault(entityId, getServerForMode(mode));
        String currentServer = plugin.getConfig().getString("server-name", "lobby");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        if (!currentServer.equalsIgnoreCase(targetServer)) {
            player.sendMessage("§a§lKZ §8» §7Connecting to §f" + capitalize(mode) + " §7server...");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            ServerUtils.sendToServer(plugin, player, targetServer);
            return;
        }

        UUID uuid = player.getUniqueId();
        if (plugin.getIslandSystem().hasIsland(uuid)) {
            IslandSystem.IslandData island = plugin.getIslandSystem().getIsland(uuid);
            if (island.mode.equalsIgnoreCase(mode)) {
                player.teleport(island.spawnPoint);
                player.sendMessage("§a§lKZ §8» §7Teleported to your §f" + capitalize(mode) + " §7island.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                player.sendMessage("§6§lKZ §8» §7You already have a §f" + capitalize(island.mode) + " §7island.");
                player.sendMessage("  §7Delete it first: §c/deleteisland");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return;
        }

        plugin.getIslandSystem().createIsland(player, mode);
    }

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
                String name = npcNames.getOrDefault(entry.getKey(), "Unknown");
                String server = npcServers.getOrDefault(entry.getKey(), "?");
                player.sendMessage("  §7" + count + ". §b" + name
                    + " §8| §7Mode: §f" + entry.getValue()
                    + " §8| §7Server: §e" + server);
            }
        }

        player.sendMessage("");
    }

    public boolean isNPC(Entity entity) {
        return npcModes.containsKey(entity.getUniqueId().toString());
    }

    // ════════════════════════════════════════════════════════════════
    //  RANK SYSTEM - Real permission management
    // ════════════════════════════════════════════════════════════════

    public void applyRankPermissions(Player player) {
        UUID uuid = player.getUniqueId();
        String rank = getRank(uuid).toLowerCase();
        RankData rd = getRankData(rank);

        RankData memberData = rankConfig.get("member");
        if (memberData != null) {
            for (String perm : memberData.permissions()) {
                if (!player.hasPermission(perm)) {
                    player.addAttachment(plugin, perm, true);
                }
            }
        }

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
        RankData rd = getRankData(rank);
        return rd.chatTag();
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
        Bukkit.broadcastMessage("§c§lKZ §8» §7Ground items will be cleared in §f30 seconds§7...");

        Bukkit.getScheduler().runTaskLater(plugin, () ->
            Bukkit.broadcastMessage("§c§lKZ §8» §c10 seconds! Pick up your items!"), 400L);

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
            Bukkit.broadcastMessage("§a§lKZ §8» §a" + count + " §7items cleared. ✨");
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

    public void addKill(UUID uuid) { kills.merge(uuid, 1, Integer::sum); }

    public void addDeath(UUID uuid) { deaths.merge(uuid, 1, Integer::sum); }

    public void showStats(Player player, Player target) {
        UUID uuid = target.getUniqueId();

        String rank = getRank(uuid);
        RankData rd = getRankData(rank);
        int k = kills.getOrDefault(uuid, 0);
        int d = deaths.getOrDefault(uuid, 0);
        int pt = playtime.getOrDefault(uuid, 0);
        String platform = platforms.getOrDefault(uuid, "Unknown");
        String job = plugin.getJobSystem().getJob(uuid);
        double bal = plugin.getEconomyManager().getBalance(target);
        double kd = d > 0 ? (double) k / d : k;

        player.sendMessage("");
        player.sendMessage("§b§l┌─────────────────────────────────┐");
        player.sendMessage("§b§l│      §f§lPLAYER STATISTICS          §b§l│");
        player.sendMessage("§b§l└─────────────────────────────────┘");
        player.sendMessage("");
        player.sendMessage("  §f👤 Player   : " + rd.color() + target.getName());
        player.sendMessage("  §f👑 Rank     : " + rd.chatTag());
        player.sendMessage("  §f🎮 Platform : §f" + platform);
        player.sendMessage("");
        player.sendMessage("  §f💰 Balance  : §a" + plugin.getEconomyManager().formatBalance(bal));
        player.sendMessage("  §f💼 Job      : §f" + (job != null ? capitalize(job) : "None"));
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

    public Location getLobbySpawn() { return lobbySpawn; }

    public void setLobbySpawn(Location loc) {
        lobbySpawn = loc;
        saveData();
    }

    public void setModeSpawn(String mode, Location loc) {
        modeSpawns.put(mode, loc);
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

    public boolean isMaintenance() { return maintenance; }

    public void setMaintenance(boolean val) {
        maintenance = val;
        saveData();
    }

    public int getTotalPlayers() { return totalPlayers; }

    public Map<String, RankData> getAllRanks() {
        return Collections.unmodifiableMap(rankConfig);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
