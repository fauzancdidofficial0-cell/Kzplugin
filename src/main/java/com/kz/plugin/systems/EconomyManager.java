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

    // Per-mode balances: UUID -> (mode -> balance)
    private final Map<UUID, Map<String, Double>> balances = new HashMap<>();

    // Starting balance per mode
    private final Map<String, Double> startingBalance = new HashMap<>();

    public EconomyManager(KZPlugin plugin) {
        this.plugin = plugin;

        // Set starting balance per mode
        startingBalance.put("oneblock", 1000.0);
        startingBalance.put("skyblock", 1000.0);
        startingBalance.put("acid", 500.0);
        startingBalance.put("island", 2000.0);
        startingBalance.put("lobby", 0.0);

        loadBalances();
    }

    // ══════════════════════════════════════
    //  SAVE & LOAD
    // ══════════════════════════════════════

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
            for (String uuidStr : balanceConfig.getConfigurationSection("balances").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Double> modeBalances = new HashMap<>();

                for (String mode : balanceConfig.getConfigurationSection("balances." + uuidStr).getKeys(false)) {
                    modeBalances.put(mode, balanceConfig.getDouble("balances." + uuidStr + "." + mode));
                }

                balances.put(uuid, modeBalances);
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            for (Map.Entry<String, Double> modeEntry : entry.getValue().entrySet()) {
                balanceConfig.set("balances." + entry.getKey().toString() + "." + modeEntry.getKey(),
                    modeEntry.getValue());
            }
        }
        try {
            balanceConfig.save(balanceFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════
    //  GET PLAYER'S CURRENT MODE
    // ══════════════════════════════════════

    public String getPlayerMode(UUID uuid) {
        if (plugin.getIslandSystem() == null) return "lobby";

        IslandSystem.IslandData island = plugin.getIslandSystem().getIsland(uuid);
        if (island != null && island.active) {
            return island.mode;
        }

        // Check if member of someone else's island
        UUID ownerUUID = plugin.getIslandSystem().getOwnerOf(uuid);
        if (ownerUUID != null) {
            IslandSystem.IslandData ownerIsland = plugin.getIslandSystem().getIsland(ownerUUID);
            if (ownerIsland != null && ownerIsland.active) {
                return ownerIsland.mode;
            }
        }

        return "lobby";
    }

    public String getPlayerMode(Player player) {
        return getPlayerMode(player.getUniqueId());
    }

    // ══════════════════════════════════════
    //  BALANCE OPERATIONS (MODE-SPECIFIC)
    // ══════════════════════════════════════

    public double getBalance(UUID uuid, String mode) {
        Map<String, Double> modeBalances = balances.get(uuid);
        if (modeBalances == null) return 0.0;
        return modeBalances.getOrDefault(mode, 0.0);
    }

    public double getBalance(UUID uuid) {
        return getBalance(uuid, getPlayerMode(uuid));
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId());
    }

    public double getBalance(Player player, String mode) {
        return getBalance(player.getUniqueId(), mode);
    }

    public void setBalance(UUID uuid, String mode, double amount) {
        balances.computeIfAbsent(uuid, k -> new HashMap<>()).put(mode, amount);
    }

    public void setBalance(UUID uuid, double amount) {
        setBalance(uuid, getPlayerMode(uuid), amount);
    }

    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }

    public boolean hasEnough(UUID uuid, String mode, double amount) {
        return getBalance(uuid, mode) >= amount;
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return hasEnough(uuid, getPlayerMode(uuid), amount);
    }

    public boolean hasEnough(Player player, double amount) {
        return hasEnough(player.getUniqueId(), amount);
    }

    public void addBalance(UUID uuid, String mode, double amount) {
        setBalance(uuid, mode, getBalance(uuid, mode) + amount);
    }

    public void addBalance(UUID uuid, double amount) {
        addBalance(uuid, getPlayerMode(uuid), amount);
    }

    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), amount);
    }

    public boolean removeBalance(UUID uuid, String mode, double amount) {
        if (!hasEnough(uuid, mode, amount)) return false;
        setBalance(uuid, mode, getBalance(uuid, mode) - amount);
        return true;
    }

    public boolean removeBalance(UUID uuid, double amount) {
        return removeBalance(uuid, getPlayerMode(uuid), amount);
    }

    public boolean removeBalance(Player player, double amount) {
        return removeBalance(player.getUniqueId(), amount);
    }

    // ══════════════════════════════════════
    //  TRANSFER (SAME MODE ONLY!)
    // ══════════════════════════════════════

    public boolean transfer(UUID from, UUID to, double amount) {
        String fromMode = getPlayerMode(from);
        String toMode = getPlayerMode(to);

        // Must be same mode!
        if (!fromMode.equals(toMode)) return false;
        if (!hasEnough(from, fromMode, amount)) return false;

        removeBalance(from, fromMode, amount);
        addBalance(to, toMode, amount);
        return true;
    }

    // ══════════════════════════════════════
    //  INIT PLAYER (Per Mode)
    // ══════════════════════════════════════

    public void initPlayer(UUID uuid, String mode) {
        Map<String, Double> modeBalances = balances.computeIfAbsent(uuid, k -> new HashMap<>());
        if (!modeBalances.containsKey(mode)) {
            double start = startingBalance.getOrDefault(mode, 1000.0);
            modeBalances.put(mode, start);
        }
    }

    // Legacy support
    public void initPlayer(UUID uuid, double startBalance) {
        String mode = getPlayerMode(uuid);
        Map<String, Double> modeBalances = balances.computeIfAbsent(uuid, k -> new HashMap<>());
        if (!modeBalances.containsKey(mode)) {
            modeBalances.put(mode, startBalance);
        }
    }

    // ══════════════════════════════════════
    //  TOP BALANCES (Per Mode)
    // ══════════════════════════════════════

    public List<Map.Entry<UUID, Double>> getTopBalances(String mode, int limit) {
        Map<UUID, Double> modeBalances = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            double bal = entry.getValue().getOrDefault(mode, 0.0);
            if (bal > 0) {
                modeBalances.put(entry.getKey(), bal);
            }
        }

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(modeBalances.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        // Get top for ALL modes combined (for global baltop)
        Map<UUID, Double> totalBalances = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Double>> entry : balances.entrySet()) {
            double total = 0;
            for (double bal : entry.getValue().values()) {
                total += bal;
            }
            totalBalances.put(entry.getKey(), total);
        }

        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(totalBalances.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    // ══════════════════════════════════════
    //  GET ALL BALANCES (for /bal display)
    // ══════════════════════════════════════

    public Map<String, Double> getAllBalances(UUID uuid) {
        return balances.getOrDefault(uuid, new HashMap<>());
    }

    // ══════════════════════════════════════
    //  FORMAT
    // ══════════════════════════════════════

    public String formatBalance(double amount) {
        if (amount >= 1000000) {
            return String.format("$%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("$%.1fK", amount / 1000.0);
        }
        return "$" + (int) amount;
    }

    public String getModeName(String mode) {
        switch (mode.toLowerCase()) {
            case "oneblock": return "§aOneBlock";
            case "skyblock": return "§bSkyblock";
            case "acid": return "§cAcid Island";
            case "island": return "§eClassic Island";
            case "lobby": return "§7Lobby";
            default: return "§f" + mode;
        }
    }
}
