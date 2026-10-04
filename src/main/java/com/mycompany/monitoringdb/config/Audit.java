/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author Fredos
 */
public class Audit {

    private final StringProperty timestamp = new SimpleStringProperty();
    private final StringProperty user = new SimpleStringProperty();
    private final StringProperty action = new SimpleStringProperty();
    private final StringProperty table = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty ip = new SimpleStringProperty();
    private final StringProperty details = new SimpleStringProperty();

    public Audit() {
    }

    public Audit(String timestamp, String user, String action, String table,
            String status, String ip, String details) {
        this.timestamp.set(timestamp);
        this.user.set(user);
        this.action.set(action);
        this.table.set(table);
        this.status.set(status);
        this.ip.set(ip);
        this.details.set(details);
    }

    // Getters
    public String getTimestamp() {
        return timestamp.get();
    }

    public String getUser() {
        return user.get();
    }

    public String getAction() {
        return action.get();
    }

    public String getTable() {
        return table.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getIp() {
        return ip.get();
    }

    public String getDetails() {
        return details.get();
    }

    // Property getters
    public StringProperty timestampProperty() {
        return timestamp;
    }

    public StringProperty userProperty() {
        return user;
    }

    public StringProperty actionProperty() {
        return action;
    }

    public StringProperty tableProperty() {
        return table;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty ipProperty() {
        return ip;
    }

    public StringProperty detailsProperty() {
        return details;
    }
}
