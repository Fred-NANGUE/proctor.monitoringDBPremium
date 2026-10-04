/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import com.jcraft.jsch.Session;
import java.sql.Connection;

/**
 *
 * @author Fredos
 */
public class ConnexionHolder {

    private static Connection connection = null;
    private static Session sshSession = null;
    private static String currentUser;

    public static void setConnection(Connection conn) {
        connection = conn;
        System.out.println("Connexion stockée dans ConnexionHolder.");
    }

    public static Connection getConnection() {
        if (connection == null) {
            System.err.println("Aucune connexion dans ConnexionHolder.");
        }
        return connection;
    }

    public static void setSshSession(Session session) {
        sshSession = session;
        System.out.println("Session SSH stockée dans ConnexionHolder.");
    }

    public static Session getSshSession() {
        if (sshSession == null) {
            System.err.println("Aucune session SSH dans ConnexionHolder.");
        }
        return sshSession;
    }

    public static void setCurrentUser(String user) {
        currentUser = user;
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion Oracle fermée.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        connection = null;

        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
            System.out.println("?Session SSH fermée.");
        }
        sshSession = null;
    }
}
