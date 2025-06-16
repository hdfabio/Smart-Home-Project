package org.engcia.model.common;

public class CategoricalEvidence extends Fact {

    private String description;
    private String value;

    public CategoricalEvidence(String description, String value) {
        this.description = description;
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return (description + " = " + value);
    }

}

