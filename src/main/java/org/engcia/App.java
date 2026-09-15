package org.engcia;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.engcia.config.AppConfig;
import org.engcia.integration.Integrations;
import org.engcia.model.ConsumptionPeriod;
import org.engcia.model.Justification;
import org.engcia.services.TrackingAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class App extends Application {

    public static KieSession KS;
    public static KieContainer kContainer;
    public static BufferedReader BR;
    public static TrackingAgendaEventListener agendaEventListener;
    public static Map<Integer, Justification> justifications;
    public static List<String> conclusionsList;
    public static Map<String, List<ConsumptionPeriod>> consumptionPeriods;
    public static Scene scene;
    public static List<String> questionListCategorical;
    public static List<String> questionListNumerical;
    public static Stage popupStage;
    public static Stage stage;
    public static int conclusionCounter;
    public static String consumerName;

    @Override
    public void start(Stage stage) throws IOException {
        AppConfig.load();
        Integrations.start();
        App.stage = stage;
        scene = new Scene(loadFXML("consumidor"));
        stage.setTitle("Smart Home Energy Advisor");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        Integrations.stop();
    }

    public static void setRoot(String fxml) throws IOException {
        if (fxml.equals("primary")) {
            scene.getWindow().setWidth(840);
            scene.getWindow().setHeight(480);
        } else {
            scene.getWindow().setHeight(178);
            scene.getWindow().setWidth(283);
        }
        scene.getWindow().centerOnScreen();
        scene.setRoot(loadFXML(fxml));
    }

    public static void setPopupScene(String fxml) throws IOException {
        if (popupStage != null && popupStage.isShowing()) {
            popupStage.close();
        }
        popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(stage);
        Scene popupScene = new Scene(loadFXML(fxml), 300, 200);
        popupStage.setScene(popupScene);
        popupStage.show();
    }

    public static Stage getCurrentStage(Node node) {
        return (Stage) node.getScene().getWindow();
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void populateQuestionList() {
        // Question lists are owned by ExpertEngine.startConsumer / resetCurrentConsumer.
    }

    public static void main(String[] args) {
        launch();
    }
}
