package org.engcia.services;

import org.engcia.model.AppliancesEfficiency;
import org.engcia.model.ContractedPower;
import org.engcia.model.Questions;
import org.engcia.model.SolarPanel;
import org.engcia.model.common.NumericalEvidence;

import java.util.HashMap;
import java.util.Map;

public class Calculate {



    public static Double calculateContractedPower(){
        Double maxConsumption = EvidenceHelper.getValueNumerical("Max Consumption");
        for(ContractedPower a: ContractedPower.values()){
            if(a.getValue() >=maxConsumption){
                return a.getValue();
            }
        }
        return maxConsumption;
    }

    public static int numberOfPanels(){
        double area = EvidenceHelper.getValueNumerical(Questions.INSTALL_SPACE);
        return (int)Math.floor(area/SolarPanel.AREA);
    }

    public static Double calculateSolarPanelProduction(){
        return (SolarPanel.CAPACITY/SolarPanel.AREA) *EvidenceHelper.getValueNumerical(Questions.INSTALL_SPACE);
    }

    public static Double calculateEnergyToSell(){
        double average_consumption =  EvidenceHelper.getValueNumerical("Average Consumption");
        double toSell =calculateSolarPanelProduction()-average_consumption;
        return (toSell<0)?0:toSell;
    }

    //Criar metodo para calcular o minimo que se tem de baixar de energia para mudar de potencia contratada
    public static String calculateMinimiumOneLayer(){

        String ce1 = AppliancesEfficiency.getValue("Washing Machine",EvidenceHelper.getCategoricalEvidence("Washing Machine Efficiency").getValue());
        String ce2 = AppliancesEfficiency.getValue("Dish washer",EvidenceHelper.getCategoricalEvidence("Dish washer Efficiency").getValue());
        String ce3 = AppliancesEfficiency.getValue("Refrigerator",EvidenceHelper.getCategoricalEvidence("Refrigerator Efficiency").getValue());

        NumericalEvidence contracted = EvidenceHelper.getNumericalEvidence("Contracted Power");
        Map<String,Double> reduce_wm = new HashMap<>();
        Map<String,Double> reduce_dw = new HashMap<>();
        Map<String,Double> reduce_r = new HashMap<>();


        double maxConsumptionWM = EvidenceHelper.getValueNumerical("Washing Machine Max");
        double maxConsumptionDW = EvidenceHelper.getValueNumerical("Dish washer Max");
        double maxConsumptionR =EvidenceHelper.getValueNumerical("Refrigerator Max");

        double maxConsumption = EvidenceHelper.getValueNumerical("Average Consumption");
            for (int i = 0; i < AppliancesEfficiency.washingMachine.length; i++) {
                if (Double.parseDouble(ce1) > Double.parseDouble(AppliancesEfficiency.washingMachine[i]))
                    reduce_wm.put(AppliancesEfficiency.effciency[i], (1-(Double.parseDouble(AppliancesEfficiency.washingMachine[i])/Double.parseDouble(ce1))) * maxConsumptionWM);
            }
            for (int i = 0; i < AppliancesEfficiency.dishWasher.length; i++) {
                if (Double.parseDouble(ce2) > Double.parseDouble(AppliancesEfficiency.dishWasher[i]))
                    reduce_dw.put(AppliancesEfficiency.effciency[i], (1-(Double.parseDouble(AppliancesEfficiency.dishWasher[i])/Double.parseDouble(ce2))) *  maxConsumptionDW);
            }
            for (int i = 0; i < AppliancesEfficiency.refrigerator.length; i++) {
                if (Double.parseDouble(ce3) > Double.parseDouble(AppliancesEfficiency.refrigerator[i]))
                    reduce_r.put(AppliancesEfficiency.effciency[i], (1-(Double.parseDouble(AppliancesEfficiency.refrigerator[i])/Double.parseDouble(ce3))) * maxConsumptionR);
            }

            double min = 99999;
            String temp = "";
            for (Map.Entry<String, Double> a : reduce_r.entrySet()) {
                for (Map.Entry<String, Double> b : reduce_dw.entrySet()) {
                    for (Map.Entry<String, Double> c : reduce_wm.entrySet()) {
                        double saved = a.getValue() + b.getValue() + c.getValue();
                        if (maxConsumption-saved < closestContracted(contracted.getValue()) &&
                                saved < min ) {
                            min = saved;
                            temp = "Refrigerator:" + a.getKey() + " Dish Washer:" + b.getKey() + " Washing Machine:" + c.getKey();
                        }
                    }
                }
            }
        return temp;
    }
    public static Double closestContracted(Double contracted){
        double max =0;
        for(ContractedPower a: ContractedPower.values()){
            if(a.getValue() < contracted && a.getValue() > max){
                max = a.getValue();
            }
        }
        return max;
    }

}
