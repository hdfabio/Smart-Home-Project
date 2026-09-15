package org.engcia.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.engcia.App;
import org.engcia.services.ExpertEngine;

import java.io.IOException;

public class ConsumidorController {

    @FXML
    private Button buttonNext;

    @FXML
    private Text question;

    @FXML
    private TextField questionInput;

    @FXML
    void buttonEvent(ActionEvent event) throws IOException {
        String consumerId = this.questionInput.getText() == null ? "" : this.questionInput.getText().trim();
        if (consumerId.isEmpty()) {
            showError("Enter a consumer number (available: " + ExpertEngine.availableConsumers() + ").");
            return;
        }
        if (!ExpertEngine.consumerExists(consumerId)) {
            showError("Unknown consumer '" + consumerId + "'. Available: " + ExpertEngine.availableConsumers() + ".");
            return;
        }
        ExpertEngine.get().startConsumer(consumerId);
        App.setRoot("primary");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid consumer");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
