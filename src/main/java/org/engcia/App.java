package org.engcia;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.engcia.model.Appliances;
import org.engcia.model.ConsumptionPeriod;
import org.engcia.model.Justification;
import org.engcia.model.Questions;
import org.engcia.model.common.CategoricalEvidence;
import org.engcia.model.common.Conclusion;
import org.engcia.model.common.NumericalEvidence;
import org.engcia.services.Calculate;
import org.engcia.services.FileReader;
import org.engcia.services.How;
import org.engcia.services.TrackingAgendaEventListener;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.LiveQuery;
import org.kie.api.runtime.rule.Row;
import org.kie.api.runtime.rule.ViewChangedEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class App extends Application {
    public static KieSession KS;
    public static KieContainer kContainer;
    public static BufferedReader BR;
    public static TrackingAgendaEventListener agendaEventListener;
    public static Map<Integer, Justification> justifications;

    public static List<String> conclusionsList;

    public static Map<String,List<ConsumptionPeriod>> consumptionPeriods;
    public static Scene scene;


    public static final Object lock = new Object();
    public static final Object lock1 = new Object();

    public static List<String> questionListCategorical;
    public static List<String> questionListNumerical;

    public static Stage popupStage;

    public static Stage stage;

    public static int conclusionCounter;

    public static String consumerName;
    @Override
    public void start(Stage stage) throws IOException {
        App.stage = stage;
        scene = new Scene(loadFXML("consumidor"));
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {

        if(fxml.equals("primary")) {
            scene.getWindow().setWidth(840);
            scene.getWindow().setHeight(480);
        }else{
            scene.getWindow().setHeight(178);
            scene.getWindow().setWidth(283);
        }
        scene.getWindow().centerOnScreen();
        scene.setRoot(loadFXML(fxml));
    }

     public static void setPopupScene(String fxml) throws IOException {
        popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initOwner(stage);
        Scene popupScene = new Scene(loadFXML(fxml),300,200);

        popupStage.setScene(popupScene);
        popupStage.show();
    }
    public static void setMainScene(String fxml) throws IOException {
        App.stage = new Stage();
        Scene popupScene = new Scene(loadFXML(fxml),300,200);

        App.stage.setScene(popupScene);
        App.stage.show();
    }

    public static Stage getCurrentStage(Node node){
       return (Stage) node.getScene().getWindow();
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
    public static void main(String[] args) {
//        new Thread(App::runEngine).start();
        launch();
    }

    public static void populateConsumptionPeriods(){
        Map<String, List<Double>> totalConsumptions = FileReader.readXLSX("src/main/resources/org/engcia/Book1.xlsx");
        int day =1;
        int period =1;
        consumptionPeriods = new HashMap<>();
        for(Map.Entry<String, List<Double>> a : totalConsumptions.entrySet()){
            if(period== 96){
                period=1;
                day++;
            }
            if(!a.getKey().equals("")) {
                consumptionPeriods.put(a.getKey(), new ArrayList<>());
                for (Double a1 : a.getValue()) {
                    consumptionPeriods.get(a.getKey()).add(new ConsumptionPeriod(a.getKey(), a1, day, period));
                    period++;
                    }
                }
            }
        }
        public static void populateQuestionList(){
            questionListCategorical = new ArrayList<>();
            questionListNumerical = new ArrayList<>();
            questionListCategorical.add(Questions.BI_SHEDULE);
            questionListCategorical.add(Questions.INVEST_RENEWABLE_ENERGY);
            questionListCategorical.add(Questions.SELL_ENERGY);
            questionListCategorical.add(Questions.SWITCH_APPLICANCES);
            questionListCategorical.add("Dish washer Efficiency");
            questionListCategorical.add("Dish washer is programmable");
            questionListCategorical.add("Washing Machine Efficiency");
            questionListCategorical.add("Washing Machine is programmable");
            questionListCategorical.add("Refrigerator Efficiency");
            questionListCategorical.add(Questions.LOCOMOTION);
            questionListCategorical.add(Questions.SWITCH_LOCOMOTION);
            questionListCategorical.add(Questions.SLOW_CHARGE);
            questionListNumerical.add("Contracted Power");
            questionListNumerical.add(Questions.INSTALL_SPACE);
            questionListNumerical.add(Questions.DISTANCE);
        }

    public static void populateConsumptionPeriods1() throws IOException {
        ObjectMapper om = new ObjectMapper();
        String consumer_max ="consumer"+App.consumerName+"_max.json";
        String consumer_mean ="consumer"+App.consumerName+"_mean.json";

        Appliances appliancesMax = om.readValue(new File("src/main/resources/org/engcia/"+consumer_max), Appliances.class);
        Appliances appliancesMean = om.readValue(new File("src/main/resources/org/engcia/"+consumer_mean), Appliances.class);

        KS.insert(new NumericalEvidence("Save Contracted Power", 0));

        App.KS.insert(new NumericalEvidence("Max Consumption", (appliancesMax.totalConsumption._1 > appliancesMax.totalConsumption._0) ?appliancesMax.totalConsumption._1:appliancesMax.totalConsumption._0));
        KS.insert(new NumericalEvidence("Best Contracted Power", Calculate.calculateContractedPower()));
        App.KS.insert(new NumericalEvidence("Average Consumption", (appliancesMean.totalConsumption._1+appliancesMean.totalConsumption._0)/2));
        App.KS.insert(new NumericalEvidence("Average Consumption Bi-Schedule", appliancesMean.totalConsumption._0));
        App.KS.insert(new NumericalEvidence("Solar Panel Production", Calculate.calculateSolarPanelProduction()));
        App.KS.insert(new NumericalEvidence("Energy to Sell", Calculate.calculateEnergyToSell()));

        List<String> efAppliance = new ArrayList<>(Arrays.asList("Washing Machine","Dish washer","Refrigerator"));

        App.KS.insert(new NumericalEvidence("Washing Machine Max", (appliancesMax.washingMachine._1 > appliancesMax.washingMachine._0) ?appliancesMax.washingMachine._1:appliancesMax.washingMachine._0));
        App.KS.insert(new NumericalEvidence("Dish washer Max", (appliancesMax.dishWasher._1 > appliancesMax.dishWasher._0) ?appliancesMax.dishWasher._1:appliancesMax.dishWasher._0));
        App.KS.insert(new NumericalEvidence("Refrigerator Max", (appliancesMax.refrigerator._1 > appliancesMax.refrigerator._0) ?appliancesMax.refrigerator._1:appliancesMax.refrigerator._0));

        for(String a : efAppliance){
            KS.insert(new CategoricalEvidence("Appliance", a));
        }

    }
public static void startSession(){
    final KieSession kSession = kContainer.newKieSession("ksession");
    App.KS = kSession;
    //depois de acionado uma regra adicionar às justificacao da regra
    App.agendaEventListener = new TrackingAgendaEventListener();
    kSession.addEventListener(agendaEventListener);
}
    public static void runEngine() {
        try {
            App.justifications = new TreeMap<Integer, Justification>();
            App.conclusionsList = new ArrayList<>();
            conclusionCounter =1;
            // load up the knowledge base
            KieServices ks = KieServices.Factory.get();
            kContainer = ks.getKieClasspathContainer();
            startSession();
            populateQuestionList();
            //populateConsumptionPeriods();
            populateConsumptionPeriods1();
            synchronized (App.lock1){
                App.lock1.notify();
            }
            synchronized (App.lock) {

                try {
                    App.lock.wait();

                ViewChangedEventListener listener = new ViewChangedEventListener() {
                    @Override
                    public void rowDeleted(Row row) {
                    }

                    @Override
                    public void rowInserted(Row row) {
                        Conclusion conclusion = (Conclusion) row.get("$conclusion");
                        System.out.println(">>>" + conclusion.toString());
                        How how = new How(App.justifications);
                        conclusionsList.add(conclusionCounter+++"."+how.getHowExplanation(conclusion.getId()));
                    }

                    @Override
                    public void rowUpdated(Row row) {
                    }
                };

                KS.fireAllRules();
                LiveQuery query = KS.openLiveQuery("Conclusions", null, listener);
                query.close();

                for (String a : conclusionsList) {
                    System.out.println(a);
                }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Thread Interrupted");
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}

