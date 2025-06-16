package org.engcia.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.engcia.App;
import org.engcia.model.common.CategoricalEvidence;
import org.engcia.model.common.NumericalEvidence;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class QuestionsController implements Initializable {
public static String question1;
public static boolean numeric = false;

    @FXML
        private Button buttonNext;

        @FXML
        private Text question;

        @FXML
        private TextField questionInput;

    @FXML
    synchronized void buttonEvent(ActionEvent event) throws IOException, InterruptedException {
        if(this.question.getText().equals("See Options")){
            synchronized (App.lock) {
                App.lock.notify();

            };
            Thread.sleep(1000);
            App.setRoot("primary");
            App.popupStage.close();
            App.populateQuestionList();
        }
        if(!this.question.getText().isEmpty() && numeric) {
            NumericalEvidence e = new NumericalEvidence(question.getText(), Double.parseDouble(this.questionInput.getText()));
            App.KS.insert(e);
            App.questionListNumerical.remove(0);
            App.popupStage.close();
            App.setPopupScene("questions");
        }else if(!this.question.getText().isEmpty()){
            CategoricalEvidence e = new CategoricalEvidence(question.getText(), this.questionInput.getText());
            App.KS.insert(e);
            App.questionListCategorical.remove(0);
            App.popupStage.close();
            App.setPopupScene("questions");
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String question ="";
        if(!App.questionListCategorical.isEmpty()){
            question =App.questionListCategorical.get(0);
            numeric = false;
        }else if(!App.questionListNumerical.isEmpty()){
            question = App.questionListNumerical.get(0);
            numeric = true;
        }else{
            question = "See Options";
            this.questionInput.setDisable(true);
            this.buttonNext.setText("Close");
        }
        System.out.println(question);
        this.question.setText(question);

    }
}
