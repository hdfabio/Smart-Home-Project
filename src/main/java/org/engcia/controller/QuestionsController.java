package org.engcia.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.engcia.App;
import org.engcia.model.QuestionCatalog;
import org.engcia.services.ExpertEngine;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class QuestionsController implements Initializable {

    private QuestionCatalog.QuestionDef currentQuestion;

    @FXML
    private Button buttonNext;

    @FXML
    private Text question;

    @FXML
    private TextField questionInput;

    @FXML
    private ChoiceBox<String> answerChoice;

    @FXML
    void buttonEvent(ActionEvent event) throws IOException {
        ExpertEngine engine = ExpertEngine.get();

        if (currentQuestion == null && "See recommendations".equals(this.question.getText())) {
            engine.recommend();
            if (App.popupStage != null) {
                App.popupStage.close();
            }
            App.setRoot("primary");
            return;
        }

        if (currentQuestion == null) {
            return;
        }

        if (currentQuestion.getType() == QuestionCatalog.AnswerType.CHOICE
                || currentQuestion.getType() == QuestionCatalog.AnswerType.NUMBER_CHOICE) {
            String selected = answerChoice.getValue();
            if (selected == null || selected.isBlank()) {
                showError("Please select an option.");
                return;
            }
            if (currentQuestion.isNumerical()) {
                engine.answerNumerical(currentQuestion.getKey(), Double.parseDouble(selected));
            } else {
                engine.answer(currentQuestion.getKey(), selected);
            }
        } else {
            String input = questionInput.getText() == null ? "" : questionInput.getText().trim();
            if (input.isEmpty()) {
                showError("Please enter a number.");
                return;
            }
            try {
                engine.answerNumerical(currentQuestion.getKey(), Double.parseDouble(input));
            } catch (NumberFormatException e) {
                showError("Enter a valid number (see the unit in the question).");
                return;
            }
        }

        if (App.popupStage != null) {
            App.popupStage.close();
        }
        App.setPopupScene("questions");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ExpertEngine engine = ExpertEngine.get();
        currentQuestion = engine.nextQuestionDef();
        if (currentQuestion == null) {
            this.question.setText("See recommendations");
            this.questionInput.setVisible(false);
            this.questionInput.setManaged(false);
            this.answerChoice.setVisible(false);
            this.answerChoice.setManaged(false);
            this.buttonNext.setText("Show");
            return;
        }

        this.question.setText(currentQuestion.getLabel());
        boolean useChoice = currentQuestion.getType() == QuestionCatalog.AnswerType.CHOICE
                || currentQuestion.getType() == QuestionCatalog.AnswerType.NUMBER_CHOICE;

        questionInput.setVisible(!useChoice);
        questionInput.setManaged(!useChoice);
        answerChoice.setVisible(useChoice);
        answerChoice.setManaged(useChoice);

        if (useChoice) {
            answerChoice.setItems(FXCollections.observableArrayList(currentQuestion.getOptions()));
            if (!currentQuestion.getOptions().isEmpty()) {
                answerChoice.getSelectionModel().selectFirst();
            }
        } else {
            questionInput.setPromptText("number");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
