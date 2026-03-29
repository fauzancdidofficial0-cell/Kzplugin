package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WeeklyRewardSystem {

    private final KZPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, Long> lastClaim = new HashMap<>();

    public WeeklyRewardSystem(KZPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "weekly.yml");
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
                lastClaim.put(uuid, dataConfig.getLong("players." + key, 0));
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Long> entry : lastClaim.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
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

        // Check 7 day cooldown
        if (lastTime > 0 && diff < 604800000L) { // 7 days
            long remaining = 604800000L - diff;
            long days = remaining / 86400000;
            long hours = (remaining % 86400000) / 3600000;

            player.sendMessage("§c§lKZ §8» §7Weekly reward already claimed.");
            player.sendMessage("§7  Available in: §f" + days + "d " + hours + "h");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int reward = 10000;
        lastClaim.put(uuid, now);

        plugin.getEconomyManager().addBalance(uuid, reward);

        player.sendMessage("");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§f§l  WEEKLY REWARD CLAIMED");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("§7  Reward  : §a+$" + plugin.getEconomyManager().formatBalance(reward) + " §7🎁");
        player.sendMessage("§7  Balance : §a" + plugin.getEconomyManager().formatBalance(
            plugin.getEconomyManager().getBalance(uuid)));
        player.sendMessage("");
        player.sendMessage("§7  Next claim available in §f7 days§7.");
        player.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        saveData();
    }
}
