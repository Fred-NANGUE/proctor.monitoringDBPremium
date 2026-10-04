/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import java.time.LocalDateTime;
/**
 *
 * @author Fredos
 */
public class Report {
    private String fileName;
    private String type;
    private LocalDateTime date;
    private String format;
    private long size;
    private String status;
    private String filePath;

    public Report() {}

    public Report(String fileName, String type, LocalDateTime date, String format, long size, String status, String filePath) {
        this.fileName = fileName;
        this.type = type;
        this.date = date;
        this.format = format;
        this.size = size;
        this.status = status;
        this.filePath = filePath;
    }

    // Getters et Setters (générés par IDE)
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}