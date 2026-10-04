package com.mycompany.monitoringdb.config;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OracleQueries {

    // === CACHE pour les données quasi-statiques ===
    private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.SECONDS.toMillis(30); // 30 secondes

    private static class CacheEntry {

        final Object value;
        final long timestamp;

        CacheEntry(Object value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }

    @FunctionalInterface
    private interface SQLSupplier<T> {

        T get() throws SQLException;
    }

    @SuppressWarnings("unchecked")
    private <T> T getCachedOrFetch(String key, SQLSupplier<T> supplier) throws SQLException {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return (T) entry.value;
        }
        T result = supplier.get();
        cache.put(key, new CacheEntry(result));
        return result;
    }

    // === Connexion via le pool (à créer : ConnexionPool) ===
    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection(); // Vous devez créer cette classe
    }

    // === Méthode utilitaire pour une colonne unique ===
    private String getSingleValue(String sql) throws SQLException {
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getString(1) : "N/A";
        }
    }

    // === Validation des identifiants Oracle ===
    private void validateIdentifier(String id) {
        if (id == null || !id.matches("^[a-zA-Z0-9_$#]+$")) {
            throw new IllegalArgumentException("Identifiant invalide : " + id);
        }
    }

    // 1. INFORMATIONS GÉNÉRALES (avec cache)
    public String getHost() throws SQLException {
        return getCachedOrFetch("host", () -> getSingleValue("SELECT host_name FROM v$instance"));
    }

    public String getOS() throws SQLException {
        return getCachedOrFetch("os", () -> getSingleValue("SELECT platform_name FROM v$database"));
    }

    public String getVersion() throws SQLException {
        return getCachedOrFetch("version", () -> getSingleValue("SELECT version FROM v$instance"));
    }

    public String getStatus() throws SQLException {
        return getCachedOrFetch("status", () -> getSingleValue("SELECT status FROM v$instance"));
    }

    public String getUptime() throws SQLException {
        return getSingleValue("SELECT TO_CHAR(startup_time, 'DD-MON-YYYY HH24:MI:SS') FROM v$instance");
    }

    public String getSID() throws SQLException {
        return getCachedOrFetch("sid", () -> getSingleValue("SELECT instance_name FROM v$instance"));
    }

    public String getStatusArchiveLog() throws SQLException {
        return getCachedOrFetch("archivelog", () -> getSingleValue("SELECT ARCHIVER FROM v$instance"));
    }

    public String getDBSize() throws SQLException {
        return getCachedOrFetch("dbsize", () -> {
            String val = getSingleValue("SELECT ROUND(SUM(bytes)/1024/1024/1024, 2) FROM dba_data_files");
            return val.equals("N/A") ? val : val + " GB";
        });
    }

    public String getTablespaceUsage() throws SQLException {
        return getCachedOrFetch("tablespace_usage", () -> {
            String val = getSingleValue("SELECT ROUND((SELECT SUM(bytes) FROM dba_data_files) - (SELECT SUM(bytes) FROM dba_free_space), 2) / 1024 / 1024 / 1024 FROM dual");
            return val.equals("N/A") ? val : val + " GB used";
        });
    }

    public String getActiveSessions() throws SQLException {
        // Pas de cache car valeur dynamique
        return getSingleValue("SELECT COUNT(*) FROM v$session WHERE status = 'ACTIVE' AND username IS NOT NULL");
    }

    public String getSgaUsagePercent() throws SQLException {
        return getSingleValue("SELECT (used_bytes / total_bytes) * 100 FROM v$sga");
    }

    public String getEtatAWR() throws SQLException {
        return getCachedOrFetch("awr", () -> getSingleValue("SELECT value FROM v$parameter WHERE name = 'control_management_pack_access'"));
    }

    // Graphique 
    public String getCdbName() throws SQLException {
        String sql = "SELECT name FROM v$database";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "N/A";
        }
    }

    public List<Map<String, Object>> getPdbList() throws SQLException {
    List<Map<String, Object>> pdbList = new ArrayList<>();
    
    // Essayer avec CDB_DATA_FILES (si dans le CDB et droits suffisants)
    try {
        String sql = "SELECT p.pdb_name, ROUND(SUM(df.bytes)/1024/1024/1024, 2) AS size_gb " +
                     "FROM cdb_data_files df JOIN v$pdbs p ON df.con_id = p.con_id " +
                     "GROUP BY p.pdb_name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> pdb = new HashMap<>();
                pdb.put("name", rs.getString("pdb_name"));
                pdb.put("size", rs.getDouble("size_gb"));
                pdb.put("sessions", 0);
                pdbList.add(pdb);
            }
            return pdbList;
        }
    } catch (SQLException e) {
        System.out.println("ℹ️ CDB_DATA_FILES non accessible, fallback sur DBA_DATA_FILES");
    }
    
    // Fallback : DBA_DATA_FILES (accessible dans la PDB)
    try {
        String sql = "SELECT tablespace_name, ROUND(SUM(bytes)/1024/1024/1024, 2) AS size_gb " +
                     "FROM dba_data_files GROUP BY tablespace_name ORDER BY tablespace_name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> pdb = new HashMap<>();
                pdb.put("name", rs.getString("tablespace_name"));
                pdb.put("size", rs.getDouble("size_gb"));
                pdb.put("sessions", 0);
                pdbList.add(pdb);
            }
            return pdbList;
        }
    } catch (SQLException e) {
        System.err.println("⚠️ Erreur getPdbList : " + e.getMessage());
    }
    
    // Fallback final : données simulées
    pdbList.add(createPdbData("SYSTEM", 3.5));
    pdbList.add(createPdbData("SYSAUX", 2.8));
    pdbList.add(createPdbData("USERS", 5.2));
    return pdbList;
}

    private Map<String, Object> createPdbData(String name, double size) {
    Map<String, Object> pdb = new HashMap<>();
    pdb.put("name", name);
    pdb.put("size", size);
    pdb.put("sessions", 0);
    return pdb;
}

    public Map<String, Object> getSGAData() throws SQLException {
    Map<String, Object> sga = new HashMap<>();
    
    try {
        // Essayer avec V$SGAINFO
        String sql = "SELECT name, bytes FROM v$sgainfo " +
                     "WHERE name IN ('Buffer Cache Size', 'Shared Pool Size', 'Redo Buffers')";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            double total = 0;
            while (rs.next()) {
                String name = rs.getString("name");
                double bytes = rs.getDouble("bytes") / 1024 / 1024 / 1024;
                
                if ("Buffer Cache Size".equals(name)) {
                    sga.put("buffer_cache", bytes);
                } else if ("Shared Pool Size".equals(name)) {
                    sga.put("shared_pool", bytes);
                } else if ("Redo Buffers".equals(name)) {
                    sga.put("redo_log", bytes);
                }
                total += bytes;
            }
            sga.put("total", total);
        }
    } catch (SQLException e) {
        // Fallback : utiliser V$SGA
        try {
            String sql = "SELECT name, value/1024/1024/1024 as size_gb FROM v$sga";
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                double total = 0;
                while (rs.next()) {
                    String name = rs.getString("name");
                    double value = rs.getDouble("size_gb");
                    total += value;
                }
                sga.put("total", total);
                sga.put("buffer_cache", total * 0.5);
                sga.put("shared_pool", total * 0.3);
                sga.put("redo_log", total * 0.2);
            }
        } catch (SQLException e2) {
            // Fallback final : données simulées
            System.err.println("Impossible de récupérer les données SGA, utilisation de données simulées");
            sga.put("total", 4.0);
            sga.put("buffer_cache", 2.0);
            sga.put("shared_pool", 1.5);
            sga.put("redo_log", 0.5);
        }
    }
    return sga;
}

    public Map<String, Object> getPGAData() throws SQLException {
    Map<String, Object> pga = new HashMap<>();
    
    try {
        String sql = "SELECT name, value FROM v$pgastat " +
                     "WHERE name IN ('total PGA allocated', 'maximum PGA allocated', 'PGA memory freed back to OS')";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            double total = 0;
            double used = 0;
            while (rs.next()) {
                String name = rs.getString("name");
                double value = rs.getDouble("value") / 1024 / 1024 / 1024;
                
                if ("total PGA allocated".equals(name)) {
                    total = value;
                    used = value;
                }
            }
            pga.put("total", total);
            pga.put("used", used);
            pga.put("free", total - used);
            pga.put("hit_ratio", total > 0 ? (used/total)*100 : 0);
        }
    } catch (SQLException e) {
        // Fallback : données simulées
        System.err.println("⚠️ Impossible de récupérer les données PGA, utilisation de données simulées");
        pga.put("total", 2.0);
        pga.put("used", 1.2);
        pga.put("free", 0.8);
        pga.put("hit_ratio", 60.0);
    }
    return pga;
}

    // STATISTIQUES DES REQUÊTES (cache court : 10 secondes)
    public double getQueryStat(String type) throws SQLException {
        String key = "queryStat_" + type;
        CacheEntry entry = cache.get(key);
        if (entry != null && System.currentTimeMillis() - entry.timestamp < 10000) {
            return (double) entry.value;
        }

        String sql = switch (type) {
            case "all" ->
                "SELECT COUNT(*) FROM v$sql WHERE executions > 0";
            case "success" ->
                "SELECT COUNT(*) FROM v$sql WHERE executions > 0 AND rows_processed > 0";
            case "failed" ->
                "SELECT COUNT(*) FROM v$sql WHERE executions > 0 AND rows_processed = 0 AND parsing_schema_name NOT LIKE '%SYS%'";
            case "slow" ->
                "SELECT COUNT(*) FROM v$sql WHERE executions > 0 AND elapsed_time/executions > 1000000";
            default ->
                null;
        };
        if (sql == null) {
            return 0;
        }

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            double val = rs.next() ? rs.getDouble(1) : 0;
            cache.put(key, new CacheEntry(val));
            return val;
        } catch (SQLException e) {
            if (e.getErrorCode() == 942) { // ORA-00942: table or view does not exist
                return 0;
            }
            throw e;
        }
    }

    // AUDIT
    public ObservableList<Audit> getAuditLogs(String action, String user, LocalDate startDate, LocalDate endDate) throws SQLException {
        ObservableList<Audit> logs = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder(
                "SELECT TO_CHAR(timestamp, 'DD-MON-YYYY HH24:MI:SS') AS ts, "
                + "utilisateur, action, table_name, statut, ip_source, details "
                + "FROM sys.audit_log WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (action != null && !action.isEmpty() && !"Tous".equals(action)) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (user != null && !user.isEmpty() && !"Tous".equals(user)) {
            sql.append(" AND utilisateur = ?");
            params.add(user);
        }
        if (startDate != null) {
            sql.append(" AND TRUNC(timestamp) >= ?");
            params.add(java.sql.Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND TRUNC(timestamp) <= ?");
            params.add(java.sql.Date.valueOf(endDate));
        }
        sql.append(" ORDER BY timestamp DESC");

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(new Audit(
                            rs.getString("ts"),
                            rs.getString("utilisateur"),
                            rs.getString("action"),
                            rs.getString("table_name"),
                            rs.getString("statut"),
                            rs.getString("ip_source"),
                            rs.getString("details")
                    ));
                }
            }
        }
        return logs;
    }

    public List<String> getAuditActions() throws SQLException {
        List<String> actions = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT action FROM sys.audit_log ORDER BY action")) {
            while (rs.next()) {
                actions.add(rs.getString(1));
            }
        }
        return actions;
    }

    public List<String> getAuditUsers() throws SQLException {
        List<String> users = new ArrayList<>();
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT utilisateur FROM sys.audit_log ORDER BY utilisateur")) {
            while (rs.next()) {
                users.add(rs.getString(1));
            }
        }
        return users;
    }

    // ALERTES
    public ObservableList<Alertes> getAlertes() throws SQLException {
        ObservableList<Alertes> alerts = FXCollections.observableArrayList();
        String sql = "SELECT TO_CHAR(event_time, 'DD-MON-YYYY HH24:MI:SS') AS ts, "
                + "severity AS niveau, message, username AS source, statut "
                + "FROM monitoring_user.alerts ORDER BY event_time DESC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                alerts.add(new Alertes(
                        rs.getString("ts"),
                        rs.getString("niveau"),
                        rs.getString("message"),
                        rs.getString("source"),
                        rs.getString("statut")
                ));
            }
        }
        return alerts;
    }

    public void marquerAlertesLues() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE monitoring_user.alerts SET statut = 'READ' WHERE statut = 'UNREAD'");
        }
    }

    // LOGS
    public ObservableList<ObservableList<String>> getLogs(String level, String source, LocalDate startDate, LocalDate endDate) throws SQLException {
        ObservableList<ObservableList<String>> logs = FXCollections.observableArrayList();
        StringBuilder sql = new StringBuilder(
                "SELECT TO_CHAR(event_time, 'DD-MON-YYYY HH24:MI:SS') AS ts, "
                + "event_type AS niveau, module AS source, username AS utilisateur, message "
                + "FROM monitoring_user.logs WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (level != null && !level.isEmpty() && !"Tous".equals(level)) {
            sql.append(" AND event_type = ?");
            params.add(level);
        }
        if (source != null && !source.isEmpty() && !"Tous".equals(source)) {
            sql.append(" AND module = ?");
            params.add(source);
        }
        if (startDate != null) {
            sql.append(" AND TRUNC(event_time) >= ?");
            params.add(java.sql.Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append(" AND TRUNC(event_time) <= ?");
            params.add(java.sql.Date.valueOf(endDate));
        }
        sql.append(" ORDER BY event_time DESC");

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    row.add(rs.getString("ts"));
                    row.add(rs.getString("niveau"));
                    row.add(rs.getString("source"));
                    row.add(rs.getString("utilisateur"));
                    row.add(rs.getString("message"));
                    logs.add(row);
                }
            }
        }
        return logs;
    }

    public void insertLog(String niveau, String source, String utilisateur, String message) throws SQLException {
        String sql = "INSERT INTO monitoring_user.logs (event_type, module, username, message) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, niveau);
            pstmt.setString(2, source);
            pstmt.setString(3, utilisateur);
            pstmt.setString(4, message);
            pstmt.executeUpdate();
        }
    }

    public void insertAlert(String niveau, String message, String source) throws SQLException {
        String sql = "INSERT INTO monitoring_user.alerts (severity, message, username) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, niveau);
            pstmt.setString(2, message);
            pstmt.setString(3, source);
            pstmt.executeUpdate();
        }
    }

    // PARAMÈTRES
    public String getParametre(String nom) throws SQLException {
        String sql = "SELECT valeur FROM monitoring_user.parametres WHERE nom = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public void setParametre(String nom, String valeur) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM monitoring_user.parametres WHERE nom = ?";
        try (Connection conn = getConnection(); PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, nom);
            try (ResultSet rs = checkStmt.executeQuery()) {
                rs.next();
                boolean exists = rs.getInt(1) > 0;

                if (exists) {
                    String updateSql = "UPDATE monitoring_user.parametres SET valeur = ?, date_modification = SYSTIMESTAMP WHERE nom = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, valeur);
                        updateStmt.setString(2, nom);
                        updateStmt.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO monitoring_user.parametres (nom, valeur, date_modification) VALUES (?, ?, SYSTIMESTAMP)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, nom);
                        insertStmt.setString(2, valeur);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    public Map<String, String> getAllParametres() throws SQLException {
        Map<String, String> params = new HashMap<>();
        String sql = "SELECT nom, valeur FROM monitoring_user.parametres ORDER BY nom";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                params.put(rs.getString(1), rs.getString(2));
            }
        }
        return params;
    }

    public Map<String, String> getParametreDetails(String nom) throws SQLException {
        Map<String, String> details = new HashMap<>();
        String sql = "SELECT nom, valeur, description, categorie FROM monitoring_user.parametres WHERE nom = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    details.put("nom", rs.getString("nom"));
                    details.put("valeur", rs.getString("valeur"));
                    details.put("description", rs.getString("description"));
                    details.put("categorie", rs.getString("categorie"));
                }
            }
        }
        return details;
    }

    public Map<String, String> getParametresByCategorie(String categorie) throws SQLException {
        Map<String, String> params = new HashMap<>();
        String sql = "SELECT nom, valeur FROM monitoring_user.parametres WHERE categorie = ? ORDER BY nom";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, categorie);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    params.put(rs.getString(1), rs.getString(2));
                }
            }
        }
        return params;
    }

    public void resetParametreToDefault(String nom) throws SQLException {
        Map<String, String> defaults = getAllDefaultParametres();
        String defaultValeur = defaults.getOrDefault(nom, "");
        if (!defaultValeur.isEmpty()) {
            setParametre(nom, defaultValeur);
        }
    }

    public Map<String, String> getAllDefaultParametres() {
        Map<String, String> defaults = new HashMap<>();

        // Collecte
        defaults.put("INTERVALLE", "30");
        defaults.put("INTERVALLE_CHARTS", "5");

        // Seuils
        defaults.put("SEUIL_CPU", "80");
        defaults.put("SEUIL_RAM", "85");
        defaults.put("SEUIL_DISQUE", "90");
        defaults.put("SEUIL_TABLESPACE", "85");

        // Notifications
        defaults.put("SON_ACTIF", "true");
        defaults.put("NOTIF_SYSTEME", "true");
        defaults.put("EMAIL_ACTIF", "false");

        // Email
        defaults.put("EMAIL", "admin@monitoring.com, dba@monitoring.com");
        defaults.put("EMAIL_SCHEDULE", "FRIDAY_18:00");

        // Sécurité
        defaults.put("SESSION_TIMEOUT", "30");

        // Personnalisation
        defaults.put("THEME", "light");
        defaults.put("LANGUE", "fr");

        return defaults;
    }

    public void resetAllParametres() throws SQLException {
        Map<String, String> defaults = getAllDefaultParametres();

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<String, String> entry : defaults.entrySet()) {
                    String sql = "UPDATE monitoring_user.parametres SET valeur = ?, date_modification = SYSTIMESTAMP WHERE nom = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, entry.getValue());
                        pstmt.setString(2, entry.getKey());
                        pstmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public void saveMultipleParametres(Map<String, String> params) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String sql = "UPDATE monitoring_user.parametres SET valeur = ?, date_modification = SYSTIMESTAMP WHERE nom = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setString(1, entry.getValue());
                        pstmt.setString(2, entry.getKey());
                        pstmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean paramExists(String nom) throws SQLException {
        String sql = "SELECT COUNT(*) FROM monitoring_user.parametres WHERE nom = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public Map<String, List<Map<String, String>>> getParametresByCategories() throws SQLException {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        String sql = "SELECT nom, valeur, description, categorie FROM monitoring_user.parametres ORDER BY categorie, nom";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String categorie = rs.getString("categorie");
                Map<String, String> param = new LinkedHashMap<>();
                param.put("nom", rs.getString("nom"));
                param.put("valeur", rs.getString("valeur"));
                param.put("description", rs.getString("description"));

                result.computeIfAbsent(categorie, k -> new ArrayList<>()).add(param);
            }
        }
        return result;
    }

    // GESTION DES UTILISATEURS ORACLE (VERSION ÉPURÉE)
    public void createOracleUserAdvanced(String username, String password, String tablespace,
            String quota, String profile, String status, boolean isCommon) throws SQLException {

        // Gestion du préfixe C## pour les common users
        String finalUsername = username;
        if (isCommon && !username.toUpperCase().startsWith("C##")) {
            finalUsername = "C##" + username;
        } else if (!isCommon && username.toUpperCase().startsWith("C##")) {
            finalUsername = username.substring(3);
        }

        // Validation du nom
        String cleanName = finalUsername.toUpperCase().startsWith("C##") ? finalUsername.substring(3) : finalUsername;
        if (!cleanName.matches("^[a-zA-Z][a-zA-Z0-9_#$]{1,29}$")) {
            throw new SQLException("Nom d'utilisateur invalide : " + finalUsername);
        }

        // Construction de la requête
        StringBuilder sql = new StringBuilder("CREATE USER " + finalUsername + " IDENTIFIED BY \"")
                .append(password.replace("\"", "\"\"")).append("\" ");

        if (tablespace != null && !tablespace.isEmpty()) {
            sql.append("DEFAULT TABLESPACE ").append(tablespace).append(" ");
            if (quota != null && !quota.isEmpty()) {
                sql.append("QUOTA ").append(quota).append(" ON ").append(tablespace).append(" ");
            }
        }
        if (profile != null && !"DEFAULT".equals(profile)) {
            sql.append("PROFILE ").append(profile).append(" ");
        }
        if (status != null && !"OPEN".equals(status)) {
            sql.append("ACCOUNT ").append(status).append(" ");
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql.toString());
            System.out.println("✅ Utilisateur " + finalUsername + " créé avec succès");
        }
    }

    public void changeUserPassword(String username, String newPassword) throws SQLException {
        validateIdentifier(username);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER USER " + username + " IDENTIFIED BY \"" + newPassword.replace("\"", "\"\"") + "\"");
        }
    }

    public void changeUserStatus(String username, String status) throws SQLException {
        validateIdentifier(username);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER USER " + username + " ACCOUNT " + status);
        }
    }

    public void dropOracleUser(String username) throws SQLException {
        validateIdentifier(username);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP USER " + username + " CASCADE");
        }
    }

    public ObservableList<ObservableList<String>> getOracleUsers() throws SQLException {
        ObservableList<ObservableList<String>> users = FXCollections.observableArrayList();
        String sql = "SELECT username, account_status, default_tablespace, "
                + "TO_CHAR(expiry_date, 'DD-MON-YYYY') AS expiry "
                + "FROM dba_users WHERE username NOT IN ('SYS','SYSTEM','XDB','DBSNMP','ORACLE_OCM') ORDER BY username";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(rs.getString("username"));
                row.add("Oracle");
                row.add(rs.getString("account_status"));
                row.add(rs.getString("default_tablespace"));
                row.add(rs.getString("expiry") != null ? rs.getString("expiry") : "N/A");
                users.add(row);
            }
        }
        return users;
    }

    public void grantRole(String username, String role) throws SQLException {
        validateIdentifier(username);
        validateIdentifier(role);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("GRANT " + role + " TO " + username);
        }
    }

    public void revokeRole(String username, String role) throws SQLException {
        validateIdentifier(username);
        validateIdentifier(role);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("REVOKE " + role + " FROM " + username);
        }
    }

    public boolean hasRole(String username, String role) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dba_role_privs WHERE grantee = UPPER(?) AND granted_role = UPPER(?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, role);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<String> getUserRoles(String username) throws SQLException {
        List<String> roles = new ArrayList<>();
        String sql = "SELECT granted_role FROM dba_role_privs WHERE grantee = UPPER(?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(rs.getString(1));
                }
            }
        }
        return roles;
    }

    public void grantPrivilege(String username, String privilege) throws SQLException {
        validateIdentifier(username);
        validateIdentifier(privilege);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("GRANT " + privilege + " TO " + username);
        }
    }

    public void revokePrivilege(String username, String privilege) throws SQLException {
        validateIdentifier(username);
        validateIdentifier(privilege);
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("REVOKE " + privilege + " FROM " + username);
        }
    }

    public boolean hasPrivilege(String username, String privilege) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dba_sys_privs WHERE grantee = UPPER(?) AND privilege = UPPER(?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, privilege);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public List<String> getUserPrivileges(String username) throws SQLException {
        List<String> privileges = new ArrayList<>();
        String sql = "SELECT privilege FROM dba_sys_privs WHERE grantee = UPPER(?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    privileges.add(rs.getString(1));
                }
            }
        }
        return privileges;
    }

    // 9. RAPPORTS
    public Map<String, Object> getSyntheseReport(LocalDate start, LocalDate end) throws SQLException {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("type", "Synthèse");
    data.put("periode", start + " - " + end);
    data.put("date_generation", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

    // Sessions
    data.put("total_sessions", Integer.parseInt(getSingleValue("SELECT COUNT(*) FROM v$session WHERE username IS NOT NULL")));

    // Alertes sur la période - ✅ CORRIGÉ
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(
             "SELECT COUNT(*) FROM monitoring_user.alerts WHERE TRUNC(event_time) BETWEEN ? AND ?")) {
        pstmt.setDate(1, java.sql.Date.valueOf(start));
        pstmt.setDate(2, java.sql.Date.valueOf(end));
        try (ResultSet rs = pstmt.executeQuery()) {
            data.put("total_alertes", rs.next() ? rs.getInt(1) : 0);
        }
    }

    // Taille base
    String sizeVal = getSingleValue("SELECT ROUND(SUM(bytes)/1024/1024/1024, 2) FROM dba_data_files");
    data.put("taille_base_gb", sizeVal.equals("N/A") ? 0.0 : Double.parseDouble(sizeVal));

    // Tablespace utilisé
    String tsVal = getSingleValue("SELECT ROUND((SELECT SUM(bytes) FROM dba_data_files) - (SELECT SUM(bytes) FROM dba_free_space), 2)/1024/1024/1024 FROM dual");
    data.put("tablespace_utilise_gb", tsVal.equals("N/A") ? 0.0 : Double.parseDouble(tsVal));

    return data;
}

    public List<Map<String, Object>> getSessionsReport(LocalDate start, LocalDate end) throws SQLException {
        List<Map<String, Object>> sessions = new ArrayList<>();
        String sql = "SELECT SID, SERIAL#, USERNAME, STATUS, MACHINE, PROGRAM, "
                + "TO_CHAR(LOGON_TIME, 'DD/MM/YYYY HH24:MI:SS') AS LOGON_TIME "
                + "FROM v$session WHERE USERNAME IS NOT NULL ORDER BY LOGON_TIME DESC";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("SID", rs.getInt("SID"));
                row.put("SERIAL", rs.getInt("SERIAL#"));
                row.put("USERNAME", rs.getString("USERNAME"));
                row.put("STATUS", rs.getString("STATUS"));
                row.put("MACHINE", rs.getString("MACHINE"));
                row.put("PROGRAM", rs.getString("PROGRAM"));
                row.put("LOGON_TIME", rs.getString("LOGON_TIME"));
                sessions.add(row);
            }
        }
        return sessions;
    }

    public List<Map<String, Object>> getTablespacesReport() throws SQLException {
        List<Map<String, Object>> tsList = new ArrayList<>();
        String sql = "SELECT TABLESPACE_NAME, STATUS, CONTENTS, "
                + "ROUND(SUM(BYTES)/1024/1024, 2) AS TOTAL_MB, "
                + "ROUND(SUM(MAXBYTES)/1024/1024, 2) AS MAX_MB "
                + "FROM DBA_DATA_FILES GROUP BY TABLESPACE_NAME, STATUS, CONTENTS ORDER BY TABLESPACE_NAME";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("TABLESPACE", rs.getString("TABLESPACE_NAME"));
                row.put("STATUS", rs.getString("STATUS"));
                row.put("CONTENTS", rs.getString("CONTENTS"));
                row.put("TOTAL_MB", rs.getDouble("TOTAL_MB"));
                row.put("MAX_MB", rs.getDouble("MAX_MB"));
                tsList.add(row);
            }
        }
        return tsList;
    }

    public List<Map<String, Object>> getLogReport(LocalDate start, LocalDate end) throws SQLException {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT TO_CHAR(event_time, 'DD/MM/YYYY HH24:MI:SS') AS date_heure, "
                + "event_type, username, os_user, host, ip_address, terminal, program, "
                + "module, action, session_id, message, error_code, error_stack "
                + "FROM monitoring_user.logs "
                + "WHERE TRUNC(event_time) BETWEEN ? AND ? "
                + "ORDER BY event_time DESC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(start));
            pstmt.setDate(2, java.sql.Date.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Date/Heure", rs.getString("date_heure"));
                    row.put("Type", rs.getString("event_type"));
                    row.put("Utilisateur", rs.getString("username"));
                    row.put("OS User", rs.getString("os_user"));
                    row.put("Hôte", rs.getString("host"));
                    row.put("IP", rs.getString("ip_address"));
                    row.put("Terminal", rs.getString("terminal"));
                    row.put("Programme", rs.getString("program"));
                    row.put("Module", rs.getString("module"));
                    row.put("Action", rs.getString("action"));
                    row.put("Session ID", rs.getObject("session_id") != null ? rs.getInt("session_id") : null);
                    row.put("Message", rs.getString("message"));
                    row.put("Code Erreur", rs.getObject("error_code") != null ? rs.getInt("error_code") : null);
                    row.put("Stack Erreur", rs.getString("error_stack"));
                    logs.add(row);
                }
            }
        }
        return logs;
    }

    public List<Map<String, Object>> getAlertesReport(LocalDate start, LocalDate end) throws SQLException {
        List<Map<String, Object>> alertes = new ArrayList<>();
        String sql = "SELECT TO_CHAR(event_time, 'DD/MM/YYYY HH24:MI:SS') AS date_heure, "
                + "alert_type, username, ip_address, severity, message "
                + "FROM monitoring_user.alerts "
                + "WHERE TRUNC(event_time) BETWEEN ? AND ? "
                + "ORDER BY event_time DESC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(start));
            pstmt.setDate(2, java.sql.Date.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("Date/Heure", rs.getString("date_heure"));
                    row.put("Type", rs.getString("alert_type"));
                    row.put("Utilisateur", rs.getString("username"));
                    row.put("IP", rs.getString("ip_address"));
                    row.put("Sévérité", rs.getString("severity"));
                    row.put("Message", rs.getString("message"));
                    alertes.add(row);
                }
            }
        }
        return alertes;
    }

    public List<Map<String, Object>> getAuditReport(LocalDate start, LocalDate end) throws SQLException {
        List<Map<String, Object>> audits = new ArrayList<>();
        String sql = "SELECT TO_CHAR(timestamp, 'DD/MM/YYYY HH24:MI:SS') AS TS, "
                + "utilisateur, action, table_name, statut, ip_source, details "
                + "FROM sys.audit_log "
                + "WHERE TRUNC(timestamp) BETWEEN ? AND ? ORDER BY timestamp DESC";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, java.sql.Date.valueOf(start));
            pstmt.setDate(2, java.sql.Date.valueOf(end));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("DATE", rs.getString("TS"));
                    row.put("UTILISATEUR", rs.getString("utilisateur"));
                    row.put("ACTION", rs.getString("action"));
                    row.put("TABLE", rs.getString("table_name"));
                    row.put("STATUT", rs.getString("statut"));
                    row.put("IP", rs.getString("ip_source"));
                    row.put("DETAILS", rs.getString("details"));
                    audits.add(row);
                }
            }
        }
        return audits;
    }

    public List<Map<String, Object>> getUsersReport() throws SQLException {
        List<Map<String, Object>> users = new ArrayList<>();
        String sql = "SELECT username, account_status, default_tablespace, "
                + "TO_CHAR(expiry_date, 'DD/MM/YYYY') AS EXPIRY "
                + "FROM dba_users WHERE username NOT IN ('SYS','SYSTEM','XDB','DBSNMP') ORDER BY username";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("USERNAME", rs.getString("username"));
                row.put("STATUS", rs.getString("account_status"));
                row.put("TABLESPACE", rs.getString("default_tablespace"));
                row.put("EXPIRY", rs.getString("EXPIRY") != null ? rs.getString("EXPIRY") : "N/A");
                users.add(row);
            }
        }
        return users;
    }
}
