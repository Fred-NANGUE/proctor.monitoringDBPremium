package com.mycompany.monitoringdb.ServiceExterne;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException; // Ajouté pour la gestion du nom d'expéditeur
import java.util.Properties;

public class EmailService {

    private static final Properties config = new Properties();

    static {
        try (InputStream input = EmailService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                config.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void sendEmailWithAttachment(String[] to, String subject, String body, File attachment)
            throws MessagingException, IOException {

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", config.getProperty("mail.smtp.host", "smtp.gmail.com"));
        props.put("mail.smtp.port", config.getProperty("mail.smtp.port", "587"));
        props.put("mail.smtp.ssl.trust", config.getProperty("mail.smtp.host", "smtp.gmail.com"));

        final String name = config.getProperty("mail.smtp.name");
        final String user = config.getProperty("mail.smtp.user");
        final String pass = config.getProperty("mail.smtp.password");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        session.setDebug(Boolean.parseBoolean(config.getProperty("mail.debug", "false")));

        Message message = new MimeMessage(session);

        try {
            if (name != null && !name.trim().isEmpty()) {
                message.setFrom(new InternetAddress(user, name, "UTF-8"));
            } else {
                message.setFrom(new InternetAddress(user));
            }
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(user));
        }
        
        String encode_subject = MimeUtility.encodeText(subject, "UTF-8", "B");
        message.setSubject(encode_subject);

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(String.join(",", to)));

        Multipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();

        textPart.setText(body, "UTF-8");
        multipart.addBodyPart(textPart);

        if (attachment != null && attachment.exists()) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));
            attachmentPart.setFileName(attachment.getName());
            multipart.addBodyPart(attachmentPart);
        }

        message.setContent(multipart);
        Transport.send(message);
    }

    public void sendSimpleEmail(String[] to, String subject, String body) throws MessagingException, IOException {
        sendEmailWithAttachment(to, subject, body, null);
    }
}
