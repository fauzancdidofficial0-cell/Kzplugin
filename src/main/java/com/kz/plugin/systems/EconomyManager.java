// ============================================================
// PATH: src/main/java/com/kz/plugin/systems/EconomyManager.java
// ============================================================
package com.kz.plugin.systems;

import com.kz.plugin.KZPlugin;
import com.kz.plugin.data.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EconomyManager {

    private static final double MAX_BALANCE = 999_999_999_999.0;
    private static final double MIN_BALANCE = 0.00;
    private static final long AUTOSAVE_INTERVAL_TICKS = 6000L;

    // ── Cache ──────────────────────────────────────────────────────
    /** Key: UUID → Map(mode → balance) */
    private final Map<UUID, Map<String, Double>> balanceCache = new ConcurrentHashMap<>();
    /** UUIDs with unsaved changes */
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    /** UUID → last known player name */
    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();
    /** Manual mode override (from NPC click, proxy, etc) */
    private final Map<UUID, String> playerModeOverride = new ConcurrentHashMap<>();

    // ── Config ─────────────────────────────────────────────────────
    private final Map<String, Double> startingBalance = new HashMap<>();
    private final Map<String, String> worldModeMap = new HashMap<>();

    // ── Dependencies ───────────────────────────────────────────────
    private final KZPlugin plugin;
    private final DatabaseManager db;

    // ── Autosave ───────────────────────────────────────────────────
    private int autoSaveTaskId = -1;

    // ════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════

    public EconomyManager(KZPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;

        loadStartingBalances();
        loadWorldModeMap();
        startAutoSave();
    }

    // ════════════════════════════════════════════════════════════════
    //  CONFIG LOADING
    // ════════════════════════════════════════════════════════════════

    private void loadStartingBalances() {
        startingBalance.put("lobby", plugin.getConfig().getDouble("economy.starting-balance.lobby", 0.0));
        startingBalance.put("survival", plugin.getConfig().getDouble("economy.starting-balance.survival", 1000.0));
        startingBalance.put("vanilla", plugin.getConfig().getDouble("economy.starting-balance.vanilla", 1000.0));
        startingBalance.put("oneblock", plugin.getConfig().getDouble("economy.starting-balance.oneblock", 1000.0));
        startingBalance.put("skyblock", plugin.getConfig().getDouble("economy.starting-balance.skyblock", 1000.0));
        startingBalance.put("island", plugin.getConfig().getDouble("economy.starting-balance.island", 2000.0));
        startingBalance.put("acid", plugin.getConfig().getDouble("economy.starting-balance.acid", 500.0));

        plugin.getLogger().info("[Economy] Starting balances loaded: " + startingBalance);
    }

    private void loadWorldModeMap() {
        if (plugin.getConfig().isConfigurationSection("economy.world-mode-map")) {
            Objects.requireNonNull(
                    plugin.getConfig().getConfigurationSection("economy.world-mode-map")
            ).getKeys(false).forEach(worldName -> {
                String modeName = plugin.getConfig().getString(
                        "economy.world-mode-map." + worldName, "lobby"
                );
                worldModeMap.put(worldName.toLowerCase(), modeName.toLowerCase());
            });
        } else {
            worldModeMap.put("world_lobby", "lobby");
            worldModeMap.put("world_survival", "survival");
            worldModeMap.put("world_vanilla", "vanilla");
            worldModeMap.put("world_oneblock", "oneblock");
            worldModeMap.put("world_skyblock", "skyblock");
            worldModeMap.put("world_island", "island");
            worldModeMap.put("world_acid", "acid");
            worldModeMap.put("world", "survival");
            worldModeMap.put("world_nether", "survival");
            worldModeMap.put("world_the_end", "survival");
        }

        plugin.getLogger().info("[Economy] World-mode map loaded: " + worldModeMap);
    }

    // ════════════════════════════════════════════════════════════════
    //  AUTOSAVE SCHEDULER
    // ════════════════════════════════════════════════════════════════

    private void startAutoSave() {
        autoSaveTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Set<UUID> toSave = new HashSet<>(dirtyPlayers);
            if (toSave.isEmpty()) return;

            plugin.getLogger().info("[Economy] Autosave: saving " + toSave.size() + " player(s)...");
            for (UUID uuid : toSave) {
                savePlayerSync(uuid);
            }
            plugin.getLogger().info("[Economy] Autosave complete.");
        }, AUTOSAVE_INTERVAL_TICKS, AUTOSAVE_INTERVAL_TICKS).getTaskId();

        plugin.getLogger().info("[Economy] Autosave scheduler started (every "
                + (AUTOSAVE_INTERVAL_TICKS / 20) + "s).");
    }

    private void stopAutoSave() {
        if (autoSaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autoSaveTaskId);
            autoSaveTaskId = -1;
            plugin.getLogger().info("[Economy] Autosave scheduler stopped.");
        }
    }

    /**
     * Clean shutdown: stop autosave, save all dirty players, clear cache.
     */
    public void shutdown() {
        stopAutoSave();
        saveAll();
        balanceCache.clear();
        nameCache.clear();
        dirtyPlayers.clear();
        playerModeOverride.clear();
        plugin.getLogger().info("[Economy] Economy system shut down.");
    }

    // ════════════════════════════════════════════════════════════════
    //  MODE DETECTION - With manual override support
    // ════════════════════════════════════════════════════════════════

    /**
     * Get player's current game mode.
     * Priority: 1) Manual override  2) World-based detection  3) Server default
     */
    public String getPlayerMode(Player player) {
        // 1. Check manual override first (from NPC click, proxy, form selection)
        String override = playerModeOverride.get(player.getUniqueId());
        if (override != null) return override;

        // 2. World-based detection
        String worldName = player.getWorld().getName().toLowerCase();
        String mode = worldModeMap.get(worldName);
        if (mode != null) return mode;

        // 3. Fallback to server default
        plugin.getLogger().warning(String.format(
                "[Economy] Unknown world '%s' for player %s, using server default.",
                worldName, player.getName()
        ));
        return getServerDefaultMode();
    }

    public String getPlayerMode(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            return getPlayerMode(player);
        }
        // Check override even for offline
        String override = playerModeOverride.get(uuid);
        if (override != null) return override;
        return getServerDefaultMode();
    }

    /**
     * Manually set player's game mode override.
     * Called by: LobbySystem, ProxyMessageListener, BedrockFormManager
     */
    public void setPlayerMode(Player player, String mode) {
        playerModeOverride.put(player.getUniqueId(), mode.toLowerCase());
    }

    /**
     * Clear mode override (e.g. when player changes world naturally)
     */
    public void clearModeOverride(Player player) {
        playerModeOverride.remove(player.getUniqueId());
    }

    public String getServerDefaultMode() {
        String serverName = plugin.getConfig().getString("server-name", "lobby");
        return switch (serverName.toLowerCase()) {
            case "survival" -> "survival";
            case "void" -> "oneblock";
            case "custom" -> "island";
            default -> "lobby";
        };
    }

    public String getModeFromWorld(String worldName) {
        return worldModeMap.getOrDefault(worldName.toLowerCase(), getServerDefaultMode());
    }

    public Map<String, String> getWorldModeMap() {
        return Collections.unmodifiableMap(worldModeMap);
    }

    // ════════════════════════════════════════════════════════════════
    //  PLAYER LIFECYCLE
    // ════════════════════════════════════════════════════════════════

    public CompletableFuture<Void> loadPlayer(UUID uuid, String playerName) {
        return CompletableFuture.runAsync(() -> {
            nameCache.put(uuid, playerName);

            String sql = "SELECT mode_name, balance FROM player_economy WHERE player_uuid = ?";

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    Map<String, Double> modeBalances = new ConcurrentHashMap<>();

                    while (rs.next()) {
                        modeBalances.put(
                                rs.getString("mode_name"),
                                rs.getDouble("balance")
                        );
                    }

                    if (modeBalances.isEmpty()) {
                        plugin.getLogger().info("[Economy] New player: " + playerName + ". Initializing...");
                        initNewPlayer(uuid, playerName, modeBalances);
                    } else {
                        updatePlayerNameSync(uuid, playerName);
                    }

                    // Add any new modes
                    boolean newModesAdded = false;
                    for (Map.Entry<String, Double> entry : startingBalance.entrySet()) {
                        if (modeBalances.putIfAbsent(entry.getKey(), entry.getValue()) == null) {
                            newModesAdded = true;
                        }
                    }
                    if (newModesAdded) {
                        dirtyPlayers.add(uuid);
                    }

                    balanceCache.put(uuid, modeBalances);
                    plugin.getLogger().info("[Economy] Loaded economy data for: " + playerName);
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[Economy] Failed to load player " + playerName + ": " + e.getMessage(), e
                );
                balanceCache.put(uuid, new ConcurrentHashMap<>(startingBalance));
            }
        });
    }

    /**
     * Alias for loadPlayer() - called by ProxyMessageListener
     */
    public void loadPlayerData(Player player) {
        loadPlayer(player.getUniqueId(), player.getName());
    }

    public CompletableFuture<Void> unloadPlayer(UUID uuid) {
        return savePlayer(uuid).thenRun(() -> {
            balanceCache.remove(uuid);
            nameCache.remove(uuid);
            dirtyPlayers.remove(uuid);
            playerModeOverride.remove(uuid);
            plugin.getLogger().info("[Economy] Unloaded economy data for UUID: " + uuid);
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  INIT PLAYER - Public wrapper for IslandSystem etc.
    // ════════════════════════════════════════════════════════════════

    /**
     * Initialize a player with a starting balance in their current mode.
     * Called by: IslandSystem.createIsland()
     */
    public void initPlayer(UUID uuid, int startBalance) {
        Player player = Bukkit.getPlayer(uuid);
        String playerName = player != null ? player.getName() : nameCache.getOrDefault(uuid, "Unknown");
        String mode = player != null ? getPlayerMode(player) : getServerDefaultMode();

        // Update cache immediately
        balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(mode, (double) startBalance);
        dirtyPlayers.add(uuid);

        // Persist to DB async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = """
                    INSERT INTO player_economy (player_uuid, player_name, mode_name, balance)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        player_name = VALUES(player_name),
                        balance = VALUES(balance)
                    """;

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setString(3, mode);
                ps.setDouble(4, startBalance);
                ps.executeUpdate();

                plugin.getLogger().info("[Economy] initPlayer: " + playerName
                        + " = $" + startBalance + " in " + mode);

            } catch (SQLException e) {
                plugin.getLogger().warning("[Economy] initPlayer failed: " + e.getMessage());
            }
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  SAVE OPERATIONS
    // ════════════════════════════════════════════════════════════════

    public CompletableFuture<Void> savePlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> savePlayerSync(uuid));
    }

    private void savePlayerSync(UUID uuid) {
        Map<String, Double> modeBalances = balanceCache.get(uuid);
        if (modeBalances == null) return;
        if (!dirtyPlayers.contains(uuid)) return;

        String playerName = Optional.ofNullable(Bukkit.getPlayer(uuid))
                .map(Player::getName)
                .orElse(nameCache.getOrDefault(uuid, "Unknown"));

        Map<String, Double> snapshot = new HashMap<>(modeBalances);

        String sql = """
                INSERT INTO player_economy (player_uuid, player_name, mode_name, balance)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name = VALUES(player_name),
                    balance     = VALUES(balance)
                """;

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
                ps.setString(1, uuid.toString());
                ps.setString(2, playerName);
                ps.setString(3, entry.getKey());
                ps.setDouble(4, entry.getValue());
                ps.addBatch();
            }

            ps.executeBatch();
            dirtyPlayers.remove(uuid);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Economy] Failed to save player " + playerName + ": " + e.getMessage(), e
            );
        }
    }

    public void saveAll() {
        Set<UUID> toSave = new HashSet<>(dirtyPlayers);

        if (toSave.isEmpty()) {
            plugin.getLogger().info("[Economy] No unsaved changes. Skip saveAll.");
            return;
        }

        plugin.getLogger().info("[Economy] Saving " + toSave.size() + " player(s)...");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uuid : toSave) {
            futures.add(savePlayer(uuid));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            plugin.getLogger().info("[Economy] All economy data saved successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Economy] Error during saveAll: " + e.getMessage(), e
            );
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  BALANCE GETTERS
    // ════════════════════════════════════════════════════════════════

    public double getBalance(UUID uuid, String mode) {
        Map<String, Double> modeBalances = balanceCache.get(uuid);
        if (modeBalances == null) return MIN_BALANCE;
        return modeBalances.getOrDefault(mode.toLowerCase(), MIN_BALANCE);
    }

    public double getBalance(UUID uuid) {
        return getBalance(uuid, getPlayerMode(uuid));
    }

    public double getBalance(Player player) {
        return getBalance(player.getUniqueId(), getPlayerMode(player));
    }

    public double getBalance(Player player, String mode) {
        return getBalance(player.getUniqueId(), mode);
    }

    public Map<String, Double> getAllBalances(UUID uuid) {
        Map<String, Double> data = balanceCache.get(uuid);
        if (data == null) return Collections.emptyMap();
        return Collections.unmodifiableMap(new HashMap<>(data));
    }

    // ════════════════════════════════════════════════════════════════
    //  BALANCE SETTERS
    // ════════════════════════════════════════════════════════════════

    public void setBalance(UUID uuid, String mode, double amount) {
        double clamped = Math.max(MIN_BALANCE, Math.min(MAX_BALANCE, amount));
        balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(mode.toLowerCase(), clamped);
        dirtyPlayers.add(uuid);
    }

    public void setBalance(UUID uuid, double amount) {
        setBalance(uuid, getPlayerMode(uuid), amount);
    }

    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), getPlayerMode(player), amount);
    }

    public void setBalance(Player player, String mode, double amount) {
        setBalance(player.getUniqueId(), mode, amount);
    }

    // ════════════════════════════════════════════════════════════════
    //  BALANCE OPERATIONS
    // ════════════════════════════════════════════════════════════════

    public void addBalance(UUID uuid, String mode, double amount) {
        if (amount <= 0) return;
        setBalance(uuid, mode, getBalance(uuid, mode) + amount);
    }

    public void addBalance(UUID uuid, double amount) {
        addBalance(uuid, getPlayerMode(uuid), amount);
    }

    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), getPlayerMode(player), amount);
    }

    public boolean removeBalance(UUID uuid, String mode, double amount) {
        if (amount <= 0) return false;
        if (!hasEnough(uuid, mode, amount)) return false;
        setBalance(uuid, mode, getBalance(uuid, mode) - amount);
        return true;
    }

    public boolean removeBalance(UUID uuid, double amount) {
        return removeBalance(uuid, getPlayerMode(uuid), amount);
    }

    public boolean removeBalance(Player player, double amount) {
        return removeBalance(player.getUniqueId(), getPlayerMode(player), amount);
    }

    // ════════════════════════════════════════════════════════════════
    //  BALANCE CHECKS
    // ════════════════════════════════════════════════════════════════

    public boolean hasEnough(UUID uuid, String mode, double amount) {
        return getBalance(uuid, mode) >= amount;
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return hasEnough(uuid, getPlayerMode(uuid), amount);
    }

    public boolean hasEnough(Player player, double amount) {
        return hasEnough(player.getUniqueId(), getPlayerMode(player), amount);
    }

    public boolean isLoaded(UUID uuid) {
        return balanceCache.containsKey(uuid);
    }

    public boolean isLoaded(Player player) {
        return isLoaded(player.getUniqueId());
    }

    // ════════════════════════════════════════════════════════════════
    //  TRANSFER
    // ════════════════════════════════════════════════════════════════

    public TransferResult transfer(UUID from, UUID to, double amount) {
        if (amount <= 0) return TransferResult.INVALID_AMOUNT;
        if (!isLoaded(from)) return TransferResult.SENDER_NOT_LOADED;
        if (!isLoaded(to)) return TransferResult.RECEIVER_NOT_LOADED;

        String fromMode = getPlayerMode(from);
        String toMode = getPlayerMode(to);

        if (!fromMode.equals(toMode)) return TransferResult.DIFFERENT_MODE;
        if (!hasEnough(from, fromMode, amount)) return TransferResult.INSUFFICIENT_FUNDS;

        removeBalance(from, fromMode, amount);
        addBalance(to, toMode, amount);

        return TransferResult.SUCCESS;
    }

    public enum TransferResult {
        SUCCESS,
        INVALID_AMOUNT,
        SENDER_NOT_LOADED,
        RECEIVER_NOT_LOADED,
        DIFFERENT_MODE,
        INSUFFICIENT_FUNDS
    }

    // ════════════════════════════════════════════════════════════════
    //  TOP BALANCES
    // ════════════════════════════════════════════════════════════════

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

                ps.setString(1, mode.toLowerCase());
                ps.setInt(2, Math.max(1, limit));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(Map.entry(
                                rs.getString("player_name"),
                                rs.getDouble("balance")
                        ));
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "[Economy] Failed to get top balances: " + e.getMessage(), e
                );
            }

            return result;
        });
    }

    // ════════════════════════════════════════════════════════════════
    //  FORMAT & DISPLAY
    // ════════════════════════════════════════════════════════════════

    public String formatBalance(double amount) {
        if (amount < 0) return "-" + formatBalance(-amount);
        if (amount >= 1_000_000_000) return String.format("$%.1fB", amount / 1_000_000_000.0);
        if (amount >= 1_000_000) return String.format("$%.1fM", amount / 1_000_000.0);
        if (amount >= 1_000) return String.format("$%.1fK", amount / 1_000.0);
        return String.format("$%,.0f", amount);
    }

    public String formatBalanceFull(double amount) {
        return String.format("$%,.2f", amount);
    }

    public String getModeName(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival" -> "§aSurvival";
            case "vanilla" -> "§2Vanilla";
            case "oneblock" -> "§aOneBlock";
            case "skyblock" -> "§bSkyBlock";
            case "island" -> "§eClassic Island";
            case "acid" -> "§cAcid Island";
            case "lobby" -> "§7Lobby";
            default -> "§f" + mode;
        };
    }

    public String getModeDisplayName(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival" -> "Survival";
            case "vanilla" -> "Vanilla";
            case "oneblock" -> "OneBlock";
            case "skyblock" -> "SkyBlock";
            case "island" -> "Classic Island";
            case "acid" -> "Acid Island";
            case "lobby" -> "Lobby";
            default -> mode;
        };
    }

    // ════════════════════════════════════════════════════════════════
    //  PRIVATE DB HELPERS
    // ════════════════════════════════════════════════════════════════

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

                modeBalances.put(entry.getKey(), entry.getValue());
            }

            ps.executeBatch();
            plugin.getLogger().info("[Economy] New account created for: " + playerName
                    + " with " + startingBalance.size() + " modes.");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "[Economy] Failed to init new player " + playerName + ": " + e.getMessage(), e
            );
        }
    }

    private void updatePlayerNameSync(UUID uuid, String playerName) {
        String sql = "UPDATE player_economy SET player_name = ? WHERE player_uuid = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, playerName);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING,
                    "[Economy] Failed to update name for " + playerName + ": " + e.getMessage(), e
            );
        }
    }
}
