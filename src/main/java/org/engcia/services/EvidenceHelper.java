package org.engcia.services;

import org.engcia.App;
import org.engcia.model.common.CategoricalEvidence;
import org.engcia.model.common.NumericalEvidence;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;

import java.util.Collection;
import java.util.Collections;

public class EvidenceHelper {

    @SuppressWarnings("unchecked")
    private static Collection<CategoricalEvidence> categoricalFacts() {
        KieSession session = App.KS;
        if (session == null) {
            return Collections.emptyList();
        }
        return (Collection<CategoricalEvidence>) session.getObjects(new ClassObjectFilter(CategoricalEvidence.class));
    }

    @SuppressWarnings("unchecked")
    private static Collection<NumericalEvidence> numericalFacts() {
        KieSession session = App.KS;
        if (session == null) {
            return Collections.emptyList();
        }
        return (Collection<NumericalEvidence>) session.getObjects(new ClassObjectFilter(NumericalEvidence.class));
    }

    public static String getValue(String evidence) {
        for (CategoricalEvidence e : categoricalFacts()) {
            if (e.getDescription().compareTo(evidence) == 0) {
                return e.getValue();
            }
        }
        return "";
    }

    public static double getValueNumerical(String evidence) {
        for (NumericalEvidence e : numericalFacts()) {
            if (e.getDescription().compareTo(evidence) == 0) {
                return e.getValue();
            }
        }
        return 0;
    }

    public static NumericalEvidence getNumericalEvidence(String evidence) {
        for (NumericalEvidence e : numericalFacts()) {
            if (e.getDescription().compareTo(evidence) == 0) {
                return e;
            }
        }
        return null;
    }

    public static CategoricalEvidence getCategoricalEvidence(String evidence) {
        for (CategoricalEvidence e : categoricalFacts()) {
            if (e.getDescription().compareTo(evidence) == 0) {
                return e;
            }
        }
        return null;
    }
}
