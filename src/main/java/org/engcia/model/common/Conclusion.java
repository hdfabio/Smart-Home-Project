package org.engcia.model.common;

import org.engcia.App;

public class Conclusion extends Fact {

    private String description;

    public Conclusion(String description) {
        this.description = description;
        App.agendaEventListener.addRhs(this);
    }


    public String getDescription() {
        return description;
    }

    public String toString() {
        return ("Conclusion: " + description);
    }

}
