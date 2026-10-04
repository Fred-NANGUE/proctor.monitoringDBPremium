/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

/**
 * Classe modèle pour les alertes Correspond à la table
 * c##monitoring_user.alerts
 *
 * @author Fredos
 */
public class Alertes {

    // ===== CHAMPS =====
    private Long id;
    private String timestamp;      // event_time
    private String alertType;      // alert_type
    private String username;
    private String ipAddress;      // ip_address
    private String severity;       // 'INFO', 'WARNING', 'CRITICAL'
    private String message;
    private String status;         // 'UNREAD', 'READ' (statut)

    public Alertes() {
    }

    public Alertes(Long id, String timestamp, String alertType, String username,
            String ipAddress, String severity, String message, String status) {
        this.id = id;
        this.timestamp = timestamp;
        this.alertType = alertType;
        this.username = username;
        this.ipAddress = ipAddress;
        this.severity = severity;
        this.message = message;
        this.status = status;
    }

    @Deprecated
    public Alertes(String timestamp, String level, String message, String source, String status) {
        this.timestamp = timestamp;
        this.severity = level;
        this.alertType = source;
        this.username = source;
        this.message = message;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getAlertType() {
        return alertType;
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getSeverity() {
        return severity;
    }

    public String getLevel() {
        return severity; // Alias pour compatibilité
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return alertType; // Alias pour compatibilité
    }

    public String getStatus() {
        return status;
    }

    // ===== SETTERS =====
    public void setId(Long id) {
        this.id = id;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setLevel(String level) {
        this.severity = level;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSource(String source) {
        this.alertType = source;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSeverityIcon() {
        if (severity == null) {
            return "🔵";
        }
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return "🔴";
            case "WARNING":
                return "🟡";
            default:
                return "🔵";
        }
    }

    public String getStatusIcon() {
        return "READ".equals(status) ? "✅" : "🔄";
    }

    public boolean isRead() {
        return "READ".equals(status);
    }

    public boolean isUnread() {
        return "UNREAD".equals(status);
    }

    @Override
    public String toString() {
        return getSeverityIcon() + " [" + timestamp + "] " + message + " " + getStatusIcon();
    }

    public String toFormattedString() {
        return String.format("%s | %s | %s | %s | %s | %s | %s | %s",
                id, timestamp, alertType, username, ipAddress, severity, message, status);
    }
}
