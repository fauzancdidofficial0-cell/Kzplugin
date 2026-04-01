// Path: src/main/java/com/kz/plugin/data/DatabaseManager.java
package com.kz.plugin.data;

import com.kz.plugin.KZPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final KZPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(KZPlugin plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════
    //  CONNECT KE PLANETSCALE
    // ══════════════════════════════════════

    public void connect() {
        HikariConfig config = new HikariConfig();

        // Ambil dari config.yml
        String host = plugin.getConfig().getString("database.host");
        int port = plugin.getConfig().getInt("database.port", 3306);
        String name = plugin.getConfig().getString("database.name");
        String user = plugin.getConfig().getString("database.username");
        String pass = plugin.getConfig().getString("database.password");
        boolean ssl = plugin.getConfig().getBoolean("database.ssl", true);

        // JDBC URL untuk PlanetScale
        config.setJdbcUrl(String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=%s&autoReconnect=true&characterEncoding=utf8",
            host, port, name, ssl
        ));

        config.setUsername(user);
        config.setPassword(pass);

        // Pool settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);  // 30 detik
        config.setIdleTimeout(600000);       // 10 menit
        config.setMaxLifetime(1800000);      // 30 menit
        config.setPoolName("KZPlugin-Pool");

        // Optimasi MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("§aDatabase terhubung ke PlanetScale!");
            createTables(); // Auto buat tabel
        } catch (Exception e) {
            plugin.getLogger().severe("§cGagal konek database: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════
    //  BUAT TABEL OTOMATIS
    // ══════════════════════════════════════

    private void createTables() {
        // Tabel ekonomi per mode
        String economyTable = """
            CREATE TABLE IF NOT EXISTS player_economy (
                player_uuid  VARCHAR(36)    NOT NULL,
                player_name  VARCHAR(16)    NOT NULL,
                mode_name    VARCHAR(32)    NOT NULL,
                balance      DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
                last_updated TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (player_uuid, mode_name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(economyTable);
            plugin.getLogger().info("§aTabel database siap!");
        } catch (SQLException e) {
            plugin.getLogger().severe("§cGagal buat tabel: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════
    //  GET CONNECTION
    // ══════════════════════════════════════

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database tidak terhubung!");
        }
        return dataSource.getConnection();
    }

    // ══════════════════════════════════════
    //  DISCONNECT
    // ══════════════════════════════════════

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("§eDatabase disconnected.");
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
