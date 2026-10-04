/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

/**
 *
 * @author Fredos
 */
public class TooltipUtils {

    /**
     * Crée un Tooltip personnalisé avec plusieurs lignes d'informations.
     */
    public static Tooltip createDetailTooltip(String title, String... details) {
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setStyle("-fx-background-color: #0D1B2A; -fx-padding: 10; -fx-border-color: #D4AF37; -fx-border-radius: 5;");
        
        // Titre
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold; -fx-font-size: 14px;");
        content.getChildren().add(titleLabel);
        
        // Détails
        for (String detail : details) {
            Label detailLabel = new Label(detail);
            detailLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
            content.getChildren().add(detailLabel);
        }
        
        Tooltip tooltip = new Tooltip();
        tooltip.setGraphic(content);
        tooltip.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-color: transparent;");
        return tooltip;
    }
}