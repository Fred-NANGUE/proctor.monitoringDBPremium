package com.mycompany.monitoringdb;

import com.mycompany.monitoringdb.ServiceExterne.EmailService;
import com.mycompany.monitoringdb.ServiceExterne.GeoIPService;
import com.mycompany.monitoringdb.config.Alertes;
import com.mycompany.monitoringdb.config.AlertNotification;
import com.mycompany.monitoringdb.config.Audit;
import com.mycompany.monitoringdb.config.ConnexionHolder;
import com.mycompany.monitoringdb.config.DatabaseConnection;
import com.mycompany.monitoringdb.config.ExportCSV;
import com.mycompany.monitoringdb.config.LinuxQueries;
import com.mycompany.monitoringdb.config.NotificationHelper;
import static com.mycompany.monitoringdb.config.NotificationHelper.showNotification;
import com.mycompany.monitoringdb.config.OracleQueries;
import com.mycompany.monitoringdb.config.Report;
import com.mycompany.monitoringdb.config.SpecialExport;
import static com.mycompany.monitoringdb.config.SpecialExport.formatSize;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import com.mycompany.monitoringdb.config.TooltipUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

public class SecondaryController implements Initializable {

    /* Attribut du Top */
    @FXML
    private Button btnDeconnectInterface, btnNotification;
    @FXML
    private Label dateInterface, heureInterface, nbreNotification;
    private Popup notificationPopup;
    private VBox popupContent;
    private boolean isPopupShowing = false;

    /* Attribut du Left */
    @FXML
    private Button btn_dashboard, btn_graphic, btn_console, btn_session, btn_alert, btn_logs, btn_audit,
            btn_parametre, btn_utilisateur, btn_rapport;

    /* Attribut Right */
    @FXML
    private Label hote_environnement, os_environnement, cpu_environnement, coeur_environnement,
            ram_environnement, swap_environnement, disque_environnement, utilisateur_environnement;
    @FXML
    private Label version_oracle, statut_oracle, uptime_oracle, sid_oracle, etatAWR_oracle,
            statutArchiveLog_oracle, taille_oracle, tablespace_oracle, session_oracle;
    @FXML
    private FontIcon cacheRightIconBorderPane;
    @FXML
    private SplitPane RightBorderPane;

    /* Attribut Bottom */
    @FXML
    private Label hostLabelBottom, cpuLabelBottom,
            ramLabelBottom, uploadLabelBottom, downloadLabelBottom,
            uptimeLabelBottom, userLabelBottom, diskLabelBottom;
    @FXML
    private Pane cpuChartContainer;
    private AreaChart<Number, Number> cpuChart;
    private XYChart.Series<Number, Number> cpuSeries;
    private int timeIndex = 0;
    private final int MAX_DATA_POINTS = 20;
    @FXML
    private Button closeButtonBottom;

    /* Attribut pages */
    @FXML
    private AnchorPane dashboard_page, graphic_page, console_page, session_page, alert_page,
            log_page, audit_page, parametre_page, utilisateur_page, rapport_page;

    /* Attribut Graphique */
    @FXML
    private BarChart<String, Number> pdbSizeChart, pdbSessionsChart;
    @FXML
    private Label cdbNameLabel, pdbCountLabel, totalSizeLabel;
    @FXML
    private PieChart sgaPieChart, pgaPieChart;
    @FXML
    private Label sgaTotalLabel, sgaBufferLabel, sgaSharedLabel, sgaRedoLabel, pgaTotalLabel,
            pgaUsedLabel, pgaHitLabel, sgaTitleLabel, pgaTitleLabel;
    @FXML
    private Button refreshChartsButton;
    private boolean chartsInitialized = false;

    /* Attribut Console SQL */
    @FXML
    private TextArea sqlZoneTextArea;

    @FXML
    private Button startQuery_button, cleanQuery_button, exportQuery_button;
    @FXML
    private FontIcon execute_stopQueryIcon;
    // Pour gérer l'arrêt de la requête
    private Statement currentStatement;
    private boolean isQueryRunning = false;
    @FXML
    private TableView<ObservableList<String>> resultatQueries;
    public static ObservableList<ObservableList<String>> lastResultData = FXCollections.observableArrayList();
    public static List<String> lastResultColumns = new ArrayList<>();

    /* Attribut Tableau de Bord */
    @FXML
    private LineChart<String, Number> sessionsLineChart, queriesLineChart;
    @FXML
    private AreaChart<String, Number> cpuAreaChart, ramAreaChart;
    private XYChart.Series<String, Number> cpuSeriesTab, ramSeriesTab, sessionsSeriesTab, queriesAllSeriesTab,
            queriesSuccessSeriesTab, queriesFailedSeriesTab, queriesSlowSeriesTab;
    private double ramPercent = 0.0;
    @FXML
    private ComboBox<String> quelTypeDeDonneePourLesRequetesBox;
    private int timeIndexChart = 0;
    private final int MAX_DATACHARTS_POINTS = 30; // Nombre de points affichés


    /* Attribut Sessions */
    @FXML
    private Label totalSessionsLabel, activeSessionsLabel, inactiveSessionsLabel;
    @FXML
    private Button refreshSessions_button, exportSessions_button, viewPositionButton;
    @FXML
    private TableView<ObservableList<String>> sessionsTableView;

    /* Attribut Alertes */
    @FXML
    private Label criticalAlertLabel, warningAlertLabel, infoAlertLabel;
    @FXML
    private Button refreshAlert_button, exportAlert_button, markAllRead_button;
    @FXML
    private ListView<Alertes> alertListView;  // ← ListView
    private ObservableList<Alertes> alertData = FXCollections.observableArrayList();
    private Set<String> notifiedAlertIds = new HashSet<>();

    /* Attribut Logs */
    @FXML
    private Label totalLogsLabel, errorLogsLabel, warningLogsLabel, infoLogsLabel;
    @FXML
    private ComboBox<String> logLevelFilter, logSourceFilter;
    @FXML
    private DatePicker logStartDate, logEndDate;
    @FXML
    private Button filterLogButton, refreshLogButton, exportLogButton;
    @FXML
    private TableView<ObservableList<String>> logTableView;

    /* Attribut de l'audit */
    @FXML
    private Label totalAuditLabel, successAuditLabel, failedAuditLabel;
    @FXML
    private ComboBox<String> auditActionBoxFilter, auditUserBoxFilter;
    @FXML
    private DatePicker auditStartDate, auditEndDate;
    @FXML
    private Button filterAudit_button, refreshAudit_button, exportAudit_button;
    @FXML
    private TableView<ObservableList<String>> auditTableView;
    private ObservableList<ObservableList<String>> auditData = FXCollections.observableArrayList();

    /* Attribut de parametre */
    @FXML
    private TextField intervalSettingField, cpuThresholdSettingField, ramThresholdSettingField,
            diskThresholdSettingField, tablespaceThresholdSettingField, sessionTimeoutField, chartsIntervalField;
    @FXML
    private CheckBox soundAlertCheckBox, systemNotificationCheckBox, emailNotificationCheckBox;
    @FXML
    private TextArea add_emailRecepteurs;
    private String currentTheme = "light";
    private String currentLanguage = "fr";
    @FXML
    private Button saveSettings_button, resetSettings_button, sendEmailNowButton;
    @FXML
    private Label statusParamLabel;
    @FXML
    private ComboBox<String> themeComboBox, emailDayComboBox, emailHourComboBox, languageComboBox;

    /* Attribut Utilisateur */
    @FXML
    private Button addUserOracleButton, editUserOracleButton,
            refreshUsersOracleButton, exportUsersOracleButton, deleteUserOracleButton;
    @FXML
    private Label totalUsersLabel, totalUsersStandardLabel, totalUsersSuperLabel;
    @FXML
    private TableView<ObservableList<String>> usersOracleTableView;
    private ObservableList<ObservableList<String>> usersData = FXCollections.observableArrayList();

    /* Attribut Rapport */
    @FXML
    private DatePicker reportStartDate, reportEndDate;
    @FXML
    private ComboBox<String> reportTypeComboBox, reportFormatComboBox;
    @FXML
    private Button generateReportButton, downloadReportButton, deleteReportButton;
    @FXML
    private ProgressBar progressReportIndicator;
    @FXML
    private TableView reportsTableView;
    @FXML
    private Label reportStatusLabel;
    @FXML
    private TextArea reportPreviewArea;
    private ObservableList<Report> reportsData = FXCollections.observableArrayList();
    private File reportsDirectory;


    /* Attribut pour la navigation */
    private Map<Button, AnchorPane> pageMap = new HashMap<>();
    private Button currentButton = null;

    /* Attribut des requetes */
    private OracleQueries Queries;
    private LinuxQueries linux;

    private int secondCounter = 0;
    private static final int REFRESH_INTERVAL1 = 30;  // ← 30 secondes
    private static final int REFRESH_INTERVAL2 = 7;  // ← 7 secondes
    private static final int REFRESH_INTERVAL3 = 1;  // ← 1 secondes

    private String currentUser = "Administrateur"; // valeur par défaut

    /* Methodes */
    private void initClock() {
        // Mettre à jour l'heure et la date toutes les secondes
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    updateDateTime();
                    // 2. Compter les secondes
                    secondCounter++;

                    // 3. Rafraîchir le RIGHT toutes les 30 secondes
                    if (secondCounter % REFRESH_INTERVAL1 == 0) {
                        // remplirDashboard();
                    }
                    // 3. Rafraîchir de Alert et tooltip toutes les 7 secondes
                    if (secondCounter % REFRESH_INTERVAL2 == 0) {
                        refreshAlertes();
                        refreshLogs();
                    }
                    if (secondCounter % REFRESH_INTERVAL2 == 0) {
                        updateTooltips();
                    }
                    //   if (secondCounter % REFRESH_INTERVAL3 == 0) {
                    //      long unreadCount = alertData.stream().filter(a -> "UNREAD".equals(a.getStatus())).count();
                    //      nbreNotification.setText(String.valueOf(unreadCount));
                    //   }

                })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        heureInterface.setText(now.format(timeFormatter));
        dateInterface.setText(now.format(dateFormatter));
    }

    public void DeconnexionCentral() throws IOException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Voulez-vous vraiment vous déconnecter ?");
        alert.setContentText("Vous allez être redirigé vers la page de connexion.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            PrimaryController clean = new PrimaryController();
            clean.cleanup();
            App.setRoot("primary");
        }
    }

    private void initNotificationBell() {
        // Créer la popup
        notificationPopup = new Popup();
        notificationPopup.setAutoHide(true);
        notificationPopup.setHideOnEscape(true);

        // Contenu de la popup
        popupContent = new VBox(5);
        popupContent.setStyle(
                "-fx-background-color: #0D1B2A; "
                + "-fx-border-color: #D4AF37; "
                + "-fx-border-width: 2px; "
                + "-fx-border-radius: 8px; "
                + "-fx-background-radius: 8px; "
                + "-fx-padding: 10px; "
                + "-fx-min-width: 300px; "
                + "-fx-max-width: 350px; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0.3, 0, 5);"
        );

        notificationPopup.getContent().add(popupContent);

        // Action du bouton cloche
        btnNotification.setOnAction(event -> toggleNotificationPopup());

        // Mettre à jour le badge
        updateNotificationBadge();
    }

    private void toggleNotificationPopup() {
        if (isPopupShowing) {
            notificationPopup.hide();
            isPopupShowing = false;
        } else {
            showNotificationPopup();
        }
    }

    private void showNotificationPopup() {
        try {
            // Récupérer les alertes non lues
            ObservableList<Alertes> unreadAlerts = alertData.stream()
                    .filter(a -> "UNREAD".equals(a.getStatus()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));

            // Vider le contenu
            popupContent.getChildren().clear();

            // Titre avec compteur
            Label titleLabel = new Label("🔔 Notifications (" + unreadAlerts.size() + " non lues)");
            titleLabel.setStyle(
                    "-fx-text-fill: #D4AF37; "
                    + "-fx-font-weight: bold; "
                    + "-fx-font-size: 14px; "
                    + "-fx-padding: 0 0 5px 0;"
            );
            popupContent.getChildren().add(titleLabel);

            // Séparateur
            Separator separator = new Separator();
            separator.setStyle("-fx-background-color: #D4AF37;");
            popupContent.getChildren().add(separator);

            if (unreadAlerts.isEmpty()) {
                // Aucune alerte non lue
                Label noAlertLabel = new Label("✅ Toutes les alertes ont été consultées");
                noAlertLabel.setStyle(
                        "-fx-text-fill: #27AE60; "
                        + "-fx-font-size: 12px; "
                        + "-fx-padding: 10px 0;"
                );
                popupContent.getChildren().add(noAlertLabel);

                // Vérifier si la table est vide
                if (alertData.isEmpty()) {
                    Label emptyLabel = new Label("📭 Aucune alerte signalée");
                    emptyLabel.setStyle(
                            "-fx-text-fill: #7f8c8d; "
                            + "-fx-font-size: 12px; "
                            + "-fx-padding: 5px 0 10px 0;"
                    );
                    popupContent.getChildren().add(emptyLabel);
                }

            } else {
                // Afficher les 3 premières alertes non lues
                int count = 0;
                for (Alertes alert : unreadAlerts) {
                    if (count >= 3) {
                        break;
                    }
                    count++;

                    // Conteneur de l'alerte
                    VBox alertBox = new VBox(2);
                    alertBox.setStyle(
                            "-fx-background-color: #1a2a3e; "
                            + "-fx-padding: 8px; "
                            + "-fx-border-radius: 5px; "
                            + "-fx-background-radius: 5px; "
                            + "-fx-border-color: #2a3a4e; "
                            + "-fx-border-width: 1px;"
                    );

                    // Icône + niveau
                    String icon = "CRITICAL".equals(alert.getLevel()) ? "🔴"
                            : "WARNING".equals(alert.getLevel()) ? "🟡" : "🔵";

                    Label levelLabel = new Label(icon + " " + alert.getLevel() + " - " + alert.getSource());
                    levelLabel.setStyle(
                            "-fx-text-fill: #ecf0f1; "
                            + "-fx-font-weight: bold; "
                            + "-fx-font-size: 12px;"
                    );

                    // Message (tronqué)
                    String message = alert.getMessage();
                    if (message.length() > 50) {
                        message = message.substring(0, 47) + "...";
                    }
                    Label messageLabel = new Label(message);
                    messageLabel.setStyle(
                            "-fx-text-fill: #b0b8c0; "
                            + "-fx-font-size: 11px; "
                            + "-fx-wrap-text: true;"
                    );
                    messageLabel.setMaxWidth(280);

                    // Heure
                    Label timeLabel = new Label("🕐 " + alert.getTimestamp());
                    timeLabel.setStyle(
                            "-fx-text-fill: #7f8c8d; "
                            + "-fx-font-size: 10px;"
                    );

                    alertBox.getChildren().addAll(levelLabel, messageLabel, timeLabel);
                    popupContent.getChildren().add(alertBox);
                }

                // Bouton "Voir plus" si plus de 3 alertes
                if (unreadAlerts.size() > 3) {
                    Button seeMoreButton = new Button("📋 Voir toutes les alertes (" + (unreadAlerts.size() - 3) + " autres)");
                    seeMoreButton.setStyle(
                            "-fx-background-color: #D4AF37; "
                            + "-fx-text-fill: #0D1B2A; "
                            + "-fx-font-weight: bold; "
                            + "-fx-font-size: 12px; "
                            + "-fx-padding: 5px 15px; "
                            + "-fx-border-radius: 5px; "
                            + "-fx-background-radius: 5px; "
                            + "-fx-cursor: hand;"
                    );
                    seeMoreButton.setMaxWidth(Double.MAX_VALUE);
                    seeMoreButton.setOnAction(e -> {
                        notificationPopup.hide();
                        isPopupShowing = false;
                        // Naviguer vers la page Alertes
                        Paserelle(btn_alert);
                    });
                    popupContent.getChildren().add(seeMoreButton);
                }
            }

            // Positionner la popup sous le bouton
            Node button = btnNotification;
            Bounds bounds = button.localToScreen(button.getBoundsInLocal());
            double x = bounds.getMinX() + (bounds.getWidth() / 2) - 175;
            double y = bounds.getMaxY() + 5;

            // S'assurer que la popup reste dans l'écran
            Screen screen = Screen.getPrimary();
            Rectangle2D visualBounds = screen.getVisualBounds();
            if (x + 350 > visualBounds.getMaxX()) {
                x = visualBounds.getMaxX() - 360;
            }
            if (x < visualBounds.getMinX()) {
                x = visualBounds.getMinX() + 10;
            }
            if (y + popupContent.getBoundsInLocal().getHeight() > visualBounds.getMaxY()) {
                y = visualBounds.getMaxY() - popupContent.getBoundsInLocal().getHeight() - 10;
            }

            notificationPopup.show(button, x, y);
            isPopupShowing = true;

        } catch (Exception e) {
            System.err.println("Erreur affichage popup : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateNotificationBadge() {
        long unreadCount = alertData.stream()
                .filter(a -> "UNREAD".equals(a.getStatus()))
                .count();

        nbreNotification.setText(String.valueOf(unreadCount));

        // Cacher le badge si 0
        if (unreadCount == 0) {
            nbreNotification.setVisible(false);
        } else {
            nbreNotification.setVisible(true);
        }
    }

    private void refreshNotificationBell() {
        updateNotificationBadge();
        // Si la popup est ouverte, la rafraîchir
        if (isPopupShowing) {
            showNotificationPopup();
        }
    }

    private void Paserelle(Button clickedButton) {
        // 1. Désélectionner l'ancien bouton
        if (currentButton != null) {
            currentButton.getStyleClass().remove("selected");
            // Cacher la page précédente
            AnchorPane oldPage = pageMap.get(currentButton);
            if (oldPage != null) {
                oldPage.setVisible(false);
                oldPage.setManaged(false);
            }
        }

        // 2. Sélectionner le nouveau bouton
        clickedButton.getStyleClass().add("selected");
        currentButton = clickedButton;

        // 3. Afficher la page correspondante
        AnchorPane newPage = pageMap.get(clickedButton);
        if (newPage != null) {
            newPage.setVisible(true);
            newPage.setManaged(true);
        }
    }

    public void Cache_DeployRight() {
        if (cacheRightIconBorderPane.getIconLiteral().equals("fas-angle-double-left")) {
            RightBorderPane.setVisible(true);
            RightBorderPane.setManaged(true);
            cacheRightIconBorderPane.setIconLiteral("fas-minus");
        } else if (cacheRightIconBorderPane.getIconLiteral().equals("fas-minus")) {
            RightBorderPane.setVisible(false);
            RightBorderPane.setManaged(false);
            cacheRightIconBorderPane.setIconLiteral("fas-angle-double-left");
        }
    }

    private void remplirRight() {
        try {
            if (Queries == null) {
                System.err.println("OracleQueries n'est pas initialisé.");
                return;
            }

            hote_environnement.setText(Queries.getHost());
            os_environnement.setText(Queries.getOS());
            cpu_environnement.setText(linux.getSystemNameCPU());
            coeur_environnement.setText(linux.getSystemCPUCores());
            ram_environnement.setText(linux.getSystemRAM());
            swap_environnement.setText(linux.getSystemSwap());
            disque_environnement.setText(linux.getSystemDiskTotal());
            utilisateur_environnement.setText(Queries.getActiveSessions());

            version_oracle.setText(Queries.getVersion());
            statut_oracle.setText(Queries.getStatus());
            uptime_oracle.setText(Queries.getUptime());
            sid_oracle.setText(Queries.getSID());
            statutArchiveLog_oracle.setText(Queries.getStatusArchiveLog());
            if (Queries.getStatusArchiveLog().equals("STOPPED")) {
                statutArchiveLog_oracle.setStyle("-fx-text-fill : red; -fx-font-weight: bold;");
            } else {
                statutArchiveLog_oracle.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold;");
            }
            taille_oracle.setText(Queries.getDBSize());
            tablespace_oracle.setText(Queries.getTablespaceUsage());
            session_oracle.setText(Queries.getActiveSessions());
            etatAWR_oracle.setText(Queries.getEtatAWR());
            if (Queries.getEtatAWR().equals("NONE")) {
                etatAWR_oracle.setStyle("-fx-text-fill : red; -fx-font-weight: bold;");
            } else {
                etatAWR_oracle.setStyle("-fx-text-fill: #D4AF37; -fx-font-weight: bold;");
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initBottomMetrics() {
        Tooltip cpuTooltip = TooltipUtils.createDetailTooltip(
                "CPU",
                "Utilisation : " + cpuLabelBottom.getText(),
                "Fréquence : 2.4 GHz",
                "Cœurs : " + linux.getSystemCPUCores(),
                "Threads : " + Integer.parseInt(linux.getSystemCPUCores()) * 2
        );
        Tooltip.install(cpuLabelBottom, cpuTooltip);

        Tooltip ramTooltip = TooltipUtils.createDetailTooltip(
                "RAM",
                "Utilisée : 2.21",
                "Totale : 5.56",
                "Disponible : 3.35",
                "Utilisation : 23%"
        );
        Tooltip.install(ramLabelBottom, ramTooltip);

        Tooltip diskTooltip = TooltipUtils.createDetailTooltip(
                "Disque",
                "Utilisé : 14 GB",
                "Total : 60 GB",
                "Libre : 46 GB",
                "Utilisation : 23%"
        );
        Tooltip.install(diskLabelBottom, diskTooltip);
    }

    private double extractNumericValue(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Supprime tout ce qui n'est pas un chiffre, un point ou un signe moins
        String cleaned = text.replaceAll("[^0-9.,-]", "");
        // Remplace la virgule par un point (si utilisée comme séparateur décimal)
        cleaned = cleaned.replace(',', '.');
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void updateTooltips() {
        try {
            // CPU
            double cpu = extractNumericValue(cpuLabelBottom.getText());
            Tooltip cpuTooltip = new Tooltip(String.format(
                    "💻 CPU\nUtilisation : %.1f%%\nFréquence : 2.4 GHz\nCœurs : 4",
                    cpu
            ));
            Tooltip.install(cpuLabelBottom, cpuTooltip);

            // RAM
            String ramText = ramLabelBottom.getText(); // "2.21 / 5.56 GB"
            String[] parts = ramText.split("/");
            if (parts.length == 2) {
                double used = extractNumericValue(parts[0]);
                double total = extractNumericValue(parts[1]);
                double percent = total > 0 ? (used / total) * 100 : 0;

                Tooltip ramTooltip = new Tooltip(String.format(
                        "🧠 RAM\nUtilisée : %.2f GB\nTotale : %.2f GB\nUtilisation : %.1f%%",
                        used, total, percent
                ));
                Tooltip.install(ramLabelBottom, ramTooltip);
            }

            // Disque
            double disk = extractNumericValue(diskLabelBottom.getText());
            Tooltip diskTooltip = new Tooltip(String.format(
                    "💾 Disque\nUtilisation : %.1f%%\nTotal : 60 GB\nLibre : %.1f GB",
                    disk, 60 - (disk / 100) * 60
            ));
            Tooltip.install(diskLabelBottom, diskTooltip);

        } catch (Exception e) {
            System.err.println("Erreur updateTooltips : " + e.getMessage());
        }
    }

    /* Methode graphique */
    private void initAdvancedCharts() {
        if (chartsInitialized) {
            return;
        }

        try {
            // ===== BOUTON RAFRAÎCHIR =====
            refreshChartsButton.setOnAction(event -> refreshAdvancedCharts());

            // ===== CHARGEMENT DES DONNÉES =====
            loadCDBPDBData();
            loadSGAPGAData();

            chartsInitialized = true;
            System.out.println("✅ Graphiques avancés initialisés");

        } catch (Exception e) {
            System.err.println("❌ Erreur initAdvancedCharts : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCDBPDBData() {
        try {
            // 1. Récupérer le nom du CDB
            String cdbName = Queries.getCdbName();
            cdbNameLabel.setText("CDB : " + (cdbName != null ? cdbName : "N/A"));

            // 2. Récupérer la liste des PDB
            List<Map<String, Object>> pdbList = Queries.getPdbList();
            pdbCountLabel.setText("PDB : " + pdbList.size());

            // 3. Calculer la taille totale
            double totalSize = 0;
            for (Map<String, Object> pdb : pdbList) {
                totalSize += (double) pdb.getOrDefault("size", 0.0);
            }
            totalSizeLabel.setText(String.format("Total Size : %.2f GB", totalSize));

            // 4. Remplir le graphique des tailles
            XYChart.Series<String, Number> sizeSeries = new XYChart.Series<>();
            sizeSeries.setName("Taille (GB)");

            // 5. Remplir le graphique des sessions
            XYChart.Series<String, Number> sessionSeries = new XYChart.Series<>();
            sessionSeries.setName("Sessions");

            for (Map<String, Object> pdb : pdbList) {
                String name = (String) pdb.getOrDefault("name", "N/A");
                double size = (double) pdb.getOrDefault("size", 0.0);
                int sessions = (int) pdb.getOrDefault("sessions", 0);

                sizeSeries.getData().add(new XYChart.Data<>(name, size));
                sessionSeries.getData().add(new XYChart.Data<>(name, sessions));
            }

            // Ajouter les séries aux graphiques
            pdbSizeChart.getData().clear();
            pdbSizeChart.getData().add(sizeSeries);

            pdbSessionsChart.getData().clear();
            pdbSessionsChart.getData().add(sessionSeries);

        } catch (SQLException e) {
            System.err.println("Erreur loadCDBPDBData : " + e.getMessage());
            cdbNameLabel.setText("CDB : Erreur");
            pdbCountLabel.setText("PDB : Erreur");
            totalSizeLabel.setText("Total Size : Erreur");
        }
    }

    private void loadSGAPGAData() {
        try {
            // ===== SGA =====
            Map<String, Object> sgaData = Queries.getSGAData();

            // Titre
            sgaTitleLabel.setText("SGA (System Global Area) - "
                    + String.format("%.2f GB", (double) sgaData.getOrDefault("total", 0.0)));

            // Labels
            sgaTotalLabel.setText(String.format("%.2f GB", (double) sgaData.getOrDefault("total", 0.0)));
            sgaBufferLabel.setText(String.format("%.2f GB", (double) sgaData.getOrDefault("buffer_cache", 0.0)));
            sgaSharedLabel.setText(String.format("%.2f GB", (double) sgaData.getOrDefault("shared_pool", 0.0)));
            sgaRedoLabel.setText(String.format("%.2f GB", (double) sgaData.getOrDefault("redo_log", 0.0)));

            // PieChart SGA
            sgaPieChart.getData().clear();
            sgaPieChart.getData().addAll(
                    new PieChart.Data("Buffer Cache", (double) sgaData.getOrDefault("buffer_cache", 0.0)),
                    new PieChart.Data("Shared Pool", (double) sgaData.getOrDefault("shared_pool", 0.0)),
                    new PieChart.Data("Redo Log", (double) sgaData.getOrDefault("redo_log", 0.0))
            );

            // ===== PGA =====
            Map<String, Object> pgaData = Queries.getPGAData();

            // Titre
            pgaTitleLabel.setText("PGA (Program Global Area) - "
                    + String.format("%.2f GB", (double) pgaData.getOrDefault("total", 0.0)));

            // Labels
            pgaTotalLabel.setText(String.format("%.2f GB", (double) pgaData.getOrDefault("total", 0.0)));
            pgaUsedLabel.setText(String.format("%.2f GB", (double) pgaData.getOrDefault("used", 0.0)));
            pgaHitLabel.setText(String.format("%.1f%%", (double) pgaData.getOrDefault("hit_ratio", 0.0)));

            // PieChart PGA
            pgaPieChart.getData().clear();
            pgaPieChart.getData().addAll(
                    new PieChart.Data("Utilisée", (double) pgaData.getOrDefault("used", 0.0)),
                    new PieChart.Data("Libre", (double) pgaData.getOrDefault("free", 0.0))
            );

        } catch (SQLException e) {
            System.err.println("Erreur loadSGAPGAData : " + e.getMessage());
        }
    }

    private void refreshAdvancedCharts() {
        System.out.println("Rafraîchissement des graphiques avancés...");
        loadCDBPDBData();
        loadSGAPGAData();
        NotificationHelper.showSuccess("Graphiques rafraîchis !");
    }

    /* Methode de la console SQL */
    private void initSqlConsole() {
        startQuery_button.setOnAction(event -> handleExecuteOrStop());
        cleanQuery_button.setOnAction(event -> clearSqlConsole());
        exportQuery_button.setOnAction(event -> exportResultsQuery());

        //exportResultsQuery Raccourci Ctrl+Enter
        sqlZoneTextArea.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                handleExecuteOrStop();
            }
        });
    }

    private void executeSql() {
        String sql = sqlZoneTextArea.getText().trim();

        if (sql.isEmpty()) {
            NotificationHelper.showWarning("Veuillez saisir une requête SQL.");
            return;
        }

        // Changer l'état du bouton
        isQueryRunning = true;
        startQuery_button.setText("STOPPER");
        execute_stopQueryIcon.setIconLiteral("fas-stop");
        startQuery_button.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;"
                + " -fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;"
                + " -fx-padding: 8 25 8 25; -fx-background-radius: 5;");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                executeSqlQuery(sql);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            resetExecuteButton();
            NotificationHelper.showSuccess("Requête exécutée avec succès.");
        });

        task.setOnFailed(e -> {
            resetExecuteButton();
            Throwable ex = task.getException();
            if (ex instanceof SQLException && ex.getMessage().contains("cancelled")) {
                NotificationHelper.showInfo("Requête annulée par l'utilisateur.");
            } else {
                NotificationHelper.showError("Erreur : " + ex.getMessage());
            }
        });

        new Thread(task).start();
    }

    private void executeSqlQuery(String sql) throws SQLException {
        Connection connection = ConnexionHolder.getConnection();

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connexion interrumpue.");
        }
        // Utiliser Statement pour pouvoir l'annuler
        currentStatement = connection.createStatement();

        try (Statement stmt = connection.createStatement()) {
            if (sql.trim().toLowerCase().startsWith("select")) {
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    // === VIDER LES ANCIENNES DONNÉES ===
                    Platform.runLater(() -> {
                        resultatQueries.getColumns().clear();
                        resultatQueries.getItems().clear();
                    });

                    // === RÉCUPÉRER LES MÉTADONNÉES ===
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    // === STOCKER LES NOMS DES COLONNES POUR L'EXPORT ===
                    lastResultColumns.clear();
                    for (int i = 1; i <= columnCount; i++) {
                        lastResultColumns.add(metaData.getColumnName(i));
                    }

                    // === CRÉER LES COLONNES ===
                    List<TableColumn<ObservableList<String>, String>> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        final int colIndex = i - 1;
                        TableColumn<ObservableList<String>, String> column = new TableColumn<>(metaData.getColumnName(i));
                        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(colIndex)));
                        column.setPrefWidth(150);
                        columns.add(column);
                    }

                    // === AJOUTER LES COLONNES ===
                    Platform.runLater(() -> resultatQueries.getColumns().addAll(columns));

                    // === REMPLIR LES DONNÉES ===
                    ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
                    while (rs.next()) {
                        ObservableList<String> row = FXCollections.observableArrayList();
                        for (int i = 1; i <= columnCount; i++) {
                            row.add(rs.getString(i) != null ? rs.getString(i) : "null");
                        }
                        data.add(row);
                    }

                    // === STOCKER LE RÉSULTAT POUR L'EXPORT ===
                    lastResultData = data;  // ← Stocker les données

                    Platform.runLater(() -> resultatQueries.setItems(data));
                }
            } else {
                // UPDATE, INSERT, DELETE
                int rowsAffected = stmt.executeUpdate(sql);
                showNotification("Okay" + rowsAffected + " ligne(s) affectée(s).", "success");

                // Vider les résultats précédents car ce n'était pas un SELECT
                lastResultData.clear();
                lastResultColumns.clear();
                Platform.runLater(() -> {
                    resultatQueries.getColumns().clear();
                    resultatQueries.getItems().clear();
                });
            }
        }
    }

    private void handleExecuteOrStop() {
        if (isQueryRunning) {
            // === DEMANDER LA CONFIRMATION ===
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Arrêt de la requête");
            confirm.setHeaderText("Voulez-vous vraiment stopper la requête en cours ?");
            confirm.setContentText("L'arrêt peut prendre quelques secondes.");

            ButtonType btnStop = new ButtonType("Oui, stopper");
            ButtonType btnContinue = new ButtonType("Non, continuer", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(btnStop, btnContinue);

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == btnStop) {
                stopQuery();
            }
        } else {
            // Démarrer la requête
            executeSql();
        }
    }

    private void stopQuery() {
        try {
            if (currentStatement != null && !currentStatement.isClosed()) {
                currentStatement.cancel();  // ← Annule la requête
                resetExecuteButton();
                System.out.println("Requête annulée par l'utilisateur.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            NotificationHelper.showError("Erreur lors de l'arrêt : " + e.getMessage());
        }
    }

    private void resetExecuteButton() {
        isQueryRunning = false;
        startQuery_button.setText("EXECUTER");
        execute_stopQueryIcon.setIconLiteral("fas-play");
        startQuery_button.setStyle("-fx-background-color: #0D1B2A; -fx-text-fill: white;"
                + "-fx-font-weight: bold;-fx-font-size: 13px; -fx-cursor: hand;-fx-padding: 8 25 8 25;"
                + " -fx-background-radius: 5;");
    }

    private void clearSqlConsole() {
        // Vérifier si une requête est en cours
        if (isQueryRunning) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attention");
            alert.setHeaderText("Une requête est en cours d'exécution.");
            alert.setContentText("Voulez-vous vraiment effacer la console ? La requête continuera en arrière-plan.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        sqlZoneTextArea.clear();
        resultatQueries.getColumns().clear();
        resultatQueries.getItems().clear();
        lastResultColumns.clear();
        lastResultData.clear();
    }

    private void exportResultsQuery() {
        if (resultatQueries.getItems().isEmpty()) {
            NotificationHelper.showNotification("Aucun résultat à exporter.", "warning");
            return;
        }

        // Récupérer les en-têtes
        List<String> headers = resultatQueries.getColumns().stream()
                .map(col -> col.getText())
                .collect(Collectors.toList());

        // Récupérer les données
        ObservableList<ObservableList<String>> data = resultatQueries.getItems();

        // Exporter
        ExportCSV.exportSimple(headers, data,
                resultatQueries.getScene().getWindow(),
                "resultats_sql");
        // Tracqueur
        try {
            Queries.insertLog("INFO", "Export", currentUser, "Export des données de requete en CSV");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'écriture du log : " + e.getMessage());
        }
    }

    /* Methode de la Session */
    private void initSessions() {
        refreshSessions_button.setOnAction(event -> refreshSessions());
        exportSessions_button.setOnAction(event -> exportSessions());
        // ✅ Bouton Voir Position
        viewPositionButton.setOnAction(event -> showSessionPosition());

        // ✅ Activer/désactiver selon la sélection
        sessionsTableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    viewPositionButton.setVisible(newVal != null);
                    viewPositionButton.setManaged(newVal != null);
                }
        );

        refreshSessions();
    }

    private void refreshSessions() {
        // Désactiver le bouton pendant le chargement
        refreshSessions_button.setDisable(true);
        refreshSessions_button.setText("Chargement...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                loadSessions();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            refreshSessions_button.setDisable(false);
            refreshSessions_button.setText("RAFRAICHIR");
        });

        task.setOnFailed(e -> {
            refreshSessions_button.setDisable(false);
            refreshSessions_button.setText("RAFRAICHIR");
            Throwable ex = task.getException();
            NotificationHelper.showNotification("Erreur : " + ex.getMessage(), "error");
        });

        new Thread(task).start();
    }

    private void loadSessions() throws SQLException {
        Connection connection = ConnexionHolder.getConnection();

        if (connection == null || connection.isClosed()) {
            throw new SQLException("Connexion interrumpue.");
        }

        String sql = "SELECT SID, SERIAL#, USERNAME, STATUS, MACHINE, PROGRAM, "
                + "TO_CHAR(LOGON_TIME, 'DD/MM/YYYY HH24:MI:SS') AS LOGON_TIME, SQL_ID "
                + "FROM V$SESSION "
                + "WHERE USERNAME IS NOT NULL "
                + "ORDER BY LOGON_TIME DESC";

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            // === VIDER L'ANCIEN TABLEAU ===
            Platform.runLater(() -> {
                sessionsTableView.getColumns().clear();
                sessionsTableView.getItems().clear();
            });

            // === MÉTADONNÉES ===
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // === NOMS DES COLONNES ===
            String[] columnNames = {"SID", "SERIAL#", "USERNAME", "STATUS", "MACHINE", "PROGRAM", "LOGON TIME", "SQL_ID"};

            // === CRÉER LES COLONNES ===
            List<TableColumn<ObservableList<String>, String>> columns = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                final int colIndex = i;
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnNames[i]);
                column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(colIndex)));
                column.setPrefWidth(i == 1 || i == 6 ? 80 : 120);
                columns.add(column);
            }

            // === REMPLIR LES DONNÉES ===
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            int total = 0, active = 0, inactive = 0;

            while (rs.next()) {
                ObservableList<String> row = FXCollections.observableArrayList();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getString(i) != null ? rs.getString(i) : "");
                }
                data.add(row);
                total++;
                if ("ACTIVE".equalsIgnoreCase(rs.getString("STATUS"))) {
                    active++;
                } else {
                    inactive++;
                }
            }

            final int finalTotal = total;
            final int finalActive = active;
            final int finalInactive = inactive;
            final ObservableList<ObservableList<String>> finalData = data;
            final List<TableColumn<ObservableList<String>, String>> finalColumns = columns;

            // === METTRE À JOUR L'UI ===
            Platform.runLater(() -> {
                sessionsTableView.getColumns().addAll(finalColumns);
                sessionsTableView.setItems(finalData);

                totalSessionsLabel.setText("Total : " + finalTotal);
                activeSessionsLabel.setText("Actives : " + finalActive);
                inactiveSessionsLabel.setText("Inactives : " + finalInactive);
            });

            System.out.println("Sessions chargées : " + total);
        }
    }

    private void exportSessions() {
        // Récupérer les en-têtes
        List<String> headers = sessionsTableView.getColumns().stream()
                .map(col -> col.getText())
                .collect(Collectors.toList());

        // Récupérer les données
        ObservableList<ObservableList<String>> data = sessionsTableView.getItems();

        // Exporter
        ExportCSV.exportSimple(headers, data,
                sessionsTableView.getScene().getWindow(),
                "sessions");

        // Tracqueur
        try {
            Queries.insertLog("INFO", "Export", currentUser, "Export des sessions en CSV");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'écriture du log : " + e.getMessage());
        }
    }

    private void showSessionPosition() {
        ObservableList<String> selected = sessionsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationHelper.showWarning("Veuillez sélectionner une session.");
            return;
        }

        String ipAddress = extractIPFromSession(selected);
        if (ipAddress == null || ipAddress.isEmpty() || "N/A".equals(ipAddress)) {
            NotificationHelper.showWarning("Impossible de déterminer l'IP de cette session.");
            return;
        }

        GeoIPService.GeoIPInfo geoInfo = GeoIPService.getGeoIPInfo(ipAddress);
        showGeoPopup(selected, geoInfo);
    }

    private String extractIPFromSession(ObservableList<String> row) {
        String machine = row.get(4); // INDEX 4 = MACHINE
        if (machine != null && !machine.isEmpty()) {
            if (machine.contains("/")) {
                String[] parts = machine.split("/");
                if (parts.length > 1 && parts[1].matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                    return parts[1];
                }
            }
            if (machine.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                return machine;
            }
        }
        return null;
    }

    private void showGeoPopup(ObservableList<String> session, GeoIPService.GeoIPInfo geoInfo) {
        Stage popupStage = new Stage();
        popupStage.setTitle("📍 Position de la session");
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setResizable(true);

        VBox mainContainer = new VBox(12);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #0D1B2A; -fx-background-radius: 10px;");

        Label titleLabel = new Label("📍 Position de la session");
        titleLabel.setStyle("-fx-text-fill: #D4AF37; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Infos session
        GridPane sessionInfoGrid = new GridPane();
        sessionInfoGrid.setHgap(10);
        sessionInfoGrid.setVgap(5);
        sessionInfoGrid.setPadding(new Insets(5, 0, 10, 0));

        String[] labels = {"Utilisateur", "SID", "Status", "Machine"};
        for (int i = 0; i < Math.min(labels.length, session.size()); i++) {
            Label keyLabel = new Label(labels[i] + " :");
            keyLabel.setStyle("-fx-text-fill: #a0b4c8; -fx-font-weight: bold;");
            Label valueLabel = new Label(session.get(i) != null ? session.get(i) : "N/A");
            valueLabel.setStyle("-fx-text-fill: #ecf0f1;");
            sessionInfoGrid.add(keyLabel, 0, i);
            sessionInfoGrid.add(valueLabel, 1, i);
        }
        sessionInfoGrid.setStyle("-fx-border-color: #D4AF37; -fx-border-width: 0 0 1px 0; -fx-padding: 0 0 10px 0;");

        // Infos géolocalisation
        GridPane geoGrid = new GridPane();
        geoGrid.setHgap(10);
        geoGrid.setVgap(5);
        geoGrid.setPadding(new Insets(10, 0, 10, 0));

        if (geoInfo.isSuccess()) {
            String[][] geoData = {
                {"🌐 IP", geoInfo.getIpAddress()},
                {"📍 Pays", geoInfo.getCountry()},
                {"🏙️ Ville", geoInfo.getCity()},
                {"🗺️ Région", geoInfo.getRegion()},
                {"🔌 FAI", geoInfo.getIsp()},
                {"🏢 Organisation", geoInfo.getOrganization()},
                {"📍 Coordonnées", String.format("%.4f, %.4f", geoInfo.getLatitude(), geoInfo.getLongitude())}
            };

            int row = 0;
            for (String[] data : geoData) {
                Label keyLabel = new Label(data[0] + " :");
                keyLabel.setStyle("-fx-text-fill: #a0b4c8; -fx-font-weight: bold;");
                Label valueLabel = new Label(data[1]);
                valueLabel.setStyle("-fx-text-fill: #ecf0f1;");
                geoGrid.add(keyLabel, 0, row);
                geoGrid.add(valueLabel, 1, row);
                row++;
            }

            if (geoInfo.isSuspicious()) {
                Label suspiciousLabel = new Label("⚠️ IP suspecte (connexion externe)");
                suspiciousLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                geoGrid.add(suspiciousLabel, 0, row, 2, 1);
            }
        } else {
            Label errorLabel = new Label("❌ Impossible de géolocaliser cette IP : " + geoInfo.getIpAddress());
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            geoGrid.add(errorLabel, 0, 0, 2, 1);
        }

        // Carte
        WebView webView = new WebView();
        webView.setPrefSize(600, 400);
        webView.setStyle("-fx-background-radius: 8px; -fx-background-color: #1a2a3e;");

        String htmlMap;
        if (geoInfo.isSuccess() && geoInfo.getLatitude() != 0 && geoInfo.getLongitude() != 0) {
            htmlMap = getMapHTML(geoInfo.getLatitude(), geoInfo.getLongitude(),
                    geoInfo.getCity() + ", " + geoInfo.getCountry());
        } else {
            htmlMap = getMapHTML(48.8566, 2.3522, "Position non disponible");
        }
        webView.getEngine().loadContent(htmlMap);

        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button closeButton = new Button("✕ Fermer");
        closeButton.setStyle(
                "-fx-background-color: #e74c3c; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 8 25 8 25; -fx-border-radius: 5px; -fx-background-radius: 5px;"
        );
        closeButton.setOnAction(e -> popupStage.close());

        if (geoInfo.isSuccess() && geoInfo.isSuspicious()) {
            Button alertButton = new Button("🚨 Créer une alerte");
            alertButton.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; "
                    + "-fx-font-weight: bold; -fx-padding: 8 25 8 25; -fx-border-radius: 5px; -fx-background-radius: 5px;"
            );
            alertButton.setOnAction(e -> {
                try {
                    String message = String.format("Session suspecte depuis %s (%s, %s) - IP: %s",
                            geoInfo.getCity(), geoInfo.getCountry(), geoInfo.getIsp(), geoInfo.getIpAddress());
                    Queries.insertAlert("CRITICAL", message, "Sécurité IP");
                    Queries.insertLog("WARNING", "Sécurité", currentUser, message);
                    NotificationHelper.showSuccess("Alerte créée avec succès !");
                } catch (SQLException ex) {
                    NotificationHelper.showError("Erreur : " + ex.getMessage());
                }
            });
            buttonBox.getChildren().add(alertButton);
        }

        buttonBox.getChildren().add(closeButton);

        mainContainer.getChildren().addAll(titleLabel, sessionInfoGrid, geoGrid, webView, buttonBox);

        Scene scene = new Scene(mainContainer, 700, 750);
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }

    private String getMapHTML(double lat, double lon, String locationName) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "    <meta charset='UTF-8'>"
                + "    <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />"
                + "    <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "    <style>"
                + "        body { margin: 0; padding: 0; background: #1a2a3e; }"
                + "        #map { height: 100%; width: 100%; }"
                + "        .custom-marker {"
                + "            background: #e74c3c;"
                + "            border: 3px solid #D4AF37;"
                + "            border-radius: 50%;"
                + "            width: 20px;"
                + "            height: 20px;"
                + "            box-shadow: 0 0 20px rgba(212,175,55,0.5);"
                + "        }"
                + "    </style>"
                + "</head>"
                + "<body>"
                + "    <div id='map'></div>"
                + "    <script>"
                + "        var map = L.map('map').setView([" + lat + ", " + lon + "], 13);"
                + "        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {"
                + "            attribution: '&copy; OpenStreetMap contributors',"
                + "            maxZoom: 19"
                + "        }).addTo(map);"
                + "        var customIcon = L.divIcon({ html: '<div class=\"custom-marker\"></div>', iconSize: [20, 20] });"
                + "        L.marker([" + lat + ", " + lon + "], { icon: customIcon })"
                + "         .bindPopup('<b>" + locationName + "</b><br>Lat: " + lat + "<br>Lon: " + lon + "')"
                + "         .addTo(map);"
                + "        setTimeout(function() { map.invalidateSize(); }, 500);"
                + "    </script>"
                + "</body>"
                + "</html>";
    }

    /* Methode de l'audit */
    private void initAudit() {
        // 1. Remplir les filtres
        loadAuditFilters();

        // 2. Dates par défaut
        auditStartDate.setValue(LocalDate.now().minusDays(7));
        auditEndDate.setValue(LocalDate.now());

        // 3. Configurer les colonnes
        setupAuditColumns();

        // 4. Actions
        filterAudit_button.setOnAction(event -> refreshAudit());
        refreshAudit_button.setOnAction(event -> refreshAudit());
        exportAudit_button.setOnAction(event -> exportAudit());

        // 5. Charger les données
        refreshAudit();
    }

    private void loadAuditFilters() {
        try {
            // Charger les actions
            List<String> actions = Queries.getAuditActions();
            auditActionBoxFilter.getItems().add("Tous");
            auditActionBoxFilter.getItems().addAll(actions);
            auditActionBoxFilter.setValue("Tous");

            // Charger les utilisateurs
            List<String> users = Queries.getAuditUsers();
            auditUserBoxFilter.getItems().add("Tous");
            auditUserBoxFilter.getItems().addAll(users);
            auditUserBoxFilter.setValue("Tous");

        } catch (SQLException e) {
            NotificationHelper.showError("Erreur lors du chargement des filtres : " + e.getMessage());
        }
    }

    private void setupAuditColumns() {
        String[] columnNames = {"Horodatage", "Utilisateur", "Action", "Table", "Statut", "IP Source", "Détails"};
        int[] widths = {150, 120, 130, 120, 90, 120, 300};

        for (int i = 0; i < columnNames.length; i++) {
            final int colIndex = i;
            TableColumn<ObservableList<String>, String> column = new TableColumn<>(columnNames[i]);
            column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(colIndex)));
            column.setPrefWidth(widths[i]);

            // Colorer la colonne Statut
            if (i == 4) {
                column.setCellFactory(col -> new TableCell<ObservableList<String>, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }
                        setText(item);
                        if ("SUCCESS".equalsIgnoreCase(item)) {
                            setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                        }
                    }
                });
            }

            auditTableView.getColumns().add(column);
        }
    }

    private void refreshAudit() {
        refreshAudit_button.setDisable(true);
        refreshAudit_button.setText("Chargement...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String action = auditActionBoxFilter.getValue();
                String user = auditUserBoxFilter.getValue();
                LocalDate start = auditStartDate.getValue();
                LocalDate end = auditEndDate.getValue();

                if ("Tous".equals(action)) {
                    action = null;
                }
                if ("Tous".equals(user)) {
                    user = null;
                }

                ObservableList<Audit> logs = Queries.getAuditLogs(action, user, start, end);

                // Convertir en ObservableList<ObservableList<String>>
                ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
                int success = 0, failed = 0;

                for (Audit log : logs) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    row.add(log.getTimestamp());
                    row.add(log.getUser());
                    row.add(log.getAction());
                    row.add(log.getTable());
                    row.add(log.getStatus());
                    row.add(log.getIp());
                    row.add(log.getDetails());
                    data.add(row);

                    if ("SUCCESS".equals(log.getStatus())) {
                        success++;
                    } else {
                        failed++;
                    }
                }

                final int total = data.size();
                final int finalSuccess = success;
                final int finalFailed = failed;
                final ObservableList<ObservableList<String>> finalData = data;

                Platform.runLater(() -> {
                    auditData.clear();
                    auditData.addAll(finalData);
                    auditTableView.setItems(auditData);

                    totalAuditLabel.setText("Total : " + total);
                    successAuditLabel.setText("Succès : " + finalSuccess);
                    failedAuditLabel.setText("Échecs : " + finalFailed);
                });

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            refreshAudit_button.setDisable(false);
            refreshAudit_button.setText("RAFRAÎCHIR");
        });

        task.setOnFailed(e -> {
            refreshAudit_button.setDisable(false);
            refreshAudit_button.setText("RAFRAÎCHIR");
            NotificationHelper.showError("Erreur : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void exportAudit() {
        if (auditTableView.getItems().isEmpty()) {
            NotificationHelper.showWarning("Aucune donnée d'audit à exporter.");
            return;
        }

        List<String> headers = auditTableView.getColumns().stream()
                .map(col -> col.getText())
                .collect(Collectors.toList());

        ObservableList<ObservableList<String>> data = auditTableView.getItems();

        ExportCSV.exportSimple(headers, data,
                auditTableView.getScene().getWindow(),
                "audit");
    }

    /* Methode du DashBoard */
    private void initCharts() {
        try {
            if (cpuAreaChart == null || ramAreaChart == null || sessionsLineChart == null || queriesLineChart == null) {
                System.err.println("⚠️ Un ou plusieurs graphiques sont null. Vérifiez les fx:id dans le FXML.");
                return;
            }

            // ========== CONFIGURATION DES AXES ==========
            // --- CPU (pourcentage 0-100%) ---
            cpuAreaChart.setAnimated(false);
            cpuAreaChart.setCreateSymbols(false);
            cpuAreaChart.setLegendVisible(false);
            NumberAxis cpuYAxis = (NumberAxis) cpuAreaChart.getYAxis();
            if (cpuYAxis != null) {
                cpuYAxis.setAutoRanging(false);
                cpuYAxis.setLowerBound(0);
                cpuYAxis.setUpperBound(100);
                cpuYAxis.setTickUnit(20);
                cpuYAxis.setLabel("CPU (%)");
            }

            // --- RAM en GB (amplitude = capacité totale) ---
            ramAreaChart.setAnimated(false);
            ramAreaChart.setCreateSymbols(false);
            ramAreaChart.setLegendVisible(false);
            NumberAxis ramYAxis = (NumberAxis) ramAreaChart.getYAxis();
            if (ramYAxis != null) {
                // Récupérer la capacité totale de la RAM
                double totalRAM = getTotalRAMInGB();
                ramYAxis.setAutoRanging(false);
                ramYAxis.setLowerBound(0);
                ramYAxis.setUpperBound(totalRAM);
                ramYAxis.setTickUnit(totalRAM / 5); // 5 graduations (ex: 0, 1.6, 3.2, 4.8, 6.4, 8)
                ramYAxis.setLabel("RAM (GB)");
            }

            // --- Sessions (dynamique) ---
            sessionsLineChart.setAnimated(false);
            sessionsLineChart.setCreateSymbols(false);

            // --- Requêtes (dynamique) ---
            queriesLineChart.setAnimated(false);
            queriesLineChart.setCreateSymbols(false);

            // Création des séries
            cpuSeriesTab = new XYChart.Series<>();
            cpuSeriesTab.setName("CPU");
            cpuAreaChart.getData().add(cpuSeriesTab);

            ramSeriesTab = new XYChart.Series<>();
            ramSeriesTab.setName("RAM");
            ramAreaChart.getData().add(ramSeriesTab);

            sessionsSeriesTab = new XYChart.Series<>();
            sessionsSeriesTab.setName("Sessions");
            sessionsLineChart.getData().add(sessionsSeriesTab);

            queriesAllSeriesTab = new XYChart.Series<>();
            queriesAllSeriesTab.setName("Toutes");
            queriesSuccessSeriesTab = new XYChart.Series<>();
            queriesSuccessSeriesTab.setName("Succès");
            queriesFailedSeriesTab = new XYChart.Series<>();
            queriesFailedSeriesTab.setName("Échecs");
            queriesSlowSeriesTab = new XYChart.Series<>();
            queriesSlowSeriesTab.setName("Lentes (>1s)");

            queriesLineChart.getData().addAll(queriesAllSeriesTab, queriesSuccessSeriesTab, queriesFailedSeriesTab, queriesSlowSeriesTab);

            // Masquer les séries secondaires par défaut
            queriesSuccessSeriesTab.getNode().setVisible(false);
            queriesFailedSeriesTab.getNode().setVisible(false);
            queriesSlowSeriesTab.getNode().setVisible(false);

            // Configuration du ComboBox
            quelTypeDeDonneePourLesRequetesBox.getItems().addAll("Auto (toutes)", "Succès", "Échecs", "Lentes");
            quelTypeDeDonneePourLesRequetesBox.setValue("Auto (toutes)");
            quelTypeDeDonneePourLesRequetesBox.setOnAction(event -> filterQueries());

            startChartRefresh();

        } catch (Exception e) {
            System.err.println("Erreur dans initCharts : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double getTotalRAMInGB() {
        try {
            String ramInfo = linux.getSystemRAM();
            if (ramInfo != null && !"N/A".equals(ramInfo) && !ramInfo.isEmpty()) {
                // Nettoyer : supprimer "GB" et remplacer la virgule par un point
                String clean = ramInfo.replace("GB", "").replace("Go", "").trim().replace(',', '.');
                return Double.parseDouble(clean);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la RAM totale : " + e.getMessage());
        }
        return 8.0; // Valeur par défaut (8 GB)
    }

    private void startChartRefresh() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> updateCharts())
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateCharts() {
        try {
            if (cpuSeriesTab == null || ramSeriesTab == null || sessionsSeriesTab == null) {
                System.err.println("⚠️ Séries non initialisées, abandon de la mise à jour.");
                return;
            }

            // ===== RÉCUPÉRATION DES SEUILS DEPUIS LA BASE =====
            Map<String, String> params = Queries.getAllParametres();
            double seuilCPU = Double.parseDouble(params.getOrDefault("SEUIL_CPU", "80"));
            double seuilRAM = Double.parseDouble(params.getOrDefault("SEUIL_RAM", "85"));
            double seuilDISQUE = Double.parseDouble(params.getOrDefault("SEUIL_DISQUE", "90"));
            double seuilTABLESPACE = Double.parseDouble(params.getOrDefault("SEUIL_TABLESPACE", "85"));

            // ===== RÉCUPÉRATION DES MÉTRIQUES =====
            double cpu = 0.0;
            double ramUsedGB = 0.0;
            double sessions = 0.0;
            double allQueries = 0, successQueries = 0, failedQueries = 0, slowQueries = 0;
            double diskUsage = 0.0;
            double tablespaceUsage = 0.0;

            // CPU
            try {
                String cpuStr = linux.getSystemCPUChart();
                if (!"N/A".equals(cpuStr)) {
                    cpu = Double.parseDouble(cpuStr.replace("%", "").trim().replace(',', '.'));
                    if (cpu > 100) {
                        cpu = 100;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur CPU : " + e.getMessage());
            }

            // RAM
            try {
                String ramStr = linux.getSystemRAMUsed();
                if (!"N/A".equals(ramStr) && ramStr.contains("/")) {
                    String[] parts = ramStr.split("/");
                    if (parts.length == 2) {
                        String usedPart = parts[0].trim().replace(" GB", "").trim();
                        ramUsedGB = Double.parseDouble(usedPart.replace(',', '.'));
                        // Calcul du pourcentage RAM utilisé
                        double totalRAM = getTotalRAMInGB();
                        ramPercent = (ramUsedGB / totalRAM) * 100;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur RAM : " + e.getMessage());
            }

            // Disque
            try {
                String diskStr = linux.getSystemDiskUsage();
                if (!"N/A".equals(diskStr)) {
                    diskUsage = Double.parseDouble(diskStr.replace("%", "").trim().replace(',', '.'));
                }
            } catch (Exception e) {
                System.err.println("Erreur Disque : " + e.getMessage());
            }

            // Tablespace
            try {
                String tsStr = Queries.getTablespaceUsage();
                if (tsStr != null && !"N/A".equals(tsStr) && tsStr.contains("used")) {
                    String clean = tsStr.replace(" GB used", "").trim().replace(',', '.');
                    double usedGB = Double.parseDouble(clean);
                    // Récupérer la taille totale pour calculer le pourcentage
                    String sizeStr = Queries.getDBSize();
                    if (sizeStr != null && !"N/A".equals(sizeStr)) {
                        double totalGB = Double.parseDouble(sizeStr.replace(" GB", "").trim().replace(',', '.'));
                        tablespaceUsage = (usedGB / totalGB) * 100;
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur Tablespace : " + e.getMessage());
            }

            // Sessions
            try {
                String sessionsStr = Queries.getActiveSessions();
                if (!"N/A".equals(sessionsStr) && !sessionsStr.isEmpty()) {
                    sessions = Double.parseDouble(sessionsStr);
                }
            } catch (Exception e) {
                System.err.println("Erreur Sessions : " + e.getMessage());
            }

            // Requêtes
            try {
                allQueries = Queries.getQueryStat("all");
                successQueries = Queries.getQueryStat("success");
                failedQueries = Queries.getQueryStat("failed");
                slowQueries = Queries.getQueryStat("slow");
            } catch (SQLException e) {
                System.err.println("Erreur requêtes : " + e.getMessage());
            }

            // ===== VÉRIFICATION DES SEUILS ET CRÉATION D'ALERTES =====
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
            boolean alertTriggered = false;

            // 1. Vérification CPU
            if (cpu > seuilCPU) {
                String message = String.format("⚠️ CPU critique : %.1f%% (seuil : %.1f%%)", cpu, seuilCPU);
                insertAlert("CRITICAL", message, "CPU Monitor");
                alertTriggered = true;
            }

            // 2. Vérification RAM
            if (ramPercent > seuilRAM) {
                String message = String.format("⚠️ RAM critique : %.1f%% (seuil : %.1f%%)", ramPercent, seuilRAM);
                insertAlert("WARNING", message, "RAM Monitor");
                alertTriggered = true;
            }

            // 3. Vérification Disque
            if (diskUsage > seuilDISQUE) {
                String message = String.format("⚠️ Disque critique : %.1f%% (seuil : %.1f%%)", diskUsage, seuilDISQUE);
                insertAlert("WARNING", message, "Disk Monitor");
                alertTriggered = true;
            }

            // 4. Vérification Tablespace
            if (tablespaceUsage > seuilTABLESPACE) {
                String message = String.format("⚠️ Tablespace critique : %.1f%% (seuil : %.1f%%)", tablespaceUsage, seuilTABLESPACE);
                insertAlert("CRITICAL", message, "Tablespace Monitor");
                alertTriggered = true;
            }

            // Si une alerte a été déclenchée, rafraîchir la liste des alertes
            if (alertTriggered) {
                Platform.runLater(() -> {
                    try {
                        refreshAlertes();
                    } catch (Exception e) {
                        System.err.println("Erreur rafraîchissement alertes : " + e.getMessage());
                    }
                });
            }

            // ===== MISE À JOUR DES GRAPHIQUES =====
            String timeLabel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            cpuSeriesTab.getData().add(new XYChart.Data<>(timeLabel, Math.min(cpu, 100)));
            ramSeriesTab.getData().add(new XYChart.Data<>(timeLabel, ramUsedGB));
            sessionsSeriesTab.getData().add(new XYChart.Data<>(timeLabel, sessions));
            queriesAllSeriesTab.getData().add(new XYChart.Data<>(timeLabel, allQueries));
            queriesSuccessSeriesTab.getData().add(new XYChart.Data<>(timeLabel, successQueries));
            queriesFailedSeriesTab.getData().add(new XYChart.Data<>(timeLabel, failedQueries));
            queriesSlowSeriesTab.getData().add(new XYChart.Data<>(timeLabel, slowQueries));

            // Limiter le nombre de points
            int maxPoints = MAX_DATACHARTS_POINTS;
            if (cpuSeriesTab.getData().size() > maxPoints) {
                cpuSeriesTab.getData().remove(0);
                ramSeriesTab.getData().remove(0);
                sessionsSeriesTab.getData().remove(0);
                queriesAllSeriesTab.getData().remove(0);
                queriesSuccessSeriesTab.getData().remove(0);
                queriesFailedSeriesTab.getData().remove(0);
                queriesSlowSeriesTab.getData().remove(0);
            }

        } catch (Exception e) {
            System.err.println("Erreur mise à jour graphiques : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertAlert(String level, String message, String source) {
        try {
            // 1. Insérer dans la base de données
            Queries.insertAlert(level, message, source);
            System.out.println("Alerte " + level + " : " + message);

            // 2. Récupérer l'alerte pour notification
            ObservableList<Alertes> alerts = Queries.getAlertes();
            if (!alerts.isEmpty()) {
                Alertes lastAlert = alerts.get(0); // La plus récente
                Platform.runLater(() -> {
                    // 3. Notification système
                    AlertNotification.notifyAlert(lastAlert);
                });
            }

            // 4. Mettre à jour le compteur d'alertes
            Platform.runLater(() -> {
                long unreadCount = alertData.stream()
                        .filter(a -> "UNREAD".equals(a.getStatus()))
                        .count();
                nbreNotification.setText(String.valueOf(unreadCount));
            });

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'insertion de l'alerte : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void filterQueries() {
        String selected = quelTypeDeDonneePourLesRequetesBox.getValue();
        if (selected == null) {
            return;
        }

        // Masquer toutes les séries
        queriesAllSeriesTab.getNode().setVisible(false);
        queriesSuccessSeriesTab.getNode().setVisible(false);
        queriesFailedSeriesTab.getNode().setVisible(false);
        queriesSlowSeriesTab.getNode().setVisible(false);

        // Afficher la série sélectionnée
        switch (selected) {
            case "Auto (toutes)":
                queriesAllSeriesTab.getNode().setVisible(true);
                break;
            case "Succès":
                queriesSuccessSeriesTab.getNode().setVisible(true);
                break;
            case "Échecs":
                queriesFailedSeriesTab.getNode().setVisible(true);
                break;
            case "Lentes":
                queriesSlowSeriesTab.getNode().setVisible(true);
                break;
            default:
                queriesAllSeriesTab.getNode().setVisible(true);
                break;
        }

        // Activer la légende
        queriesLineChart.setLegendVisible(true);
    }

    /* Methode de Alertes */
    private void initAlertes() {
        // 1. Configuration de la ListView
        alertListView.setCellFactory(listView -> new ListCell<Alertes>() {
            @Override
            protected void updateItem(Alertes alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(alert.toString());
                switch (alert.getLevel()) {
                    case "CRITICAL":
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-background-color: #1A1A2E; -fx-padding: 5;");
                        break;
                    case "WARNING":
                        setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold; -fx-background-color: #1A1A2E; -fx-padding: 5;");
                        break;
                    default:
                        setStyle("-fx-text-fill: #3498DB; -fx-background-color: #1A1A2E; -fx-padding: 5;");
                        break;
                }
            }
        });

        // 2. Actions des boutons
        refreshAlert_button.setOnAction(event -> refreshAlertes());
        exportAlert_button.setOnAction(event -> exportAlertes());
        markAllRead_button.setOnAction(event -> markAllRead());

        // 3. Charger les alertes
        refreshAlertes();
    }

    private void refreshAlertes() {
        refreshAlert_button.setDisable(true);
        refreshAlert_button.setText("Chargement...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                loadAlertesFromDatabase();
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            refreshAlert_button.setDisable(false);
            refreshAlert_button.setText("RAFRAÎCHIR");
        });

        task.setOnFailed(e -> {
            refreshAlert_button.setDisable(false);
            refreshAlert_button.setText("RAFRAÎCHIR");
            Throwable ex = task.getException();
            NotificationHelper.showError("Erreur : " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void loadAlertesFromDatabase() throws SQLException {
        ObservableList<Alertes> alerts = Queries.getAlertes();

        Platform.runLater(() -> {
            alertData.clear();
            alertData.addAll(alerts);
            alertListView.setItems(alertData);
            updateAlertStats();
            updateNotificationBadge(); // ✅ Mettre à jour le badge

            // Notifications pour les alertes non lues
            for (Alertes a : alerts) {
                String key = a.getTimestamp() + a.getMessage();
                if (!notifiedAlertIds.contains(key) && "UNREAD".equals(a.getStatus())) {
                    AlertNotification.notifyAlert(a);
                    notifiedAlertIds.add(key);
                }
            }
        });
    }

    private void updateAlertStats() {
        long critical = alertData.stream().filter(a -> "CRITICAL".equals(a.getLevel())).count();
        long warning = alertData.stream().filter(a -> "WARNING".equals(a.getLevel())).count();
        long info = alertData.stream().filter(a -> "INFO".equals(a.getLevel())).count();

        criticalAlertLabel.setText("🔴 Critiques : " + critical);
        warningAlertLabel.setText("🟡 Avertissements : " + warning);
        infoAlertLabel.setText("🔵 Info : " + info);
    }

    private void exportAlertes() {
        if (alertListView.getItems().isEmpty()) {
            NotificationHelper.showWarning("Aucune alerte à exporter.");
            return;
        }

        List<String> headers = Arrays.asList("Horodatage", "Niveau", "Message", "Source", "Statut");

        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
        for (Alertes alert : alertListView.getItems()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            row.add(alert.getTimestamp());
            row.add(alert.getLevel());
            row.add(alert.getMessage());
            row.add(alert.getSource());
            row.add(alert.getStatus());
            data.add(row);
        }

        ExportCSV.exportSimple(headers, data,
                alertListView.getScene().getWindow(),
                "alertes");
    }

    private void markAllRead() {
        try {
            Queries.marquerAlertesLues();

            for (Alertes alert : alertData) {
                alert.setStatus("READ");
            }
            alertListView.refresh();
            updateAlertStats();
            updateNotificationBadge(); // ✅ Mettre à jour le badge
            refreshNotificationBell(); // ✅ Rafraîchir la popup si ouverte

            NotificationHelper.showSuccess("Toutes les alertes ont été marquées comme lues.");

        } catch (SQLException e) {
            NotificationHelper.showError("Erreur : " + e.getMessage());
        }
    }

    // SECTION PARAMÈTRES
    private void initParametres() {
        // ===== INITIALISATION DES COMBOBOX =====
        themeComboBox.getItems().addAll("Clair", "Sombre", "Système");
        themeComboBox.setValue("Clair");

        languageComboBox.getItems().addAll("Français", "English");
        languageComboBox.setValue("Français");

        // Jours de la semaine
        emailDayComboBox.getItems().addAll("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche");
        emailDayComboBox.setValue("Vendredi");

        // Heures
        for (int h = 0; h < 24; h++) {
            emailHourComboBox.getItems().add(String.format("%02d:00", h));
        }
        emailHourComboBox.setValue("18:00");

        loadPreferences();

        saveSettings_button.setOnAction(event -> saveSettings());
        resetSettings_button.setOnAction(event -> resetSettings());
        sendEmailNowButton.setOnAction(event -> sendManualEmail());

        themeComboBox.setOnAction(event -> {
            String selected = themeComboBox.getValue();
            if (selected != null) {
                applyTheme(selected);
            }
        });

        languageComboBox.setOnAction(event -> {
            String selected = languageComboBox.getValue();
            if (selected != null) {
                changeLanguage(selected);
            }
        });

        emailNotificationCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean visible = newVal != null && newVal;
            add_emailRecepteurs.setVisible(visible);
            add_emailRecepteurs.setManaged(visible);
            emailDayComboBox.setVisible(visible);
            emailDayComboBox.setManaged(visible);
            emailHourComboBox.setVisible(visible);
            emailHourComboBox.setManaged(visible);
        });

        loadSettings();
    }

    private void loadPreferences() {
        try {
            // Thème
            String theme = Queries.getParametre("THEME");
            if (theme != null && !theme.isEmpty()) {
                currentTheme = theme;
                switch (theme) {
                    case "light":
                        themeComboBox.setValue("Clair");
                        break;
                    case "dark":
                        themeComboBox.setValue("Sombre");
                        break;
                    case "system":
                        themeComboBox.setValue("Système");
                        break;
                    default:
                        themeComboBox.setValue("Clair");
                }
                applyTheme(theme);
            }

            // Langue
            String lang = Queries.getParametre("LANGUE");
            if (lang != null && !lang.isEmpty()) {
                currentLanguage = lang;
                languageComboBox.setValue(lang.equals("en") ? "English" : "Français");
                changeLanguage(currentLanguage);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement préférences : " + e.getMessage());
            statusParamLabel.setText("Erreur chargement préférences : " + e.getMessage());
            statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
        }
    }

    private void loadSettings() {
        try {
            Map<String, String> params = Queries.getAllParametres();

            // Collecte
            intervalSettingField.setText(params.getOrDefault("INTERVALLE", "30"));
            chartsIntervalField.setText(params.getOrDefault("INTERVALLE_CHARTS", "5"));

            // Seuils
            cpuThresholdSettingField.setText(params.getOrDefault("SEUIL_CPU", "80"));
            ramThresholdSettingField.setText(params.getOrDefault("SEUIL_RAM", "85"));
            diskThresholdSettingField.setText(params.getOrDefault("SEUIL_DISQUE", "90"));
            tablespaceThresholdSettingField.setText(params.getOrDefault("SEUIL_TABLESPACE", "85"));

            // Notifications
            soundAlertCheckBox.setSelected("true".equals(params.getOrDefault("SON_ACTIF", "true")));
            systemNotificationCheckBox.setSelected("true".equals(params.getOrDefault("NOTIF_SYSTEME", "true")));

            // Email
            boolean emailActif = "true".equals(params.getOrDefault("EMAIL_ACTIF", "false"));
            emailNotificationCheckBox.setSelected(emailActif);
            add_emailRecepteurs.setText(params.getOrDefault("EMAIL", ""));
            add_emailRecepteurs.setVisible(emailActif);
            add_emailRecepteurs.setManaged(emailActif);
            emailDayComboBox.setVisible(emailActif);
            emailDayComboBox.setManaged(emailActif);
            emailHourComboBox.setVisible(emailActif);
            emailHourComboBox.setManaged(emailActif);

            // Programmation email
            String schedule = params.getOrDefault("EMAIL_SCHEDULE", "FRIDAY_18:00");
            if (schedule != null && schedule.contains("_")) {
                String[] parts = schedule.split("_");
                if (parts.length == 2) {
                    String day = parts[0];
                    String[] dayMapping = {"LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI", "DIMANCHE"};
                    for (int i = 0; i < dayMapping.length; i++) {
                        if (dayMapping[i].equalsIgnoreCase(day)) {
                            emailDayComboBox.setValue(dayMapping[i].charAt(0) + dayMapping[i].substring(1).toLowerCase());
                            break;
                        }
                    }
                    emailHourComboBox.setValue(parts[1]);
                }
            }

            // Sécurité
            sessionTimeoutField.setText(params.getOrDefault("SESSION_TIMEOUT", "30"));

            // Appliquer les paramètres immédiatement
            appliquerParametres();

            statusParamLabel.setText("Paramètres chargés");
            statusParamLabel.setStyle("-fx-text-fill: #27AE60;");

        } catch (SQLException e) {
            statusParamLabel.setText("Erreur : " + e.getMessage());
            statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        try {
            // Validation
            int interval = Integer.parseInt(intervalSettingField.getText().trim());
            int charts = Integer.parseInt(chartsIntervalField.getText().trim());
            int cpu = Integer.parseInt(cpuThresholdSettingField.getText().trim());
            int ram = Integer.parseInt(ramThresholdSettingField.getText().trim());
            int disk = Integer.parseInt(diskThresholdSettingField.getText().trim());
            int tablespace = Integer.parseInt(tablespaceThresholdSettingField.getText().trim());
            int timeout = Integer.parseInt(sessionTimeoutField.getText().trim());

            // Vérifications des plages
            if (interval < 5 || interval > 300) {
                statusParamLabel.setText("Intervalle entre 5 et 300 secondes");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }
            if (charts < 1 || charts > 60) {
                statusParamLabel.setText("Charts entre 1 et 60 secondes");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }
            if (cpu < 10 || cpu > 100) {
                statusParamLabel.setText("Seuil CPU entre 10 et 100 %");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }
            if (ram < 10 || ram > 100) {
                statusParamLabel.setText("Seuil RAM entre 10 et 100 %");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }
            if (disk < 10 || disk > 100) {
                statusParamLabel.setText("Seuil Disque entre 10 et 100 %");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }
            if (tablespace < 10 || tablespace > 100) {
                statusParamLabel.setText(" Seuil Tablespace entre 10 et 100 %");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }
            if (timeout < 5 || timeout > 120) {
                statusParamLabel.setText("Timeout entre 5 et 120 minutes");
                statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                return;
            }

            // Sauvegarde en base
            Queries.setParametre("INTERVALLE", String.valueOf(interval));
            Queries.setParametre("INTERVALLE_CHARTS", String.valueOf(charts));
            Queries.setParametre("SEUIL_CPU", String.valueOf(cpu));
            Queries.setParametre("SEUIL_RAM", String.valueOf(ram));
            Queries.setParametre("SEUIL_DISQUE", String.valueOf(disk));
            Queries.setParametre("SEUIL_TABLESPACE", String.valueOf(tablespace));
            Queries.setParametre("SON_ACTIF", String.valueOf(soundAlertCheckBox.isSelected()));
            Queries.setParametre("NOTIF_SYSTEME", String.valueOf(systemNotificationCheckBox.isSelected()));
            Queries.setParametre("EMAIL_ACTIF", String.valueOf(emailNotificationCheckBox.isSelected()));
            Queries.setParametre("EMAIL", add_emailRecepteurs.getText());
            Queries.setParametre("SESSION_TIMEOUT", String.valueOf(timeout));

            // Programmation email
            String day = emailDayComboBox.getValue();
            String hour = emailHourComboBox.getValue();
            if (day != null && hour != null) {
                String dayCode = day.toUpperCase();
                String[] frDays = {"LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI", "DIMANCHE"};
                String[] enDays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
                for (int i = 0; i < frDays.length; i++) {
                    if (frDays[i].equals(dayCode)) {
                        dayCode = enDays[i];
                        break;
                    }
                }
                Queries.setParametre("EMAIL_SCHEDULE", dayCode + "_" + hour);
            }

            // Application des changements
            appliquerParametres();
            reloadThresholds(); // ✅ Recharger les seuils pour updateCharts()

            // Log
            StringBuilder changes = new StringBuilder();
            changes.append("INTERVALLE=").append(interval)
                    .append(", CHARTS=").append(charts)
                    .append(", CPU=").append(cpu)
                    .append(", RAM=").append(ram)
                    .append(", DISQUE=").append(disk)
                    .append(", TABLESPACE=").append(tablespace)
                    .append(", SON=").append(soundAlertCheckBox.isSelected())
                    .append(", NOTIF=").append(systemNotificationCheckBox.isSelected())
                    .append(", EMAIL=").append(emailNotificationCheckBox.isSelected())
                    .append(", TIMEOUT=").append(timeout);

            Queries.insertLog("INFO", "Paramètres", currentUser, "Sauvegarde : " + changes.toString());

            statusParamLabel.setText("Paramètres enregistrés !");
            statusParamLabel.setStyle("-fx-text-fill: #27AE60;");
            NotificationHelper.showSuccess("Paramètres sauvegardés avec succès !");

        } catch (NumberFormatException e) {
            statusParamLabel.setText("Veuillez saisir des nombres valides");
            statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
        } catch (SQLException e) {
            statusParamLabel.setText("Erreur : " + e.getMessage());
            statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
            e.printStackTrace();
        }
    }

    private void appliquerParametres() {
        try {
            Map<String, String> params = Queries.getAllParametres();

            int interval = Integer.parseInt(params.getOrDefault("INTERVALLE", "30"));
            boolean sonActif = "true".equals(params.getOrDefault("SON_ACTIF", "true"));
            boolean notifSysteme = "true".equals(params.getOrDefault("NOTIF_SYSTEME", "true"));
            boolean emailActif = "true".equals(params.getOrDefault("EMAIL_ACTIF", "false"));
            String emails = params.getOrDefault("EMAIL", "");
            int timeout = Integer.parseInt(params.getOrDefault("SESSION_TIMEOUT", "30"));

            // Application aux services
            AlertNotification.setSoundEnabled(sonActif);
            AlertNotification.setSystemNotificationEnabled(notifSysteme);
            AlertNotification.setEmailEnabled(emailActif);
            AlertNotification.setEmailRecipients(emails);

            System.out.println("Paramètres appliqués : intervalle=" + interval + "s, son=" + sonActif
                    + ", notif=" + notifSysteme + ", email=" + emailActif + ", timeout=" + timeout + "min");

        } catch (SQLException e) {
            System.err.println("Erreur application paramètres : " + e.getMessage());
        }
    }

    private void reloadThresholds() {
        try {
            Map<String, String> params = Queries.getAllParametres();

            double seuilCPU = Double.parseDouble(params.getOrDefault("SEUIL_CPU", "80"));
            double seuilRAM = Double.parseDouble(params.getOrDefault("SEUIL_RAM", "85"));
            double seuilDISQUE = Double.parseDouble(params.getOrDefault("SEUIL_DISQUE", "90"));
            double seuilTABLESPACE = Double.parseDouble(params.getOrDefault("SEUIL_TABLESPACE", "85"));

            System.out.println("Seuils rechargés : CPU=" + seuilCPU + "%, RAM=" + seuilRAM
                    + "%, Disque=" + seuilDISQUE + "%, Tablespace=" + seuilTABLESPACE + "%");

        } catch (SQLException e) {
            System.err.println("Erreur lors du rechargement des seuils : " + e.getMessage());
        }
    }

    private void resetSettings() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Réinitialisation");
        confirmation.setHeaderText("Réinitialiser les paramètres ?");
        confirmation.setContentText("Tous les paramètres seront rétablis aux valeurs par défaut.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Queries.resetAllParametres();
                loadSettings();
                appliquerParametres();
                reloadThresholds(); // Recharger les seuils

                Queries.insertLog("INFO", "Paramètres", currentUser, "Réinitialisation des paramètres");

                statusParamLabel.setText("Paramètres réinitialisés !");
                statusParamLabel.setStyle("-fx-text-fill: #27AE60;");
                NotificationHelper.showSuccess("Paramètres réinitialisés avec succès !");

            } catch (SQLException e) {
                statusParamLabel.setText("Erreur : " + e.getMessage());
                statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
                e.printStackTrace();
            }
        }
    }

    private void applyTheme(String theme) {
        if (theme == null) {
            return;
        }

        String themeValue;
        switch (theme) {
            case "Sombre":
                themeValue = "dark";
                break;
            case "Système":
                themeValue = "system";
                break;
            default:
                themeValue = "light";
        }

        currentTheme = themeValue;
        Scene scene = btn_dashboard.getScene();
        if (scene == null) {
            System.err.println("Scene non disponible");
            return;
        }

        try {
            scene.getStylesheets().removeIf(css -> css.contains("theme-"));

            // Chemin CORRECT pour les fichiers CSS
            String themeCss = getClass().getResource("/com/mycompany/monitoringdb/theme-" + themeValue + ".css").toExternalForm();
            scene.getStylesheets().add(themeCss);
            Queries.setParametre("THEME", themeValue);
            System.out.println("Thème appliqué : " + themeValue);

        } catch (Exception e) {
            System.err.println("Erreur application du thème : " + e.getMessage());
            statusParamLabel.setText("Erreur thème : " + e.getMessage());
            statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
        }
    }

    private void changeLanguage(String lang) {
        if (lang == null) {
            return;
        }

        String langValue = lang.equals("English") ? "en" : "fr";
        currentLanguage = langValue;

        try {
            Locale locale = new Locale(langValue);
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "com.mycompany.monitoringdb.i18n.messages",
                    locale
            );

            // Mettre à jour les textes
            updateAllTexts(bundle);

            // Sauvegarder en base
            Queries.setParametre("LANGUE", langValue);
            System.out.println("Langue changée : " + langValue);

        } catch (MissingResourceException e) {
            System.err.println("Bundle introuvable pour : " + langValue);
            statusParamLabel.setText("Langue indisponible");
            statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
        } catch (Exception e) {
            System.err.println("Erreur changement de langue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateAllTexts(ResourceBundle bundle) {
        try {
            // Mettre à jour la page Paramètres
            updateSettingsPageTexts(bundle);

            // Mettre à jour la page Dashboard
            updateDashboardPageTexts(bundle);

            // Mettre à jour la page Sessions
            updateSessionsPageTexts(bundle);

            // Mettre à jour la page Alertes
            updateAlertesPageTexts(bundle);

            // Mettre à jour la page Logs
            updateLogsPageTexts(bundle);

            // Mettre à jour la page Audit
            updateAuditPageTexts(bundle);

            // Mettre à jour la page Utilisateurs
            updateUsersPageTexts(bundle);

            // Mettre à jour la page Rapports
            updateReportsPageTexts(bundle);

            // Mettre à jour la page Console SQL
            updateConsolePageTexts(bundle);

            // Mettre à jour les boutons communs
            updateCommonButtonsTexts(bundle);

            System.out.println("✅ Tous les textes ont été mis à jour");

        } catch (Exception e) {
            System.err.println("⚠️ Erreur mise à jour des textes : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateDashboardPageTexts(ResourceBundle bundle) {
        try {
            // Titre
            Label titleLabel = (Label) dashboard_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("dashboard.title"));
            }

            // Labels des KPI
            totalSessionsLabel.setText(bundle.getString("dashboard.total") + " : ");
            activeSessionsLabel.setText(bundle.getString("dashboard.active") + " : ");
            inactiveSessionsLabel.setText(bundle.getString("dashboard.inactive") + " : ");

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Dashboard : " + e.getMessage());
        }
    }

    private void updateSessionsPageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) session_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("sessions.title"));
            }

            totalSessionsLabel.setText(bundle.getString("sessions.total") + " : ");
            activeSessionsLabel.setText(bundle.getString("sessions.active") + " : ");
            inactiveSessionsLabel.setText(bundle.getString("sessions.inactive") + " : ");

            refreshSessions_button.setText(bundle.getString("sessions.refresh"));
            exportSessions_button.setText(bundle.getString("sessions.export"));

            // Colonnes du tableau
            for (TableColumn<?, ?> col : sessionsTableView.getColumns()) {
                String text = col.getText();
                switch (text) {
                    case "SID":
                        col.setText(bundle.getString("sessions.columns.sid"));
                        break;
                    case "SERIAL#":
                        col.setText(bundle.getString("sessions.columns.serial"));
                        break;
                    case "USERNAME":
                        col.setText(bundle.getString("sessions.columns.username"));
                        break;
                    case "STATUS":
                        col.setText(bundle.getString("sessions.columns.status"));
                        break;
                    case "MACHINE":
                        col.setText(bundle.getString("sessions.columns.machine"));
                        break;
                    case "PROGRAM":
                        col.setText(bundle.getString("sessions.columns.program"));
                        break;
                    case "LOGON TIME":
                        col.setText(bundle.getString("sessions.columns.logon"));
                        break;
                    case "SQL_ID":
                        col.setText(bundle.getString("sessions.columns.sql"));
                        break;
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Sessions : " + e.getMessage());
        }
    }

    private void updateAlertesPageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) alert_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("alerts.title"));
            }

            criticalAlertLabel.setText(bundle.getString("alerts.critical"));
            warningAlertLabel.setText(bundle.getString("alerts.warning"));
            infoAlertLabel.setText(bundle.getString("alerts.info"));

            refreshAlert_button.setText(bundle.getString("alerts.refresh"));
            exportAlert_button.setText(bundle.getString("alerts.export"));
            markAllRead_button.setText(bundle.getString("alerts.markread"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Alertes : " + e.getMessage());
        }
    }

    private void updateLogsPageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) log_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("logs.title"));
            }

            totalLogsLabel.setText(bundle.getString("logs.total") + " : ");
            errorLogsLabel.setText(bundle.getString("logs.errors") + " : ");
            warningLogsLabel.setText(bundle.getString("logs.warnings") + " : ");
            infoLogsLabel.setText(bundle.getString("logs.info") + " : ");

            filterLogButton.setText(bundle.getString("logs.filter"));
            refreshLogButton.setText(bundle.getString("logs.refresh"));
            exportLogButton.setText(bundle.getString("logs.export"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Logs : " + e.getMessage());
        }
    }

    private void updateAuditPageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) audit_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("audit.title"));
            }

            totalAuditLabel.setText(bundle.getString("audit.total") + " : ");
            successAuditLabel.setText(bundle.getString("audit.success") + " : ");
            failedAuditLabel.setText(bundle.getString("audit.failed") + " : ");

            filterAudit_button.setText(bundle.getString("audit.filter"));
            refreshAudit_button.setText(bundle.getString("audit.refresh"));
            exportAudit_button.setText(bundle.getString("audit.export"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Audit : " + e.getMessage());
        }
    }

    private void updateUsersPageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) utilisateur_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("users.title"));
            }

            totalUsersLabel.setText(bundle.getString("users.total") + " : ");
            totalUsersStandardLabel.setText(bundle.getString("users.standard") + " : ");
            totalUsersSuperLabel.setText(bundle.getString("users.super") + " : ");

            addUserOracleButton.setText(bundle.getString("users.add"));
            editUserOracleButton.setText(bundle.getString("users.edit"));
            deleteUserOracleButton.setText(bundle.getString("users.delete"));
            refreshUsersOracleButton.setText(bundle.getString("users.refresh"));
            exportUsersOracleButton.setText(bundle.getString("users.export"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Utilisateurs : " + e.getMessage());
        }
    }

    private void updateReportsPageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) rapport_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("reports.title"));
            }

            generateReportButton.setText(bundle.getString("reports.generate"));
            downloadReportButton.setText(bundle.getString("reports.download"));
            deleteReportButton.setText(bundle.getString("reports.delete"));

            reportTypeComboBox.setPromptText(bundle.getString("reports.type"));
            reportFormatComboBox.setPromptText(bundle.getString("reports.format"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Rapports : " + e.getMessage());
        }
    }

    private void updateConsolePageTexts(ResourceBundle bundle) {
        try {
            Label titleLabel = (Label) console_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("console.title"));
            }

            startQuery_button.setText(bundle.getString("console.execute"));
            cleanQuery_button.setText(bundle.getString("console.clear"));
            exportQuery_button.setText(bundle.getString("console.export"));

            sqlZoneTextArea.setPromptText(bundle.getString("console.placeholder"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour Console : " + e.getMessage());
        }
    }

    private void updateSettingsPageTexts(ResourceBundle bundle) {
        try {
            // Titre
            Label titleLabel = (Label) parametre_page.lookup(".titreCentral");
            if (titleLabel != null) {
                titleLabel.setText(bundle.getString("settings.title"));
            }

            // Sections (TitledPane)
            String[] sectionKeys = {
                "settings.personalization",
                "settings.collection",
                "settings.thresholds",
                "settings.notifications",
                "settings.email",
                "settings.security"
            };
            List<Node> titledPanes = parametre_page.lookupAll(".sectionTitledPane").stream()
                    .filter(n -> n instanceof TitledPane)
                    .collect(Collectors.toList());
            for (int i = 0; i < Math.min(titledPanes.size(), sectionKeys.length); i++) {
                if (titledPanes.get(i) instanceof TitledPane) {
                    try {
                        ((TitledPane) titledPanes.get(i)).setText(bundle.getString(sectionKeys[i]));
                    } catch (MissingResourceException e) {
                        // Clé non trouvée, ignorer
                    }
                }
            }

            // Labels des champs
            String[] labelKeys = {
                "settings.theme", "settings.language", "settings.interval",
                "settings.charts", "settings.cpu", "settings.ram",
                "settings.disk", "settings.tablespace", "settings.sound",
                "settings.notification", "settings.email", "settings.email.recipients",
                "settings.session.timeout"
            };
            List<Node> labels = parametre_page.lookupAll(".labelColModern").stream()
                    .filter(n -> n instanceof Label)
                    .collect(Collectors.toList());
            for (int i = 0; i < Math.min(labels.size(), labelKeys.length); i++) {
                if (labels.get(i) instanceof Label) {
                    try {
                        ((Label) labels.get(i)).setText(bundle.getString(labelKeys[i]));
                    } catch (MissingResourceException e) {
                        // Clé non trouvée, ignorer
                    }
                }
            }

            // Boutons
            saveSettings_button.setText(bundle.getString("settings.save"));
            resetSettings_button.setText(bundle.getString("settings.reset"));

            // Status
            statusParamLabel.setText(bundle.getString("settings.loaded"));

            // CheckBoxes
            soundAlertCheckBox.setText(bundle.getString("settings.active"));
            systemNotificationCheckBox.setText(bundle.getString("settings.active"));
            emailNotificationCheckBox.setText(bundle.getString("settings.active"));

            // Prompt
            add_emailRecepteurs.setPromptText(bundle.getString("settings.email.prompt"));

            // ComboBox (les items sont chargés en Java, pas besoin de les traduire ici)
        } catch (Exception e) {
            System.err.println("⚠️ Erreur mise à jour Paramètres : " + e.getMessage());
        }
    }

    private void updateCommonButtonsTexts(ResourceBundle bundle) {
        try {
            // Bouton de déconnexion
            btnDeconnectInterface.setText(bundle.getString("common.close"));

            // Boutons de navigation (si textes en dur)
            btn_dashboard.setText(bundle.getString("dashboard.title"));
            btn_console.setText(bundle.getString("console.title"));
            btn_session.setText(bundle.getString("sessions.title"));
            btn_alert.setText(bundle.getString("alerts.title"));
            btn_logs.setText(bundle.getString("logs.title"));
            btn_audit.setText(bundle.getString("audit.title"));
            btn_utilisateur.setText(bundle.getString("users.title"));
            btn_rapport.setText(bundle.getString("reports.title"));
            btn_parametre.setText(bundle.getString("settings.title"));

        } catch (Exception e) {
            System.err.println("Erreur mise à jour des boutons communs : " + e.getMessage());
        }
    }

    /* Methode de Utilisateur */
    private void initUsers() {
        setupUsersColumns();

        // Actions des boutons
        addUserOracleButton.setOnAction(event -> addUserOracle());
        editUserOracleButton.setOnAction(event -> editUserOracle());
        deleteUserOracleButton.setOnAction(event -> deleteUserOracle());
        refreshUsersOracleButton.setOnAction(event -> refreshUsers());
        exportUsersOracleButton.setOnAction(event -> exportUsers());

        refreshUsers();
    }

    private void addUserOracle() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Création d'un utilisateur Oracle");
        dialog.setHeaderText("Configurer un utilisateur (local ou commun)");
        // ===== CHARGER LE CSS =====
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/mycompany/monitoringdb/dialog-style.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("dialog-custom");

        ButtonType btnOk = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);

        ScrollPane scrollPane = new ScrollPane();
        //scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox mainVBox = new VBox(15);
        mainVBox.setPadding(new Insets(15, 20, 20, 20));
        mainVBox.setStyle("-fx-background-color: #f9fafc;");

        // --- 1. Identité ---
        TitledPane identityPane = new TitledPane("Identité", null);
        GridPane identityGrid = new GridPane();
        identityGrid.setHgap(15);
        identityGrid.setVgap(10);
        identityGrid.setPadding(new Insets(10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        usernameField.setPrefWidth(200);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setPrefWidth(200);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirmer");
        confirmField.setPrefWidth(200);

        CheckBox chkCommon = new CheckBox("Commun (C##)");
        chkCommon.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");

        chkCommon.setOnAction(e -> {
            String name = usernameField.getText().trim();
            if (chkCommon.isSelected()) {
                if (!name.toUpperCase().startsWith("C##")) {
                    usernameField.setText("C##" + name);
                }
            } else {
                if (name.toUpperCase().startsWith("C##")) {
                    usernameField.setText(name.substring(3));
                }
            }
        });

        identityGrid.add(new Label("Nom :"), 0, 0);
        identityGrid.add(usernameField, 1, 0);
        identityGrid.add(new Label("Mot de passe :"), 2, 0);
        identityGrid.add(passwordField, 3, 0);
        identityGrid.add(new Label("Confirmation :"), 4, 0);
        identityGrid.add(confirmField, 5, 0);
        identityGrid.add(chkCommon, 6, 0);

        Label commonInfo = new Label("(Les common users sont partagés entre toutes les PDB)");
        commonInfo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-font-style: italic;");
        identityGrid.add(commonInfo, 0, 1, 7, 1);

        identityPane.setContent(identityGrid);
        mainVBox.getChildren().add(identityPane);

        // --- 2. Stockage ---
        TitledPane storagePane = new TitledPane("Stockage", null);
        GridPane storageGrid = new GridPane();
        storageGrid.setHgap(15);
        storageGrid.setVgap(10);
        storageGrid.setPadding(new Insets(10));

        TextField tablespaceField = new TextField("USERS");
        tablespaceField.setPromptText("Tablespace par défaut");
        tablespaceField.setPrefWidth(200);

        TextField quotaField = new TextField("UNLIMITED");
        quotaField.setPromptText("Quota (ex: 100M, 1G)");
        quotaField.setPrefWidth(200);

        storageGrid.add(new Label("Tablespace :"), 0, 0);
        storageGrid.add(tablespaceField, 1, 0);
        storageGrid.add(new Label("Quota :"), 2, 0);
        storageGrid.add(quotaField, 3, 0);

        storagePane.setContent(storageGrid);
        mainVBox.getChildren().add(storagePane);

        // --- 3. Profil et statut ---
        TitledPane profilePane = new TitledPane("Profil et statut", null);
        GridPane profileGrid = new GridPane();
        profileGrid.setHgap(15);
        profileGrid.setVgap(10);
        profileGrid.setPadding(new Insets(10));

        ComboBox<String> profileCombo = new ComboBox<>();
        profileCombo.getItems().addAll("DEFAULT", "MONITORING_PROFILE");
        profileCombo.setValue("DEFAULT");
        profileCombo.setPrefWidth(180);

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("OPEN", "LOCKED", "EXPIRED");
        statusCombo.setValue("OPEN");
        statusCombo.setPrefWidth(180);

        profileGrid.add(new Label("Profil :"), 0, 0);
        profileGrid.add(profileCombo, 1, 0);
        profileGrid.add(new Label("Statut :"), 2, 0);
        profileGrid.add(statusCombo, 3, 0);

        profilePane.setContent(profileGrid);
        mainVBox.getChildren().add(profilePane);

        // --- 4. Privilèges et rôles ---
        TitledPane privPane = new TitledPane("Privilèges et rôles", null);
        GridPane privGrid = new GridPane();
        privGrid.setHgap(40);
        privGrid.setVgap(8);
        privGrid.setPadding(new Insets(10));

        VBox leftPrivBox = new VBox(6);
        leftPrivBox.setPadding(new Insets(5));
        leftPrivBox.getChildren().add(new Label("Privilèges système :"));

        CheckBox chkConnect = new CheckBox("CONNECT");
        chkConnect.setSelected(true);
        CheckBox chkResource = new CheckBox("RESOURCE");
        CheckBox chkDba = new CheckBox("DBA");
        CheckBox chkSelectAny = new CheckBox("SELECT ANY TABLE");
        CheckBox chkCreateSession = new CheckBox("CREATE SESSION");
        chkCreateSession.setSelected(true);
        CheckBox chkUnlimited = new CheckBox("UNLIMITED TABLESPACE");

        leftPrivBox.getChildren().addAll(
                chkConnect, chkResource, chkDba, chkSelectAny,
                chkCreateSession, chkUnlimited
        );

        VBox rightPrivBox = new VBox(6);
        rightPrivBox.setPadding(new Insets(5));
        rightPrivBox.getChildren().add(new Label("Rôles :"));

        CheckBox chkRoleConnect = new CheckBox("Rôle CONNECT");
        CheckBox chkRoleResource = new CheckBox("Rôle RESOURCE");
        CheckBox chkRoleDba = new CheckBox("Rôle DBA");

        rightPrivBox.getChildren().addAll(
                chkRoleConnect, chkRoleResource, chkRoleDba
        );

        privGrid.add(leftPrivBox, 0, 0);
        privGrid.add(rightPrivBox, 1, 0);

        privPane.setContent(privGrid);
        mainVBox.getChildren().add(privPane);

        // --- 5. Aperçu SQL ---
        TitledPane previewPane = new TitledPane("Aperçu de la commande", null);
        TextArea sqlPreview = new TextArea();
        sqlPreview.setEditable(false);
        sqlPreview.setPrefRowCount(4);
        sqlPreview.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: #ecf0f1;");
        previewPane.setContent(sqlPreview);
        mainVBox.getChildren().add(previewPane);

        // --- Mise à jour dynamique de l'aperçu ---
        Runnable updatePreview = () -> {
            String user = usernameField.getText().trim();
            String ts = tablespaceField.getText().trim();
            String quota = quotaField.getText().trim();
            String profile = profileCombo.getValue();
            String status = statusCombo.getValue();
            if (user.isEmpty()) {
                sqlPreview.setText("Remplissez les champs pour voir l'aperçu.");
                return;
            }
            StringBuilder sql = new StringBuilder("CREATE USER " + user + " IDENTIFIED BY \"***\" ");
            if (!ts.isEmpty()) {
                sql.append("DEFAULT TABLESPACE ").append(ts).append(" ");
            }
            if (!quota.isEmpty() && !ts.isEmpty()) {
                sql.append("QUOTA ").append(quota).append(" ON ").append(ts).append(" ");
            }
            if (profile != null && !"DEFAULT".equals(profile)) {
                sql.append("PROFILE ").append(profile).append(" ");
            }
            if (status != null && !"OPEN".equals(status)) {
                sql.append("ACCOUNT ").append(status).append(" ");
            }
            sqlPreview.setText(sql.toString());
        };

        usernameField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        passwordField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        tablespaceField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        quotaField.textProperty().addListener((obs, o, n) -> updatePreview.run());
        profileCombo.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        statusCombo.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        chkCommon.setOnAction(e -> updatePreview.run());

        // --- Validation du bouton Créer ---
        Node createButton = dialog.getDialogPane().lookupButton(btnOk);
        createButton.setDisable(true);

        Runnable validateFields = () -> {
            String user = usernameField.getText().trim();
            String pwd = passwordField.getText();
            String confirm = confirmField.getText();
            boolean valid = !user.isEmpty() && !pwd.isEmpty() && pwd.equals(confirm);
            createButton.setDisable(!valid);
        };

        usernameField.textProperty().addListener((obs, o, n) -> validateFields.run());
        passwordField.textProperty().addListener((obs, o, n) -> validateFields.run());
        confirmField.textProperty().addListener((obs, o, n) -> validateFields.run());

        scrollPane.setContent(mainVBox);
        dialog.getDialogPane().setContent(scrollPane);

        dialog.getDialogPane().setPrefWidth(900);
        dialog.getDialogPane().setPrefHeight(700);
        dialog.getDialogPane().setMaxWidth(1100);
        dialog.getDialogPane().setMaxHeight(850);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnOk) {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String tablespace = tablespaceField.getText().trim();
            String quota = quotaField.getText().trim();
            String profile = profileCombo.getValue();
            String status = statusCombo.getValue();
            boolean isCommon = chkCommon.isSelected();

            try {
                Queries.createOracleUserAdvanced(username, password, tablespace, quota, profile, status, isCommon);

                if (chkConnect.isSelected()) {
                    Queries.grantPrivilege(username, "CONNECT");
                }
                if (chkResource.isSelected()) {
                    Queries.grantPrivilege(username, "RESOURCE");
                }
                if (chkDba.isSelected()) {
                    Queries.grantPrivilege(username, "DBA");
                }
                if (chkSelectAny.isSelected()) {
                    Queries.grantPrivilege(username, "SELECT ANY TABLE");
                }
                if (chkCreateSession.isSelected()) {
                    Queries.grantPrivilege(username, "CREATE SESSION");
                }
                if (chkUnlimited.isSelected()) {
                    Queries.grantPrivilege(username, "UNLIMITED TABLESPACE");
                }

                if (chkRoleConnect.isSelected()) {
                    Queries.grantRole(username, "CONNECT");
                }
                if (chkRoleResource.isSelected()) {
                    Queries.grantRole(username, "RESOURCE");
                }
                if (chkRoleDba.isSelected()) {
                    Queries.grantRole(username, "DBA");
                }

                // ✅ Log
                Queries.insertLog("INFO", "Gestion Utilisateurs", currentUser,
                        "Création de l'utilisateur " + username + " (commun=" + isCommon + ")");

                NotificationHelper.showSuccess("✅ Utilisateur " + username + " créé avec succès.");
                refreshUsers();

            } catch (SQLException e) {
                NotificationHelper.showError("❌ Erreur : " + e.getMessage());
            }
        }
    }

    private void editUserOracle() {
        int index = usersOracleTableView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            NotificationHelper.showWarning("Veuillez sélectionner un utilisateur.");
            return;
        }

        ObservableList<String> selected = usersOracleTableView.getItems().get(index);
        String username = selected.get(0);
        String currentStatus = selected.get(2);

        // Récupération des privilèges et rôles actuels
        List<String> currentPrivileges;
        List<String> currentRoles;
        try {
            currentPrivileges = Queries.getUserPrivileges(username);
            currentRoles = Queries.getUserRoles(username);
        } catch (SQLException e) {
            NotificationHelper.showError("Impossible de charger les droits : " + e.getMessage());
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'utilisateur");
        dialog.setHeaderText("Propriétés de : " + username);

        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/mycompany/monitoringdb/dialog-style.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("dialog-custom");

        ButtonType btnOk = new ButtonType("Appliquer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, btnCancel);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox mainVBox = new VBox(12);
        mainVBox.setPadding(new Insets(15));

        // --- Mot de passe ---
        TitledPane pwdPane = new TitledPane("Changer le mot de passe", null);
        GridPane pwdGrid = new GridPane();
        pwdGrid.setHgap(10);
        pwdGrid.setVgap(10);
        pwdGrid.setPadding(new Insets(10));

        PasswordField newPwdField = new PasswordField();
        newPwdField.setPromptText("Nouveau mot de passe (laisser vide pour ne pas changer)");
        PasswordField confirmPwdField = new PasswordField();
        confirmPwdField.setPromptText("Confirmer");

        pwdGrid.add(new Label("Nouveau mot de passe :"), 0, 0);
        pwdGrid.add(newPwdField, 1, 0);
        pwdGrid.add(new Label("Confirmation :"), 0, 1);
        pwdGrid.add(confirmPwdField, 1, 1);

        pwdPane.setContent(pwdGrid);
        mainVBox.getChildren().add(pwdPane);

        // --- Statut ---
        TitledPane statusPane = new TitledPane("Statut du compte", null);
        GridPane statusGrid = new GridPane();
        statusGrid.setHgap(10);
        statusGrid.setVgap(10);
        statusGrid.setPadding(new Insets(10));

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("OPEN", "LOCKED", "EXPIRED");
        statusCombo.setValue(currentStatus);

        statusGrid.add(new Label("Statut :"), 0, 0);
        statusGrid.add(statusCombo, 1, 0);

        statusPane.setContent(statusGrid);
        mainVBox.getChildren().add(statusPane);

        // --- Privilèges système ---
        TitledPane privPane = new TitledPane("Privilèges système", null);
        VBox privVBox = new VBox(6);
        privVBox.setPadding(new Insets(10));

        CheckBox chkConnect = new CheckBox("CONNECT");
        chkConnect.setSelected(currentPrivileges.contains("CONNECT"));
        CheckBox chkResource = new CheckBox("RESOURCE");
        chkResource.setSelected(currentPrivileges.contains("RESOURCE"));
        CheckBox chkDba = new CheckBox("DBA");
        chkDba.setSelected(currentPrivileges.contains("DBA"));
        CheckBox chkSelectAny = new CheckBox("SELECT ANY TABLE");
        chkSelectAny.setSelected(currentPrivileges.contains("SELECT ANY TABLE"));
        CheckBox chkCreateSession = new CheckBox("CREATE SESSION");
        chkCreateSession.setSelected(currentPrivileges.contains("CREATE SESSION"));
        CheckBox chkUnlimited = new CheckBox("UNLIMITED TABLESPACE");
        chkUnlimited.setSelected(currentPrivileges.contains("UNLIMITED TABLESPACE"));

        privVBox.getChildren().addAll(
                new Label("Sélectionnez les privilèges à accorder :"),
                chkConnect, chkResource, chkDba, chkSelectAny, chkCreateSession, chkUnlimited
        );
        privPane.setContent(privVBox);
        mainVBox.getChildren().add(privPane);

        // --- Rôles ---
        TitledPane rolePane = new TitledPane("Rôles", null);
        VBox roleVBox = new VBox(6);
        roleVBox.setPadding(new Insets(10));

        CheckBox chkRoleConnect = new CheckBox("Rôle CONNECT");
        chkRoleConnect.setSelected(currentRoles.contains("CONNECT"));
        CheckBox chkRoleResource = new CheckBox("Rôle RESOURCE");
        chkRoleResource.setSelected(currentRoles.contains("RESOURCE"));
        CheckBox chkRoleDba = new CheckBox("Rôle DBA");
        chkRoleDba.setSelected(currentRoles.contains("DBA"));

        roleVBox.getChildren().addAll(
                new Label("Sélectionnez les rôles :"),
                chkRoleConnect, chkRoleResource, chkRoleDba
        );
        rolePane.setContent(roleVBox);
        mainVBox.getChildren().add(rolePane);

        scrollPane.setContent(mainVBox);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(650);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == btnOk) {
            try {
                boolean modified = false;
                StringBuilder changes = new StringBuilder();

                // 1. Mot de passe
                String newPwd = newPwdField.getText();
                if (!newPwd.isEmpty()) {
                    Queries.changeUserPassword(username, newPwd);
                    modified = true;
                    changes.append("mot de passe changé; ");
                }

                // 2. Statut
                String newStatus = statusCombo.getValue();
                if (!newStatus.equals(currentStatus)) {
                    Queries.changeUserStatus(username, newStatus);
                    modified = true;
                    changes.append("statut -> ").append(newStatus).append("; ");
                }

                // 3. Privilèges
                modified |= updatePrivilegeIfChanged(username, "CONNECT", chkConnect.isSelected(), changes);
                modified |= updatePrivilegeIfChanged(username, "RESOURCE", chkResource.isSelected(), changes);
                modified |= updatePrivilegeIfChanged(username, "DBA", chkDba.isSelected(), changes);
                modified |= updatePrivilegeIfChanged(username, "SELECT ANY TABLE", chkSelectAny.isSelected(), changes);
                modified |= updatePrivilegeIfChanged(username, "CREATE SESSION", chkCreateSession.isSelected(), changes);
                modified |= updatePrivilegeIfChanged(username, "UNLIMITED TABLESPACE", chkUnlimited.isSelected(), changes);

                // 4. Rôles
                modified |= updateRoleIfChanged(username, "CONNECT", chkRoleConnect.isSelected(), changes);
                modified |= updateRoleIfChanged(username, "RESOURCE", chkRoleResource.isSelected(), changes);
                modified |= updateRoleIfChanged(username, "DBA", chkRoleDba.isSelected(), changes);

                if (modified) {
                    Queries.insertLog("INFO", "Gestion Utilisateurs", currentUser,
                            "Modification de l'utilisateur " + username + " : " + changes.toString());
                    NotificationHelper.showSuccess("✅ Utilisateur " + username + " modifié.");
                    refreshUsers();
                } else {
                    NotificationHelper.showInfo("Aucune modification effectuée.");
                }

            } catch (SQLException e) {
                NotificationHelper.showError("❌ Erreur : " + e.getMessage());
            }
        }
    }

    private boolean updatePrivilegeIfChanged(String username, String privilege, boolean grant, StringBuilder changes) throws SQLException {
        boolean has = Queries.hasPrivilege(username, privilege);
        if (grant && !has) {
            Queries.grantPrivilege(username, privilege);
            changes.append("+").append(privilege).append("; ");
            return true;
        } else if (!grant && has) {
            Queries.revokePrivilege(username, privilege);
            changes.append("-").append(privilege).append("; ");
            return true;
        }
        return false;
    }

    private boolean updateRoleIfChanged(String username, String role, boolean grant, StringBuilder changes) throws SQLException {
        boolean has = Queries.hasRole(username, role);
        if (grant && !has) {
            Queries.grantRole(username, role);
            changes.append("+ rôle ").append(role).append("; ");
            return true;
        } else if (!grant && has) {
            Queries.revokeRole(username, role);
            changes.append("- rôle ").append(role).append("; ");
            return true;
        }
        return false;
    }

    private void deleteUserOracle() {
        int selectedIndex = usersOracleTableView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            NotificationHelper.showWarning("Veuillez sélectionner un utilisateur.");
            return;
        }

        ObservableList<String> selected = usersOracleTableView.getItems().get(selectedIndex);
        String username = selected.get(0);
        String type = selected.get(1);

        if ("Linux".equals(type) && ("root".equals(username) || "oracle".equals(username))) {
            NotificationHelper.showWarning("Impossible de supprimer l'utilisateur système : " + username);
            return;
        }
        if ("Oracle".equals(type) && ("SYS".equals(username) || "SYSTEM".equals(username))) {
            NotificationHelper.showWarning("Impossible de supprimer l'utilisateur système : " + username);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'utilisateur " + username + " ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if ("Oracle".equals(type)) {
                    Queries.dropOracleUser(username);
                    Queries.insertLog("INFO", "Gestion Utilisateurs", currentUser,
                            "Suppression de l'utilisateur Oracle " + username);
                    NotificationHelper.showSuccess("✅ Utilisateur " + username + " supprimé.");
                    refreshUsers();
                } else {
                    NotificationHelper.showWarning("La suppression des utilisateurs Linux n'est pas supportée.");
                }
            } catch (SQLException e) {
                NotificationHelper.showError("Erreur : " + e.getMessage());
            }
        }
    }

    private void refreshUsers() {
        refreshUsersOracleButton.setDisable(true);
        refreshUsersOracleButton.setText("Chargement...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ObservableList<ObservableList<String>> combined = FXCollections.observableArrayList();

                // 1. Utilisateurs Linux
                List<String[]> linuxUsers = linux.getLinuxUsers();
                for (String[] u : linuxUsers) {
                    ObservableList<String> row = FXCollections.observableArrayList();
                    row.add(u[0]);
                    row.add("Linux");
                    row.add("ACTIVE");
                    row.add("UID: " + u[1]);
                    row.add("N/A");
                    combined.add(row);
                }

                // 2. Utilisateurs Oracle
                ObservableList<ObservableList<String>> oracleUsers = Queries.getOracleUsers();
                combined.addAll(oracleUsers);

                Platform.runLater(() -> {
                    usersData.clear();
                    usersData.addAll(combined);
                    usersOracleTableView.setItems(usersData);
                    updateUserStats(combined);
                    try {
                        Queries.insertLog("INFO", "Gestion Utilisateurs", currentUser,
                                "Rafraîchissement de la liste des utilisateurs");
                    } catch (SQLException ignored) {
                    }
                });
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            refreshUsersOracleButton.setDisable(false);
            refreshUsersOracleButton.setText("RAFRAICHIR");
        });
        task.setOnFailed(e -> {
            refreshUsersOracleButton.setDisable(false);
            refreshUsersOracleButton.setText("RAFRAICHIR");
            NotificationHelper.showError("Erreur : " + task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private void updateUserStats(ObservableList<ObservableList<String>> users) {
        int total = users.size();
        long standard = users.stream()
                .filter(row -> !row.get(1).equals("Linux") || row.get(0).equals("oracle"))
                .count();
        long superUsers = users.stream()
                .filter(row -> row.get(0).equals("root") || row.get(0).equals("SYS"))
                .count();

        totalUsersLabel.setText("Total Utilisateur: " + total);
        totalUsersStandardLabel.setText("Total Utilisateurs Standard: " + standard);
        totalUsersSuperLabel.setText("Total Super Utilisateurs: " + superUsers);
    }

    private void setupUsersColumns() {
        usersOracleTableView.getColumns().clear();

        String[] columns = {"Nom", "Type", "Statut", "Info", "Dernière Connexion"};
        int[] widths = {150, 80, 120, 150, 150};

        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(idx)));
            col.setPrefWidth(widths[i]);
            usersOracleTableView.getColumns().add(col);
        }
    }

    private void exportUsers() {
        if (usersOracleTableView.getItems().isEmpty()) {
            NotificationHelper.showWarning("Aucun utilisateur à exporter.");
            return;
        }

        List<String> headers = usersOracleTableView.getColumns().stream()
                .map(col -> col.getText())
                .collect(Collectors.toList());

        ExportCSV.exportSimple(headers, usersOracleTableView.getItems(),
                usersOracleTableView.getScene().getWindow(), "utilisateurs");

        try {
            Queries.insertLog("INFO", "Export", currentUser,
                    "Export de la liste des utilisateurs en CSV");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'écriture du log : " + e.getMessage());
        }
    }

    /* Methode Logs */
    private void initLogs() {
        // 1. Remplir les filtres
        logLevelFilter.getItems().addAll("Tous", "ERROR", "WARNING", "INFO", "DEBUG");
        logLevelFilter.setValue("Tous");
        logSourceFilter.getItems().addAll("Tous", "Système", "Base Oracle", "Audit", "Sécurité", "Backup");
        logSourceFilter.setValue("Tous");

        // 2. Dates par défaut
        logStartDate.setValue(LocalDate.now().minusDays(1));
        logEndDate.setValue(LocalDate.now());

        // 3. Configurer les colonnes
        setupLogColumns();

        // 4. Actions
        filterLogButton.setOnAction(event -> refreshLogs());
        refreshLogButton.setOnAction(event -> refreshLogs());
        exportLogButton.setOnAction(event -> exportLogs());

        // 5. Charger
        refreshLogs();
    }

    private void setupLogColumns() {
        logTableView.getColumns().clear();

        String[] columns = {"Horodatage", "Niveau", "Source", "Utilisateur", "Message"};
        int[] widths = {150, 80, 120, 120, 350};

        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(idx)));
            col.setPrefWidth(widths[i]);

            // Colorer la colonne Niveau
            if (i == 1) {
                col.setCellFactory(c -> new TableCell<ObservableList<String>, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }
                        setText(item);
                        switch (item) {
                            case "ERROR":
                                setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                                break;
                            case "WARNING":
                                setStyle("-fx-text-fill: #F39C12; -fx-font-weight: bold;");
                                break;
                            case "INFO":
                                setStyle("-fx-text-fill: #3498DB;");
                                break;
                            default:
                                setStyle("");
                        }
                    }
                });
            }

            logTableView.getColumns().add(col);
        }
    }

    private void refreshLogs() {
        refreshLogButton.setDisable(true);
        refreshLogButton.setText("Chargement...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String level = logLevelFilter.getValue();
                String source = logSourceFilter.getValue();
                LocalDate start = logStartDate.getValue();
                LocalDate end = logEndDate.getValue();

                if ("Tous".equals(level)) {
                    level = null;
                }
                if ("Tous".equals(source)) {
                    source = null;
                }

                ObservableList<ObservableList<String>> logs = Queries.getLogs(level, source, start, end);

                Platform.runLater(() -> {
                    logTableView.getItems().clear();
                    logTableView.getItems().addAll(logs);
                    updateLogStats(logs);  // ← Mettre à jour les statistiques
                });

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            refreshLogButton.setDisable(false);
            refreshLogButton.setText("RAFRAÎCHIR");
        });

        task.setOnFailed(e -> {
            refreshLogButton.setDisable(false);
            refreshLogButton.setText("RAFRAÎCHIR");
            NotificationHelper.showError("Erreur : " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void updateLogStats(ObservableList<ObservableList<String>> logs) {
        int total = logs.size();
        long errors = logs.stream().filter(row -> "ERROR".equals(row.get(1))).count();
        long warnings = logs.stream().filter(row -> "WARNING".equals(row.get(1))).count();
        long infos = logs.stream().filter(row -> "INFO".equals(row.get(1))).count();

        totalLogsLabel.setText("Total : " + total);
        errorLogsLabel.setText("Erreurs : " + errors);
        warningLogsLabel.setText("Avertissements : " + warnings);
        infoLogsLabel.setText("Info : " + infos);
    }

    private void exportLogs() {
        if (logTableView.getItems().isEmpty()) {
            NotificationHelper.showWarning("Aucun log à exporter.");
            return;
        }

        List<String> headers = logTableView.getColumns().stream()
                .map(col -> col.getText())
                .collect(Collectors.toList());

        ExportCSV.exportSimple(headers, logTableView.getItems(),
                logTableView.getScene().getWindow(), "logs");
        // Tracqueur
        try {
            Queries.insertLog("INFO", "Export", currentUser, "Export des logs en CSV");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'écriture du log : " + e.getMessage());
        }
    }

    /* === GESTION DES RAPPORTS === */
    private void initRapports() {
        // 1. Remplir les ComboBox
        reportTypeComboBox.getItems().addAll("Synthèse", "Sessions", "Tablespaces", "Logs", "Alertes", "Audit", "Utilisateurs");
        reportTypeComboBox.setValue("Synthèse");
        reportFormatComboBox.getItems().addAll("PDF", "JSON", "XML", "TXT", "CSV");
        reportFormatComboBox.setValue("PDF");
        reportStartDate.setValue(LocalDate.now().minusDays(7));
        reportEndDate.setValue(LocalDate.now());

        // 2. Configurer la TableView
        setupReportsTable();
        reportsTableView.setItems(reportsData);

        // Écouteur de sélection pour l'aperçu
        reportsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            previewReport((Report) newVal);
        });

        // 3. Actions des boutons
        generateReportButton.setOnAction(event -> generateReport());
        downloadReportButton.setOnAction(event -> downloadReport());
        deleteReportButton.setOnAction(event -> deleteReport());

        // 4. Charger la liste des rapports existants
        loadReportsList();

        reportStatusLabel.setText("Dossier : " + SpecialExport.REPORTS_DIR);
    }

    private void setupReportsTable() {
        reportsTableView.getColumns().clear();

        TableColumn<Report, String> colName = new TableColumn<>("Nom");
        colName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        colName.setPrefWidth(200);

        TableColumn<Report, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setPrefWidth(120);

        TableColumn<Report, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDate();
            if (date == null) {
                return new SimpleStringProperty("");
            }
            return new SimpleStringProperty(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        });
        colDate.setPrefWidth(150);

        TableColumn<Report, String> colFormat = new TableColumn<>("Format");
        colFormat.setCellValueFactory(new PropertyValueFactory<>("format"));
        colFormat.setPrefWidth(80);

        TableColumn<Report, String> colSize = new TableColumn<>("Taille");
        colSize.setCellValueFactory(cellData -> {
            long size = cellData.getValue().getSize();
            return new SimpleStringProperty(SpecialExport.formatSize(size));
        });
        colSize.setPrefWidth(100);

        TableColumn<Report, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setPrefWidth(100);

        reportsTableView.getColumns().addAll(colName, colType, colDate, colFormat, colSize, colStatus);
    }

    private void loadReportsList() {
        List<Report> newList = new ArrayList<>();
        List<File> files = SpecialExport.listReports();
        for (File file : files) {
            String name = file.getName();
            String type = name.contains("_") ? name.substring(0, name.lastIndexOf('_')) : name;
            String format = name.substring(name.lastIndexOf('.') + 1).toUpperCase();
            long[] stats = SpecialExport.getFileStats(name);
            long size = stats[0];
            long lastModified = stats[1];
            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochSecond(lastModified), ZoneId.systemDefault());
            String status = (size == 0) ? "Vide" : "Généré";
            Report report = new Report(name, type, date, format, size, status, file.getAbsolutePath());
            newList.add(report);
        }
        reportsData.setAll(newList);
        reportsTableView.refresh();
    }

    private void previewReport(Report report) {
        if (report == null) {
            reportPreviewArea.clear();
            return;
        }

        String format = report.getFormat().toLowerCase();
        if ("txt".equals(format) || "csv".equals(format) || "json".equals(format) || "xml".equals(format)) {
            try {
                File tempFile = File.createTempFile("preview_", "." + format);
                SpecialExport.downloadReport(report.getFileName(), tempFile);

                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(tempFile))) {
                    String line;
                    int lines = 0;
                    while ((line = reader.readLine()) != null && lines < 500) {
                        content.append(line).append("\n");
                        lines++;
                    }
                    if (lines >= 500) {
                        content.append("\n... (affichage limité aux 500 premières lignes)");
                    }
                }
                reportPreviewArea.setText(content.toString());
                tempFile.delete();

            } catch (Exception e) {
                reportPreviewArea.setText("Erreur de lecture du fichier distant : " + e.getMessage());
            }
        } else {
            reportPreviewArea.setText("Aperçu non disponible pour le format " + report.getFormat().toUpperCase() + ".\n"
                    + "Téléchargez le fichier pour le visualiser.");
        }
    }

    private void generateReport() {
        String type = reportTypeComboBox.getValue();
        String format = reportFormatComboBox.getValue();
        LocalDate start = reportStartDate.getValue();
        LocalDate end = reportEndDate.getValue();

        if (start == null || end == null) {
            NotificationHelper.showWarning("Veuillez sélectionner une période.");
            return;
        }

        progressReportIndicator.setVisible(true);
        generateReportButton.setDisable(true);
        reportPreviewArea.clear();

        String generatedBy = (currentUser != null && !currentUser.isEmpty()) ? currentUser : "Administrateur";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Object data = null;
                switch (type) {
                    case "Synthèse":
                        data = Queries.getSyntheseReport(start, end);
                        break;
                    case "Sessions":
                        data = Queries.getSessionsReport(start, end);
                        break;
                    case "Tablespaces":
                        data = Queries.getTablespacesReport();
                        break;
                    case "Logs":
                        data = Queries.getLogReport(start, end);
                        break;
                    case "Alertes":
                        data = Queries.getAlertesReport(start, end);
                        break;
                    case "Audit":
                        data = Queries.getAuditReport(start, end);
                        break;
                    case "Utilisateurs":
                        data = Queries.getUsersReport();
                        break;
                    default:
                        data = Queries.getSyntheseReport(start, end);
                }

                File file = SpecialExport.generateReport(type, format, data, generatedBy);
                if (file == null) {
                    Platform.runLater(() -> {
                        progressReportIndicator.setVisible(false);
                        generateReportButton.setDisable(false);
                        NotificationHelper.showError("Erreur : le fichier n'a pas pu être créé.");
                    });
                    return null;
                }

                final File finalFile = file;
                Platform.runLater(() -> {
                    loadReportsList();
                    reportPreviewArea.setText("Rapport généré : " + finalFile.getName() + "\n"
                            + "Emplacement : " + finalFile.getAbsolutePath() + "\n"
                            + "Taille : " + SpecialExport.formatSize(finalFile.length()) + "\n"
                            + "Généré le : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                    reportStatusLabel.setText("Rapport généré : " + finalFile.getName());
                    progressReportIndicator.setVisible(false);
                    generateReportButton.setDisable(false);
                    NotificationHelper.showSuccess("Rapport généré avec succès.");
                    try {
                        Queries.insertLog("INFO", "Rapport", currentUser, "Génération du rapport " + type + " en " + format);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
                return null;
            }
        };

        task.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent e) {
                progressReportIndicator.setVisible(false);
                generateReportButton.setDisable(false);
                Throwable ex = task.getException();
                NotificationHelper.showError("Erreur : " + ex.getMessage());
                reportPreviewArea.setText("Erreur : " + ex.getMessage());
            }
        });

        new Thread(task).start();
    }

    private void downloadReport() {
        Report selected = (Report) reportsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationHelper.showWarning("Veuillez sélectionner un rapport.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Télécharger le rapport");
        chooser.setInitialFileName(selected.getFileName());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(selected.getFormat().toUpperCase(), "*." + selected.getFormat().toLowerCase())
        );

        File destination = chooser.showSaveDialog(reportsTableView.getScene().getWindow());
        if (destination != null) {
            try {
                SpecialExport.downloadReport(selected.getFileName(), destination);
                NotificationHelper.showSuccess("Rapport téléchargé : " + destination.getName());
                try {
                    Queries.insertLog("INFO", "Rapport", currentUser, "Téléchargement du rapport " + selected.getFileName() + " en " + selected.getFormat());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                NotificationHelper.showError("Erreur de téléchargement : " + e.getMessage());
                try {
                    Queries.insertLog("ERROR", "Rapport", currentUser, "Erreur de téléchargement du rapport " + selected.getFileName() + " en " + selected.getFormat());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void deleteReport() {
        Report selected = (Report) reportsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            NotificationHelper.showWarning("Veuillez sélectionner un rapport.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer le rapport " + selected.getFileName() + " ?");
        confirm.setContentText("Cette action est irréversible.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean deleted = SpecialExport.deleteReport(selected.getFileName());
            if (deleted) {
                reportsData.remove(selected);
                reportsTableView.refresh();
                NotificationHelper.showSuccess("Rapport supprimé.");
                reportPreviewArea.clear();
                try {
                    Queries.insertLog("INFO", "Rapport", currentUser, "Suppression du rapport " + selected.getFileName() + " en " + selected.getFormat());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            } else {
                NotificationHelper.showError("Impossible de supprimer le fichier.");
                try {
                    Queries.insertLog("ERROR", "Rapport", currentUser, "Impossible de supprimer le rapport " + selected.getFileName() + " en " + selected.getFormat());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // GESTION DES EMAILS PLANIFIÉS
    private ScheduledExecutorService schedulerEmail = Executors.newScheduledThreadPool(1);

    private void scheduleWeeklyEmail() {
        try {
            // 1. Récupérer la configuration depuis la base
            String schedule = Queries.getParametre("EMAIL_SCHEDULE");
            boolean emailActif = "true".equals(Queries.getParametre("EMAIL_ACTIF"));

            // 2. Vérifier que l'email est activé
            if (!emailActif) {
                System.out.println("Envoi d'emails désactivé dans les paramètres.");
                return;
            }

            // 3. Vérifier qu'une programmation existe
            if (schedule == null || schedule.isEmpty()) {
                System.out.println("Aucune programmation définie. Utilisation de la valeur par défaut.");
                schedule = "FRIDAY_18:00";
            }

            // 4. Analyser la programmation (format: JOUR_HEURE)
            String[] parts = schedule.split("_");
            if (parts.length != 2) {
                System.err.println("Format de programmation invalide : " + schedule + ". Utilisation de FRIDAY_18:00");
                schedule = "FRIDAY_18:00";
                parts = schedule.split("_");
            }

            // 5. Extraire le jour et l'heure
            String dayStr = parts[0].toUpperCase();
            String hourStr = parts[1];

            // 6. Convertir le jour en DayOfWeek
            DayOfWeek scheduledDay;
            try {
                scheduledDay = DayOfWeek.valueOf(dayStr);
            } catch (IllegalArgumentException e) {
                System.err.println("Jour invalide : " + dayStr + ". Utilisation de FRIDAY");
                scheduledDay = DayOfWeek.FRIDAY;
            }

            // 7. Extraire l'heure
            int scheduledHour = 18;
            int scheduledMinute = 0;
            try {
                String[] timeParts = hourStr.split(":");
                if (timeParts.length == 2) {
                    scheduledHour = Integer.parseInt(timeParts[0]);
                    scheduledMinute = Integer.parseInt(timeParts[1]);
                }
            } catch (Exception e) {
                System.err.println("Heure invalide : " + hourStr + ". Utilisation de 18:00");
            }

            // 8. Calculer le délai initial
            long initialDelay = computeInitialDelay(scheduledDay, scheduledHour, scheduledMinute);
            long period = 7 * 24 * 60 * 60; // 7 jours en secondes

            // 9. Planifier l'envoi
            schedulerEmail.scheduleAtFixedRate(() -> {
                sendWeeklyLogs();
            }, initialDelay, period, TimeUnit.SECONDS);

            System.out.println("Email scheduler activé !");
            System.out.println("   - Jour : " + scheduledDay);
            System.out.println("   - Heure : " + String.format("%02d:%02d", scheduledHour, scheduledMinute));
            System.out.println("   - Destinataires : " + Queries.getParametre("EMAIL"));
            System.out.println("   - Prochain envoi dans : " + initialDelay + " secondes");

        } catch (Exception e) {
            System.err.println("Erreur lors de la planification des emails : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private long computeInitialDelay(DayOfWeek scheduledDay, int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextExecution = now.with(TemporalAdjusters.next(scheduledDay))
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        // Debug
        System.out.println("Prochaine exécution : " + nextExecution);

        // Si la prochaine exécution est passée, passer à la semaine suivante
        if (now.isAfter(nextExecution)) {
            nextExecution = nextExecution.plusWeeks(1);
            System.out.println("Date passée, repoussée à : " + nextExecution);
        }

        return ChronoUnit.SECONDS.between(now, nextExecution);
    }

    private void sendManualEmail() {
        // Désactiver le bouton pendant l'envoi
        sendEmailNowButton.setDisable(true);
        sendEmailNowButton.setText("⏳ ENVOI EN COURS...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    System.out.println("📧 Envoi manuel du rapport demandé...");

                    // Vérifier que l'email est activé
                    boolean emailActif = "true".equals(Queries.getParametre("EMAIL_ACTIF"));
                    if (!emailActif) {
                        Platform.runLater(() -> {
                            NotificationHelper.showWarning("⚠️ L'envoi d'emails est désactivé dans les paramètres.");
                            statusParamLabel.setText("⚠️ Email désactivé, activez-le d'abord");
                            statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                        });
                        return null;
                    }

                    // Récupérer les destinataires
                    String emailList = Queries.getParametre("EMAIL");
                    if (emailList == null || emailList.isEmpty()) {
                        Platform.runLater(() -> {
                            NotificationHelper.showWarning("⚠️ Aucun destinataire configuré.");
                            statusParamLabel.setText("⚠️ Aucun destinataire");
                            statusParamLabel.setStyle("-fx-text-fill: #F39C12;");
                        });
                        return null;
                    }

                    // Envoyer le rapport
                    sendWeeklyLogs();

                    Platform.runLater(() -> {
                        NotificationHelper.showSuccess("✅ Rapport envoyé avec succès !");
                        statusParamLabel.setText("✅ Email envoyé à " + emailList);
                        statusParamLabel.setStyle("-fx-text-fill: #27AE60;");
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        NotificationHelper.showError("❌ Erreur : " + e.getMessage());
                        statusParamLabel.setText("❌ Erreur : " + e.getMessage());
                        statusParamLabel.setStyle("-fx-text-fill: #E74C3C;");
                    });
                    e.printStackTrace();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            sendEmailNowButton.setDisable(false);
            sendEmailNowButton.setText("📧 ENVOYER MAINTENANT");
        });

        task.setOnFailed(e -> {
            sendEmailNowButton.setDisable(false);
            sendEmailNowButton.setText("📧 ENVOYER MAINTENANT");
        });

        new Thread(task).start();
    }

    private void sendWeeklyLogs() {
        try {
            // 1. Vérifier que l'email est activé
            boolean emailActif = "true".equals(Queries.getParametre("EMAIL_ACTIF"));
            if (!emailActif) {
                System.out.println("📧 Envoi d'emails désactivé.");
                return;
            }

            // 2. Récupérer la liste des destinataires
            String emailList = Queries.getParametre("EMAIL");
            if (emailList == null || emailList.isEmpty()) {
                System.out.println("📧 Aucun destinataire configuré.");
                return;
            }

            // 3. Récupérer les données des 7 derniers jours
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(7);

            // 3.1 Récupérer les logs
            ObservableList<ObservableList<String>> logs = Queries.getLogs(null, null, start, end);

            // 3.2 Récupérer les alertes
            ObservableList<Alertes> alerts = Queries.getAlertes();
            List<Alertes> recentAlerts = alerts.stream()
                    .filter(a -> {
                        try {
                            String timestamp = a.getTimestamp();
                            if (timestamp != null && !timestamp.isEmpty()) {
                                String datePart = timestamp.substring(0, Math.min(11, timestamp.length()));
                                LocalDate alertDate = LocalDate.parse(datePart,
                                        DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                                return !alertDate.isBefore(start) && !alertDate.isAfter(end);
                            }
                        } catch (Exception ignored) {
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            // 3.3 Récupérer les statistiques de performance
            Map<String, Object> perfStats = Queries.getSyntheseReport(start, end);

            // 4. Générer un fichier Excel
            File tempFile = File.createTempFile("rapport_hebdomadaire_", ".xlsx");
            ExportCSV.generateExcelReport(tempFile, logs, recentAlerts, perfStats, start, end);
            System.out.println("📄 Rapport Excel généré : " + tempFile.getAbsolutePath());

            // 5. Préparer et envoyer l'email
            String[] recipients = emailList.split("[ ,]+");
            String subject = "Rapport hebdomadaire - "
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String body = "Veuillez trouver en pièce jointe le rapport hebdomadaire du "
                    + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " au "
                    + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".\n\n"
                    + "Le fichier contient 3 feuilles :\n"
                    + "📊 Feuille 1 : Statistiques de performance\n"
                    + "📋 Feuille 2 : Logs de la semaine\n"
                    + "🔔 Feuille 3 : Alertes de la semaine\n\n"
                    + "Cordialement,\n"
                    + "Monitoring DB Premium";

            EmailService.sendEmailWithAttachment(recipients, subject, body, tempFile);
            System.out.println("📧 Email envoyé avec succès à " + emailList);

            // 6. Log dans la base
            try {
                Queries.insertLog("INFO", "Email", "Système",
                        "Rapport hebdomadaire envoyé à " + emailList);
            } catch (SQLException e) {
                System.err.println("⚠️ Erreur lors de l'insertion du log : " + e.getMessage());
            }

            // 7. Nettoyer le fichier temporaire
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                System.out.println("🗑️ Fichier temporaire supprimé : " + tempFile.getName());
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi des logs : " + e.getMessage());
            e.printStackTrace();

            try {
                Queries.insertLog("ERROR", "Email", "Système",
                        "Erreur lors de l'envoi du rapport : " + e.getMessage());
            } catch (SQLException ex) {
                System.err.println("⚠️ Erreur log : " + ex.getMessage());
            }
        }
    }

    /* INITIALISATION */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // === INITIALISATION DES SERVICES ===
        try {
            Queries = new OracleQueries();
            linux = new LinuxQueries();
            System.out.println("Services OracleQueries et LinuxQueries initialisés.");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des services : " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 1. Remplir la map avec les associations bouton ↔ page
        pageMap.put(btn_dashboard, dashboard_page);
        pageMap.put(btn_graphic, graphic_page);
        pageMap.put(btn_console, console_page);
        pageMap.put(btn_session, session_page);
        pageMap.put(btn_alert, alert_page);
        pageMap.put(btn_logs, log_page);
        pageMap.put(btn_audit, audit_page);
        pageMap.put(btn_parametre, parametre_page);
        pageMap.put(btn_utilisateur, utilisateur_page);
        pageMap.put(btn_rapport, rapport_page);

        // 2. Ajouter un listener à chaque bouton
        for (Button btn : pageMap.keySet()) {
            btn.setOnAction(event -> Paserelle(btn));
        }

        // 3. Page par défaut : Dashboard
        Paserelle(btn_dashboard);

        // ÉTAPE A : INITIALISATION DU GRAPHIQUE DYNAMIQUE CPU
        NumberAxis xAxis = new NumberAxis();
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setMinorTickVisible(false);
        xAxis.setLabel(null);

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setAutoRanging(false);
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setLabel(null);

        // 1. Instanciation de l'AreaChart EN PREMIER
        cpuChart = new AreaChart<>(xAxis, yAxis);

        // 2. Configuration des marges à zéro (Maintenant que l'objet existe !)
        cpuChart.setPadding(javafx.geometry.Insets.EMPTY);

        cpuChart.setCreateSymbols(false);
        cpuChart.setLegendVisible(false);
        cpuChart.setHorizontalGridLinesVisible(false);
        cpuChart.setVerticalGridLinesVisible(false);

        // 3. Forcer la taille stricte de 67x25 pour écraser les marges internes de JavaFX
        cpuChart.setMinSize(67, 25);
        cpuChart.setMaxSize(67, 25);
        cpuChart.setPrefSize(67, 25);

        // Liaison dynamique des dimensions au conteneur Pane de Scene Builder
        cpuChart.prefWidthProperty().bind(cpuChartContainer.widthProperty());
        cpuChart.prefHeightProperty().bind(cpuChartContainer.heightProperty());

        cpuSeries = new XYChart.Series<>();
        cpuChart.getData().add(cpuSeries);
        cpuChartContainer.getChildren().add(cpuChart);

        // ÉTAPE B : CHARGEMENT DES DONNÉES INITIALES
        try {
            // Vérifier si la connexion est disponible via DatabaseConnection
            DatabaseConnection.getConnection();

            initNotificationBell();
            remplirRight();
            initBottomMetrics();
            initClock();
            initSqlConsole();
            initSessions();
            initAudit();
            initAlertes();
            initLogs();
            initParametres();
            initUsers();
            initRapports();
            initCharts();
            initAdvancedCharts();

        } catch (SQLException ex) {
            System.err.println("Erreur de connexion lors de l'initialisation : " + ex.getMessage());
            Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Récupérer l'utilisateur depuis le Holder
        String user = ConnexionHolder.getCurrentUser();
        if (user != null && !user.isEmpty()) {
            this.currentUser = user;
            System.out.println("Utilisateur récupéré : " + this.currentUser);
        }

        // ÉTAPE C : CRÉATION ET DÉMARRAGE DU MONITORING ASYNC
        ScheduledService<Void> monitoringService = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        try {
                            String host = Queries.getHost();
                            String cpu = linux.getSystemCPUUsage();
                            String ram = linux.getSystemRAMUsed();
                            String uptime = linux.getUptime();
                            String user = linux.getUserConnect();
                            String disk = linux.getDisk();
                            String upload = linux.getUpload();
                            String download = linux.getDownload();

                            Platform.runLater(() -> {
                                hostLabelBottom.setText(host);
                                cpuLabelBottom.setText(cpu);
                                String cleanCpu = cpu.trim().replace(",", ".");
                                try {
                                    double valueAlertCpu = Double.parseDouble(cleanCpu.replace("%", "").trim());
                                    if (valueAlertCpu >= 90) {
                                        cpuLabelBottom.setStyle("-fx-text-fill: red; -fx-font-weight:bold;");
                                    } else if (valueAlertCpu >= 70) {
                                        cpuLabelBottom.setStyle("-fx-text-fill: orange; -fx-font-weight:bold;");
                                    } else {
                                        cpuLabelBottom.setStyle("-fx-text-fill: #00ff00;");
                                    }
                                    cpuSeries.getData().add(new XYChart.Data<>(timeIndex++, valueAlertCpu));
                                    if (cpuSeries.getData().size() > MAX_DATA_POINTS) {
                                        cpuSeries.getData().remove(0);
                                    }
                                } catch (NumberFormatException ignored) {
                                }

                                ramLabelBottom.setText(ram);
                                uploadLabelBottom.setText(upload);
                                downloadLabelBottom.setText(download);
                                uptimeLabelBottom.setText(uptime);
                                userLabelBottom.setText(user);
                                diskLabelBottom.setText(disk);
                            });
                        } catch (SQLException e) {
                            System.err.println("Erreur monitoring : " + e.getMessage());
                        }
                        return null;
                    }
                };
            }
        };

        scheduleWeeklyEmail();
        monitoringService.setPeriod(Duration.seconds(3));
        monitoringService.start();
    }

}
