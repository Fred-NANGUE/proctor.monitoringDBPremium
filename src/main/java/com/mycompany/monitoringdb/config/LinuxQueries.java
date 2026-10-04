/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb.config;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author Fredos
 */
public class LinuxQueries {

    /**
     * Exécute une commande sur le serveur via SSH.
     */
    public static String execRemoteCommand(String command) {
        // Récupérer la session SSH depuis le Holder
        Session session = ConnexionHolder.getSshSession();

        if (session == null || !session.isConnected()) {
            System.err.println("Session SSH non disponible.");
            return "N/A";
        }

        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            channel.connect();

            StringBuilder output = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > 0) {
                output.append(new String(buffer, 0, bytesRead));
            }
            channel.disconnect();
            return output.toString().trim();
        } catch (Exception e) {
            System.err.println("Erreur commande SSH : " + e.getMessage());
            return "N/A";
        }
    }

    // ================================================================
    // MÉTHODES SYSTÈME
    // ================================================================
    public static String getUptime() {
        String output = execRemoteCommand("uptime -p | sed 's/up //'");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getUserConnect() { // pour ignorer " il faut faire \"
        String output = execRemoteCommand("ps -eo user,comm | grep -E \"sshd|bash|sh\" | grep -v \"grep\" | awk '{print $1}' | sort | uniq -c | awk '{if ($1 > 1) printf \"%s *(%d), \", $2, $1; else printf \"%s, \", $2}' | sed 's/, $//'");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getDisk() { // pour ignorer " il faut faire \"
        String output = execRemoteCommand("df -hT | awk 'NR>1 && $2 ~ /^(ext[234]|xfs|btrfs|vfat|ntfs)$/ {printf \"%s: %s \", $7, $6}'");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getUpload() {
        String output = execRemoteCommand("old=$(awk 'NR>2 && $1 != \"lo:\" {sub(/:/, \"\"); sum+=$10} END {print sum}' /proc/net/dev); sleep 1; new=$(awk 'NR>2 && $1 != \"lo:\" {sub(/:/, \"\"); sum+=$10} END {print sum}' /proc/net/dev); bytes=$((new - old)); if [ $bytes -lt 1024 ]; then echo \"${bytes} B/s\"; elif [ $bytes -lt 1048576 ]; then echo \"$((bytes / 1024)) KB/s\"; elif [ $bytes -lt 1073741824 ]; then echo \"$((bytes / 1048576)) MB/s\"; else echo \"$((bytes / 1073741824)) GB/s\"; fi");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getDownload() {
        String output = execRemoteCommand("old=$(awk 'NR>2 && $1 != \"lo:\" {sub(/:/, \"\"); sum+=$2} END {print sum}' /proc/net/dev); sleep 1; new=$(awk 'NR>2 && $1 != \"lo:\" {sub(/:/, \"\"); sum+=$2} END {print sum}' /proc/net/dev); bytes=$((new - old)); if [ $bytes -lt 1024 ]; then echo \"${bytes} B/s\"; elif [ $bytes -lt 1048576 ]; then echo \"$((bytes / 1024)) KB/s\"; elif [ $bytes -lt 1073741824 ]; then echo \"$((bytes / 1048576)) MB/s\"; else echo \"$((bytes / 1073741824)) GB/s\"; fi");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getSystemRAM() {
        String output = execRemoteCommand("free -m | grep Mem | awk '{print $2}'");
        if (!output.isEmpty()) {
            try {
                long mb = Long.parseLong(output);
                return String.format("%.2f", mb / 1024.0) + " GB";
            } catch (NumberFormatException e) {
                return output + " MB";
            }
        }
        return "N/A";
    }

    public static String getSystemRAMUsed() {
        String output = execRemoteCommand("free -b | awk 'NR==2 {used = $2 - $7; printf \"%.2f GB / %.2f GB\\n\", used/1073741824, $2/1073741824}'");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getSystemSwap() {
        String output = execRemoteCommand("free -m | grep Swap | awk '{print $2}'");
        if (!output.isEmpty()) {
            try {
                long mb = Long.parseLong(output);
                return String.format("%.2f", mb / 1024.0) + " Go";
            } catch (NumberFormatException e) {
                return output + " MB";
            }
        }
        return "N/A";
    }

    public static String getSystemDiskTotal() {
        // 1. Essayer d'obtenir la taille totale du disque avec lsblk
        String output = execRemoteCommand("lsblk -b | grep sda | head -1 | awk '{print $4}'");
        if (!output.isEmpty()) {
            try {
                long bytes = Long.parseLong(output);
                return String.format("%.0f", bytes / 1024.0 / 1024 / 1024) + " Go";
            } catch (NumberFormatException e) {
                // Fallback : df -h /
                String fallback = execRemoteCommand("df -h / | tail -1 | awk '{print $2}'");
                return fallback.isEmpty() ? "N/A" : fallback;
            }
        }

        // 2. Fallback : df -h /
        String fallback = execRemoteCommand("df -h / | tail -1 | awk '{print $2}'");
        return fallback.isEmpty() ? "N/A" : fallback;
    }

    public static String getSystemDiskUsed() {
        String output = execRemoteCommand("df -h / | tail -1 | awk '{print $3}'");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getSystemNameCPU() {
        String output = execRemoteCommand("cat /proc/cpuinfo | grep 'model name' | head -1 | cut -d':' -f2 | xargs");
        return output.isEmpty() ? "Intel Xeon (VM)" : output;
    }

    public static String getSystemCPUCores() {
        String output = execRemoteCommand("nproc");
        return output.isEmpty() ? "N/A" : output;
    }

    public static String getSystemCPUUsage() {
        String output = execRemoteCommand("top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'");
        if (!output.isEmpty()) {
            try {
                double value = Double.parseDouble(output);
                return String.format("%.1f%%", value);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }
        return "N/A";
    }

    public static String getSystemDiskUsage() {
        String output = execRemoteCommand("df -h / | tail -1 | awk '{print $5}' | cut -d'%' -f1");
        if (!output.isEmpty()) {
            try {
                double value = Double.parseDouble(output);
                return String.format("%.1f%%", value);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }
        return "N/A";
    }

    public static String getSystemSessions() {
        String output = execRemoteCommand("who | wc -l");
        if (!output.isEmpty() && !output.equals("N/A")) {
            try {
                int count = Integer.parseInt(output.trim());
                return String.valueOf(count);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }
        return "N/A";
    }

    public static List<String[]> getLinuxUsers() {
        List<String[]> users = new ArrayList<>();
        // Récupérer les utilisateurs avec UID >= 1000 (utilisateurs humains)
        String output = execRemoteCommand("getent passwd | awk -F: '$3 >= 1000 {print $1, $3, $6}'");
        if (output.isEmpty() || output.equals("N/A")) {
            return users;
        }
        for (String line : output.split("\n")) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 3) {
                users.add(new String[]{parts[0], parts[1], parts[2]}); // [nom, UID, home]
            }
        }
        return users;
    }

    // Dans LinuxQueries.java
    public static String getSystemCPUChart() {
        String output = execRemoteCommand("top -bn1 | grep \"Cpu(s)\" | sed \"s/.*, *\\([0-9.]*\\)%* id.*/\\1/\" | awk '{print 100 - $1}'");
        if (!output.isEmpty()) {
            try {
                double value = Double.parseDouble(output.replace(',', '.'));
                return String.format(Locale.US, "%.1f%%", value);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }
        return "N/A";
    }

    public static String getSystemRAMChart() {
        String output = execRemoteCommand("free -b | awk 'NR==2 {used = $2 - $7; printf \"%.2f\\n\", (used/$2)*100}'");
        if (!output.isEmpty()) {
            try {
                double value = Double.parseDouble(output.replace(',', '.'));
                return String.format(Locale.US, "%.1f%%", value);
            } catch (NumberFormatException e) {
                return "N/A";
            }
        }
        return "N/A";
    }

    //Pour la page Rapports
    public static void createRemoteDirectory(String remoteDir) throws JSchException, SftpException {
        Session session = ConnexionHolder.getSshSession();
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Session SSH non disponible.");
        }
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        try {
            channel.mkdir(remoteDir);
        } catch (SftpException e) {
            // Le répertoire existe probablement déjà (erreur SSH_FX_FAILURE ou SSH_FX_FILE_ALREADY_EXISTS)
            if (e.id != ChannelSftp.SSH_FX_FAILURE) {
                throw e; // relancer si ce n'est pas une erreur "existe déjà"
            }
        } finally {
            channel.disconnect();
        }
    }

    public static void uploadFile(File localFile, String remotePath) throws JSchException, SftpException, FileNotFoundException {
        Session session = ConnexionHolder.getSshSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        try {
            channel.put(new FileInputStream(localFile), remotePath);
        } finally {
            channel.disconnect();
        }
    }

    public static void downloadFile(String remotePath, File localFile) throws JSchException, SftpException, IOException {
        Session session = ConnexionHolder.getSshSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        try (FileOutputStream fos = new FileOutputStream(localFile)) {
            channel.get(remotePath, fos);
        } finally {
            channel.disconnect();
        }
    }

    public static void deleteRemoteFile(String remotePath) throws JSchException, SftpException {
        Session session = ConnexionHolder.getSshSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        try {
            channel.rm(remotePath);
        } finally {
            channel.disconnect();
        }
    }

    public static List<String> listRemoteDirectory(String remoteDir) throws JSchException, SftpException {
        Session session = ConnexionHolder.getSshSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        List<String> fileNames = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<ChannelSftp.LsEntry> entries = channel.ls(remoteDir);
            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();
                if (!".".equals(filename) && !"..".equals(filename)) {
                    fileNames.add(filename);
                }
            }
        } finally {
            channel.disconnect();
        }
        return fileNames;
    }
}
