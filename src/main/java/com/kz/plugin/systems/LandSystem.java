package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandSystem {

    private final KZPlugin plugin;
    private File landFile;
    private FileConfiguration landConfig;

    // Land data
    private final Map<String, LandData> lands = new HashMap<>(); // claimId -> data
    private final Map<UUID, String> playerLands = new HashMap<>(); // owner -> claimId
    private final Map<UUID, String> playerMemberOf = new HashMap<>(); // member -> claimId
    private final Map<UUID, Location> claimPos1 = new HashMap<>(); // temp pos1
    private final Map<UUID, String> pendingInvites = new HashMap<>(); // target -> claimId
    private int claimCounter = 0;

    // 25 Rules
    public static final String[] ALL_RULES = {
        "build", "break", "chest", "door", "pvp", "bed",
        "furnace", "crafting", "drop", "pickup", "anvil",
        "enchant", "hopper", "dispenser", "breakcrop",
        "placecrop", "feedanimal", "killanimal", "killmob",
        "leash", "minecart", "boat", "fly", "elytra", "redstone"
    };

    public static final String[] TRUST_RULES = {
        "build", "break", "chest", "door", "furnace",
        "crafting", "hopper", "dispenser", "breakcrop",
        "placecrop", "feedanimal", "killanimal", "killmob",
        "minecart", "redstone"
    };

    // Rule display names
    private static final Map<String, String> RULE_NAMES = new HashMap<>();
    private static final Map<String, Material> RULE_ICONS = new HashMap<>();

    static {
        RULE_NAMES.put("build", "Build");
        RULE_NAMES.put("break", "Break Blocks");
        RULE_NAMES.put("chest", "Interact Chest");
        RULE_NAMES.put("door", "Interact Door");
        RULE_NAMES.put("pvp", "PvP Combat");
        RULE_NAMES.put("bed", "Use Bed");
        RULE_NAMES.put("furnace", "Use Furnace");
        RULE_NAMES.put("crafting", "Use Crafting Table");
        RULE_NAMES.put("drop", "Drop Items");
        RULE_NAMES.put("pickup", "Pick Up Items");
        RULE_NAMES.put("anvil", "Use Anvil");
        RULE_NAMES.put("enchant", "Use Enchanting Table");
        RULE_NAMES.put("hopper", "Use Hopper");
        RULE_NAMES.put("dispenser", "Use Dispenser");
        RULE_NAMES.put("breakcrop", "Break Crops");
        RULE_NAMES.put("placecrop", "Place Crops");
        RULE_NAMES.put("feedanimal", "Feed Animals");
        RULE_NAMES.put("killanimal", "Kill Animals");
        RULE_NAMES.put("killmob", "Kill Hostile Mobs");
        RULE_NAMES.put("leash", "Use Leash");
        RULE_NAMES.put("minecart", "Use Minecart");
        RULE_NAMES.put("boat", "Use Boat");
        RULE_NAMES.put("fly", "Fly in Land");
        RULE_NAMES.put("elytra", "Use Elytra");
        RULE_NAMES.put("redstone", "Use Redstone");

        RULE_ICONS.put("build", Material.OAK_LOG);
        RULE_ICONS.put("break", Material.DIAMOND_PICKAXE);
        RULE_ICONS.put("chest", Material.CHEST);
        RULE_ICONS.put("door", Material.OAK_DOOR);
        RULE_ICONS.put("pvp", Material.IRON_SWORD);
        RULE_ICONS.put("bed", Material.RED_BED);
        RULE_ICONS.put("furnace", Material.FURNACE);
        RULE_ICONS.put("crafting", Material.CRAFTING_TABLE);
        RULE_ICONS.put("drop", Material.DROPPER);
        RULE_ICONS.put("pickup", Material.NETHER_STAR);
        RULE_ICONS.put("anvil", Material.ANVIL);
        RULE_ICONS.put("enchant", Material.ENCHANTING_TABLE);
        RULE_ICONS.put("hopper", Material.HOPPER);
        RULE_ICONS.put("dispenser", Material.DISPENSER);
        RULE_ICONS.put("breakcrop", Material.WHEAT);
        RULE_ICONS.put("placecrop", Material.WHEAT_SEEDS);
        RULE_ICONS.put("feedanimal", Material.CARROT);
        RULE_ICONS.put("killanimal", Material.BONE);
        RULE_ICONS.put("killmob", Material.ROTTEN_FLESH);
        RULE_ICONS.put("leash", Material.LEAD);
        RULE_ICONS.put("minecart", Material.MINECART);
        RULE_ICONS.put("boat", Material.OAK_BOAT);
        RULE_ICONS.put("fly", Material.FEATHER);
        RULE_ICONS.put("elytra", Material.ELYTRA);
        RULE_ICONS.put("redstone", Material.REDSTONE);
    }

    public static class LandData {
        public String id;
        public UUID owner;
        public String ownerName;
        public String name;
        public String world;
        public int x1, z1, x2, z2;
        public Map<UUID, String> members = new HashMap<>(); // uuid -> role (admin/staff/member)
        public Set<UUID> trusted = new HashSet<>();
        public Map<String, Boolean> memberRules = new HashMap<>();
        public Map<String, Boolean> trustRules = new HashMap<>();

        public LandData(String id, UUID owner, String ownerName, String name,
                        String world, int x1, int z1, int x2, int z2) {
            this.id = id;
            this.owner = owner;
            this.ownerName = ownerName;
            this.name = name;
            this.world = world;
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);

            // Default rules
            for (String rule : ALL_RULES) {
                memberRules.put(rule, false);
                trustRules.put(rule, false);
            }
            memberRules.put("build", true);
            memberRules.put("break", true);
            trustRules.put("build", true);
            trustRules.put("break", true);
        }

        public boolean contains(int x, int z, String w) {
            return w.equals(world) && x >= x1 && x <= x2 && z >= z1 && z <= z2;
        }

        public int getSizeX() { return x2 - x1 + 1; }
        public int getSizeZ() { return z2 - z1 + 1; }
    }

    public LandSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    // ══════════════════════════════════════
    //  SAVE & LOAD
    // ══════════════════════════════════════

    private void loadData() {
        landFile = new File(plugin.getDataFolder(), "lands.yml");
        if (!landFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                landFile.createNewFile();
            } catch (IOException e) { e.printStackTrace(); }
        }
        landConfig = YamlConfiguration.loadConfiguration(landFile);
        claimCounter = landConfig.getInt("counter", 0);

        if (landConfig.contains("lands")) {
            for (String cid : landConfig.getConfigurationSection("lands").getKeys(false)) {
                String path = "lands." + cid;
                UUID owner = UUID.fromString(landConfig.getString(path + ".owner"));
                LandData data = new LandData(cid, owner,
                    landConfig.getString(path + ".ownerName", "Unknown"),
                    landConfig.getString(path + ".name", "Unnamed"),
                    landConfig.getString(path + ".world", "world"),
                    landConfig.getInt(path + ".x1"), landConfig.getInt(path + ".z1"),
                    landConfig.getInt(path + ".x2"), landConfig.getInt(path + ".z2"));

                // Load rules
                for (String rule : ALL_RULES) {
                    data.memberRules.put(rule, landConfig.getBoolean(path + ".mrules." + rule, false));
                }
                for (String rule : TRUST_RULES) {
                    data.trustRules.put(rule, landConfig.getBoolean(path + ".trules." + rule, false));
                }

                // Load members
                if (landConfig.contains(path + ".members")) {
                    for (String mk : landConfig.getConfigurationSection(path + ".members").getKeys(false)) {
                        data.members.put(UUID.fromString(mk), landConfig.getString(path + ".members." + mk));
                        playerMemberOf.put(UUID.fromString(mk), cid);
                    }
                }

                // Load trusted
                List<String> tl = landConfig.getStringList(path + ".trusted");
                for (String t : tl) data.trusted.add(UUID.fromString(t));

                lands.put(cid, data);
                playerLands.put(owner, cid);
            }
        }
    }

    public void saveAll() {
        landConfig.set("counter", claimCounter);
        for (Map.Entry<String, LandData> entry : lands.entrySet()) {
            String path = "lands." + entry.getKey();
            LandData d = entry.getValue();
            landConfig.set(path + ".owner", d.owner.toString());
            landConfig.set(path + ".ownerName", d.ownerName);
            landConfig.set(path + ".name", d.name);
            landConfig.set(path + ".world", d.world);
            landConfig.set(path + ".x1", d.x1);
            landConfig.set(path + ".z1", d.z1);
            landConfig.set(path + ".x2", d.x2);
            landConfig.set(path + ".z2", d.z2);

            for (String rule : ALL_RULES)
                landConfig.set(path + ".mrules." + rule, d.memberRules.getOrDefault(rule, false));
            for (String rule : TRUST_RULES)
                landConfig.set(path + ".trules." + rule, d.trustRules.getOrDefault(rule, false));

            for (Map.Entry<UUID, String> m : d.members.entrySet())
                landConfig.set(path + ".members." + m.getKey().toString(), m.getValue());

            List<String> tl = new ArrayList<>();
            for (UUID t : d.trusted) tl.add(t.toString());
            landConfig.set(path + ".trusted", tl);
        }
        try { landConfig.save(landFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════
    //  GOLDEN SHOVEL CLAIM
    // ══════════════════════════════════════

    public void handleGoldenShovelClick(Player player, Location clickedLoc) {
        UUID uuid = player.getUniqueId();

        if (playerLands.containsKey(uuid)) {
            player.sendMessage("§c§lKZ §8» §7You already own a land claim.");
            return;
        }

        int x = clickedLoc.getBlockX();
        int z = clickedLoc.getBlockZ();

        if (!claimPos1.containsKey(uuid)) {
            // Set position 1
            claimPos1.put(uuid, clickedLoc);
            clickedLoc.getBlock().setType(Material.GOLD_BLOCK);

            // Revert gold block after 3 seconds
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                clickedLoc.getBlock().setType(Material.AIR);
            }, 60L);

            player.sendMessage("§b§lKZ §8» §7Corner §b1 §7selected. §f(" + x + ", " + z + ")");
            player.sendMessage("§7  Click corner §b2 §7to finish claiming.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        } else {
            // Set position 2 and create claim
            Location pos1 = claimPos1.remove(uuid);
            int x1 = pos1.getBlockX();
            int z1 = pos1.getBlockZ();
            String world = clickedLoc.getWorld().getName();

            int minX = Math.min(x1, x);
            int maxX = Math.max(x1, x);
            int minZ = Math.min(z1, z);
            int maxZ = Math.max(z1, z);

            int sizeX = maxX - minX + 1;
            int sizeZ = maxZ - minZ + 1;

            // Minimum size
            if (sizeX < 5 || sizeZ < 5) {
                player.sendMessage("§c§lKZ §8» §7Minimum claim size is §f5x5 blocks§7.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Maximum size based on rank
            String rank = plugin.getLobbySystem().getRank(uuid);
            int maxSize = getMaxClaimSize(rank);

            if (sizeX > maxSize || sizeZ > maxSize) {
                player.sendMessage("§c§lKZ §8» §7Exceeds maximum size. Max: §f" + maxSize + "x" + maxSize + "§7.");
                player.sendMessage("§7  Your rank: §b" + rank);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            // Check overlap
            for (LandData ld : lands.values()) {
                if (!ld.world.equals(world)) continue;
                if (minX <= ld.x2 && maxX >= ld.x1 && minZ <= ld.z2 && maxZ >= ld.z1) {
                    player.sendMessage("§c§lKZ §8» §7This area overlaps with §f" + ld.name + "§7.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
            }

            // Create claim
            claimCounter++;
            String cid = String.valueOf(claimCounter);
            String claimName = player.getName() + "'s Land";

            LandData data = new LandData(cid, uuid, player.getName(), claimName,
                world, minX, minZ, maxX, maxZ);

            lands.put(cid, data);
            playerLands.put(uuid, cid);

            // Visual feedback
            clickedLoc.getBlock().setType(Material.GOLD_BLOCK);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                clickedLoc.getBlock().setType(Material.AIR);
            }, 60L);

            player.sendMessage("");
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§f§l  LAND CLAIMED SUCCESSFULLY ✓");
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§7  Name : §b" + claimName);
            player.sendMessage("§7  Size : §f" + sizeX + "x" + sizeZ + " blocks");
            player.sendMessage("§7  ID   : §f#" + cid);
            player.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("");

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            saveAll();
        }
    }

    private int getMaxClaimSize(String rank) {
        if (rank == null) return 25;
        switch (rank.toLowerCase()) {
            case "iron": return 30;
            case "gold": return 35;
            case "diamond": return 40;
            case "emerald": return 50;
            case "obsidian": return 60;
            case "onyx": return 75;
            case "phantom": return 100;
            case "eclipse": return 150;
            case "ethereal": return 200;
            case "owner": return 999;
            default: return 25;
        }
    }

    // ══════════════════════════════════════
    //  ROLE SYSTEM
    // ══════════════════════════════════════

    public String getRole(String cid, UUID uuid) {
        LandData data = lands.get(cid);
        if (data == null) return "none";
        if (data.owner.equals(uuid)) return "owner";
        String memberRole = data.members.get(uuid);
        if (memberRole != null) return memberRole;
        if (data.trusted.contains(uuid)) return "trust";
        return "none";
    }

    public boolean checkPermission(String cid, UUID uuid, String rule) {
        String role = getRole(cid, uuid);
        if (role.equals("owner") || role.equals("admin")) return true;
        if (role.equals("staff")) {
            return rule.equals("build") || rule.equals("break");
        }

        LandData data = lands.get(cid);
        if (data == null) return false;

        if (role.equals("member")) {
            return data.memberRules.getOrDefault(rule, false);
        }
        if (role.equals("trust")) {
            return data.trustRules.getOrDefault(rule, false);
        }
        return false;
    }

    // ══════════════════════════════════════
    //  LAND AT LOCATION
    // ══════════════════════════════════════

    public LandData getLandAt(Location loc) {
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        String w = loc.getWorld().getName();

        for (LandData data : lands.values()) {
            if (data.contains(x, z, w)) return data;
        }
        return null;
    }

    public String getLandIdAt(Location loc) {
        LandData data = getLandAt(loc);
        return data != null ? data.id : null;
    }

    // ══════════════════════════════════════
    //  INVITE / ACCEPT / DENY
    // ══════════════════════════════════════

    public void inviteToLand(Player owner, Player target) {
        UUID ownerUUID = owner.getUniqueId();
        String cid = playerLands.get(ownerUUID);

        if (cid == null) {
            owner.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        LandData data = lands.get(cid);
        String role = getRole(cid, ownerUUID);
        if (!role.equals("owner") && !role.equals("admin")) {
            owner.sendMessage("§c§lKZ §8» §7Only the owner and admins can invite.");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        if (!getRole(cid, targetUUID).equals("none")) {
            owner.sendMessage("§c§lKZ §8» §7" + target.getName() + " is already in this land.");
            return;
        }

        pendingInvites.put(targetUUID, cid);

        owner.sendMessage("§a§lKZ §8» §7Invite sent to §f" + target.getName() + "§7.");

        target.sendMessage("");
        target.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§f§l  LAND INVITE");
        target.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§7  §f" + owner.getName() + " §7has invited you.");
        target.sendMessage("§7  Land: §b" + data.name);
        target.sendMessage("§7  Type §a/landaccept §7or §c/landdeny");
        target.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("");

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInvites.containsKey(targetUUID) && pendingInvites.get(targetUUID).equals(cid)) {
                pendingInvites.remove(targetUUID);
                if (owner.isOnline()) owner.sendMessage("§c§lKZ §8» §7Land invite expired.");
                if (target.isOnline()) target.sendMessage("§c§lKZ §8» §7Land invite expired.");
            }
        }, 1200L);
    }

    public void acceptLandInvite(Player player) {
        UUID uuid = player.getUniqueId();
        String cid = pendingInvites.remove(uuid);

        if (cid == null) {
            player.sendMessage("§c§lKZ §8» §7No pending land invites.");
            return;
        }

        LandData data = lands.get(cid);
        if (data == null) {
            player.sendMessage("§c§lKZ §8» §7That land no longer exists.");
            return;
        }

        data.members.put(uuid, "member");
        playerMemberOf.put(uuid, cid);

        player.sendMessage("§a§lKZ §8» §7You have joined §b" + data.name + "§7.");

        Player owner = Bukkit.getPlayer(data.owner);
        if (owner != null && owner.isOnline()) {
            owner.sendMessage("§a§lKZ §8» §f" + player.getName() + " §7joined as Member.");
        }
        saveAll();
    }

    public void denyLandInvite(Player player) {
        UUID uuid = player.getUniqueId();
        String cid = pendingInvites.remove(uuid);

        if (cid == null) {
            player.sendMessage("§c§lKZ §8» §7No pending land invites.");
            return;
        }

        player.sendMessage("§a§lKZ §8» §7Land invite declined.");

        LandData data = lands.get(cid);
        if (data != null) {
            Player owner = Bukkit.getPlayer(data.owner);
            if (owner != null && owner.isOnline()) {
                owner.sendMessage("§c§lKZ §8» §f" + player.getName() + " §7declined the invite.");
            }
        }
    }

    // ══════════════════════════════════════
    //  ROLE MANAGEMENT
    // ══════════════════════════════════════

    public void setRole(Player sender, String targetName, String newRole) {
        UUID senderUUID = sender.getUniqueId();
        String cid = playerLands.get(senderUUID);

        if (cid == null) {
            sender.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        if (!newRole.equals("admin") && !newRole.equals("staff") && !newRole.equals("member")) {
            sender.sendMessage("§c§lKZ §8» §7Invalid role. Use: §fadmin§7, §fstaff§7, §fmember");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§c§lKZ §8» §7Player not found.");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        LandData data = lands.get(cid);

        if (getRole(cid, targetUUID).equals("none")) {
            sender.sendMessage("§c§lKZ §8» §7" + targetName + " is not a member.");
            return;
        }

        if (newRole.equals("admin") && !data.owner.equals(senderUUID)) {
            sender.sendMessage("§c§lKZ §8» §7Only the owner can promote to Admin.");
            return;
        }

        data.members.put(targetUUID, newRole);

        sender.sendMessage("§a§lKZ §8» §f" + targetName + " §7→ §b" + capitalize(newRole) + "§7.");
        target.sendMessage("§a§lKZ §8» §7Your role has been set to §b" + capitalize(newRole) + "§7.");

        saveAll();
    }

    public void kickFromLand(Player sender, String targetName) {
        UUID senderUUID = sender.getUniqueId();
        String cid = playerLands.get(senderUUID);

        if (cid == null) {
            sender.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§c§lKZ §8» §7Player not found.");
            return;
        }

        UUID targetUUID = target.getUniqueId();
        LandData data = lands.get(cid);

        String targetRole = getRole(cid, targetUUID);
        if (targetRole.equals("none")) {
            sender.sendMessage("§c§lKZ §8» §7" + targetName + " is not a member.");
            return;
        }
        if (targetRole.equals("owner")) {
            sender.sendMessage("§c§lKZ §8» §7Cannot kick the owner.");
            return;
        }

        data.members.remove(targetUUID);
        data.trusted.remove(targetUUID);
        playerMemberOf.remove(targetUUID);

        sender.sendMessage("§a§lKZ §8» §f" + targetName + " §7has been kicked from the land.");
        target.sendMessage("§c§lKZ §8» §7You have been kicked from §f" + data.name + "§7.");

        saveAll();
    }

    // ══════════════════════════════════════
    //  TRUST LAND
    // ══════════════════════════════════════

    public void trustLand(Player owner, Player target) {
        UUID ownerUUID = owner.getUniqueId();
        String cid = playerLands.get(ownerUUID);

        if (cid == null) {
            owner.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        LandData data = lands.get(cid);
        UUID targetUUID = target.getUniqueId();

        if (data.trusted.contains(targetUUID)) {
            owner.sendMessage("§c§lKZ §8» §7" + target.getName() + " is already trusted.");
            return;
        }

        data.trusted.add(targetUUID);
        owner.sendMessage("§a§lKZ §8» §f" + target.getName() + " §7added as Trusted.");
        target.sendMessage("§a§lKZ §8» §7You are now trusted in §f" + owner.getName() + "§7's land.");

        saveAll();
    }

    // ══════════════════════════════════════
    //  RULES GUI
    // ══════════════════════════════════════

    public void openMemberRulesGUI(Player player) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.get(uuid);

        if (cid == null) {
            player.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        LandData data = lands.get(cid);
        Inventory inv = Bukkit.createInventory(null, 54,
            "§b§lMember Rules - §f" + data.name);

        int[] slots = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40
        };

        // Fill glass
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Rules
        for (int i = 0; i < ALL_RULES.length && i < slots.length; i++) {
            String rule = ALL_RULES[i];
            boolean enabled = data.memberRules.getOrDefault(rule, false);
            Material icon = RULE_ICONS.getOrDefault(rule, Material.PAPER);
            String label = RULE_NAMES.getOrDefault(rule, rule);

            ItemStack item = createItem(icon,
                "§f" + label + (enabled ? " §a§lON" : " §c§lOFF"),
                "§7Click to toggle.");
            inv.setItem(slots[i], item);
        }

        // Navigation
        ItemStack nav = createItem(Material.COMPARATOR, "§b» Trust Rules",
            "§7Click to view Trust Rules.");
        inv.setItem(49, nav);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    public void openTrustRulesGUI(Player player) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.get(uuid);

        if (cid == null) {
            player.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        LandData data = lands.get(cid);
        Inventory inv = Bukkit.createInventory(null, 36,
            "§b§lTrust Rules - §f" + data.name);

        int[] slots = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28
        };

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) inv.setItem(i, glass);

        for (int i = 0; i < TRUST_RULES.length && i < slots.length; i++) {
            String rule = TRUST_RULES[i];
            boolean enabled = data.trustRules.getOrDefault(rule, false);
            Material icon = RULE_ICONS.getOrDefault(rule, Material.PAPER);
            String label = RULE_NAMES.getOrDefault(rule, rule);

            ItemStack item = createItem(icon,
                "§f" + label + (enabled ? " §a§lON" : " §c§lOFF"),
                "§7Click to toggle.");
            inv.setItem(slots[i], item);
        }

        ItemStack back = createItem(Material.COMPARATOR, "§b« Member Rules",
            "§7Click to go back.");
        inv.setItem(31, back);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  TOGGLE RULES (Called from GUIListener)
    // ══════════════════════════════════════

    public void toggleMemberRule(Player player, int slotIndex) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.get(uuid);
        if (cid == null) return;

        LandData data = lands.get(cid);
        if (slotIndex >= 0 && slotIndex < ALL_RULES.length) {
            String rule = ALL_RULES[slotIndex];
            boolean current = data.memberRules.getOrDefault(rule, false);
            data.memberRules.put(rule, !current);
            saveAll();

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> openMemberRulesGUI(player), 1L);
        }
    }

    public void toggleTrustRule(Player player, int slotIndex) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.get(uuid);
        if (cid == null) return;

        LandData data = lands.get(cid);
        if (slotIndex >= 0 && slotIndex < TRUST_RULES.length) {
            String rule = TRUST_RULES[slotIndex];
            boolean current = data.trustRules.getOrDefault(rule, false);
            data.trustRules.put(rule, !current);
            saveAll();

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> openTrustRulesGUI(player), 1L);
        }
    }

    // ══════════════════════════════════════
    //  DELETE / RENAME / INFO
    // ══════════════════════════════════════

    public void deleteLand(Player player) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.remove(uuid);

        if (cid == null) {
            player.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        LandData data = lands.remove(cid);
        if (data != null) {
            for (UUID mUUID : data.members.keySet()) {
                playerMemberOf.remove(mUUID);
                Player m = Bukkit.getPlayer(mUUID);
                if (m != null && m.isOnline()) {
                    m.sendMessage("§c§lKZ §8» §7Land §f" + data.name + " §7has been deleted.");
                }
            }
        }

        landConfig.set("lands." + cid, null);

        player.sendMessage("§a§lKZ §8» §7Land §b" + (data != null ? data.name : "Unknown") + " §7deleted.");
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        saveAll();
    }

    public void setLandName(Player player, String name) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.get(uuid);

        if (cid == null) {
            player.sendMessage("§c§lKZ §8» §7You do not own a land claim.");
            return;
        }

        lands.get(cid).name = name;
        player.sendMessage("§a§lKZ §8» §7Land renamed to §b" + name + "§7.");
        saveAll();
    }

    public void showLandInfo(Player player) {
        UUID uuid = player.getUniqueId();
        String cid = playerLands.get(uuid);

        String rank = plugin.getLobbySystem().getRank(uuid);
        int maxSize = getMaxClaimSize(rank);

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  LAND INFORMATION");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Rank     : §b" + rank);
        player.sendMessage("§7  Max Size : §f" + maxSize + "x" + maxSize);

        if (cid != null) {
            LandData data = lands.get(cid);
            player.sendMessage("");
            player.sendMessage("§7  Name     : §b" + data.name);
            player.sendMessage("§7  Size     : §f" + data.getSizeX() + "x" + data.getSizeZ());
            player.sendMessage("§7  Members  : §f" + data.members.size());
            player.sendMessage("§7  Trusted  : §f" + data.trusted.size());
        } else {
            player.sendMessage("");
            player.sendMessage("§7  Status   : §cNo land claimed");
            player.sendMessage("§7  Use a §bGolden Shovel §7to claim land.");
        }

        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════

    public String getPlayerLandId(UUID uuid) {
        return playerLands.get(uuid);
    }

    public LandData getLand(String cid) {
        return lands.get(cid);
    }

    public boolean hasLand(UUID uuid) {
        return playerLands.containsKey(uuid);
    }

    // ══════════════════════════════════════
    //  UTILITY
    // ══════════════════════════════════════

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
