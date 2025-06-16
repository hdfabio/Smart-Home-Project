package org.engcia.model.common;

public class NumericalEvidence extends Fact{
    private String description;
    private double value;

    public NumericalEvidence(String description, double value) {
        this.description = description;
        this.value = value;
    }
    public void addValue(double value){
        this.value+= value;
    }
    public void removeValue(double value){
        this.value-= value;
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
    public String toString() {
        return (description + " = " + value);
    }
}
