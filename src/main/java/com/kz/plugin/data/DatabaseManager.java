
            }
            dataSource = null;
        }
    }
}
// ============================================================
// Path: src/main/java/com/kz/plugin/data/DatabaseManager.java
// ============================================================
package com.kz.plugin.data;

import com.kz.plugin.KZPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager - Mengelola koneksi MySQL via HikariCP Connection Pool
 *
 * Fitur:
 *   - HikariCP connection pool ke PlanetScale MySQL
 *   - SSL encryption (REQUIRED mode, cocok untuk PlanetScale)
 *   - keepaliveTime agar koneksi tidak putus saat idle
 *   - Retry mechanism 3x saat koneksi gagal
 *   - Auto-create semua tabel yang dibutuhkan plugin
 *   - Leak detection untuk debugging connection leak
 *   - Thread-safe dengan volatile dataSource
 *   - Public createTable() untuk dipakai sistem lain
 */
public class DatabaseManager {

    // ══════════════════════════════════════════════════════════════
    //  CONSTANTS
    // ══════════════════════════════════════════════════════════════

    /** Jumlah maksimal percobaan koneksi ulang */
    private static final int  MAX_RETRY_ATTEMPTS = 3;

    /** Delay antar percobaan koneksi (milliseconds) */
    private static final long RETRY_DELAY_MS     = 5_000L;

    /**
     * Maximum pool size - sesuaikan dengan PlanetScale free tier limit.
     * 4 server × 5 pool = 20 connections total (aman di bawah 1000 limit)
     */
    private static final int  MAX_POOL_SIZE      = 5;

    /** Minimum idle connection yang selalu siap */
    private static final int  MIN_IDLE           = 2;

    /** Timeout menunggu koneksi dari pool (30 detik) */
    private static final long CONNECTION_TIMEOUT  = 30_000L;

    /** Koneksi idle dihapus setelah 10 menit */
    private static final long IDLE_TIMEOUT        = 600_000L;

    /** Koneksi maksimal hidup 30 menit (PlanetScale limit ~1 jam) */
    private static final long MAX_LIFETIME        = 1_800_000L;

    /**
     * Keepalive ping setiap 60 detik.
     * KRUSIAL untuk PlanetScale yang memutus koneksi idle!
     * Harus lebih kecil dari IDLE_TIMEOUT.
     */
    private static final long KEEPALIVE_TIME      = 60_000L;

    /**
     * Leak detection threshold: 60 detik.
     * Jika Connection tidak ditutup dalam 60 detik, HikariCP log WARNING.
     * Membantu mendeteksi connection leak selama development.
     */
    private static final long LEAK_DETECTION_MS   = 60_000L;

    // ══════════════════════════════════════════════════════════════
    //  FIELDS
    // ══════════════════════════════════════════════════════════════

    private final KZPlugin plugin;

    /**
     * volatile: Menjamin visibility antar thread.
     * dataSource bisa diakses dari main thread (onEnable/onDisable)
     * dan async thread (CompletableFuture di EconomyManager).
     */
    private volatile HikariDataSource dataSource;

    // ══════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════

    public DatabaseManager(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - CONNECT (dengan retry mechanism)
    // ══════════════════════════════════════════════════════════════

    /**
     * Menghubungkan ke database PlanetScale dengan retry mechanism.
     * Dipanggil dari KZPlugin.onEnable()
     *
     * @return true jika berhasil connect, false jika gagal setelah semua retry
     */
    public boolean connect() {
        // ── Ambil konfigurasi dari config.yml ──────────────────────
        String  host = plugin.getConfig().getString("database.host", "localhost");
        int     port = plugin.getConfig().getInt("database.port", 3306);
        String  name = plugin.getConfig().getString("database.name", "kzplugin");
        String  user = plugin.getConfig().getString("database.username", "root");
        String  pass = plugin.getConfig().getString("database.password", "");
        boolean ssl  = plugin.getConfig().getBoolean("database.ssl", true);

        // ── Validasi config tidak kosong ───────────────────────────
        if (host == null || host.isBlank()) {
            plugin.getLogger().severe("[Database] 'database.host' is empty in config.yml!");
            return false;
        }
        if (name == null || name.isBlank()) {
            plugin.getLogger().severe("[Database] 'database.name' is empty in config.yml!");
            return false;
        }

        // ── Build JDBC URL ─────────────────────────────────────────
        /*
         * SSL Mode explanation:
         *   REQUIRED  → Encrypt connection, don't verify server certificate
         *               Best for PlanetScale (no local CA cert needed)
         *   DISABLED  → No SSL (only for local development)
         *
         * NOTE: autoReconnect is NOT included because:
         *   - It is deprecated by MySQL Connector/J
         *   - HikariCP handles reconnection internally via pool management
         *   - Using autoReconnect can cause silent data corruption
         */
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s"
                        + "?useSSL=%b"
                        + "&requireSSL=%b"
                        + "&sslMode=%s"
                        + "&characterEncoding=utf8"
                        + "&useUnicode=true"
                        + "&serverTimezone=UTC",
                host, port, name,
                ssl,
                ssl,
                ssl ? "REQUIRED" : "DISABLED"
        );

        // ── Build HikariConfig ─────────────────────────────────────
        HikariConfig hikariConfig = buildHikariConfig(jdbcUrl, user, pass);

        // ── Retry loop ─────────────────────────────────────────────
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                plugin.getLogger().info(String.format(
                        "[Database] Connecting to MySQL... (attempt %d/%d)",
                        attempt, MAX_RETRY_ATTEMPTS
                ));

                dataSource = new HikariDataSource(hikariConfig);

                // ── Validasi koneksi benar-benar bisa dipakai ──────
                validateConnection();

                // ── Buat semua tabel yang dibutuhkan ───────────────
                createAllTables();

                plugin.getLogger().info("[Database] Successfully connected to MySQL!");
                plugin.getLogger().info(String.format(
                        "[Database] Pool config: max=%d, minIdle=%d, keepalive=%ds",
                        MAX_POOL_SIZE, MIN_IDLE, KEEPALIVE_TIME / 1000
                ));
                return true; // ✅ Success

            } catch (Exception e) {
                // ── Gagal di attempt ini ───────────────────────────
                plugin.getLogger().warning(String.format(
                        "[Database] Connection attempt %d/%d failed: %s",
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage()
                ));

                // Tutup datasource yang gagal sebelum retry
                closeDataSourceSilently();

                // Kalau masih ada attempt tersisa, tunggu dulu
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    plugin.getLogger().info(String.format(
                            "[Database] Retrying in %d seconds...",
                            RETRY_DELAY_MS / 1000
                    ));
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        // ── Semua retry habis, gagal total ─────────────────────────
        plugin.getLogger().severe("[Database] All connection attempts failed!");
        plugin.getLogger().severe("[Database] Please check config.yml database settings.");
        return false; // ❌ Failed
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - DISCONNECT
    // ══════════════════════════════════════════════════════════════

    /**
     * Menutup semua koneksi pool dengan aman.
     * Dipanggil dari KZPlugin.onDisable()
     */
    public void disconnect() {
        closeDataSourceSilently();
        plugin.getLogger().info("[Database] Connection pool closed.");
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - GET CONNECTION
    // ══════════════════════════════════════════════════════════════

    /**
     * Mengambil koneksi dari pool untuk dipakai query.
     * WAJIB ditutup dengan try-with-resources setelah pakai!
     *
     * Contoh penggunaan:
     * <pre>
     *   try (Connection conn = dbManager.getConnection();
     *        PreparedStatement ps = conn.prepareStatement(sql)) {
     *       // execute query here
     *   }
     * </pre>
     *
     * @return Connection dari HikariCP pool
     * @throws SQLException jika pool tidak tersedia atau timeout
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("[Database] Connection pool is not available!");
        }
        return dataSource.getConnection();
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - CREATE TABLE (untuk dipakai sistem lain)
    // ══════════════════════════════════════════════════════════════

    /**
     * Utility method untuk membuat tabel dari SQL string.
     * Bisa dipanggil oleh sistem lain (IslandSystem, JobSystem, dll)
     * jika mereka butuh tabel database sendiri.
     *
     * @param createTableSQL SQL CREATE TABLE IF NOT EXISTS ...
     * @param tableName      Nama tabel untuk logging
     */
    public void createTable(String createTableSQL, String tableName) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            plugin.getLogger().info("[Database] Table '" + tableName + "' is ready.");
        } catch (SQLException e) {
            plugin.getLogger().severe(
                    "[Database] Failed to create table '" + tableName + "': " + e.getMessage()
            );
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC - STATUS CHECK
    // ══════════════════════════════════════════════════════════════

    /**
     * Cek apakah database sedang terhubung dan pool aktif.
     *
     * @return true jika pool ada dan tidak closed
     */
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    /**
     * Mendapatkan statistik pool koneksi untuk debugging / admin command.
     *
     * @return String ringkasan status pool, atau "Not connected" jika tidak aktif
     */
    public String getPoolStats() {
        if (!isConnected()) return "Not connected";

        // HikariPoolMXBean bisa null jika MXBean registration disabled
        HikariPoolMXBean mxBean = dataSource.getHikariPoolMXBean();
        if (mxBean == null) return "Connected (stats unavailable)";

        return String.format(
                "Active=%d, Idle=%d, Waiting=%d, Total=%d",
                mxBean.getActiveConnections(),
                mxBean.getIdleConnections(),
                mxBean.getThreadsAwaitingConnection(),
                mxBean.getTotalConnections()
        );
    }

    /**
     * Getter plugin - untuk akses dari class lain jika diperlukan.
     */
    public KZPlugin getPlugin() {
        return plugin;
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE - BUILD HIKARI CONFIG
    // ══════════════════════════════════════════════════════════════

    /**
     * Membangun HikariConfig dengan semua pengaturan optimal
     * untuk PlanetScale / cloud MySQL.
     */
    private HikariConfig buildHikariConfig(String jdbcUrl, String user, String pass) {
        HikariConfig config = new HikariConfig();

        // ── Koneksi dasar ──────────────────────────────────────────
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(pass);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // ── Pool sizing ────────────────────────────────────────────
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);

        // ── Timeout settings ───────────────────────────────────────
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setIdleTimeout(IDLE_TIMEOUT);
        config.setMaxLifetime(MAX_LIFETIME);
        config.setKeepaliveTime(KEEPALIVE_TIME);

        // ── Leak detection ─────────────────────────────────────────
        /*
         * Jika Connection tidak di-close dalam 60 detik,
         * HikariCP akan log WARNING dengan stack trace.
         * Sangat berguna untuk mendeteksi bug connection leak
         * saat development. Set 0 untuk disable di production.
         */
        config.setLeakDetectionThreshold(LEAK_DETECTION_MS);

        // ── Pool identity ──────────────────────────────────────────
        config.setPoolName("KZPlugin-Pool");

        // ── Connection validation ──────────────────────────────────
        /*
         * TIDAK menggunakan setConnectionTestQuery("SELECT 1") karena:
         *   - MySQL Connector/J 8.x adalah JDBC4 driver
         *   - HikariCP otomatis pakai Connection.isValid() untuk JDBC4
         *   - Connection.isValid() lebih efisien (native ping, tanpa query)
         *   - Docs HikariCP: "Don't set this unless your driver is not JDBC4"
         */

        // ── MySQL PreparedStatement cache optimization ─────────────
        config.addDataSourceProperty("cachePrepStmts",          "true");
        config.addDataSourceProperty("prepStmtCacheSize",       "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit",   "2048");
        config.addDataSourceProperty("useServerPrepStmts",      "true");

        // ── MySQL additional optimization ──────────────────────────
        config.addDataSourceProperty("useLocalSessionState",     "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata",   "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits",      "true");
        config.addDataSourceProperty("maintainTimeStats",        "false");

        return config;
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE - VALIDATE CONNECTION
    // ══════════════════════════════════════════════════════════════

    /**
     * Test query untuk memvalidasi koneksi benar-benar bekerja.
     * Dipanggil setelah HikariDataSource berhasil dibuat.
     *
     * @throws SQLException jika test query gagal
     */
    private void validateConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
            plugin.getLogger().info("[Database] Connection validation successful.");
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE - CREATE ALL TABLES
    // ══════════════════════════════════════════════════════════════

    /**
     * Membuat semua tabel yang dibutuhkan plugin.
     * Dipanggil otomatis setelah koneksi berhasil.
     *
     * ENGINE=InnoDB    → support transaction & row-level locking
     * CHARSET=utf8mb4  → support emoji dan karakter unicode penuh
     */
    private void createAllTables() {
        plugin.getLogger().info("[Database] Creating/verifying tables...");

        // ── Tabel 1: Economy per mode ──────────────────────────────
        /*
         * PRIMARY KEY (player_uuid, mode_name):
         *   → 1 player bisa punya BANYAK row, 1 per mode
         *   → Mencegah duplikat kombinasi (uuid + mode)
         *
         * 7 mode: lobby, survival, vanilla, oneblock, skyblock, island, acid
         *
         * Contoh data 1 player:
         *   abc-123 | Steve | lobby    |    0.00
         *   abc-123 | Steve | survival | 5000.00
         *   abc-123 | Steve | oneblock | 2500.00
         *   abc-123 | Steve | acid     |  800.00
         */
        createTable("""
                CREATE TABLE IF NOT EXISTS player_economy (
                    player_uuid  VARCHAR(36)   NOT NULL,
                    player_name  VARCHAR(16)   NOT NULL,
                    mode_name    VARCHAR(32)   NOT NULL,
                    balance      DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                    last_updated TIMESTAMP     NOT NULL
                                 DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_uuid, mode_name),
                    INDEX idx_economy_uuid (player_uuid),
                    INDEX idx_economy_mode (mode_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                "player_economy"
        );

        // ── Tabel 2: Player data (rank, join date, dll) ────────────
        /*
         * Data player umum yang shared antar semua server.
         * Dipakai oleh: Rank System, Admin Commands, dll.
         */
        createTable("""
                CREATE TABLE IF NOT EXISTS player_data (
                    player_uuid  VARCHAR(36)  NOT NULL,
                    player_name  VARCHAR(16)  NOT NULL,
                    rank_name    VARCHAR(32)  NOT NULL DEFAULT 'member',
                    first_join   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    last_seen    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,
                    play_time    BIGINT       NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid),
                    INDEX idx_player_rank (rank_name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """,
                "player_data"
        );

        plugin.getLogger().info("[Database] All tables verified successfully.");
    }

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE - CLOSE DATASOURCE SILENTLY
    // ══════════════════════════════════════════════════════════════

    /**
     * Menutup dataSource tanpa throw exception.
     * Dipakai di retry loop dan disconnect().
     */
    private void closeDataSourceSilently() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                plugin.getLogger().warning(
                        "[Database] Error closing datasource: " + e.getMessage()
                );
            }
            dataSource = null;
        }
    }
}
