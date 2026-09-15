package org.engcia.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.engcia.App;
import org.engcia.services.ExpertEngine;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class QuestionsController implements Initializable {
    public static boolean numeric = false;

    @FXML
    private Button buttonNext;

    @FXML
    private Text question;

    @FXML
    private TextField questionInput;

    @FXML
    void buttonEvent(ActionEvent event) throws IOException {
        ExpertEngine engine = ExpertEngine.get();
        String current = this.question.getText();
        if (current == null || current.isEmpty()) {
            return;
        }

        if ("See Options".equals(current)) {
            engine.recommend();
            if (App.popupStage != null) {
                App.popupStage.close();
            }
            App.setRoot("primary");
            return;
        }

        String input = this.questionInput.getText() == null ? "" : this.questionInput.getText().trim();
        if (input.isEmpty()) {
            showError("Please enter an answer.");
            return;
        }

        if (numeric) {
            try {
                engine.answerNumerical(current, Double.parseDouble(input));
            } catch (NumberFormatException e) {
                showError("Enter a number for this question.");
                return;
            }
        } else {
            engine.answer(current, input);
        }

        if (App.popupStage != null) {
            App.popupStage.close();
        }
        App.setPopupScene("questions");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ExpertEngine engine = ExpertEngine.get();
        String next = engine.nextQuestion();
        if (next == null) {
            this.question.setText("See Options");
            this.questionInput.setDisable(true);
            this.buttonNext.setText("Close");
            numeric = false;
            return;
        }
        numeric = engine.isNextQuestionNumerical();
        this.question.setText(next);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
