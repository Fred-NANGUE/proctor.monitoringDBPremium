/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.monitoringdb;

import com.mycompany.monitoringdb.ServiceExterne.EmailService;
import java.io.File;
import java.io.IOException;
import javax.mail.MessagingException;

/**
 * Classe de test pour l'envoi d'emails avec pièce jointe
 * @author Fredos
 */
public class testMail {
    
    EmailService mail = new EmailService();
    String[] pip = {
        "fedcomfood@gmail.com",
        "fedcomfood@gmail.com"
    };
    String subject = "Etat d'activité - Rapport de monitoring";
    String body = "Bonjour,\n\n" +
                  "Veuillez trouver en pièce jointe le rapport d'activité.\n\n" +
                  "Cordialement,\n" +
                  "Monitoring DB Premium";

    public static void main(String[] args) {
        testMail tm = new testMail();
        try {
            // ===== 1. Utiliser un fichier existant =====
            File file = new File("C:/Users/Fredos/Documents/rapport.pdf");
            
            if (!file.exists()) {
                System.out.println("⚠️ Le fichier n'existe pas, création d'un fichier de test...");
                file = File.createTempFile("rapport_test_", ".txt");
                try (java.io.FileWriter fw = new java.io.FileWriter(file)) {
                    fw.write("Rapport de test - Monitoring DB Premium");
                }
            }
            
            // ===== 2. Envoyer l'email avec pièce jointe =====
            tm.mail.sendEmailWithAttachment(tm.pip, tm.subject, tm.body, file);
            
            System.out.println("✅ Email envoyé avec succès !");
            
        } catch (MessagingException e) {
            System.err.println("❌ Erreur d'envoi d'email : " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("❌ Erreur fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }
}