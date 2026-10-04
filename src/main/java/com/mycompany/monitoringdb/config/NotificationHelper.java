/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 *
 * @author Fredos
 */
public class NotificationHelper {
    
    // ================================================================
    // 1. NOTIFICATION SIMPLE
    // ================================================================

    /**
     * Affiche une notification de type success, error, warning ou info.
     * 
     * @param message Le message à afficher
     * @param type Le type de notification (success, error, warning, info)
     */
    public static void showNotification(String message, String type) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notification");
        alert.setHeaderText(null);

        switch (type.toLowerCase()) {
            case "success":
                alert.setAlertType(Alert.AlertType.INFORMATION);
                alert.setContentText(message);
                break;
            case "error":
                alert.setAlertType(Alert.AlertType.ERROR);
                alert.setContentText(message);
                break;
            case "warning":
                alert.setAlertType(Alert.AlertType.WARNING);
                alert.setContentText(message);
                break;
            case "info":
            default:
                alert.setAlertType(Alert.AlertType.INFORMATION);
                alert.setContentText("ℹ" + message);
                break;
        }

        alert.showAndWait();
    }

    // ================================================================
    // 2. NOTIFICATION AVEC CONFIRMATION
    // ================================================================

    /**
     * Affiche une boîte de dialogue de confirmation.
     * 
     * @param title Titre de la boîte de dialogue
     * @param message Message de confirmation
     * @return true si l'utilisateur a cliqué sur "Oui", false sinon
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    // ================================================================
    // 3. NOTIFICATION D'ERREUR
    // ================================================================

    /**
     * Affiche une notification d'erreur.
     * 
     * @param message Le message d'erreur
     */
    public static void showError(String message) {
        showNotification(message, "error");
    }

    // ================================================================
    // 4. NOTIFICATION DE SUCCÈS
    // ================================================================

    /**
     * Affiche une notification de succès.
     * 
     * @param message Le message de succès
     */
    public static void showSuccess(String message) {
        showNotification(message, "success");
    }

    // ================================================================
    // 5. NOTIFICATION D'AVERTISSEMENT
    // ================================================================

    /**
     * Affiche une notification d'avertissement.
     * 
     * @param message Le message d'avertissement
     */
    public static void showWarning(String message) {
        showNotification(message, "warning");
    }

    // ================================================================
    // 6. NOTIFICATION D'INFORMATION
    // ================================================================

    /**
     * Affiche une notification d'information.
     * 
     * @param message Le message d'information
     */
    public static void showInfo(String message) {
        showNotification(message, "info");
    }
}