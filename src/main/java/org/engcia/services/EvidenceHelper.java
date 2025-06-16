package org.engcia.services;

import org.engcia.App;
import org.engcia.model.common.CategoricalEvidence;
import org.engcia.model.common.NumericalEvidence;
import org.kie.api.runtime.ClassObjectFilter;

import java.util.Collection;

public class EvidenceHelper {
    @SuppressWarnings("unchecked")
    static Collection<CategoricalEvidence> evidences = (Collection<CategoricalEvidence>) App.KS.getObjects(new ClassObjectFilter(CategoricalEvidence.class));
    @SuppressWarnings("unchecked")
    static Collection<NumericalEvidence> evidences1 = (Collection<NumericalEvidence>) App.KS.getObjects(new ClassObjectFilter(NumericalEvidence.class));

    public static String getValue(String evidence){
        for(CategoricalEvidence e : evidences){
            if(e.getDescription().compareTo(evidence)==0){
                return e.getValue();
            }
        }
        return "";
    }
    public static double getValueNumerical(String evidence){
        for(NumericalEvidence e : evidences1){
            if(e.getDescription().compareTo(evidence)==0){
                return e.getValue();
            }
        }
        return 0;
    }
    public static NumericalEvidence getNumericalEvidence(String evidence){
        for(NumericalEvidence e : evidences1){
            if(e.getDescription().compareTo(evidence)==0){
                return e;
            }
        }
        return null;
    }

    public static CategoricalEvidence getCategoricalEvidence(String evidence){
        for(CategoricalEvidence e : evidences){
            if(e.getDescription().compareTo(evidence)==0){
                return e;
            }
        }
        return null;
    }
}
