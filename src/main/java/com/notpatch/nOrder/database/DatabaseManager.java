package com.notpatch.nOrder.database;

import com.notpatch.nOrder.NOrder;
import com.notpatch.nlib.util.NLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.configuration.Configuration;

import java.io.File;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseManager {

    @Getter
    private HikariDataSource dataSource;
    @Getter
    private final ExecutorService executor;
    private final NOrder main;
    private final Configuration configuration;

    @Getter
    private boolean usingSQLite = false;

    public DatabaseManager(NOrder main) {
        this.main = main;
        this.configuration = main.getConfig();
        this.executor = Executors.newFixedThreadPool(3);
    }

    public void connect() {
        if (configuration.getString("database.type").equalsIgnoreCase("mysql")) {
            if (!connectToMySQL()) {
                NLogger.warn("Unable to connect to MySQL database, falling back to SQLite.");
                connectToSQLite();
            }
        } else {
            connectToSQLite();
        }
    }

    private boolean connectToMySQL() {
        try {
            HikariConfig config = new HikariConfig();

            String host = configuration.getString("database.host");
            String database = configuration.getString("database.database");
            String username = configuration.getString("database.username");
            String password = configuration.getString("database.password");
            int port = configuration.getInt("database.port", 3306);

            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);

            configureMySQLPool(config);

            dataSource = new HikariDataSource(config);
            testConnection();

            NLogger.info("Connected to MySQL");
            return true;

        } catch (Exception e) {
            return false;
        }
    }


    private void connectToSQLite() {
        try {
            Class.forName("org.sqlite.JDBC");

            File dataFolder = main.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "database.db");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");

            configureSQLitePool(config);

            dataSource = new HikariDataSource(config);
            usingSQLite = true;

            testConnection();
            NLogger.info("Connected to SQLite");

        } catch (Exception e) {
            NLogger.error("Unable to connect to SQLite database! Check your config.yml file for correct connection settings and try again. If the problem persists, contact the developer for support.!");
        }
    }

    private void configureMySQLPool(HikariConfig config) {

        int poolSize = configuration.getInt("database.pool-size");
        int minimumIdle = configuration.getInt("database.minimum-idle");
        long maxLifeTime = configuration.getLong("database.maximum-lifetime");
        int keepAliveTime = configuration.getInt("database.keepalive-time");
        long connectionTimeout = configuration.getLong("database.connection-timeout");

        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(minimumIdle);
        config.setMaxLifetime(maxLifeTime);
        config.setKeepaliveTime(keepAliveTime);
        config.setConnectionTimeout(connectionTimeout);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }

    private void configureSQLitePool(HikariConfig config) {
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setPoolName("norder");
    }

    private void testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                NLogger.error("Database connection test failed!");
            }
        } catch (SQLException e) {
            NLogger.error("Database connection test failed!");
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
    }

    public void createTables() {
        String createOrderTableMySQL = """
                CREATE TABLE IF NOT EXISTS orders (
                    order_id VARCHAR(8) NOT NULL PRIMARY KEY,
                    player_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    material VARCHAR(50) NOT NULL,
                    custom_item_id VARCHAR(100) DEFAULT NULL,
                    enchantments TEXT DEFAULT NULL,
                    amount INT NOT NULL,
                    price DOUBLE NOT NULL,
                    delivered INT DEFAULT 0,
                    collected INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    highlight BOOLEAN DEFAULT FALSE,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    INDEX idx_expires_at (expires_at)
                );
                
                CREATE TABLE IF NOT EXISTS player_stats (
                    player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    delivered_items INT DEFAULT 0,
                    collected_items INT DEFAULT 0,
                    total_orders INT DEFAULT 0,
                    total_earnings DOUBLE DEFAULT 0.0
                )
                """;

        String createOrderTableSQLite = """
                CREATE TABLE IF NOT EXISTS orders (
                    order_id VARCHAR(8) NOT NULL PRIMARY KEY,
                    player_id VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16) NOT NULL,
                    material VARCHAR(50) NOT NULL,
                    custom_item_id VARCHAR(100) DEFAULT NULL,
                    enchantments TEXT DEFAULT NULL,
                    amount INT NOT NULL,
                    price DOUBLE NOT NULL,
                    delivered INT DEFAULT 0,
                    collected INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    highlight BOOLEAN DEFAULT FALSE,
                    status VARCHAR(20) DEFAULT 'ACTIVE'
                );
                
                CREATE TABLE IF NOT EXISTS player_stats (
                    player_id VARCHAR(36) NOT NULL PRIMARY KEY,
                    player_name VARCHAR(16) NOT NULL,
                    delivered_items INT DEFAULT 0,
                    collected_items INT DEFAULT 0,
                    total_orders INT DEFAULT 0,
                    total_earnings DOUBLE DEFAULT 0.0
                )
                """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String createOrderTable = usingSQLite ? createOrderTableSQLite : createOrderTableMySQL;
            stmt.executeUpdate(createOrderTable);

            if (usingSQLite) {
                try {
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_expires_at ON orders(expires_at)");
                } catch (SQLException e) {
                    NLogger.warn("Failed to create index on SQLite: " + e.getMessage());
                }
            }

            NLogger.info("Created orders table successfully.");

            runMigrations(conn);

        } catch (SQLException e) {
            NLogger.error("Failed to create orders table: " + e.getMessage());
        }
    }


    private void runMigrations(Connection conn) {
        try {
            if (!columnExists(conn, "orders", "custom_item_id")) {
                addCustomItemIdColumn(conn);
            }
        } catch (Exception e) {
            NLogger.error("Failed to run migrations: " + e.getMessage());
        }
    }


    private boolean columnExists(Connection conn, String tableName, String columnName) {
        try {
            String query = usingSQLite
                    ? "PRAGMA table_info(" + tableName + ")"
                    : "SHOW COLUMNS FROM " + tableName + " LIKE ?";

            if (usingSQLite) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        String colName = rs.getString("name");
                        if (colName.equalsIgnoreCase(columnName)) {
                            return true;
                        }
                    }
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, columnName);
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        } catch (SQLException e) {
            NLogger.warn("Failed to check if column exists: " + e.getMessage());
        }
        return false;
    }


    private void addCustomItemIdColumn(Connection conn) {
        try {
            String sql = usingSQLite
                    ? "ALTER TABLE orders ADD COLUMN custom_item_id VARCHAR(100) DEFAULT NULL"
                    : "ALTER TABLE orders ADD COLUMN custom_item_id VARCHAR(100) DEFAULT NULL AFTER material";

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
        } catch (SQLException e) {
        }
    }

    public boolean isConnectionValid() {
        if (dataSource == null) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(1000);
        } catch (SQLException e) {
            return false;
        }
    }


}
