package org.engcia.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.engcia.App;
import org.engcia.services.ExpertEngine;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class PrimaryController implements Initializable {

    public static String helpText = "--------------------------------READ THIS--------------------------------- \n\n" +
            "Units are shown on each question:\n" +
            "- Contracted power: kW (pick a tariff tier)\n" +
            "- Solar area: m²\n" +
            "- Distance: km\n\n" +
            "Yes/no and efficiency answers use the dropdown.\n" +
            "Refrigerator has no D efficiency class.";

    @FXML
    private Button buttonStart;

    @FXML
    private Button buttonStart2;

    @FXML
    private TextArea middleTextArea;

    @FXML
    private Button helpButton2;

    @FXML
    public void onClickStartAnalysis(ActionEvent actionEvent) throws IOException {
        try {
            ExpertEngine.get().resetCurrentConsumer();
        } catch (IOException e) {
            showError("Select a consumer first.");
            return;
        }
        App.setPopupScene("questions");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshConclusions();
        if (buttonStart2 != null) {
            buttonStart2.setDisable(!ExpertEngine.get().hasSession());
        }
        helpButton2.setOnAction((event) -> this.middleTextArea.setText(helpText));
    }

    @FXML
    public void onClickContinueAnalysis(ActionEvent actionEvent) throws IOException {
        if (!ExpertEngine.get().hasSession()) {
            showError("Select a consumer first.");
            return;
        }
        App.setPopupScene("questions");
    }

    @FXML
    void switchConsumerOnAction(ActionEvent event) throws IOException {
        App.setRoot("consumidor");
    }

    private void refreshConclusions() {
        List<String> conclusions = ExpertEngine.get().getConclusions();
        if (conclusions.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (String conclusion : conclusions) {
            text.append(conclusion);
            text.append("\n\n");
        }
        this.middleTextArea.setText(text.toString());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
