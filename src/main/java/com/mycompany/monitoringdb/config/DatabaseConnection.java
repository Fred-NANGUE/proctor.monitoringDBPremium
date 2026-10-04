package com.mycompany.monitoringdb.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static HikariDataSource dataSource = null;
    private static final Properties configProps = new Properties();

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                configProps.load(input);
                System.out.println("Fichier config.properties chargé avec succès.");
            } else {
                System.err.println(" config.properties non trouvé dans le classpath, utilisation des valeurs par défaut.");
            }
        } catch (IOException ex) {
            System.err.println("Erreur lors du chargement de config.properties : " + ex.getMessage());
        }
    }

    /**
     * Initialise le pool avec les paramètres de connexion.
     * Cette méthode doit être appelée une fois au démarrage (ex: après la connexion).
     */
    public static synchronized void initPool(String host, String port, String sid, String user, String password) {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("Pool déjà initialisé, fermeture de l'ancien pool...");
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();
        String jdbcUrl = "jdbc:oracle:thin:@" + host + ":" + port + "/" + sid;
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("oracle.jdbc.driver.OracleDriver");

        // Lecture des propriétés avec valeurs par défaut
        int maxPoolSize = Integer.parseInt(configProps.getProperty("db.pool.maxSize", "10"));
        int minIdle = Integer.parseInt(configProps.getProperty("db.pool.minIdle", "2"));
        long idleTimeout = Long.parseLong(configProps.getProperty("db.pool.idleTimeout", "300000"));
        long connectionTimeout = Long.parseLong(configProps.getProperty("db.pool.connectionTimeout", "20000"));

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setIdleTimeout(idleTimeout);
        config.setConnectionTimeout(connectionTimeout);

        // Optimisations Oracle
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        System.out.println("Pool de connexions HikariCP initialisé !");
        System.out.println("   - JDBC URL : " + jdbcUrl);
        System.out.println("   - Utilisateur : " + user);
        System.out.println("   - Taille max : " + maxPoolSize);
        System.out.println("   - Idle min : " + minIdle);
    }

    /**
     * Retourne une connexion du pool.
     * Le pool doit être initialisé au préalable.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Le pool de connexions n'est pas initialisé. Appelez d'abord initPool().");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Pool fermé.");
        }
    }

    // Méthode de compatibilité (si vous voulez garder l'ancienne signature)
    @Deprecated
    public static synchronized Connection getConnection(String host, String port, String sid, String user, String password) throws SQLException {
        initPool(host, port, sid, user, password);
        return getConnection();
    }
}