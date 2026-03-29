package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EconomyManager {

    private final KZPlugin plugin;
    private File balanceFile;
    private FileConfiguration balanceConfig;
    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(KZPlugin plugin) {
        this.plugin = plugin;
        loadBalances();
    }

    private void loadBalances() {
        balanceFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!balanceFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                balanceFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);

        if (balanceConfig.contains("balances")) {
            for (String key : balanceConfig.getConfigurationSection("balances").getKeys(false)) {
                balances.put(UUID.fromString(key), balanceConfig.getDouble("balances." + key));
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
            balanceConfig.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        try {
            balanceConfig.save(balanceFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, amount);
    }

    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public boolean hasEnough(Player player, double amount) {
        return hasEnough(player.getUniqueId(), amount);
    }

    public void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), amount);
    }

    public boolean removeBalance(UUID uuid, double amount) {
        if (!hasEnough(uuid, amount)) return false;
        setBalance(uuid, getBalance(uuid) - amount);
        return true;
    }

    public boolean removeBalance(Player player, double amount) {
        return removeBalance(player.getUniqueId(), amount);
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (!hasEnough(from, amount)) return false;
        removeBalance(from, amount);
        addBalance(to, amount);
        return true;
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(balances.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public String formatBalance(double amount) {
        if (amount >= 1000000) {
            return String.format("$%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("$%.1fK", amount / 1000.0);
        }
        return "$" + (int) amount;
    }

    public void initPlayer(UUID uuid, double startBalance) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, startBalance);
        }
    }
                            }
