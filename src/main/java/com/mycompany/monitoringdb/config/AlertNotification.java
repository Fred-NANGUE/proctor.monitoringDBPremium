/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import javafx.application.Platform;
import javafx.scene.media.AudioClip; // ← Import explicite
import java.awt.*;
import java.net.URL;

/**
 * Gestion des notifications pour les alertes
 * @author Fredos
 */
public class AlertNotification {

    private static TrayIcon trayIcon;
    private static boolean soundEnabled = true;
    private static boolean systemNotificationEnabled = true;
    private static boolean emailEnabled = false;
    private static String emailRecipients = "";
    private static AudioClip alertSound; // ← Utilise la classe JavaFX

    static {
        // Initialisation du système tray
        try {
            if (SystemTray.isSupported()) {
                Image icon = Toolkit.getDefaultToolkit().createImage(
                    AlertNotification.class.getResource("/images/icon.png")
                );
                if (icon == null) {
                    icon = Toolkit.getDefaultToolkit().createImage(new byte[0]);
                }
                trayIcon = new TrayIcon(icon);
                trayIcon.setImageAutoSize(true);
                SystemTray.getSystemTray().add(trayIcon);
            }
        } catch (Exception ignored) {
            System.err.println("⚠️ Impossible d'initialiser le TrayIcon : " + ignored.getMessage());
        }

        // Chargement du son d'alerte
        try {
            URL soundUrl = AlertNotification.class.getResource("/sounds/alert.wav");
            if (soundUrl != null) {
                alertSound = new AudioClip(soundUrl.toExternalForm());
            } else {
                System.err.println("⚠️ Fichier son /sounds/alert.wav non trouvé");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors du chargement du son : " + e.getMessage());
        }
    }

    /**
     * Affiche une notification pour une alerte
     */
    public static void notifyAlert(Alertes alert) {
        if (alert == null) return;

        // 1. Notification système (si activée)
        if (systemNotificationEnabled && trayIcon != null) {
            Platform.runLater(() -> {
                String type = "CRITICAL".equals(alert.getLevel()) ? "🔴 CRITIQUE" 
                        : "WARNING".equals(alert.getLevel()) ? "⚠️ WARNING" 
                        : "ℹ️ INFO";
                
                trayIcon.displayMessage(
                    type + " - " + alert.getSource(),
                    alert.getMessage(),
                    "CRITICAL".equals(alert.getLevel()) ? TrayIcon.MessageType.ERROR 
                        : "WARNING".equals(alert.getLevel()) ? TrayIcon.MessageType.WARNING 
                        : TrayIcon.MessageType.INFO
                );
            });
        }

        // 2. Alerte sonore (si activée)
        if (soundEnabled) {
            // Bip système (AWT)
            Toolkit.getDefaultToolkit().beep();
            
            // Son personnalisé (si disponible)
            if (alertSound != null) {
                alertSound.play();
            }
        }

        // 3. Email (si activé)
        if (emailEnabled && emailRecipients != null && !emailRecipients.isEmpty()) {
            new Thread(() -> {
                try {
                    String[] recipients = emailRecipients.split(",");
                    String subject = "[ALERTE] " + alert.getLevel() + " - " + alert.getSource();
                    String body = "Niveau : " + alert.getLevel() + "\n"
                                + "Source : " + alert.getSource() + "\n"
                                + "Message : " + alert.getMessage() + "\n"
                                + "Date : " + alert.getTimestamp();
                    
                    // À décommenter quand EmailService sera prêt
                    // EmailService.sendSimpleEmail(recipients, subject, body);
                    System.out.println("📧 Email envoyé à : " + emailRecipients);
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
                }
            }).start();
        }
    }

    // ================================================================
    // GETTERS / SETTERS
    // ================================================================

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static void setSystemNotificationEnabled(boolean enabled) {
        systemNotificationEnabled = enabled;
    }

    public static void setEmailEnabled(boolean enabled) {
        emailEnabled = enabled;
    }

    public static void setEmailRecipients(String recipients) {
        emailRecipients = recipients;
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static boolean isSystemNotificationEnabled() {
        return systemNotificationEnabled;
    }

    public static boolean isEmailEnabled() {
        return emailEnabled;
    }

    public static String getEmailRecipients() {
        return emailRecipients;
    }

    /**
     * Test rapide de la notification
     */
    public static void testNotification() {
        Alertes test = new Alertes(
            "TEST", 
            "INFO", 
            "Ceci est un test de notification", 
            "Système", 
            "UNREAD"
        );
        notifyAlert(test);
    }
}