package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DailyRewardSystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, Long> lastClaim = new HashMap<>();
    private final Map<UUID, Integer> streaks = new HashMap<>();

    public DailyRewardSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "daily.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                lastClaim.put(uuid, dataConfig.getLong("players." + key + ".last", 0));
                streaks.put(uuid, dataConfig.getInt("players." + key + ".streak", 0));
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Long> entry : lastClaim.entrySet()) {
            String key = "players." + entry.getKey().toString();
            dataConfig.set(key + ".last", entry.getValue());
            dataConfig.set(key + ".streak", streaks.getOrDefault(entry.getKey(), 0));
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void claim(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastTime = lastClaim.getOrDefault(uuid, 0L);
        long diff = now - lastTime;

        // Check 24 hour cooldown
        if (lastTime > 0 && diff < 86400000L) { // 24 hours
            long remaining = 86400000L - diff;
            long hours = remaining / 3600000;
            long minutes = (remaining % 3600000) / 60000;

            player.sendMessage("§c§lKZ §8» §7Daily reward already claimed.");
            player.sendMessage("§7  Available in: §f" + hours + "h " + minutes + "m");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Check streak reset (48 hours = broken streak)
        int streak = streaks.getOrDefault(uuid, 0);
        if (lastTime > 0 && diff > 172800000L) { // 48 hours
            streak = 0;
        }

        streak++;
        streaks.put(uuid, streak);
        lastClaim.put(uuid, now);

        // Calculate reward based on streak
        int reward;
        String streakBonus;
        if (streak >= 30) {
            reward = 10000;
            streakBonus = "§6§l★ LEGENDARY STREAK!";
        } else if (streak >= 14) {
            reward = 5000;
            streakBonus = "§d§l★ EPIC STREAK!";
        } else if (streak >= 7) {
            reward = 3000;
            streakBonus = "§b§l★ WEEKLY STREAK!";
        } else if (streak >= 5) {
            reward = 2000;
            streakBonus = "§a★ Great Streak!";
        } else if (streak >= 3) {
            reward = 1500;
            streakBonus = "§e★ Nice Streak!";
        } else {
            reward = 1000;
            streakBonus = "";
        }

        plugin.getEconomyManager().addBalance(uuid, reward);

        player.sendMessage("");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  DAILY REWARD CLAIMED");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Streak  : §f" + streak + " days §7🔥");
        player.sendMessage("§7  Reward  : §a+$" + plugin.getEconomyManager().formatBalance(reward));
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(uuid)));

        if (!streakBonus.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("  " + streakBonus);
        }

        player.sendMessage("");
        player.sendMessage("§7  Next claim available in §f24 hours§7.");
        player.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        if (streak >= 7) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        saveData();
    }
}
