// Path: src/main/java/com/kz/plugin/systems/EconomyManager.java
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.data.DatabaseManager;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    private final KZPlugin plugin;
    private final DatabaseManager db;

    // Cache: UUID -> (mode -> balance)
    // Biar ga query DB terus tiap detik
    private final Map<UUID, Map<String, Double>> cache = new ConcurrentHashMap<>();

    // Saldo awal per mode
    private final Map<String, Double> startingBalance = new HashMap<>();

    public EconomyManager(KZPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;

        // Baca starting balance dari config.yml
        startingBalance.put("lobby",    plugin.getConfig().getDouble("economy.starting-balance.lobby",    0.0));
        startingBalance.put("survival", plugin.getConfig().getDouble("economy.starting-balance.survival", 1000.0));
        startingBalance.put("vanilla",  plugin.getConfig().getDouble("economy.starting-balance.vanilla",  1000.0));
        startingBalance.put("oneblock", plugin.getConfig().getDouble("economy.starting-balance.oneblock", 1000.0));
        startingBalance.put("skyblock", plugin.getConfig().getDouble("economy.starting-balance.skyblock", 1000.0));
        startingBalance.put("island",   plugin.getConfig().getDouble("economy.starting-balance.island",   2000.0));
        startingBalance.put("acid",     plugin.getConfig().getDouble("economy.starting-balance.acid",     500.0));
    }

    // ══════════════════════════════════════
    //  GET MODE PLAYER
    // ══════════════════════════════════════

    public String getPlayerMode(UUID uuid) {
        // Cek island system dulu
        if (plugin.getIslandSystem() != null) {
            IslandSystem.IslandData island = plugin.getIslandSystem().getIsland(uuid);
            if (island != null && island.active) {
                return island.mode;
            }

            UUID ownerUUID = plugin.getIslandSystem().getOwnerOf(uuid);
            if (ownerUUID != null) {
                IslandSystem.IslandData ownerIsland = plugin.getIslandSystem().getIsland(ownerUUID);
                if (ownerIsland != null && ownerIsland.active) {
                    return ownerIsland.mode;
                }
            }
        }

        // Kalau tidak ada island, cek nama server
        String serverName = plugin.getConfig().getString("server-name", "lobby");
        return switch (serverName) {
            case "survival" -> "survival";
            case "void"     -> "oneblock"; // default world pertama
            case "custom"   -> "island";   // default world pertama
            default         -> "lobby";
        };
    }

    public String getPlayerMode(Player player) {
        return getPlayerMode(player.getUniqueId());
    }

    // ══════════════════════════════════════
    //  LOAD PLAYER (saat join)
    // ══════════════════════════════════════

    public CompletableFuture<Void> loadPlayer(UUID uuid, String playerName) {
        return CompletableFuture.runAsync(() -> {
            // Ambil semua saldo player dari DB
            String sql = "SELECT mode_name, balance FROM player_economy WHERE player_uuid = ?";

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                Map<String, Double> modeBalances = new ConcurrentHashMap<>();

                while (rs.next()) {
                    modeBalances.put(
                        rs.getString("mode_name"),
                        rs.getDouble("balance")
                    );
                }

                // Kalau player baru, kasih saldo awal semua mode
                if (modeBalances.isEmpty()) {
                    initNewPlayer(uuid, playerName, modeBalances);
                }

                cache.put(uuid, modeBalances);

            } catch (SQLException e) {
                plugin.getLogger().severe("§cGagal load player " + playerName + ": " + e.getMessage());
            }
        });
    }

    // Buat akun baru dengan saldo awal semua mode
    private void initNewPlayer(UUID uuid, String playerName, Map<String, Double> modeBalances) {
        String sql = """
            INSERT IGNORE INTO player_economy 
            (player_uuid, player_name, mode_name, balance)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Double> entry : startingBalance.entrySet()) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setString(3, entry.getKey());
                ps.setDouble(4, entry.getValue());
                ps.addBatch();

                // Masukin ke cache juga
                modeBalances.put(entry.getKey(), entry.getValue());
            }

            ps.executeBatch();
            plugin.getLogger().info("§aAkun baru dibuat untuk: " + playerName);

        } catch (SQLException e) {
            plugin.getLogger().severe("§cGagal init player: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════
    //  UNLOAD PLAYER (saat logout)
    // ══════════════════════════════════════

    public CompletableFuture<Void> unloadPlayer(UUID uuid) {
        return savePlayer(uuid).thenRun(() -> {
            cache.remove(uuid);
        });
    }

    // ══════════════════════════════════════
    //  SAVE PLAYER KE DB
    // ══════════════════════════════════════

    public CompletableFuture<Void> savePlayer(UUID uuid) {
        Map<String, Double> modeBalances = cache.get(uuid);
        if (modeBalances == null) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_economy (player_uuid, player_name, mode_name, balance)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE balance = VALUES(balance)
                """;

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (Map.Entry<String, Double> entry : modeBalances.entrySet()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, "Unknown"); // Update nama kalau online
                    ps.setString(3, entry.getKey());
                    ps.setDouble(4, entry.getValue());
                    ps.addBatch();
                }

                ps.executeBatch();

            } catch (SQLException e) {
                plugin.getLogger().severe("§cGagal save player: " + e.getMessage());
            }
        });
    }

    // Save semua player (dipanggil saat /autosave atau shutdown)
    public void saveAll() {
        plugin.getLogger().info("§eMenyimpan semua data ekonomi...");
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (UUID uuid : cache.keySet()) {
            futures.add(savePlayer(uuid));
        }

        // Tunggu semua selesai
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        plugin.getLogger().info("§aData ekonomi tersimpan!");
    }

    // ══════════════════════════════════════
    //  GET BALANCE
    // ══════════════════════════════════════

    public double getBalance(UUID uuid, String mode) {
        Map<String, Double> modeBalances = cache.get(uuid);
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

    // ══════════════════════════════════════
    //  SET BALANCE
    // ══════════════════════════════════════

    public void setBalance(UUID uuid, String mode, double amount) {
        // Validasi amount
        if (amount < 0) amount = 0;

        double maxBalance = 999999999.0;
        if (amount > maxBalance) amount = maxBalance;

        cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
             .put(mode, amount);
    }

    public void setBalance(UUID uuid, double amount) {
        setBalance(uuid, getPlayerMode(uuid), amount);
    }

    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), amount);
    }

    // ══════════════════════════════════════
    //  ADD BALANCE
    // ══════════════════════════════════════

    public void addBalance(UUID uuid, String mode, double amount) {
        setBalance(uuid, mode, getBalance(uuid, mode) + amount);
    }

    public void addBalance(UUID uuid, double amount) {
        addBalance(uuid, getPlayerMode(uuid), amount);
    }

    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), amount);
    }

    // ══════════════════════════════════════
    //  REMOVE BALANCE
    // ══════════════════════════════════════

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
    //  HAS ENOUGH
    // ══════════════════════════════════════

    public boolean hasEnough(UUID uuid, String mode, double amount) {
        return getBalance(uuid, mode) >= amount;
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return hasEnough(uuid, getPlayerMode(uuid), amount);
    }

    public boolean hasEnough(Player player, double amount) {
        return hasEnough(player.getUniqueId(), amount);
    }

    // ══════════════════════════════════════
    //  TRANSFER (SESAMA MODE ONLY!)
    // ══════════════════════════════════════

    public boolean transfer(UUID from, UUID to, double amount) {
        String fromMode = getPlayerMode(from);
        String toMode = getPlayerMode(to);

        // Harus mode yang sama!
        if (!fromMode.equals(toMode)) return false;
        if (!hasEnough(from, fromMode, amount)) return false;

        removeBalance(from, fromMode, amount);
        addBalance(to, toMode, amount);
        return true;
    }

    // ══════════════════════════════════════
    //  TOP BALANCES
    // ══════════════════════════════════════

    // Top balance per mode (dari DB langsung, biar akurat)
    public CompletableFuture<List<Map.Entry<String, Double>>> getTopBalances(String mode, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT player_name, balance 
                FROM player_economy 
                WHERE mode_name = ? 
                ORDER BY balance DESC 
                LIMIT ?
                """;

            List<Map.Entry<String, Double>> result = new ArrayList<>();

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, mode);
                ps.setInt(2, limit);

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    result.add(Map.entry(
                        rs.getString("player_name"),
                        rs.getDouble("balance")
                    ));
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("§cGagal ambil baltop: " + e.getMessage());
            }

            return result;
        });
    }

    // ══════════════════════════════════════
    //  GET ALL BALANCES (untuk /bal display)
    // ══════════════════════════════════════

    public Map<String, Double> getAllBalances(UUID uuid) {
        return cache.getOrDefault(uuid, new HashMap<>());
    }

    // ══════════════════════════════════════
    //  FORMAT & NAMA MODE
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
        return switch (mode.toLowerCase()) {
            case "survival" -> "§aSurvival";
            case "vanilla"  -> "§2Vanilla";
            case "oneblock" -> "§aOneBlock";
            case "skyblock" -> "§bSkyblock";
            case "island"   -> "§eClassic Island";
            case "acid"     -> "§cAcid Island";
            case "lobby"    -> "§7Lobby";
            default         -> "§f" + mode;
        };
    }
}
