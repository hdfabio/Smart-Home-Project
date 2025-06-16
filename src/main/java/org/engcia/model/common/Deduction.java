package org.engcia.model.common;

import org.engcia.App;

public class Deduction extends Fact {
    private String description;


    public Deduction(String description) {
        this.description = description;
        App.agendaEventListener.addRhs(this);
    }


    public String getDescription() {
        return description;
    }

    public String toString() {
        return ("Deduction:"+description);
    }
}