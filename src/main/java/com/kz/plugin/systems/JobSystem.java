package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class JobSystem {

    private final KZPlugin plugin;
    private final Map<UUID, String> playerJobs = new HashMap<>();
    private final Map<UUID, Long> jobCooldown = new HashMap<>();

    // Job rewards
    private final Map<Material, int[]> minerRewards = new HashMap<>();
    private final Map<Material, int[]> farmerRewards = new HashMap<>();
    private final Map<EntityType, int[]> hunterRewards = new HashMap<>();

    public JobSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadRewards();
    }

    private void loadRewards() {
        // Miner rewards: [min, max] bonus
        minerRewards.put(Material.COAL_ORE, new int[]{50, 100});
        minerRewards.put(Material.DEEPSLATE_COAL_ORE, new int[]{50, 100});
        minerRewards.put(Material.IRON_ORE, new int[]{80, 150});
        minerRewards.put(Material.DEEPSLATE_IRON_ORE, new int[]{80, 150});
        minerRewards.put(Material.COPPER_ORE, new int[]{60, 120});
        minerRewards.put(Material.DEEPSLATE_COPPER_ORE, new int[]{60, 120});
        minerRewards.put(Material.GOLD_ORE, new int[]{100, 200});
        minerRewards.put(Material.DEEPSLATE_GOLD_ORE, new int[]{100, 200});
        minerRewards.put(Material.NETHER_GOLD_ORE, new int[]{75, 150});
        minerRewards.put(Material.LAPIS_ORE, new int[]{80, 160});
        minerRewards.put(Material.DEEPSLATE_LAPIS_ORE, new int[]{80, 160});
        minerRewards.put(Material.REDSTONE_ORE, new int[]{60, 120});
        minerRewards.put(Material.DEEPSLATE_REDSTONE_ORE, new int[]{60, 120});
        minerRewards.put(Material.DIAMOND_ORE, new int[]{200, 400});
        minerRewards.put(Material.DEEPSLATE_DIAMOND_ORE, new int[]{200, 400});
        minerRewards.put(Material.EMERALD_ORE, new int[]{250, 500});
        minerRewards.put(Material.DEEPSLATE_EMERALD_ORE, new int[]{250, 500});
        minerRewards.put(Material.NETHER_QUARTZ_ORE, new int[]{50, 100});
        minerRewards.put(Material.ANCIENT_DEBRIS, new int[]{500, 1000});

        // Farmer rewards
        farmerRewards.put(Material.WHEAT, new int[]{30, 70});
        farmerRewards.put(Material.CARROTS, new int[]{30, 70});
        farmerRewards.put(Material.POTATOES, new int[]{30, 70});
        farmerRewards.put(Material.BEETROOTS, new int[]{30, 70});
        farmerRewards.put(Material.SUGAR_CANE, new int[]{20, 50});
        farmerRewards.put(Material.MELON, new int[]{50, 100});
        farmerRewards.put(Material.PUMPKIN, new int[]{50, 100});
        farmerRewards.put(Material.COCOA, new int[]{40, 80});
        farmerRewards.put(Material.BAMBOO, new int[]{10, 30});
        farmerRewards.put(Material.SWEET_BERRY_BUSH, new int[]{30, 60});
        farmerRewards.put(Material.CACTUS, new int[]{20, 50});
        farmerRewards.put(Material.NETHER_WART, new int[]{50, 100});

        // Hunter rewards
        hunterRewards.put(EntityType.ZOMBIE, new int[]{50, 100});
        hunterRewards.put(EntityType.SKELETON, new int[]{50, 100});
        hunterRewards.put(EntityType.CREEPER, new int[]{80, 150});
        hunterRewards.put(EntityType.SPIDER, new int[]{50, 80});
        hunterRewards.put(EntityType.CAVE_SPIDER, new int[]{60, 100});
        hunterRewards.put(EntityType.ENDERMAN, new int[]{150, 250});
        hunterRewards.put(EntityType.BLAZE, new int[]{120, 200});
        hunterRewards.put(EntityType.WITCH, new int[]{100, 180});
        hunterRewards.put(EntityType.PHANTOM, new int[]{80, 150});
        hunterRewards.put(EntityType.PILLAGER, new int[]{100, 180});
        hunterRewards.put(EntityType.RAVAGER, new int[]{200, 350});
        hunterRewards.put(EntityType.WITHER_SKELETON, new int[]{150, 250});
        hunterRewards.put(EntityType.GHAST, new int[]{150, 250});
        hunterRewards.put(EntityType.PIGLIN_BRUTE, new int[]{120, 200});
        hunterRewards.put(EntityType.GUARDIAN, new int[]{100, 180});
        hunterRewards.put(EntityType.ELDER_GUARDIAN, new int[]{500, 1000});
        hunterRewards.put(EntityType.WARDEN, new int[]{1000, 2000});
        hunterRewards.put(EntityType.ENDER_DRAGON, new int[]{5000, 10000});
        hunterRewards.put(EntityType.WITHER, new int[]{3000, 6000});
    }

    // ══════════════════════════════════════
    //  SET JOB
    // ══════════════════════════════════════

    public boolean setJob(Player player, String job) {
        UUID uuid = player.getUniqueId();

        if (!job.equals("miner") && !job.equals("farmer") && !job.equals("hunter")) {
            player.sendMessage("§c§lKZ §8» §7Invalid job. Available: §fminer§7, §ffarmer§7, §fhunter");
            return false;
        }

        // Check cooldown (1 hour)
        if (jobCooldown.containsKey(uuid)) {
            long lastChange = jobCooldown.get(uuid);
            long diff = System.currentTimeMillis() - lastChange;
            long remaining = 3600000 - diff; // 1 hour

            if (remaining > 0) {
                long minutes = remaining / 60000;
                long seconds = (remaining % 60000) / 1000;
                player.sendMessage("§c§lKZ §8» §7Job change cooldown: §f" + minutes + "m " + seconds + "s");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return false;
            }
        }

        playerJobs.put(uuid, job);
        jobCooldown.put(uuid, System.currentTimeMillis());

        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  JOB SELECTED");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Job: §f" + capitalize(job));
        player.sendMessage("");

        switch (job) {
            case "miner":
                player.sendMessage("§7  Bonus §a$50-1000 §7per ore mined.");
                player.sendMessage("§7  Higher tier ores = more reward.");
                break;
            case "farmer":
                player.sendMessage("§7  Bonus §a$20-100 §7per harvest.");
                player.sendMessage("§7  All crops give bonus income.");
                break;
            case "hunter":
                player.sendMessage("§7  Bonus §a$50-10000 §7per mob kill.");
                player.sendMessage("§7  Boss mobs give massive rewards.");
                break;
        }

        player.sendMessage("");
        player.sendMessage("§7  Cooldown: §f1 hour §7before changing.");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        return true;
    }

    // ══════════════════════════════════════
    //  PROCESS REWARDS
    // ══════════════════════════════════════

    public void processMining(Player player, Material block) {
        UUID uuid = player.getUniqueId();
        if (!"miner".equals(playerJobs.get(uuid))) return;

        int[] reward = minerRewards.get(block);
        if (reward == null) return;

        Random random = new Random();
        int bonus = random.nextInt(reward[1] - reward[0] + 1) + reward[0];

        plugin.getEconomyManager().addBalance(uuid, bonus);
        player.sendMessage("§a+$" + bonus + " §8[§bMiner§8]");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }

    public void processFarming(Player player, Material block) {
        UUID uuid = player.getUniqueId();
        if (!"farmer".equals(playerJobs.get(uuid))) return;

        int[] reward = farmerRewards.get(block);
        if (reward == null) return;

        Random random = new Random();
        int bonus = random.nextInt(reward[1] - reward[0] + 1) + reward[0];

        plugin.getEconomyManager().addBalance(uuid, bonus);
        player.sendMessage("§a+$" + bonus + " §8[§2Farmer§8]");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }

    public void processKill(Player player, EntityType entityType) {
        UUID uuid = player.getUniqueId();
        if (!"hunter".equals(playerJobs.get(uuid))) return;

        int[] reward = hunterRewards.get(entityType);
        if (reward == null) return;

        Random random = new Random();
        int bonus = random.nextInt(reward[1] - reward[0] + 1) + reward[0];

        plugin.getEconomyManager().addBalance(uuid, bonus);
        player.sendMessage("§a+$" + bonus + " §8[§cHunter§8]");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }

    // ══════════════════════════════════════
    //  SHOW JOB INFO
    // ══════════════════════════════════════

    public void showJobInfo(Player player) {
        UUID uuid = player.getUniqueId();
        String job = playerJobs.get(uuid);

        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  JOB SYSTEM");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        if (job != null) {
            player.sendMessage("§7  Current Job: §f" + capitalize(job));
        } else {
            player.sendMessage("§7  Current Job: §cNone");
        }

        player.sendMessage("");
        player.sendMessage("§7  Available Jobs:");
        player.sendMessage("§f    /job miner  §8→ §7Bonus per ore mined");
        player.sendMessage("§f    /job farmer §8→ §7Bonus per harvest");
        player.sendMessage("§f    /job hunter §8→ §7Bonus per mob kill");
        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }

    // ══════════════════════════════════════
    //  GETTERS
    // ══════════════════════════════════════

    public String getJob(UUID uuid) {
        return playerJobs.getOrDefault(uuid, null);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
