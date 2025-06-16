package org.engcia.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.engcia.App;

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
        if(!this.questionInput.getText().isEmpty()) {
            App.consumerName = this.questionInput.getText();
            PrimaryController.count =0;
            new Thread(App::runEngine).start();
            App.setRoot("primary");
        }
    }

}
