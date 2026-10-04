/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Classe utilitaire pour l'exportation de données en CSV. Réutilisable pour
 * toutes les pages (Console SQL, Sessions, Alertes, etc.)
 *
 * @author Fred Nangue
 */
public class ExportCSV {

    private static final String DEFAULT_SEPARATOR = ";";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ================================================================
    // 1. EXPORT DEPUIS UNE TABLEVIEW
    // ================================================================
    /**
     * Exporte le contenu d'une TableView en CSV.
     *
     * @param tableView La TableView à exporter
     * @param window La fenêtre parente pour la boîte de dialogue
     * @param defaultFileName Nom par défaut du fichier (sans extension)
     * @return true si l'export a réussi, false sinon
     */
    public static boolean exportTableView(TableView<ObservableList<String>> tableView, Window window, String defaultFileName) {
        if (tableView.getItems().isEmpty()) {
            NotificationHelper.showNotification("Aucune donnée à exporter.", "warning");
            return false;
        }

        // Récupérer les en-têtes des colonnes
        List<String> headers = tableView.getColumns().stream()
                .map(TableColumn::getText)
                .collect(Collectors.toList());

        // Récupérer les données
        ObservableList<ObservableList<String>> data = tableView.getItems();

        return exportData(headers, data, window, defaultFileName);
    }

    // ================================================================
    // 2. EXPORT DEPUIS DES DONNÉES (HEADERS + DATA)
    // ================================================================
    /**
     * Exporte des données en CSV.
     *
     * @param headers Liste des noms de colonnes
     * @param data Données (ObservableList de lignes)
     * @param window La fenêtre parente pour la boîte de dialogue
     * @param defaultFileName Nom par défaut du fichier (sans extension)
     * @return true si l'export a réussi, false sinon
     */
    public static boolean exportData(List<String> headers,
            ObservableList<ObservableList<String>> data,
            Window window,
            String defaultFileName) {
        if (data.isEmpty()) {
            NotificationHelper.showNotification("Aucune donnée à exporter.", "warning");
            return false;
        }

        // === CHOISIR LE FICHIER ===
        File file = chooseFile(window, defaultFileName);
        if (file == null) {
            return false; // L'utilisateur a annulé
        }

        try {
            exportToCSV(file, headers, data);
            NotificationHelper.showNotification("Export réussi : " + file.getName(), "success");
            return true;
        } catch (IOException e) {
            NotificationHelper.showNotification("Erreur d'export : " + e.getMessage(), "error");
            e.printStackTrace();
            return false;
        }
    }

    // ================================================================
    // 3. EXPORT AVEC FICHIER DÉJÀ CHOISI
    // ================================================================
    /**
     * Exporte des données dans un fichier CSV déjà sélectionné.
     *
     * @param file Le fichier de destination
     * @param headers Liste des noms de colonnes
     * @param data Données (ObservableList de lignes)
     * @throws IOException En cas d'erreur d'écriture
     */
    public static void exportToCSV(File file, List<String> headers, ObservableList<ObservableList<String>> data) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // === ÉCRIRE L'EN-TÊTE ===
            writer.write(String.join(DEFAULT_SEPARATOR, headers));
            writer.newLine();

            // === ÉCRIRE LES DONNÉES ===
            for (ObservableList<String> row : data) {
                List<String> cleanedRow = row.stream()
                        .map(val -> val == null || val.equals("null") ? "" : val)
                        .collect(Collectors.toList());
                writer.write(String.join(DEFAULT_SEPARATOR, cleanedRow));
                writer.newLine();
            }

            writer.flush();
        }
    }

    // ================================================================
    // 4. EXPORT SIMPLE (LE PLUS UTILISÉ)
    // ================================================================
    /**
     * Exporte des données en CSV avec génération automatique du nom de fichier.
     *
     * @param headers Liste des noms de colonnes
     * @param data Données (ObservableList de lignes)
     * @param window La fenêtre parente
     * @param prefix Préfixe du nom du fichier (ex: "sessions", "alertes")
     * @return true si l'export a réussi
     */
    public static boolean exportSimple(List<String> headers,
            ObservableList<ObservableList<String>> data,
            Window window,
            String prefix) {
        String defaultFileName = prefix + "_" + LocalDateTime.now().format(DATE_FORMAT);
        return exportData(headers, data, window, defaultFileName);
    }

    // ================================================================
    // 5. MÉTHODE DE CHOIX DE FICHIER
    // ================================================================
    private static File chooseFile(Window window, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les données");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"),
                new FileChooser.ExtensionFilter("Tous les fichiers (*.*)", "*.*")
        );
        fileChooser.setInitialFileName(defaultFileName + ".csv");

        return fileChooser.showSaveDialog(window);
    }

    // Pour mes mails
public static void generateExcelReport(File file,
        ObservableList<ObservableList<String>> logs,
        List<Alertes> alerts,
        Map<String, Object> perfStats,
        LocalDate start,
        LocalDate end) throws IOException {

    try (Workbook workbook = new XSSFWorkbook()) {

        // ===== FEUILLE 1 : Statistiques =====
        Sheet perfSheet = workbook.createSheet("Statistiques");
        int rowIdx = 0;

        // Titre avec style
        Row titleRow = perfSheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RAPPORT HEBDOMADAIRE - " + start + " au " + end);
        
        // Style du titre (gras)
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        perfSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        // En-têtes des statistiques
        Row headerRow = perfSheet.createRow(rowIdx++);
        headerRow.createCell(0).setCellValue("Indicateur");
        headerRow.createCell(1).setCellValue("Valeur");
        
        // Style des en-têtes
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerRow.getCell(0).setCellStyle(headerStyle);
        headerRow.getCell(1).setCellStyle(headerStyle);

        // Données des statistiques
        for (Map.Entry<String, Object> entry : perfStats.entrySet()) {
            Row row = perfSheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue() != null ? entry.getValue().toString() : "N/A");
        }

        // Ajuster les colonnes
        perfSheet.autoSizeColumn(0);
        perfSheet.autoSizeColumn(1);

        // ===== FEUILLE 2 : Logs =====
        Sheet logSheet = workbook.createSheet("Logs");
        Row logHeader = logSheet.createRow(0);
        String[] logHeaders = {"Horodatage", "Niveau", "Source", "Utilisateur", "Message"};
        for (int i = 0; i < logHeaders.length; i++) {
            Cell cell = logHeader.createCell(i);
            cell.setCellValue(logHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        int logRow = 1;
        for (ObservableList<String> row : logs) {
            Row dataRow = logSheet.createRow(logRow++);
            for (int i = 0; i < Math.min(row.size(), logHeaders.length); i++) {
                dataRow.createCell(i).setCellValue(row.get(i) != null ? row.get(i) : "");
            }
        }

        for (int i = 0; i < logHeaders.length; i++) {
            logSheet.autoSizeColumn(i);
        }

        // ===== FEUILLE 3 : Alertes =====
        Sheet alertSheet = workbook.createSheet("Alertes");
        Row alertHeader = alertSheet.createRow(0);
        String[] alertHeaders = {"Horodatage", "Type", "Utilisateur", "IP", "Sévérité", "Message", "Statut"};
        for (int i = 0; i < alertHeaders.length; i++) {
            Cell cell = alertHeader.createCell(i);
            cell.setCellValue(alertHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        int alertRow = 1;
        for (Alertes alert : alerts) {
            Row dataRow = alertSheet.createRow(alertRow++);
            dataRow.createCell(0).setCellValue(alert.getTimestamp() != null ? alert.getTimestamp() : "");
            dataRow.createCell(1).setCellValue(alert.getAlertType() != null ? alert.getAlertType() : "");
            dataRow.createCell(2).setCellValue(alert.getUsername() != null ? alert.getUsername() : "");
            dataRow.createCell(3).setCellValue(alert.getIpAddress() != null ? alert.getIpAddress() : "");
            dataRow.createCell(4).setCellValue(alert.getSeverity() != null ? alert.getSeverity() : "");
            dataRow.createCell(5).setCellValue(alert.getMessage() != null ? alert.getMessage() : "");
            dataRow.createCell(6).setCellValue(alert.getStatus() != null ? alert.getStatus() : "");
        }

        for (int i = 0; i < alertHeaders.length; i++) {
            alertSheet.autoSizeColumn(i);
        }

        // Écrire le fichier
        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }
        
        System.out.println("✅ Rapport Excel généré : " + file.getAbsolutePath());

    } catch (IOException e) {
        System.err.println("❌ Erreur lors de la génération du rapport Excel : " + e.getMessage());
        throw e;
    }
}
    // 6. MÉTHODE POUR CHANGER LE SÉPARATEUR
    // ================================================================
    public static void setSeparator(String separator) {
        // Pour changer le séparateur (ex: "," au lieu de ";")
        // Usage: ExportCSV.setSeparator(",");
        // Cette fonctionnalité n'est pas implémentée ici, mais peut l'être
    }
}
