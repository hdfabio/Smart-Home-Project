package org.engcia.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Appliances {
    @JsonProperty("Periods")
    public Periods periods;
    @JsonProperty("Total Consumption")
    public TotalConsumption totalConsumption;
    @JsonProperty("Dish washer")
    public DishWasher dishWasher;
    @JsonProperty("Washing Machine")
    public WashingMachine washingMachine;
    @JsonProperty("Dryer")
    public Dryer dryer;
    @JsonProperty("Water heater")
    public WaterHeater waterHeater;
    @JsonProperty("TV")
    public TV tV;
    @JsonProperty("Microwave")
    public Microwave microwave;
    @JsonProperty("Kettle")
    public Kettle kettle;
    @JsonProperty("Lighting")
    public Lighting lighting;
    @JsonProperty("Refrigerator")
    public Refrigerator refrigerator;

    public class DishWasher{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class Dryer{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class Kettle{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class Lighting{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class Microwave{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class Periods{
        @JsonProperty("0")
        public String _0;
        @JsonProperty("1")
        public String _1;
    }

    public class Refrigerator{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }


    public class TotalConsumption{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class TV{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class WashingMachine{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }

    public class WaterHeater{
        @JsonProperty("0")
        public double _0;
        @JsonProperty("1")
        public double _1;
    }


}
