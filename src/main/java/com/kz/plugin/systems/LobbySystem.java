package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
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
    private final Map<String, String> npcModes = new HashMap<>(); // entityUUID -> mode
    private final Map<String, String> npcNames = new HashMap<>(); // entityUUID -> displayname

    private boolean maintenance = false;
    private int totalPlayers = 0;

    public LobbySystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

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
            for (String mode : dataConfig.getConfigurationSection("spawns").getKeys(false)) {
                World w = Bukkit.getWorld(dataConfig.getString("spawns." + mode + ".world", "world"));
                if (w != null) {
                    modeSpawns.put(mode, new Location(w,
                        dataConfig.getDouble("spawns." + mode + ".x"),
                        dataConfig.getDouble("spawns." + mode + ".y"),
                        dataConfig.getDouble("spawns." + mode + ".z")));
                }
            }
        }

        // Load player stats
        if (dataConfig.contains("stats")) {
            for (String key : dataConfig.getConfigurationSection("stats").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                ranks.put(uuid, dataConfig.getString("stats." + key + ".rank", "Member"));
                kills.put(uuid, dataConfig.getInt("stats." + key + ".kills", 0));
                deaths.put(uuid, dataConfig.getInt("stats." + key + ".deaths", 0));
                playtime.put(uuid, dataConfig.getInt("stats." + key + ".playtime", 0));
                firstJoin.put(uuid, dataConfig.getBoolean("stats." + key + ".joined", false));
            }
        }

        // Load NPCs
        if (dataConfig.contains("npcs")) {
            for (String key : dataConfig.getConfigurationSection("npcs").getKeys(false)) {
                npcModes.put(key, dataConfig.getString("npcs." + key + ".mode"));
                npcNames.put(key, dataConfig.getString("npcs." + key + ".name"));
            }
        }

        maintenance = dataConfig.getBoolean("maintenance", false);
        totalPlayers = dataConfig.getInt("totalPlayers", 0);
    }

    public void saveData() {
        // Save lobby
        if (lobbySpawn != null) {
            dataConfig.set("lobby.world", lobbySpawn.getWorld().getName());
            dataConfig.set("lobby.x", lobbySpawn.getX());
            dataConfig.set("lobby.y", lobbySpawn.getY());
            dataConfig.set("lobby.z", lobbySpawn.getZ());
            dataConfig.set("lobby.yaw", lobbySpawn.getYaw());
            dataConfig.set("lobby.pitch", lobbySpawn.getPitch());
        }

        // Save spawns
        for (Map.Entry<String, Location> entry : modeSpawns.entrySet()) {
            String path = "spawns." + entry.getKey();
            dataConfig.set(path + ".world", entry.getValue().getWorld().getName());
            dataConfig.set(path + ".x", entry.getValue().getX());
            dataConfig.set(path + ".y", entry.getValue().getY());
            dataConfig.set(path + ".z", entry.getValue().getZ());
        }

        // Save stats
        for (UUID uuid : ranks.keySet()) {
            String path = "stats." + uuid.toString();
            dataConfig.set(path + ".rank", ranks.get(uuid));
            dataConfig.set(path + ".kills", kills.getOrDefault(uuid, 0));
            dataConfig.set(path + ".deaths", deaths.getOrDefault(uuid, 0));
            dataConfig.set(path + ".playtime", playtime.getOrDefault(uuid, 0));
            dataConfig.set(path + ".joined", firstJoin.getOrDefault(uuid, false));
        }

        // Save NPCs
        for (Map.Entry<String, String> entry : npcModes.entrySet()) {
            dataConfig.set("npcs." + entry.getKey() + ".mode", entry.getValue());
            dataConfig.set("npcs." + entry.getKey() + ".name", npcNames.get(entry.getKey()));
        }

        dataConfig.set("maintenance", maintenance);
        dataConfig.set("totalPlayers", totalPlayers);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════
    //  JOIN HANDLER
    // ══════════════════════════════════════

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();

        // Teleport to lobby
        if (lobbySpawn != null) {
            player.teleport(lobbySpawn);
        }

        // Maintenance check
        if (maintenance && !player.hasPermission("kzplugin.admin")) {
            player.kickPlayer("§c§lKZ SERVER\n\n§7Server is under maintenance.\n§7Please try again later.");
            return;
        }

        // Platform detection
        String platform = player.getName().startsWith(".") ? "Bedrock" : "Java";
        platforms.put(uuid, platform);

        // First join check
        if (!firstJoin.getOrDefault(uuid, false)) {
            handleFirstJoin(player);
        } else {
            handleReturningPlayer(player);
        }
    }

    private void handleFirstJoin(Player player) {
        UUID uuid = player.getUniqueId();

        firstJoin.put(uuid, true);
        ranks.put(uuid, "Member");
        kills.put(uuid, 0);
        deaths.put(uuid, 0);
        playtime.put(uuid, 0);
        totalPlayers++;

        // Init economy for lobby
        plugin.getEconomyManager().initPlayer(uuid, "lobby");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§b§l  WELCOME TO §f§lKZ SERVER");
            player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");
            player.sendMessage("§7  Greetings, §b" + player.getName() + "§7.");
            player.sendMessage("§7  Welcome to KZ Minecraft Server.");
            player.sendMessage("");
            player.sendMessage("§7  §f💰 Starting Balance : §a$1,000");
            player.sendMessage("§7  §f👑 Rank             : §fMember");
            player.sendMessage("§7  §f🎮 Platform         : §f" + platforms.get(uuid));
            player.sendMessage("");
            player.sendMessage("§7  §fQuick Start:");
            player.sendMessage("§7  ▸ Click an NPC to select a game mode.");
            player.sendMessage("§7  ▸ Type §b/daily §7for daily rewards.");
            player.sendMessage("§7  ▸ Type §b/help §7for a full command list.");
            player.sendMessage("");
            player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            try {
                player.getWorld().spawn(player.getLocation(), Firework.class);
            } catch (Exception ignored) {}

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§a§l[+] §f" + player.getName() + " §7has joined for the first time! Welcome! 🎉");
            Bukkit.broadcastMessage("");
        }, 20L);
    }

    private void handleReturningPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String rank = ranks.getOrDefault(uuid, "Member");
            double bal = plugin.getEconomyManager().getBalance(uuid);

            player.sendMessage("");
            player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§b§l  WELCOME BACK §f§l" + player.getName().toUpperCase());
            player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");
            player.sendMessage("§7  §f💰 Balance  : §a" + plugin.getEconomyManager().formatBalance(bal));
            player.sendMessage("§7  §f👑 Rank     : §f" + rank);
            player.sendMessage("§7  §f🎮 Platform : §f" + platforms.getOrDefault(uuid, "Java"));
            player.sendMessage("");
            player.sendMessage("§7  Don't forget to claim §b/daily §7rewards.");
            player.sendMessage("");
            player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);

            Bukkit.broadcastMessage("§a§l[+] §f" + player.getName() + " §7joined the server.");
        }, 20L);
    }

    public void handleQuit(Player player) {
        Bukkit.broadcastMessage("§c§l[-] §f" + player.getName() + " §7left the server.");
        saveData();
    }

    // ══════════════════════════════════════
    //  SCOREBOARD
    // ══════════════════════════════════════

    public void updateScoreboard() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            String rank = ranks.getOrDefault(uuid, "Member");
            double bal = plugin.getEconomyManager().getBalance(uuid);
            String mode = plugin.getEconomyManager().getPlayerMode(uuid);
            String modeName = plugin.getEconomyManager().getModeName(mode);

            ScoreboardManager manager = Bukkit.getScoreboardManager();
            if (manager == null) continue;

            Scoreboard board = manager.getNewScoreboard();
            Objective obj = board.registerNewObjective("kz", "dummy",
                "§b§lKZ SERVER");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            obj.getScore("§b§m                ").setScore(8);
            obj.getScore("§f " + player.getName()).setScore(7);
            obj.getScore("§7 Rank: §b" + rank).setScore(6);
            obj.getScore("§6§m                ").setScore(5);
            obj.getScore("§a $" + plugin.getEconomyManager().formatBalance(bal)).setScore(4);
            obj.getScore("§7 " + modeName).setScore(3);
            obj.getScore("§e§m                ").setScore(2);
            obj.getScore("§e " + Bukkit.getOnlinePlayers().size() + "§7/§f" +
                Bukkit.getMaxPlayers()).setScore(1);

            player.setScoreboard(board);
        }
    }

    // ══════════════════════════════════════
    //  NPC SYSTEM
    // ══════════════════════════════════════

    public void createNPC(Player player, String mode, String displayName) {
        ArmorStand npc = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        npc.setCustomName("§b§l" + displayName);
        npc.setCustomNameVisible(true);
        npc.setGravity(false);
        npc.setVisible(true);
        npc.setInvulnerable(true);

        String entityId = npc.getUniqueId().toString();
        npcModes.put(entityId, mode);
        npcNames.put(entityId, displayName);

        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  NPC CREATED");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Name : §b" + displayName);
        player.sendMessage("§7  Mode : §f" + mode);
        player.sendMessage("§7  UUID : §f" + entityId);
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        saveData();
    }

    public void removeNearbyNPC(Player player) {
        int removed = 0;
        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof ArmorStand) {
                String entityId = entity.getUniqueId().toString();
                if (npcModes.containsKey(entityId)) {
                    npcModes.remove(entityId);
                    npcNames.remove(entityId);
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
        UUID uuid = player.getUniqueId();

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        // Already has island of same mode? → Teleport
        if (plugin.getIslandSystem().hasIsland(uuid)) {
            IslandSystem.IslandData island = plugin.getIslandSystem().getIsland(uuid);
            if (island.mode.equalsIgnoreCase(mode)) {
                player.teleport(island.spawnPoint);
                player.sendMessage("§a§lKZ §8» §7Teleported to your §f" + capitalize(mode) + " §7island.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                player.sendMessage("");
                player.sendMessage("§6§lKZ §8» §7You already have a §f" + capitalize(island.mode) + " §7island.");
                player.sendMessage("§7  Delete it first: §c/deleteisland");
                player.sendMessage("");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
            return;
        }

        // No island? → Auto-create!
        plugin.getIslandSystem().createIsland(player, mode);
    }

    // ══════════════════════════════════════
    //  NPC LIST
    // ══════════════════════════════════════

    public void listNPCs(Player player) {
        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  NPC LIST");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        if (npcModes.isEmpty()) {
            player.sendMessage("§7  No NPCs registered.");
        } else {
            int count = 0;
            for (Map.Entry<String, String> entry : npcModes.entrySet()) {
                count++;
                String name = npcNames.getOrDefault(entry.getKey(), "Unknown");
                player.sendMessage("§7  " + count + ". §b" + name + " §8| §7Mode: §f" + entry.getValue());
            }
        }

        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
    }

    // ══════════════════════════════════════
    //  NPC PROTECTION
    // ══════════════════════════════════════

    public boolean isNPC(Entity entity) {
        return npcModes.containsKey(entity.getUniqueId().toString());
    }

    // ══════════════════════════════════════
    //  PLAYTIME TRACKER
    // ══════════════════════════════════════

    public void trackPlaytime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            playtime.merge(uuid, 1, Integer::sum);
        }
    }

    // ══════════════════════════════════════
    //  CLEARLAG
    // ══════════════════════════════════════

    public void clearLag() {
        Bukkit.broadcastMessage("§c§lKZ §8» §7Ground items cleared in §f30 seconds§7...");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.broadcastMessage("§c§lKZ §8» §c10 seconds remaining! Pick up your items!");
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
            Bukkit.broadcastMessage("§a§lKZ §8» §a" + count + " §7items cleared. ✨");
        }, 600L);
    }

    // ══════════════════════════════════════
    //  FIREWORKS
    // ══════════════════════════════════════

    public void spawnFireworks() {
        if (lobbySpawn == null) return;
        try {
            for (int i = 0; i < 4; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    lobbySpawn.getWorld().spawn(lobbySpawn, Firework.class);
                }, i * 10L);
            }
        } catch (Exception ignored) {}
    }

    // ══════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════

    public void addKill(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
    }

    public void addDeath(UUID uuid) {
        deaths.merge(uuid, 1, Integer::sum);
    }

    public void showStats(Player player, Player target) {
        UUID uuid = target.getUniqueId();

        String rank = ranks.getOrDefault(uuid, "Member");
        int k = kills.getOrDefault(uuid, 0);
        int d = deaths.getOrDefault(uuid, 0);
        int pt = playtime.getOrDefault(uuid, 0);
        String platform = platforms.getOrDefault(uuid, "Unknown");
        String job = plugin.getJobSystem().getJob(uuid);
        double bal = plugin.getEconomyManager().getBalance(uuid);

        double kd = d > 0 ? (double) k / d : k;

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  PLAYER STATISTICS");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.sendMessage("§7  §f👤 Player   : §f" + target.getName());
        player.sendMessage("§7  §f👑 Rank     : §b" + rank);
        player.sendMessage("§7  §f🎮 Platform : §f" + platform);
        player.sendMessage("");
        player.sendMessage("§7  §f💰 Balance  : §a" + plugin.getEconomyManager().formatBalance(bal));
        player.sendMessage("§7  §f💼 Job      : §f" + (job != null ? capitalize(job) : "None"));
        player.sendMessage("");
        player.sendMessage("§7  §f⚔ Kills    : §a" + k);
        player.sendMessage("§7  §f💀 Deaths   : §c" + d);
        player.sendMessage("§7  §f📊 K/D      : §e" + String.format("%.2f", kd));
        player.sendMessage("§7  §f⏱ Playtime : §f" + pt + " minutes");
        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  GETTERS & SETTERS
    // ══════════════════════════════════════

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
        ranks.put(uuid, rank);
        saveData();
    }

    public boolean isMaintenance() { return maintenance; }

    public void setMaintenance(boolean val) {
        maintenance = val;
        saveData();
    }

    public int getTotalPlayers() { return totalPlayers; }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
