package org.engcia.model;

public class ConsumptionPeriod {
    private String description;
    private double value;
    private int day;
    private int dayPeriod;

    public ConsumptionPeriod(String description, double value, int day, int dayPeriod) {
        this.description = description;
        this.value = value;
        this.day = day;
        this.dayPeriod = dayPeriod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getDayPeriod() {
        return dayPeriod;
    }

    public void setDayPeriod(int dayPeriod) {
        this.dayPeriod = dayPeriod;
    }
}
