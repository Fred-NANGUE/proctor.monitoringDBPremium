package com.mycompany.monitoringdb;

import com.mycompany.monitoringdb.config.DatabaseConnection;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.mycompany.monitoringdb.config.ConnexionHolder;
import javafx.stage.Stage;

public class PrimaryController implements Initializable {

    /*Attribut SSH */
    @FXML
    private TextField ipsshField;
    @FXML
    private TextField portsshField;
    @FXML
    private TextField usersshField;
    @FXML
    private PasswordField passwordsshField;
    @FXML
    private Button connectsshButton;

    /*Attribut ORACLE*/
    @FXML
    private TextField iporacleField;
    @FXML
    private TextField portoracleField;
    @FXML
    private TextField serviceField;
    @FXML
    private TextField useroracleField;
    @FXML
    private PasswordField passwordoracleField;
    @FXML
    private Label statusConnexion;
    @FXML
    private Button loginButton;

    private Session sshSession;
    private Connection conn;
    private final int LOCAL_FORWARDED_PORT = 3321;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusConnexion.setText("");
        setOracleFieldsEditable(false);

        // === NETTOYAGE À LA FERMETURE ===
        Platform.runLater(() -> {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                cleanup();
            });
        });
    }

    @FXML
    private void handleTestSSH() {
        String ip_ssh = ipsshField.getText().trim();
        String user_ssh = usersshField.getText().trim();
        String password_ssh = passwordsshField.getText();
        String portText = portsshField.getText().trim();

        // Validation des champs
        if (portText.isEmpty() || ip_ssh.isEmpty() || user_ssh.isEmpty() || password_ssh.isEmpty()) {
            mettreAjourStatus("Veuillez remplir tous les champs SSH.", "ERROR");
            return;
        }

        int sshPort;
        try {
            sshPort = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            mettreAjourStatus("Le port SSH doit être un nombre.", "ERROR");
            return;
        }

        // Désactiver le bouton pendant la connexion
        connectsshButton.setDisable(true);
        connectsshButton.setText("Vérification...");
        mettreAjourStatus("Tentative de connexion SSH...", "SUCCESS");

        // Tâche SSH en arrière-plan
        Task<Void> sshTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JSch jsch = new JSch();
                sshSession = jsch.getSession(user_ssh, ip_ssh, sshPort);
                sshSession.setPassword(password_ssh);
                // --- FORCER L'AUTHENTIFICATION PAR MOT DE PASSE ---
            sshSession.setConfig("PreferredAuthentications", "password");
                sshSession.setConfig("StrictHostKeyChecking", "no");
                sshSession.connect(15000); // Timeout 15 secondes
                return null;
            }
        };

        // Succès SSH
        sshTask.setOnSucceeded(e -> {
            connectsshButton.setText("SSH connecté");
            connectsshButton.setDisable(true);
            mettreAjourStatus("Liaison SSH établie !", "SUCCESS");
            setOracleFieldsEditable(true);
        });

        // Échec SSH
        sshTask.setOnFailed(e -> {
            connectsshButton.setDisable(false);
            connectsshButton.setText("Test SSH");
            Throwable ex = sshTask.getException();
            mettreAjourStatus("Échec SSH : " + ex.getLocalizedMessage(), "ERROR");
        });

        new Thread(sshTask).start();
    }

    @FXML
    private void handleLogin(ActionEvent event) {

        // 6.1. RÉCUPÉRATION DES CHAMPS
        String ip_oracle = iporacleField.getText().trim();
        String service_oracle = serviceField.getText().trim();
        String user_oracle = useroracleField.getText().trim();
        String password_oracle = passwordoracleField.getText();
        String portText = portoracleField.getText().trim();

        // 6.2. VALIDATION DES CHAMPS
        if (portText.isEmpty() || ip_oracle.isEmpty() || service_oracle.isEmpty() || user_oracle.isEmpty() || password_oracle.isEmpty()) {
            statusConnexion.setStyle("-fx-text-fill: #D9534F;");
            statusConnexion.setText("Veuillez remplir tous les champs Oracle.");
            return;
        }

        int dbPort;
        try {
            dbPort = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            mettreAjourStatus("Le port Oracle doit être un nombre.", "ERROR");
            return;
        }

        // 6.3. VÉRIFICATION SSH
        if (sshSession == null || !sshSession.isConnected()) {
            mettreAjourStatus("Veuillez d'abord établir la connexion SSH.", "ERROR");
            return;
        }

        // 6.4. PRÉPARATION DE LA CONNEXION
        loginButton.setDisable(true);
        loginButton.setText("Connexion...");
        mettreAjourStatus("Connexion à Oracle via tunnel SSH...", "SUCCESS");

        // 6.5. TÂCHE ORACLE (arrière-plan)
        Task<Void> oracleTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Établir le tunnel SSH vers la base Oracle
                sshSession.setPortForwardingL(LOCAL_FORWARDED_PORT, ip_oracle, dbPort);

                // Connexion via le tunnel local
                conn = DatabaseConnection.getConnection(
                        "localhost",
                        String.valueOf(LOCAL_FORWARDED_PORT),
                        service_oracle,
                        user_oracle,
                        password_oracle
                );

                // Vérifier que la connexion est active
                if (conn == null || conn.isClosed()) {
                    throw new SQLException("La connexion Oracle est fermée.");
                }

                System.out.println("Connexion Oracle établie !");
                return null;
            }
        };

        oracleTask.setOnSucceeded(e -> {
            loginButton.setStyle("-fx-background-color: #2ec4b6; -fx-text-fill: white;");
            loginButton.setText("Connecté");
            mettreAjourStatus("Connexion Oracle réussie !", "SUCCESS");
            ConnexionHolder.setConnection(conn);
            ConnexionHolder.setSshSession(sshSession);
            ConnexionHolder.setCurrentUser(user_oracle);
            try {
                // Passer au Dashboard
                App.setRoot("dashboard");
            } catch (IOException ex) {
                Logger.getLogger(PrimaryController.class.getName()).log(Level.SEVERE, null, ex);
                mettreAjourStatus("Erreur lors du chargement du Dashboard.", "ERROR");
            }
        });

        oracleTask.setOnFailed(e -> {
            loginButton.setDisable(false);
            loginButton.setText("Se connecter");
            Throwable exception = oracleTask.getException();

            // Nettoyer le tunnel SSH en cas d'échec
            try {
                sshSession.delPortForwardingL(LOCAL_FORWARDED_PORT);
            } catch (Exception ignored) {
            }
            mettreAjourStatus("Échec Oracle : " + exception.getLocalizedMessage(), "ERROR");
        });

        // ============================================================
        // 6.8. LANCEMENT DE LA TÂCHE
        // ============================================================
        Thread thread = new Thread(oracleTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void mettreAjourStatus(String message, String type) {
        Platform.runLater(() -> {
            statusConnexion.setText(message);
            if ("SUCCESS".equalsIgnoreCase(type)) {
                statusConnexion.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold;");
            } else if ("ERROR".equalsIgnoreCase(type)) {
                statusConnexion.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
            } else if ("INFO".equalsIgnoreCase(type)) {
                statusConnexion.setStyle("-fx-text-fill: #3498DB; -fx-font-weight: bold;");
            } else {
                statusConnexion.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold;");
            }
        });
    }

    private void setOracleFieldsEditable(boolean editable) {
        iporacleField.setEditable(editable);
        portoracleField.setEditable(editable);
        serviceField.setEditable(editable);
        useroracleField.setEditable(editable);
        passwordoracleField.setEditable(editable);
    }

    public void cleanup() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Connexion Oracle fermée.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (sshSession != null && sshSession.isConnected()) {
            try {
                sshSession.delPortForwardingL(LOCAL_FORWARDED_PORT);
            } catch (Exception ignored) {
            }
            sshSession.disconnect();
            System.out.println("Session SSH fermée.");
        }
    }
}
