package com.mycompany.monitoringdb;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class App extends Application {

    private static Scene scene;
    private static Stage primaryStage;  //  Stocker la référence du Stage

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;  // ← Stocker le Stage
        // CORRECTION : On retire "640, 480" pour que la scène prenne la taille du FXML (900x560)
        scene = new Scene(loadFXML("primary"));
        //  scene.getStylesheets().add(getClass().getResource("@styleDashboard.css").toExternalForm());
        // Optionnel : Un titre pour la fenêtre de l'Africa Golden Bank
        stage.setTitle("Monitoring DB Premium");

        // Empêcher l'utilisateur de déformer le design en redimensionnant la fenêtre
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        // ✅ Chemin CORRECT
        ResourceBundle bundle = ResourceBundle.getBundle(
                "com.mycompany.monitoringdb.i18n.messages", // ← Ajouter "i18n."
                Locale.getDefault()
        );
        // Appliquer les comportements selon la page
        if ("primary".equals(fxml)) {
            primaryStage.setResizable(false);
        } else if ("dashboard".equals(fxml)) {
            primaryStage.setResizable(true);
        }
        primaryStage.centerOnScreen();
        // Retourner le contrôleur
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
