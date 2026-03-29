package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IslandSystem {

    private final KZPlugin plugin;
    private File islandFile;
    private FileConfiguration islandConfig;

    // Island data in memory
    private final Map<UUID, IslandData> islands = new HashMap<>();
    private final Map<UUID, UUID> memberOf = new HashMap<>(); // member -> owner
    private final Map<UUID, UUID> pendingInvites = new HashMap<>(); // target -> owner
    private int nextX = 5000;

    public static class IslandData {
        public UUID owner;
        public String mode;
        public Location center;
        public Location spawnPoint;
        public String name;
        public int border;
        public int blocksBroken;
        public boolean active;
        public List<UUID> team = new ArrayList<>();
        public List<UUID> trusted = new ArrayList<>();

        public IslandData(UUID owner, String mode, Location center, Location spawnPoint) {
            this.owner = owner;
            this.mode = mode;
            this.center = center;
            this.spawnPoint = spawnPoint;
            this.name = "Unnamed Island";
            this.border = 25;
            this.blocksBroken = 0;
            this.active = true;
        }
    }

    public IslandSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ══════════════════════════════════════
    //  SAVE & LOAD
    // ══════════════════════════════════════

    private void loadData() {
        islandFile = new File(plugin.getDataFolder(), "islands.yml");
        if (!islandFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                islandFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        islandConfig = YamlConfiguration.loadConfiguration(islandFile);

        nextX = islandConfig.getInt("nextX", 5000);

        if (islandConfig.contains("islands")) {
            for (String key : islandConfig.getConfigurationSection("islands").getKeys(false)) {
                String path = "islands." + key;
                UUID uuid = UUID.fromString(key);

                World world = Bukkit.getWorld(islandConfig.getString(path + ".world", "world"));
                if (world == null) world = Bukkit.getWorlds().get(0);

                Location center = new Location(world,
                    islandConfig.getDouble(path + ".cx"),
                    islandConfig.getDouble(path + ".cy"),
                    islandConfig.getDouble(path + ".cz"));
                Location spawn = new Location(world,
                    islandConfig.getDouble(path + ".sx"),
                    islandConfig.getDouble(path + ".sy"),
                    islandConfig.getDouble(path + ".sz"));

                IslandData data = new IslandData(uuid,
                    islandConfig.getString(path + ".mode", "oneblock"),
                    center, spawn);
                data.name = islandConfig.getString(path + ".name", "Unnamed Island");
                data.border = islandConfig.getInt(path + ".border", 25);
                data.blocksBroken = islandConfig.getInt(path + ".broken", 0);
                data.active = islandConfig.getBoolean(path + ".active", true);

                List<String> teamList = islandConfig.getStringList(path + ".team");
                for (String t : teamList) {
                    UUID memberUUID = UUID.fromString(t);
                    data.team.add(memberUUID);
                    memberOf.put(memberUUID, uuid);
                }

                List<String> trustList = islandConfig.getStringList(path + ".trusted");
                for (String t : trustList) {
                    data.trusted.add(UUID.fromString(t));
                }

                islands.put(uuid, data);
            }
        }
    }

    public void saveAll() {
        islandConfig.set("nextX", nextX);

        for (Map.Entry<UUID, IslandData> entry : islands.entrySet()) {
            String path = "islands." + entry.getKey().toString();
            IslandData data = entry.getValue();

            islandConfig.set(path + ".mode", data.mode);
            islandConfig.set(path + ".world", data.center.getWorld().getName());
            islandConfig.set(path + ".cx", data.center.getX());
            islandConfig.set(path + ".cy", data.center.getY());
            islandConfig.set(path + ".cz", data.center.getZ());
            islandConfig.set(path + ".sx", data.spawnPoint.getX());
            islandConfig.set(path + ".sy", data.spawnPoint.getY());
            islandConfig.set(path + ".sz", data.spawnPoint.getZ());
            islandConfig.set(path + ".name", data.name);
            islandConfig.set(path + ".border", data.border);
            islandConfig.set(path + ".broken", data.blocksBroken);
            islandConfig.set(path + ".active", data.active);

            List<String> teamList = new ArrayList<>();
            for (UUID t : data.team) teamList.add(t.toString());
            islandConfig.set(path + ".team", teamList);

            List<String> trustList = new ArrayList<>();
            for (UUID t : data.trusted) trustList.add(t.toString());
            islandConfig.set(path + ".trusted", trustList);
        }

        try {
            islandConfig.save(islandFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════
    //  CREATE ISLAND
    // ══════════════════════════════════════

    public boolean createIsland(Player player, String mode) {
        UUID uuid = player.getUniqueId();

        if (islands.containsKey(uuid) && islands.get(uuid).active) {
            player.sendMessage("§c§lKZ §8» §7You already have an island.");
            player.sendMessage("§7  Use §f/deleteisland §7to remove it first.");
            return false;
        }

        World world = Bukkit.getWorld("world");
        if (world == null) world = Bukkit.getWorlds().get(0);

        int x = nextX;
        int y = 100;
        int z = 5000;

        Location center = new Location(world, x, y, z);
        Location spawnPoint = new Location(world, x + 0.5, y + 1, z + 0.5);

        switch (mode.toLowerCase()) {
            case "oneblock":
                generateOneBlock(center);
                break;
            case "skyblock":
                generateSkyblock(center, world);
                spawnPoint = new Location(world, x + 2.5, y + 2, z + 2.5);
                break;
            case "acid":
                generateAcid(center, world);
                spawnPoint = new Location(world, x + 2.5, y + 2, z + 2.5);
                break;
            case "island":
                generateClassicIsland(center, world);
                spawnPoint = new Location(world, x + 3.5, y + 4, z + 3.5);
                break;
            default:
                player.sendMessage("§c§lKZ §8» §7Invalid mode. Available: §foneblock§7, §fskyblock§7, §facid§7, §fisland");
                return false;
        }

        IslandData data = new IslandData(uuid, mode, center, spawnPoint);

        // Init balance
        plugin.getEconomyManager().initPlayer(uuid, 1000);

        islands.put(uuid, data);
        nextX += 500;

        // Teleport
        player.teleport(spawnPoint);

        // Messages
        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  ISLAND CREATED SUCCESSFULLY");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Mode    : §f" + capitalize(mode));
        player.sendMessage("§7  Border  : §f" + data.border + " blocks");
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(uuid)));
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        return true;
    }

    // ══════════════════════════════════════
    //  GENERATE ISLAND TEMPLATES
    // ══════════════════════════════════════

    private void generateOneBlock(Location center) {
        center.getBlock().setType(Material.GRASS_BLOCK);
    }

    private void generateSkyblock(Location center, World world) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Platform 5x5
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                world.getBlockAt(x, cy, z).setType(Material.DIRT);
            }
        }

        // Top layer grass
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                world.getBlockAt(x, cy + 1, z).setType(Material.GRASS_BLOCK);
            }
        }

        // Bedrock core
        world.getBlockAt(cx, cy - 1, cz).setType(Material.BEDROCK);

        // Oak tree
        world.getBlockAt(cx, cy + 2, cz).setType(Material.OAK_LOG);
        world.getBlockAt(cx, cy + 3, cz).setType(Material.OAK_LOG);
        world.getBlockAt(cx, cy + 4, cz).setType(Material.OAK_LOG);
        world.getBlockAt(cx, cy + 5, cz).setType(Material.OAK_LOG);

        // Leaves
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                if (world.getBlockAt(x, cy + 5, z).getType() == Material.AIR)
                    world.getBlockAt(x, cy + 5, z).setType(Material.OAK_LEAVES);
                if (world.getBlockAt(x, cy + 6, z).getType() == Material.AIR)
                    world.getBlockAt(x, cy + 6, z).setType(Material.OAK_LEAVES);
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                world.getBlockAt(x, cy + 7, z).setType(Material.OAK_LEAVES);
            }
        }

        // Chest with starter items
        world.getBlockAt(cx + 2, cy + 2, cz).setType(Material.CHEST);
        Block chestBlock = world.getBlockAt(cx + 2, cy + 2, cz);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            Inventory inv = chest.getInventory();
            inv.addItem(new ItemStack(Material.ICE, 2));
            inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            inv.addItem(new ItemStack(Material.MELON_SEEDS, 1));
            inv.addItem(new ItemStack(Material.PUMPKIN_SEEDS, 1));
            inv.addItem(new ItemStack(Material.SUGAR_CANE, 1));
            inv.addItem(new ItemStack(Material.OAK_SAPLING, 2));
            inv.addItem(new ItemStack(Material.BONE_MEAL, 16));
            inv.addItem(new ItemStack(Material.COOKED_BEEF, 16));
            inv.addItem(new ItemStack(Material.WHEAT_SEEDS, 4));
            chest.update();
        }
    }

    private void generateAcid(Location center, World world) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Small platform 3x3
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                world.getBlockAt(x, cy, z).setType(Material.DIRT);
                world.getBlockAt(x, cy + 1, z).setType(Material.GRASS_BLOCK);
            }
        }

        // Bedrock base
        world.getBlockAt(cx, cy - 1, cz).setType(Material.BEDROCK);

        // Acid water around (10 block radius, y-1)
        for (int x = cx - 10; x <= cx + 10; x++) {
            for (int z = cz - 10; z <= cz + 10; z++) {
                if (Math.abs(x - cx) > 1 || Math.abs(z - cz) > 1) {
                    world.getBlockAt(x, cy, z).setType(Material.WATER);
                }
            }
        }

        // Oak sapling
        world.getBlockAt(cx, cy + 2, cz).setType(Material.OAK_SAPLING);

        // Chest
        world.getBlockAt(cx + 1, cy + 2, cz).setType(Material.CHEST);
        Block chestBlock = world.getBlockAt(cx + 1, cy + 2, cz);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            Inventory inv = chest.getInventory();
            inv.addItem(new ItemStack(Material.ICE, 2));
            inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            inv.addItem(new ItemStack(Material.BONE_MEAL, 16));
            inv.addItem(new ItemStack(Material.COOKED_BEEF, 8));
            chest.update();
        }
    }

    private void generateClassicIsland(Location center, World world) {
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Large platform 7x7 (3 layers)
        for (int y = cy; y <= cy + 2; y++) {
            int radius = (y == cy) ? 3 : ((y == cy + 1) ? 3 : 3);
            Material mat = (y == cy) ? Material.STONE :
                           (y == cy + 1) ? Material.DIRT : Material.GRASS_BLOCK;

            for (int x = cx - radius; x <= cx + radius; x++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double dist = Math.sqrt(Math.pow(x - cx, 2) + Math.pow(z - cz, 2));
                    if (dist <= radius + 0.5) {
                        world.getBlockAt(x, y, z).setType(mat);
                    }
                }
            }
        }

        // Bedrock core
        world.getBlockAt(cx, cy - 1, cz).setType(Material.BEDROCK);

        // Oak tree
        for (int y = cy + 3; y <= cy + 6; y++) {
            world.getBlockAt(cx, y, cz).setType(Material.OAK_LOG);
        }

        // Leaves
        for (int x = cx - 2; x <= cx + 2; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                for (int y = cy + 5; y <= cy + 7; y++) {
                    if (world.getBlockAt(x, y, z).getType() == Material.AIR) {
                        world.getBlockAt(x, y, z).setType(Material.OAK_LEAVES);
                    }
                }
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                world.getBlockAt(x, cy + 8, z).setType(Material.OAK_LEAVES);
            }
        }

        // Chest with good starter items
        world.getBlockAt(cx + 3, cy + 3, cz).setType(Material.CHEST);
        Block chestBlock = world.getBlockAt(cx + 3, cy + 3, cz);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            Inventory inv = chest.getInventory();
            inv.addItem(new ItemStack(Material.ICE, 4));
            inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            inv.addItem(new ItemStack(Material.WATER_BUCKET, 1));
            inv.addItem(new ItemStack(Material.OAK_SAPLING, 4));
            inv.addItem(new ItemStack(Material.BONE_MEAL, 32));
            inv.addItem(new ItemStack(Material.COOKED_BEEF, 32));
            inv.addItem(new ItemStack(Material.WHEAT_SEEDS, 8));
            inv.addItem(new ItemStack(Material.MELON_SEEDS, 4));
            inv.addItem(new ItemStack(Material.PUMPKIN_SEEDS, 4));
            inv.addItem(new ItemStack(Material.SUGAR_CANE, 4));
            inv.addItem(new ItemStack(Material.COBBLESTONE, 32));
            inv.addItem(new ItemStack(Material.WOODEN_PICKAXE, 1));
            chest.update();
        }
    }

    // ══════════════════════════════════════
    //  DELETE ISLAND
    // ══════════════════════════════════════

    public boolean deleteIsland(Player player) {
        UUID uuid = player.getUniqueId();
        IslandData data = islands.get(uuid);

        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return false;
        }

        // Notify team members
        for (UUID memberUUID : data.team) {
            memberOf.remove(memberUUID);
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null && member.isOnline()) {
                member.sendMessage("§c§lKZ §8» §7The island owner has deleted the island.");
                Location lobby = plugin.getLobbySystem().getLobbySpawn();
                if (lobby != null) member.teleport(lobby);
            }
        }

        // Clear data
        data.active = false;
        data.team.clear();
        data.trusted.clear();

        // Remove from config
        islandConfig.set("islands." + uuid.toString(), null);

        // Teleport to lobby
        Location lobby = plugin.getLobbySystem().getLobbySpawn();
        if (lobby != null) {
            player.teleport(lobby);
        }

        player.sendMessage("");
        player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  ISLAND DELETED");
        player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Your island has been removed.");
        player.sendMessage("§7  All team members have been notified.");
        player.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        return true;
    }

    // ══════════════════════════════════════
    //  HOME (TELEPORT)
    // ══════════════════════════════════════

    public void teleportHome(Player player) {
        UUID uuid = player.getUniqueId();

        // Check if member of someone else's island
        if (memberOf.containsKey(uuid)) {
            UUID ownerUUID = memberOf.get(uuid);
            IslandData ownerIsland = islands.get(ownerUUID);
            if (ownerIsland != null && ownerIsland.active) {
                player.teleport(ownerIsland.spawnPoint);
                player.sendMessage("§a§lKZ §8» §7Teleported to team island.");
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                return;
            }
        }

        IslandData data = islands.get(uuid);
        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        player.teleport(data.spawnPoint);
        player.sendMessage("§a§lKZ §8» §7Teleported to your island.");
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  UPGRADE ISLAND BORDER
    // ══════════════════════════════════════

    public void upgradeIsland(Player player) {
        UUID uuid = player.getUniqueId();
        IslandData data = islands.get(uuid);

        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        int cost = data.border * 100;

        if (!plugin.getEconomyManager().hasEnough(uuid, cost)) {
            player.sendMessage("§c§lKZ §8» §7Insufficient balance. Required: §a$" + cost);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        plugin.getEconomyManager().removeBalance(uuid, cost);
        data.border += 5;

        player.sendMessage("");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  ISLAND UPGRADED");
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Border  : §f" + data.border + " blocks");
        player.sendMessage("§7  Cost    : §c-$" + cost);
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(uuid)));
        player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  ISLAND INFO
    // ══════════════════════════════════════

    public void showInfo(Player player) {
        UUID uuid = player.getUniqueId();
        IslandData data = islands.get(uuid);

        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        int upgradeCost = data.border * 100;

        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  ISLAND INFORMATION");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Name    : §f" + data.name);
        player.sendMessage("§7  Mode    : §f" + capitalize(data.mode));
        player.sendMessage("§7  Border  : §f" + data.border + " blocks");
        player.sendMessage("§7  Upgrade : §a$" + upgradeCost);
        player.sendMessage("§7  Team    : §f" + data.team.size() + " members");
        player.sendMessage("§7  Trusted : §f" + data.trusted.size() + " players");
        player.sendMessage("");

        if (data.mode.equalsIgnoreCase("oneblock")) {
            player.sendMessage(plugin.getOneBlockSystem().getPhaseInfo(data.blocksBroken));
        }

        player.sendMessage("");
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(uuid)));
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  VISIT
    // ══════════════════════════════════════

    public void visitIsland(Player player, Player target) {
        UUID targetUUID = target.getUniqueId();
        IslandData data = islands.get(targetUUID);

        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7" + target.getName() + " does not have an island.");
            return;
        }

        player.teleport(data.spawnPoint);

        String islandName = data.name.equals("Unnamed Island") ?
            "Island of " + target.getName() : data.name;

        player.sendMessage("§a§lKZ §8» §7Visiting §f" + islandName + "§7.");
        target.sendMessage("§a§lKZ §8» §f" + player.getName() + " §7is visiting your island.");

        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  NAME ISLAND
    // ══════════════════════════════════════

    public void setIslandName(Player player, String name) {
        UUID uuid = player.getUniqueId();
        IslandData data = islands.get(uuid);

        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        data.name = name;
        player.sendMessage("§a§lKZ §8» §7Island renamed to §f" + name + "§7.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  INVITE SYSTEM
    // ══════════════════════════════════════

    public void invitePlayer(Player owner, Player target) {
        UUID ownerUUID = owner.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        IslandData data = islands.get(ownerUUID);

        if (data == null || !data.active) {
            owner.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        if (data.team.contains(targetUUID)) {
            owner.sendMessage("§c§lKZ §8» §7" + target.getName() + " is already a team member.");
            return;
        }

        if (pendingInvites.containsKey(targetUUID)) {
            owner.sendMessage("§c§lKZ §8» §7" + target.getName() + " already has a pending invite.");
            return;
        }

        pendingInvites.put(targetUUID, ownerUUID);

        owner.sendMessage("§a§lKZ §8» §7Invite sent to §f" + target.getName() + "§7.");

        target.sendMessage("");
        target.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§f§l  ISLAND INVITE");
        target.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§7  §f" + owner.getName() + " §7has invited you.");
        target.sendMessage("§7  Type §a/accept §7to join.");
        target.sendMessage("§7  Type §c/deny §7to decline.");
        target.sendMessage("§7  Expires in §f60 seconds§7.");
        target.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("");

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Auto-expire after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.containsKey(targetUUID) &&
                pendingInvites.get(targetUUID).equals(ownerUUID)) {
                pendingInvites.remove(targetUUID);
                if (owner.isOnline())
                    owner.sendMessage("§c§lKZ §8» §7Invite to §f" + target.getName() + " §7has expired.");
                if (target.isOnline())
                    target.sendMessage("§c§lKZ §8» §7Invite from §f" + owner.getName() + " §7has expired.");
            }
        }, 1200L);
    }

    public void acceptInvite(Player player) {
        UUID uuid = player.getUniqueId();

        if (!pendingInvites.containsKey(uuid)) {
            player.sendMessage("§c§lKZ §8» §7You have no pending invites.");
            return;
        }

        UUID ownerUUID = pendingInvites.remove(uuid);
        IslandData data = islands.get(ownerUUID);

        if (data == null || !data.active) {
            player.sendMessage("§c§lKZ §8» §7That island no longer exists.");
            return;
        }

        data.team.add(uuid);
        memberOf.put(uuid, ownerUUID);

        player.teleport(data.spawnPoint);
        player.sendMessage("§a§lKZ §8» §7You have joined the island.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§a§lKZ §8» §f" + player.getName() + " §7has joined your island.");
        }
    }

    public void denyInvite(Player player) {
        UUID uuid = player.getUniqueId();

        if (!pendingInvites.containsKey(uuid)) {
            player.sendMessage("§c§lKZ §8» §7You have no pending invites.");
            return;
        }

        UUID ownerUUID = pendingInvites.remove(uuid);
        player.sendMessage("§a§lKZ §8» §7Invite declined.");

        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§c§lKZ §8» §f" + player.getName() + " §7declined your invite.");
        }
    }

    // ══════════════════════════════════════
    //  TRUST SYSTEM
    // ══════════════════════════════════════

    public void trustPlayer(Player owner, Player target) {
        UUID ownerUUID = owner.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        IslandData data = islands.get(ownerUUID);

        if (data == null || !data.active) {
            owner.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        if (data.trusted.contains(targetUUID)) {
            owner.sendMessage("§c§lKZ §8» §7" + target.getName() + " is already trusted.");
            return;
        }

        data.trusted.add(targetUUID);
        owner.sendMessage("§a§lKZ §8» §f" + target.getName() + " §7has been trusted on your island.");
        target.sendMessage("§a§lKZ §8» §7You have been trusted on §f" + owner.getName() + "§7's island.");
    }

    public void untrustPlayer(Player owner, String targetName) {
        UUID ownerUUID = owner.getUniqueId();
        IslandData data = islands.get(ownerUUID);

        if (data == null || !data.active) {
            owner.sendMessage("§c§lKZ §8» §7You do not have an island.");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            owner.sendMessage("§c§lKZ §8» §7Player not found.");
            return;
        }

        UUID targetUUID = target.getUniqueId();

        boolean removed = false;

        if (data.trusted.remove(targetUUID)) removed = true;
        if (data.team.remove(targetUUID)) {
            memberOf.remove(targetUUID);
            removed = true;
        }

        if (removed) {
            owner.sendMessage("§a§lKZ §8» §f" + targetName + " §7has been removed from all access.");
            target.sendMessage("§c§lKZ §8» §7You have been removed from §f" + owner.getName() + "§7's island.");
        } else {
            owner.sendMessage("§c§lKZ §8» §7" + targetName + " is not on your island.");
        }
    }

    // ══════════════════════════════════════
    //  PERMISSION CHECKS
    // ══════════════════════════════════════

    public boolean canInteract(UUID playerUUID, Location location) {
        for (IslandData data : islands.values()) {
            if (!data.active) continue;
            double dist = location.distance(data.center);
            if (dist <= data.border) {
                // Owner
                if (data.owner.equals(playerUUID)) return true;
                // Team member
                if (data.team.contains(playerUUID)) return true;
                // Trusted
                if (data.trusted.contains(playerUUID)) return true;
                // Not authorized
                return false;
            }
        }
        return true; // Not on any island
    }

    public boolean isWithinBorder(UUID ownerUUID, Location location) {
        IslandData data = islands.get(ownerUUID);
        if (data == null || !data.active) return true;
        return location.distance(data.center) <= data.border;
    }

    public IslandData getIslandAt(Location location) {
        for (IslandData data : islands.values()) {
            if (!data.active) continue;
            double dist = location.distance(data.center);
            if (dist <= data.border) return data;
        }
        return null;
    }

    // ══════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════

    public IslandData getIsland(UUID uuid) {
        return islands.get(uuid);
    }

    public UUID getOwnerOf(UUID memberUUID) {
        return memberOf.get(memberUUID);
    }

    public boolean hasIsland(UUID uuid) {
        IslandData data = islands.get(uuid);
        return data != null && data.active;
    }

    public Map<UUID, IslandData> getAllIslands() {
        return islands;
    }

    // ══════════════════════════════════════
    //  TOP ISLANDS
    // ══════════════════════════════════════

    public List<Map.Entry<UUID, IslandData>> getTopIslands(int limit) {
        List<Map.Entry<UUID, IslandData>> sorted = new ArrayList<>();
        for (Map.Entry<UUID, IslandData> entry : islands.entrySet()) {
            if (entry.getValue().active) {
                sorted.add(entry);
            }
        }
        sorted.sort((a, b) -> Integer.compare(b.getValue().blocksBroken, a.getValue().blocksBroken));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    // ══════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
