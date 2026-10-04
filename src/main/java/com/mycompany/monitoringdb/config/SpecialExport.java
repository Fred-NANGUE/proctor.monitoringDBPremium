package com.mycompany.monitoringdb.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SpecialExport {

    // ===== Configuration =====
    public static final String REPORTS_DIR = "/home/oracle/MonitoringDB_Reports/"; // distant
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    // Création du répertoire distant au chargement
    static {
        try {
            String mkdirCmd = "mkdir -p " + REPORTS_DIR;
            LinuxQueries.execRemoteCommand(mkdirCmd);
            System.out.println("Répertoire distant créé/vérifié : " + REPORTS_DIR);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du répertoire distant : " + e.getMessage());
        }
    }

    public static File generateReport(String type, String format, Object data, String generatedBy) throws Exception {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = type + "_" + generatedBy + "_" + dateStr + "." + getExtension(format);
        File localTemp = new File(TEMP_DIR, fileName);

        writeReport(localTemp, format, data, type, generatedBy);

        String remotePath = REPORTS_DIR + fileName;
        LinuxQueries.uploadFile(localTemp, remotePath);

        localTemp.delete();
        return new File(remotePath);
    }

    //  ÉCRITURE DES RAPPORTS (PDF, JSON, XML, TXT, CSV)
    private static void writeReport(File file, String format, Object data, String type, String generatedBy) throws Exception {
        switch (format.toUpperCase()) {
            case "PDF": writePDF(file, data, type, generatedBy); break;
            case "JSON": writeJSON(file, data); break;
            case "XML": writeXML(file, data); break;
            case "TXT": writeTXT(file, data); break;
            case "CSV": writeCSV(file, data); break;
            default: throw new IllegalArgumentException("Format non supporté : " + format);
        }
    }

    //  PDF (avec watermark NON SÉLECTIONNABLE)
    private static void writePDF(File file, Object data, String reportType, String generatedBy) throws Exception {
        try (PdfWriter writer = new PdfWriter(file);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document doc = new Document(pdfDoc, PageSize.A4.rotate())) {

            doc.setMargins(30, 30, 30, 30);

            Color blueMarine = new DeviceRgb(13, 27, 42);
            Color gold = new DeviceRgb(212, 175, 55);
            Color lightGray = new DeviceRgb(240, 242, 245);
            Color white = new DeviceRgb(255, 255, 255);
            Color darkGray = new DeviceRgb(50, 50, 50);

            PdfFont fontBold = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont fontNormal = PdfFontFactory.createFont("Helvetica");
            PdfFont fontItalic = PdfFontFactory.createFont("Helvetica-Oblique");
            PdfFont fontSmall = PdfFontFactory.createFont("Helvetica");

            // Watermark
            Paragraph watermark = new Paragraph("🗄️")
                    .setFontSize(150)
                    .setOpacity(0.06f)
                    .setFontColor(blueMarine)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(100, 200, 400);
            doc.add(watermark);

            Paragraph watermarkText = new Paragraph("MONITORING DB PREMIUM")
                    .setFont(fontBold)
                    .setFontSize(60)
                    .setOpacity(0.04f)
                    .setFontColor(blueMarine)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(100, 300, 500);
            doc.add(watermarkText);

            // En‑tête
            Table headerTable = new Table(2);
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setBackgroundColor(blueMarine);
            headerTable.setPadding(10);

            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Cell titleCell = new Cell()
                    .add(new Paragraph("MONITORING DB PREMIUM")
                            .setFont(fontBold).setFontSize(22).setFontColor(gold).setTextAlignment(TextAlignment.LEFT))
                    .setBorder(null)
                    .setPadding(5);

            Cell metaCell = new Cell()
                    .add(new Paragraph("Généré le : " + dateStr + "\nPar : " + generatedBy)
                            .setFont(fontNormal).setFontSize(10).setFontColor(white).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(null)
                    .setPadding(5);

            headerTable.addCell(titleCell);
            headerTable.addCell(metaCell);
            doc.add(headerTable);

            // Titre
            Paragraph reportTitle = new Paragraph("RAPPORT " + reportType.toUpperCase())
                    .setFont(fontBold).setFontSize(18).setFontColor(blueMarine)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(15).setMarginBottom(5);
            doc.add(reportTitle);

            Paragraph separator = new Paragraph("__________________________________________________")
                    .setFontColor(gold).setFontSize(14).setTextAlignment(TextAlignment.CENTER).setMarginBottom(15);
            doc.add(separator);

            // Corps
            if (data instanceof Map) {
                Table mapTable = new Table(2);
                mapTable.setWidth(UnitValue.createPercentValue(70));
                mapTable.setHorizontalAlignment(HorizontalAlignment.CENTER);
                mapTable.setBackgroundColor(lightGray);
                mapTable.setPadding(5);

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                    Cell keyCell = new Cell()
                            .add(new Paragraph(entry.getKey().toString()).setFont(fontBold))
                            .setBackgroundColor(new DeviceRgb(220, 220, 220))
                            .setPadding(5);
                    Cell valueCell = new Cell()
                            .add(new Paragraph(entry.getValue().toString()).setFont(fontNormal))
                            .setPadding(5);
                    mapTable.addCell(keyCell);
                    mapTable.addCell(valueCell);
                }
                doc.add(mapTable);

            } else if (data instanceof List && !((List<?>) data).isEmpty()) {
                List<?> list = (List<?>) data;
                Object first = list.get(0);

                if (first instanceof Map) {
                    Map<?, ?> firstMap = (Map<?, ?>) first;
                    List<String> headers = new ArrayList<>(firstMap.keySet().stream()
                            .map(Object::toString).collect(Collectors.toList()));

                    Table table = new Table(headers.size());
                    table.setWidth(UnitValue.createPercentValue(98));
                    table.setHorizontalAlignment(HorizontalAlignment.CENTER);
                    table.setKeepTogether(false);

                    // En‑têtes du tableau
                    for (String header : headers) {
                        Cell headerCell = new Cell()
                                .add(new Paragraph(header).setFont(fontBold).setFontColor(white).setFontSize(8))
                                .setBackgroundColor(blueMarine)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(3)
                                .setBorder(null);
                        table.addCell(headerCell);
                    }

                    // Lignes de données
                    for (Object obj : list) {
                        Map<?, ?> rowMap = (Map<?, ?>) obj;
                        for (String key : headers) {
                            Object value = rowMap.get(key);
                            String text = (value != null) ? value.toString() : "";
                            if (text.length() > 150) text = text.substring(0, 147) + "...";
                            Cell cell = new Cell()
                                    .add(new Paragraph(text).setFont(fontSmall).setFontSize(7))
                                    .setPadding(2)
                                    .setBorder(null);
                            table.addCell(cell);
                        }
                    }
                    doc.add(table);

                } else {
                    for (Object obj : list) {
                        doc.add(new Paragraph("• " + obj.toString()).setFont(fontNormal).setFontSize(10));
                    }
                }
            } else {
                doc.add(new Paragraph("Aucune donnée à afficher.").setFont(fontItalic));
            }

            // Pied de page
            doc.add(new Paragraph(" ").setMarginBottom(30));
            Paragraph footer = new Paragraph("Design By Fred Nangue | Monitoring DB Premium")
                    .setFont(fontItalic).setFontSize(9).setFontColor(darkGray).setTextAlignment(TextAlignment.CENTER);
            doc.add(footer);
        }
    }

    // ========== JSON ==========
    private static void writeJSON(File file, Object data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, data);
    }

    // ========== XML ==========
    @XmlRootElement(name = "rapport")
    public static class ReportWrapper {
        private List<Map<String, Object>> lignes;
        public ReportWrapper() {}
        public ReportWrapper(List<Map<String, Object>> lignes) { this.lignes = lignes; }
        @XmlElement(name = "ligne")
        public List<Map<String, Object>> getLignes() { return lignes; }
        public void setLignes(List<Map<String, Object>> lignes) { this.lignes = lignes; }
    }

    private static void writeXML(File file, Object data) throws Exception {
        ReportWrapper wrapper;
        if (data instanceof List && !((List<?>) data).isEmpty() && ((List<?>) data).get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            wrapper = new ReportWrapper(list);
        } else {
            List<Map<String, Object>> fakeList = new ArrayList<>();
            Map<String, Object> single = new LinkedHashMap<>();
            single.put("contenu", data != null ? data.toString() : "null");
            fakeList.add(single);
            wrapper = new ReportWrapper(fakeList);
        }
        JAXBContext context = JAXBContext.newInstance(ReportWrapper.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            marshaller.marshal(wrapper, fos);
        }
    }

    // ========== TXT ==========
    private static void writeTXT(File file, Object data) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("=== RAPPORT MONITORING DB PREMIUM ===");
            writer.newLine();
            writer.write("Généré le : " + LocalDateTime.now());
            writer.newLine();
            writer.write("=======================================");
            writer.newLine();

            if (data instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                    writer.write(entry.getKey() + " : " + entry.getValue());
                    writer.newLine();
                }
            } else if (data instanceof List) {
                List<?> list = (List<?>) data;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    for (Object obj : list) {
                        writer.write(obj.toString());
                        writer.newLine();
                    }
                } else {
                    for (Object obj : list) {
                        writer.write(obj.toString());
                        writer.newLine();
                    }
                }
            } else {
                writer.write(data != null ? data.toString() : "null");
            }
            writer.newLine();
            writer.write("=======================================");
            writer.newLine();
            writer.write("Fin du rapport");
        }
    }

    // ========== CSV ==========
    private static void writeCSV(File file, Object data) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    Map<?, ?> firstMap = (Map<?, ?>) list.get(0);
                    writer.write(String.join(";", firstMap.keySet().stream().map(Object::toString).toArray(String[]::new)));
                    writer.newLine();
                    for (Object obj : list) {
                        Map<?, ?> row = (Map<?, ?>) obj;
                        writer.write(String.join(";", row.values().stream().map(Object::toString).toArray(String[]::new)));
                        writer.newLine();
                    }
                }
            }
        }
    }

    //  GESTION DES FICHIERS DISTANTS (via SSH)
    public static List<File> listReports() {
        List<File> list = new ArrayList<>();
        try {
            List<String> fileNames = LinuxQueries.listRemoteDirectory(REPORTS_DIR);
            for (String name : fileNames) {
                list.add(new File(REPORTS_DIR + name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void downloadReport(String fileName, File destination) throws IOException {
        try {
            String remotePath = REPORTS_DIR + fileName;
            LinuxQueries.downloadFile(remotePath, destination);
        } catch (Exception e) {
            throw new IOException("Erreur de téléchargement SSH : " + e.getMessage(), e);
        }
    }

    public static boolean deleteReport(String fileName) {
        try {
            String remotePath = REPORTS_DIR + fileName;
            LinuxQueries.deleteRemoteFile(remotePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static long[] getFileStats(String fileName) {
        try {
            String remotePath = REPORTS_DIR + fileName;
            String stat = LinuxQueries.execRemoteCommand("stat -c '%s %Y' " + remotePath);
            if (stat != null && !stat.isEmpty()) {
                String[] parts = stat.split(" ");
                if (parts.length == 2) {
                    long size = Long.parseLong(parts[0]);
                    long lastModified = Long.parseLong(parts[1]);
                    return new long[]{size, lastModified};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new long[]{0, 0};
    }

    //  UTILITAIRES
    private static String getExtension(String format) {
        if (format == null) return "txt";
        switch (format.toUpperCase()) {
            case "PDF": return "pdf";
            case "JSON": return "json";
            case "XML": return "xml";
            case "TXT": return "txt";
            case "CSV": return "csv";
            default: return "txt";
        }
    }

    public static String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
}