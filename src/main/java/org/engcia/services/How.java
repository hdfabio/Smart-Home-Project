package org.engcia.services;

import org.engcia.model.Justification;
import org.engcia.model.common.Deduction;
import org.engcia.model.common.Fact;

import java.util.Map;



public class How {
    private Map<Integer, Justification> justifications;

    public How(Map<Integer, Justification> justifications) {
        this.justifications = justifications;
    }

    public String getHowExplanation(Integer factNumber) {
        return (getHowExplanation(factNumber, 0));
    }

    private String getHowExplanation(Integer factNumber, int level) {
        StringBuilder sb = new StringBuilder();
        Justification j = justifications.get(factNumber);
        if (j != null) { // justification for Fact factNumber was found
            sb.append(getIdentation(level));
            sb.append(j.getConclusion() + " was obtained by rule " + j.getRuleName() + " because");
            sb.append('\n');
            int l = level + 1;
            for (Fact f : j.getLhs()) {
                sb.append(getIdentation(l));
                sb.append(f);
                sb.append('\n');
                if (f instanceof Deduction) {
                    String s = getHowExplanation(f.getId(), l + 1);
                    sb.append(s);
                }
            }
        }

        return sb.toString();
    }

    private String getIdentation(int level) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i < level; i++) {
            sb.append('\t');
        }
        return sb.toString();
    }
}
