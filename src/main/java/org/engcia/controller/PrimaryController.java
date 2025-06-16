package org.engcia.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.engcia.App;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class PrimaryController implements Initializable {

public static String conclusions;
public static int count;
public static String helpText = "--------------------------------READ THIS--------------------------------- \n\n" +
                                "UNITS: Distance is in Km --- Area is in m^2 ---- Contracted Power is in KWh\n\n" +
                                "Efficiency ranges:[A+++,A++,A+,A,B,C,D], except Refrigerator doesnt have D efficiency\n\n"+
                                "Other answers to the questions should be 'yes' or 'no'";

    @FXML
    private Button buttonStart;

    @FXML
    private Button buttonStart2;

    @FXML
    private TextArea middleTextArea;

    @FXML
    private Button helpButton2;

    @FXML
    public void onClickStartAnalysis(ActionEvent actionEvent) throws IOException, InterruptedException {
        if(count>1) {
            new Thread(App::runEngine).start();

            synchronized (App.lock1) {
                App.lock1.wait();
            }
        }
        App.setPopupScene("questions");
        this.buttonStart2.setDisable(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        StringBuilder a = new StringBuilder();
        this.buttonStart2.setDisable(true);
        if(App.conclusionsList!=null) {
            for (String b : App.conclusionsList) {
                a.append(b);
                a.append("\n\n");
            }
            this.middleTextArea.setText(a.toString());
        }
        if(count>0) {
            new Thread(App::runEngine).start();

            synchronized (App.lock1) {
                try {
                    App.lock1.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        count++;

        helpButton2.setOnAction((event) ->{
            this.middleTextArea.setText(helpText);
        });
    }

    @FXML
    public void onClickContinueAnalysis(ActionEvent actionEvent) throws IOException {
        App.setPopupScene("questions");
    }

    @FXML
    void switchConsumerOnAction(ActionEvent event) throws IOException {
        App.setRoot("consumidor");
    }
}