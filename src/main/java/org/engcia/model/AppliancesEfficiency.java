package org.engcia.model;

public class AppliancesEfficiency {
    public static String[] effciency ={"A+++","A++","A+","A","B","C","D"};
    public static String[] washingMachine = {"150", "174", "197", "227", "257", "291", "334"};
    public static String[] dishWasher = {"208", "240", "273", "314", "356", "402","462"};
    public static String[] refrigerator = {"206", "270", "339", "408", "612", "816"};

    public static String getValue(String appliance, String eff){
        switch (eff){
            case "A+++":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[0];
                    case "Dish washer":
                        return dishWasher[0];
                    case "Refrigerator":
                        return refrigerator[0];
                }
            case "A++":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[1];
                    case "Dish washer":
                        return dishWasher[1];
                    case "Refrigerator":
                        return refrigerator[1];
                }
            case "A+":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[2];
                    case "Dish washer":
                        return dishWasher[2];
                    case "Refrigerator":
                        return refrigerator[2];
                }
            case "A":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[3];
                    case "Dish washer":
                        return dishWasher[3];
                    case "Refrigerator":
                        return refrigerator[3];
                }
            case "B":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[4];
                    case "Dish washer":
                        return dishWasher[4];
                    case "Refrigerator":
                        return refrigerator[4];
                }
            case "C":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[5];
                    case "Dish washer":
                        return dishWasher[5];
                    case "Refrigerator":
                        return refrigerator[5];
                }
            case "D":
                switch (appliance) {
                    case "Washing Machine":
                        return washingMachine[6];
                    case "Dish washer":
                        return dishWasher[6];
                }
        }
        return "";
    }
}
