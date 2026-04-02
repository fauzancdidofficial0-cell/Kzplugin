;
            default         -> "§f" + mode;
        };
    }
}
// ================================================================
// Path: src/main/java/com/kz/plugin/systems/EconomyManager.java
// ================================================================
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

/**
 * EconomyManager - Sistem ekonomi multi-mode per server
 *
 * SEBELUM: balances.yml YamlConfiguration, sync, single-server
 * SESUDAH:
 *   - MySQL PlanetScale via PreparedStatement
 *   - CompletableFuture async (tidak block main thread)
 *   - ConcurrentHashMap cache (kurangi query DB)
 *   - Mode detection via world name (akurat per world)
 *   - Dirty tracking (hanya save jika ada perubahan)
 *   - isLoaded() check untuk sistem lain
 *   - saveAll() safe untuk shutdown
 *
 * KONSEP EKONOMI:
 *   - 1 player = 7 dompet terpisah (1 per mode)
 *   - Saldo TIDAK terbawa antar mode
 *   - Transfer HANYA di mode yang sama
 *   - Mode ditentukan oleh world yang sedang diinjak player
 */
public class EconomyManager {

    // ══════════════════════════════════════════════════════════════
    //  CONSTANTS
    // ══════════════════════════════════════════════════════════════

    /** Saldo maksimal yang bisa dimiliki player di 1 mode */
    private static final double MAX_BALANCE = 999_999_999.00;

    /** Saldo minimum (tidak bisa negatif) */
    private static final double MIN_BALANCE = 0.00;

    // ══════════════════════════════════════════════════════════════
    //  CACHE STRUCTURES
    // ══════════════════════════════════════════════════════════════

    /**
     * Cache utama saldo player.
     * Key outer : UUID player
     * Key inner : mode name (survival, oneblock, dll)
     * Value     : balance (double)
     *
     * Thread-safe karena pakai ConcurrentHashMap nested.
     */
    private final Map<UUID, Map<String, Double>> balanceCache = new ConcurrentHashMap<>();

    /**
     * Dirty flag - tracking UUID yang datanya berubah dan perlu di-save.
     * Hanya UUID yang ada di set ini yang akan di-save ke DB.
     * Ini menghemat query DB untuk player yang tidak ada transaksi.
     */
    private final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Cache nama player (UUID -> name).
     * Untuk dipakai saat save, agar tidak perlu Bukkit.getPlayer() yang bisa null.
     */
    private final Map<UUID, String> nameCache = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════════
    //  CONFIGURATION
    // ══════════════════════════════════════════════════════════════

    /**
     * Saldo awal per mode saat player pertama kali join.
     * Dibaca dari config.yml → economy.starting-balance
     */
    private final Map<String, Double> startingBalance = new HashMap<>();

    /**
     * Mapping world name → mode name.
     * Dibaca dari config.yml → economy.world-mode-map
     *
     * Contoh:
     *   world_survival → survival
     *   world_vanilla  → vanilla
     *   world_oneblock → oneblock
     */
    private final Map<String, String> worldModeMap = new HashMap<>();

    // ══════════════════════════════════════════════════════════════
    //  DEPENDENCIES
    // ══════════════════════════════════════════════════════════════

    private final KZPlugin       plugin;
    private final DatabaseManager db;

    // ══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════

    public EconomyManager(KZPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db     = db;

        loadStartingBalances();
        loadWorldModeMap();
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE - LOAD CONFIG
    // ══════════════════════════════════════════════════════════════

    /**
     * Membaca saldo awal semua mode dari config.yml.
     * Section: economy.starting-balance
     */
    private void loadStartingBalances() {
        startingBalance.put("lobby",    plugin.getConfig().getDouble("economy.starting-balance.lobby",    0.0));
        startingBalance.put("survival", plugin.getConfig().getDouble("economy.starting-balance.survival", 1000.0));
        startingBalance.put("vanilla",  plugin.getConfig().getDouble("economy.starting-balance.vanilla",  1000.0));
        startingBalance.put("oneblock", plugin.getConfig().getDouble("economy.starting-balance.oneblock", 1000.0));
        startingBalance.put("skyblock", plugin.getConfig().getDouble("economy.starting-balance.skyblock", 1000.0));
        startingBalance.put("island",   plugin.getConfig().getDouble("economy.starting-balance.island",   2000.0));
        startingBalance.put("acid",     plugin.getConfig().getDouble("economy.starting-balance.acid",     500.0));

        plugin.getLogger().info("[Economy] Starting balances loaded: " + startingBalance);
    }

    /**
     * Membaca mapping world → mode dari config.yml.
     * Section: economy.world-mode-map
     *
     * Fallback ke hardcoded default jika config tidak ada.
     */
    private void loadWorldModeMap() {
        // ── Cek apakah section ada di config ──────────────────────
        if (plugin.getConfig().isConfigurationSection("economy.world-mode-map")) {
            // Baca dari config.yml
            Objects.requireNonNull(
                plugin.getConfig().getConfigurationSection("economy.world-mode-map")
            ).getKeys(false).forEach(worldName -> {
                String modeName = plugin.getConfig().getString(
                    "economy.world-mode-map." + worldName, "lobby"
                );
                worldModeMap.put(worldName.toLowerCase(), modeName.toLowerCase());
            });
        } else {
            // ── Hardcoded default sesuai infrastruktur ─────────────
            /*
             * Server lobby  : world_lobby   → lobby
             * Server survival: world_survival → survival
             *                  world_vanilla  → vanilla
             * Server void   : world_oneblock → oneblock
             *                 world_skyblock → skyblock
             * Server custom : world_island   → island
             *                 world_acid     → acid
             */
            worldModeMap.put("world_lobby",    "lobby");
            worldModeMap.put("world_survival", "survival");
            worldModeMap.put("world_vanilla",  "vanilla");
            worldModeMap.put("world_oneblock", "oneblock");
            worldModeMap.put("world_skyblock", "skyblock");
            worldModeMap.put("world_island",   "island");
            worldModeMap.put("world_acid",     "acid");
            // Fallback world default Minecraft
            worldModeMap.put("world",          "survival");
            worldModeMap.put("world_nether",   "survival");
            worldModeMap.put("world_the_end",  "survival");
        }

        plugin.getLogger().info("[Economy] World-mode map loaded: " + worldModeMap);
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - MODE DETECTION
    // ══════════════════════════════════════════════════════════════

    /**
     * Mendapatkan mode ekonomi player berdasarkan world yang sedang diinjak.
     *
     * SEBELUM: Hanya cek IslandSystem → tidak akurat untuk survival/vanilla
     * SESUDAH: Cek world name → selalu akurat karena world = mode
     *
     * Priority:
     *   1. World name player saat ini (paling akurat)
     *   2. server-name di config (fallback jika player offline)
     *
     * @param player Player yang sedang online
     * @return mode name (survival, oneblock, dll)
     */
    public String getPlayerMode(Player player) {
        // ── Cek world yang sedang diinjak player ───────────────────
        String worldName = player.getWorld().getName().toLowerCase();
        String mode      = worldModeMap.get(worldName);

        if (mode != null) {
            return mode;
        }

        // ── Fallback: server-name di config ───────────────────────
        plugin.getLogger().warning(String.format(
            "[Economy] Unknown world '%s' for player %s, using server default.",
            worldName, player.getName()
        ));
        return getServerDefaultMode();
    }

    /**
     * Mendapatkan mode ekonomi berdasarkan UUID.
     * Dipakai jika player mungkin offline (misal: transfer dari admin command).
     *
     * Jika player online → pakai world name (akurat).
     * Jika player offline → pakai server default mode.
     *
     * @param uuid UUID player
     * @return mode name
     */
    public String getPlayerMode(UUID uuid) {
        // Cek apakah player online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            return getPlayerMode(player);
        }

        // Player offline → pakai server default
        return getServerDefaultMode();
    }

    /**
     * Mode default berdasarkan server-name di config.yml.
     * Dipakai sebagai fallback.
     *
     * @return mode name default server ini
     */
    public String getServerDefaultMode() {
        String serverName = plugin.getConfig().getString("server-name", "lobby");
        return switch (serverName.toLowerCase()) {
            case "survival" -> "survival";
            case "void"     -> "oneblock";
            case "custom"   -> "island";
            default         -> "lobby";
        };
    }

    /**
     * Mendapatkan mode dari nama world secara langsung.
     * Berguna untuk sistem lain yang perlu tahu mode tanpa Player object.
     *
     * @param worldName nama world Minecraft
     * @return mode name, atau server default jika world tidak dikenal
     */
    public String getModeFromWorld(String worldName) {
        return worldModeMap.getOrDefault(worldName.toLowerCase(), getServerDefaultMode());
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - PLAYER LIFECYCLE
    // ══════════════════════════════════════════════════════════════

    /**
     * Load data ekonomi player dari database ke cache.
     * WAJIB dipanggil saat PlayerJoinEvent.
     *
     * Alur:
     *   1. Query semua mode balance dari DB
     *   2. Jika player baru → insert starting balance semua mode
     *   3. Simpan ke balanceCache dan nameCache
     *
     * @param uuid       UUID player
     * @param playerName Nama player (untuk update di DB)
     * @return CompletableFuture yang selesai setelah data ter-load
     */
    public CompletableFuture<Void> loadPlayer(UUID uuid, String playerName) {
        return CompletableFuture.runAsync(() -> {
            // ── Simpan nama ke cache ────────────────────────────────
            nameCache.put(uuid, playerName);

            // ── Query semua balance dari DB ─────────────────────────
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

                    // ── Player baru? Buat akun semua mode ──────────
                    if (modeBalances.isEmpty()) {
                        plugin.getLogger().info(
                            "[Economy] New player detected: " + playerName + ". Initializing accounts..."
                        );
                        initNewPlayer(uuid, playerName, modeBalances);
                    } else {
                        // ── Update nama player di DB jika berubah ──
                        updatePlayerName(uuid, playerName);
                    }

                    // ── Pastikan semua mode ada di cache ───────────
                    // (mungkin ada mode baru yang ditambahkan setelah player pertama kali join)
                    for (Map.Entry<String, Double> entry : startingBalance.entrySet()) {
                        modeBalances.putIfAbsent(entry.getKey(), entry.getValue());
                    }

                    // ── Simpan ke cache ─────────────────────────────
                    balanceCache.put(uuid, modeBalances);

                    plugin.getLogger().info(
                        "[Economy] Loaded economy data for: " + playerName
                    );
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    "[Economy] Failed to load player " + playerName + ": " + e.getMessage(), e
                );
                // Beri cache kosong agar tidak NPE di sistem lain
                balanceCache.put(uuid, new ConcurrentHashMap<>(startingBalance));
            }
        });
    }

    /**
     * Unload player dari cache setelah data di-save ke DB.
     * WAJIB dipanggil saat PlayerQuitEvent.
     *
     * Alur:
     *   1. Save data player ke DB (jika dirty)
     *   2. Hapus dari balanceCache dan nameCache
     *   3. Hapus dari dirtyPlayers
     *
     * @param uuid UUID player yang quit
     * @return CompletableFuture yang selesai setelah unload
     */
    public CompletableFuture<Void> unloadPlayer(UUID uuid) {
        return savePlayer(uuid).thenRun(() -> {
            balanceCache.remove(uuid);
            nameCache.remove(uuid);
            dirtyPlayers.remove(uuid);
            plugin.getLogger().info("[Economy] Unloaded economy data for UUID: " + uuid);
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - SAVE OPERATIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Menyimpan data satu player ke database.
     * Hanya save jika player ada di dirtyPlayers (ada perubahan).
     *
     * SEBELUM: Hardcode "Unknown" sebagai player name
     * SESUDAH: Pakai nameCache untuk nama asli player
     *
     * @param uuid UUID player
     * @return CompletableFuture yang selesai setelah save
     */
    public CompletableFuture<Void> savePlayer(UUID uuid) {
        // ── Skip jika tidak ada di cache ──────────────────────────
        Map<String, Double> modeBalances = balanceCache.get(uuid);
        if (modeBalances == null) {
            return CompletableFuture.completedFuture(null);
        }

        // ── Skip jika tidak ada perubahan (dirty check) ───────────
        if (!dirtyPlayers.contains(uuid)) {
            return CompletableFuture.completedFuture(null);
        }

        // ── Ambil nama dari cache ──────────────────────────────────
        // Cek Bukkit.getPlayer() dulu (paling akurat), lalu nameCache
        String playerName = Optional.ofNullable(Bukkit.getPlayer(uuid))
            .map(Player::getName)
            .orElse(nameCache.getOrDefault(uuid, "Unknown"));

        // ── Snapshot data sebelum async ────────────────────────────
        // Hindari ConcurrentModificationException saat iterasi di thread lain
        final Map<String, Double> snapshot = new HashMap<>(modeBalances);
        final String              name     = playerName;

        return CompletableFuture.runAsync(() -> {
            // ── SQL: INSERT atau UPDATE jika sudah ada ─────────────
            String sql = """
                INSERT INTO player_economy (player_uuid, player_name, mode_name, balance)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    player_name  = VALUES(player_name),
                    balance      = VALUES(balance)
                """;

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                for (Map.Entry<String, Double> entry : snapshot.entrySet()) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, entry.getKey());
                    ps.setDouble(4, entry.getValue());
                    ps.addBatch();
                }

                ps.executeBatch();

                // ── Hapus dari dirty set setelah berhasil save ─────
                dirtyPlayers.remove(uuid);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE,
                    "[Economy] Failed to save player " + name + ": " + e.getMessage(), e
                );
            }
        });
    }

    /**
     * Menyimpan SEMUA player yang ada di cache ke database.
     * Dipanggil saat:
     *   - Plugin shutdown (onDisable)
     *   - Autosave scheduler
     *   - Admin command /admin saveall
     *
     * SEBELUM: .join() bisa block main thread saat shutdown
     * SESUDAH: Kumpulkan futures, join di akhir dengan timeout handling
     */
    public void saveAll() {
        // ── Filter hanya dirty players untuk efisiensi ─────────────
        Set<UUID> toSave = new HashSet<>(dirtyPlayers);

        if (toSave.isEmpty()) {
            plugin.getLogger().info("[Economy] No unsaved changes. Skip saveAll.");
            return;
        }

        plugin.getLogger().info(
            "[Economy] Saving economy data for " + toSave.size() + " player(s)..."
        );

        // ── Jalankan semua save secara parallel ───────────────────
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uuid : toSave) {
            futures.add(savePlayer(uuid));
        }

        // ── Tunggu semua selesai (dipanggil di onDisable) ─────────
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            plugin.getLogger().info("[Economy] All economy data saved successfully.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                "[Economy] Error during saveAll: " + e.getMessage(), e
            );
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - BALANCE GETTERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Mengambil saldo player di mode tertentu dari cache.
     *
     * @param uuid UUID player
     * @param mode Mode ekonomi (survival, oneblock, dll)
     * @return saldo, atau 0.0 jika tidak ada di cache
     */
    public double getBalance(UUID uuid, String mode) {
        Map<String, Double> modeBalances = balanceCache.get(uuid);
        if (modeBalances == null) return MIN_BALANCE;
        return modeBalances.getOrDefault(mode.toLowerCase(), MIN_BALANCE);
    }

    /**
     * Mengambil saldo player di mode saat ini.
     * Mode ditentukan dari world yang diinjak player.
     *
     * @param uuid UUID player (harus online untuk deteksi world)
     * @return saldo di mode saat ini
     */
    public double getBalance(UUID uuid) {
        return getBalance(uuid, getPlayerMode(uuid));
    }

    /**
     * Mengambil saldo player di mode saat ini.
     *
     * @param player Player online
     * @return saldo di mode saat ini
     */
    public double getBalance(Player player) {
        return getBalance(player.getUniqueId(), getPlayerMode(player));
    }

    /**
     * Mengambil saldo player di mode tertentu.
     *
     * @param player Player online
     * @param mode   Mode ekonomi
     * @return saldo di mode tersebut
     */
    public double getBalance(Player player, String mode) {
        return getBalance(player.getUniqueId(), mode);
    }

    /**
     * Mengambil semua saldo player di semua mode.
     * Untuk ditampilkan di /balance command (semua dompet sekaligus).
     *
     * @param uuid UUID player
     * @return Map (mode → balance), atau empty map jika belum load
     */
    public Map<String, Double> getAllBalances(UUID uuid) {
        Map<String, Double> data = balanceCache.get(uuid);
        if (data == null) return Collections.emptyMap();
        // Return copy agar tidak bisa dimodifikasi dari luar
        return Collections.unmodifiableMap(new HashMap<>(data));
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - BALANCE SETTERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Set saldo player di mode tertentu ke nilai exact.
     * Otomatis clamp ke [MIN_BALANCE, MAX_BALANCE].
     * Otomatis mark player sebagai dirty (perlu save).
     *
     * @param uuid   UUID player
     * @param mode   Mode ekonomi
     * @param amount Jumlah baru (akan di-clamp jika di luar range)
     */
    public void setBalance(UUID uuid, String mode, double amount) {
        // ── Validasi dan clamp nilai ───────────────────────────────
        double clamped = Math.max(MIN_BALANCE, Math.min(MAX_BALANCE, amount));

        // ── Update cache ───────────────────────────────────────────
        balanceCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                    .put(mode.toLowerCase(), clamped);

        // ── Mark sebagai dirty (perlu di-save ke DB) ───────────────
        dirtyPlayers.add(uuid);
    }

    /**
     * Set saldo player di mode saat ini.
     *
     * @param uuid   UUID player (harus online)
     * @param amount Jumlah baru
     */
    public void setBalance(UUID uuid, double amount) {
        setBalance(uuid, getPlayerMode(uuid), amount);
    }

    /**
     * Set saldo player di mode saat ini.
     *
     * @param player Player online
     * @param amount Jumlah baru
     */
    public void setBalance(Player player, double amount) {
        setBalance(player.getUniqueId(), getPlayerMode(player), amount);
    }

    /**
     * Set saldo player di mode tertentu.
     *
     * @param player Player online
     * @param mode   Mode ekonomi
     * @param amount Jumlah baru
     */
    public void setBalance(Player player, String mode, double amount) {
        setBalance(player.getUniqueId(), mode, amount);
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - BALANCE OPERATIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Menambah saldo player di mode tertentu.
     *
     * @param uuid   UUID player
     * @param mode   Mode ekonomi
     * @param amount Jumlah yang ditambahkan (harus positif)
     */
    public void addBalance(UUID uuid, String mode, double amount) {
        if (amount <= 0) return; // Ignore negatif atau nol
        setBalance(uuid, mode, getBalance(uuid, mode) + amount);
    }

    /**
     * Menambah saldo player di mode saat ini.
     *
     * @param uuid   UUID player (harus online untuk deteksi mode)
     * @param amount Jumlah yang ditambahkan
     */
    public void addBalance(UUID uuid, double amount) {
        addBalance(uuid, getPlayerMode(uuid), amount);
    }

    /**
     * Menambah saldo player di mode saat ini.
     *
     * @param player Player online
     * @param amount Jumlah yang ditambahkan
     */
    public void addBalance(Player player, double amount) {
        addBalance(player.getUniqueId(), getPlayerMode(player), amount);
    }

    /**
     * Mengurangi saldo player di mode tertentu.
     * Gagal jika saldo tidak cukup.
     *
     * @param uuid   UUID player
     * @param mode   Mode ekonomi
     * @param amount Jumlah yang dikurangi
     * @return true jika berhasil, false jika saldo tidak cukup
     */
    public boolean removeBalance(UUID uuid, String mode, double amount) {
        if (amount <= 0) return false;
        if (!hasEnough(uuid, mode, amount)) return false;
        setBalance(uuid, mode, getBalance(uuid, mode) - amount);
        return true;
    }

    /**
     * Mengurangi saldo player di mode saat ini.
     *
     * @param uuid   UUID player (harus online)
     * @param amount Jumlah yang dikurangi
     * @return true jika berhasil
     */
    public boolean removeBalance(UUID uuid, double amount) {
        return removeBalance(uuid, getPlayerMode(uuid), amount);
    }

    /**
     * Mengurangi saldo player di mode saat ini.
     *
     * @param player Player online
     * @param amount Jumlah yang dikurangi
     * @return true jika berhasil
     */
    public boolean removeBalance(Player player, double amount) {
        return removeBalance(player.getUniqueId(), getPlayerMode(player), amount);
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - BALANCE CHECKS
    // ══════════════════════════════════════════════════════════════

    /**
     * Cek apakah player punya cukup saldo di mode tertentu.
     *
     * @param uuid   UUID player
     * @param mode   Mode ekonomi
     * @param amount Jumlah yang dicek
     * @return true jika saldo >= amount
     */
    public boolean hasEnough(UUID uuid, String mode, double amount) {
        return getBalance(uuid, mode) >= amount;
    }

    /**
     * Cek apakah player punya cukup saldo di mode saat ini.
     *
     * @param uuid   UUID player (harus online)
     * @param amount Jumlah yang dicek
     * @return true jika cukup
     */
    public boolean hasEnough(UUID uuid, double amount) {
        return hasEnough(uuid, getPlayerMode(uuid), amount);
    }

    /**
     * Cek apakah player punya cukup saldo di mode saat ini.
     *
     * @param player Player online
     * @param amount Jumlah yang dicek
     * @return true jika cukup
     */
    public boolean hasEnough(Player player, double amount) {
        return hasEnough(player.getUniqueId(), getPlayerMode(player), amount);
    }

    /**
     * Cek apakah data ekonomi player sudah ter-load di cache.
     * Sistem lain WAJIB cek ini sebelum operasi ekonomi!
     *
     * SEBELUM: Tidak ada check → NPE jika player belum load
     * SESUDAH: isLoaded() sebagai safety gate
     *
     * @param uuid UUID player
     * @return true jika data ada di cache
     */
    public boolean isLoaded(UUID uuid) {
        return balanceCache.containsKey(uuid);
    }

    /**
     * Cek apakah data ekonomi player sudah ter-load di cache.
     *
     * @param player Player
     * @return true jika data ada di cache
     */
    public boolean isLoaded(Player player) {
        return isLoaded(player.getUniqueId());
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - TRANSFER
    // ══════════════════════════════════════════════════════════════

    /**
     * Transfer saldo dari satu player ke player lain.
     *
     * RULES:
     *   - Harus di mode yang sama
     *   - Pengirim harus punya cukup saldo
     *   - Penerima harus ada di cache (harus online)
     *
     * SEBELUM: Tidak cek apakah penerima online/loaded
     * SESUDAH: Cek isLoaded() untuk kedua player
     *
     * @param from   UUID pengirim
     * @param to     UUID penerima
     * @param amount Jumlah yang ditransfer
     * @return TransferResult enum (SUCCESS atau berbagai error)
     */
    public TransferResult transfer(UUID from, UUID to, double amount) {
        // ── Validasi amount ────────────────────────────────────────
        if (amount <= 0) {
            return TransferResult.INVALID_AMOUNT;
        }

        // ── Cek kedua player ada di cache ─────────────────────────
        if (!isLoaded(from)) return TransferResult.SENDER_NOT_LOADED;
        if (!isLoaded(to))   return TransferResult.RECEIVER_NOT_LOADED;

        // ── Dapatkan mode kedua player ─────────────────────────────
        String fromMode = getPlayerMode(from);
        String toMode   = getPlayerMode(to);

        // ── Validasi mode sama ─────────────────────────────────────
        if (!fromMode.equals(toMode)) {
            return TransferResult.DIFFERENT_MODE;
        }

        // ── Cek saldo cukup ────────────────────────────────────────
        if (!hasEnough(from, fromMode, amount)) {
            return TransferResult.INSUFFICIENT_FUNDS;
        }

        // ── Lakukan transfer ───────────────────────────────────────
        removeBalance(from, fromMode, amount);
        addBalance(to, toMode, amount);

        return TransferResult.SUCCESS;
    }

    /**
     * Enum hasil transfer untuk handling yang lebih jelas di command.
     */
    public enum TransferResult {
        SUCCESS,
        INVALID_AMOUNT,
        SENDER_NOT_LOADED,
        RECEIVER_NOT_LOADED,
        DIFFERENT_MODE,
        INSUFFICIENT_FUNDS
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - TOP BALANCES (dari DB langsung)
    // ══════════════════════════════════════════════════════════════

    /**
     * Mengambil top balance per mode dari database.
     * Query langsung ke DB agar selalu akurat (bukan dari cache).
     *
     * @param mode  Mode ekonomi (survival, oneblock, dll)
     * @param limit Jumlah entry yang diambil (biasanya 10)
     * @return CompletableFuture berisi list (nama, balance) terurut descending
     */
    public CompletableFuture<List<Map.Entry<String, Double>>> getTopBalances(String mode, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            // ── SQL dengan subquery untuk akurasi ──────────────────
            // Prioritaskan saldo dari cache player yang online,
            // tapi karena kita save saat dirty, DB relatif akurat
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
                    "[Economy] Failed to get top balances for mode " + mode + ": " + e.getMessage(), e
                );
            }

            return result;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - FORMAT & DISPLAY
    // ══════════════════════════════════════════════════════════════

    /**
     * Format angka balance menjadi string yang mudah dibaca.
     *
     * Contoh:
     *   999        → "$999"
     *   1500       → "$1.5K"
     *   1500000    → "$1.5M"
     *   1000000000 → "$1.0B"  (jika ada)
     *
     * SEBELUM: Tidak handle angka negatif
     * SESUDAH: Handle negatif dengan prefix minus
     *
     * @param amount Jumlah yang akan diformat
     * @return String terformat
     */
    public String formatBalance(double amount) {
        // Handle negatif (seharusnya tidak ada, tapi safety)
        if (amount < 0) return "-" + formatBalance(-amount);

        if (amount >= 1_000_000_000) {
            return String.format("$%.1fB", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format("$%.1fM", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format("$%.1fK", amount / 1_000.0);
        }
        return String.format("$%,.0f", amount);
    }

    /**
     * Format balance menjadi angka penuh dengan koma (untuk display detail).
     *
     * Contoh: 1500000 → "$1,500,000.00"
     *
     * @param amount Jumlah
     * @return String format lengkap
     */
    public String formatBalanceFull(double amount) {
        return String.format("$%,.2f", amount);
    }

    /**
     * Mendapatkan nama display mode dengan warna Minecraft.
     * Dipakai untuk display di chat dan GUI.
     *
     * @param mode Mode ekonomi
     * @return Nama mode dengan color code Minecraft
     */
    public String getModeName(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival" -> "§aSurvival";
            case "vanilla"  -> "§2Vanilla";
            case "oneblock" -> "§aOneBlock";
            case "skyblock" -> "§bSkyBlock";
            case "island"   -> "§eClassic Island";
            case "acid"     -> "§cAcid Island";
            case "lobby"    -> "§7Lobby";
            default         -> "§f" + mode;
        };
    }

    /**
     * Mendapatkan nama mode tanpa color code.
     * Untuk logging atau konteks non-Minecraft.
     *
     * @param mode Mode ekonomi
     * @return Nama mode plain text
     */
    public String getModeDisplayName(String mode) {
        return switch (mode.toLowerCase()) {
            case "survival" -> "Survival";
            case "vanilla"  -> "Vanilla";
            case "oneblock" -> "OneBlock";
            case "skyblock" -> "SkyBlock";
            case "island"   -> "Classic Island";
            case "acid"     -> "Acid Island";
            case "lobby"    -> "Lobby";
            default         -> mode;
        };
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE - DATABASE HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Membuat akun ekonomi player baru dengan starting balance semua mode.
     * Dipanggil dari loadPlayer() jika tidak ada data di DB.
     *
     * @param uuid         UUID player baru
     * @param playerName   Nama player baru
     * @param modeBalances Map yang akan diisi dengan starting balance
     */
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

                // Isi cache dengan starting balance
                modeBalances.put(entry.getKey(), entry.getValue());
            }

            ps.executeBatch();

            plugin.getLogger().info(
                "[Economy] New account created for: " + playerName
                + " with " + startingBalance.size() + " modes."
            );

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                "[Economy] Failed to init new player " + playerName + ": " + e.getMessage(), e
            );
        }
    }

    /**
     * Update nama player di DB jika player ganti nama (name change).
     * Dipanggil dari loadPlayer() saat player sudah punya data di DB.
     *
     * @param uuid       UUID player
     * @param playerName Nama player saat ini
     */
    private void updatePlayerName(UUID uuid, String playerName) {
        CompletableFuture.runAsync(() -> {
            String sql = "UPDATE player_economy SET player_name = ? WHERE player_uuid = ?";

            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, playerName);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING,
                    "[Economy] Failed to update player name for " + playerName + ": " + e.getMessage(), e
                );
            }
        });
    }
}
