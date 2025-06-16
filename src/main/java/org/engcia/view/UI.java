package org.engcia.view;

import org.engcia.App;
import org.engcia.controller.QuestionsController;
import org.engcia.model.common.CategoricalEvidence;
import org.engcia.model.common.NumericalEvidence;
import org.kie.api.runtime.ClassObjectFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

public class UI {
    public static BufferedReader br;
    public static QuestionsController questionsController;

    public static void uiInit() {
        br = new BufferedReader(new InputStreamReader(System.in));

    }

    public static void uiClose() {
        if (br != null) {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized static boolean answer(String question, String ev, String v) throws IOException {
        @SuppressWarnings("unchecked")
        Collection<CategoricalEvidence> evidences = (Collection<CategoricalEvidence>) App.KS.getObjects(new ClassObjectFilter(CategoricalEvidence.class));
        boolean questionFound = false;
        CategoricalEvidence evidence = null;
        for (CategoricalEvidence e: evidences) {
            if (e.getDescription().compareTo(ev) == 0) {
                questionFound = true;
                evidence = e;
                break;
            }
        }
        if (questionFound) {
            if (evidence.getValue().compareTo(v) == 0) {
                App.agendaEventListener.addLhs(evidence);
                return true;
            } else {
                return false;
            }
        }
        System.out.print(question + "? ");

        String value=readLine();
//
//
        CategoricalEvidence e = new CategoricalEvidence(ev, value);
        App.KS.insert(e);

        if (value.compareTo(v) == 0 ) {
            App.agendaEventListener.addLhs(e);
            return true;
        } else {
            return false;
        }

    }
    public synchronized static boolean answerValueCategorical(String question, String ev) throws IOException {
        @SuppressWarnings("unchecked")
        Collection<CategoricalEvidence> evidences = (Collection<CategoricalEvidence>) App.KS.getObjects(new ClassObjectFilter(CategoricalEvidence.class));
        boolean questionFound = false;
        CategoricalEvidence evidence = null;
        for (CategoricalEvidence e: evidences) {
            if (e.getDescription().compareTo(ev) == 0) {
                questionFound = true;
                evidence = e;
                break;
            }
        }

        System.out.print(question + "? ");

        String value=readLine();

        if (questionFound) {
            if (evidence.getValue().compareTo(value) == 0) {
                App.agendaEventListener.addLhs(evidence);
                return true;
            } else {
                return false;
            }
        }
        CategoricalEvidence e = new CategoricalEvidence(ev, value);
        App.KS.insert(e);
        return true;

    }
    public synchronized static boolean answerValueNumerical(String question, String ev) throws IOException {
        @SuppressWarnings("unchecked")
        Collection<NumericalEvidence> evidences = (Collection<NumericalEvidence>) App.KS.getObjects(new ClassObjectFilter(NumericalEvidence.class));
        boolean questionFound = false;
        NumericalEvidence evidence = null;
        for (NumericalEvidence e: evidences) {
            if (e.getDescription().compareTo(ev) == 0) {
                questionFound = true;
                evidence = e;
                break;
            }
        }
        System.out.print(question + "? ");

        double value =0;
        try {
             value = Double.parseDouble(readLine());
        }catch (NumberFormatException e){
        }
        if (questionFound) {
            if (evidence.getValue()==(value)) {
                App.agendaEventListener.addLhs(evidence);
                return true;
            } else {
                return false;
            }
        }
        NumericalEvidence e = new NumericalEvidence(ev, value);
        App.KS.insert(e);

        return true;

    }

    public static String readLine() {
        String input = "";

        try {
            input = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }

}
