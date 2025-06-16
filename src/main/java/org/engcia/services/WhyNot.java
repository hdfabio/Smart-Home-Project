package org.engcia.services;

import org.drools.compiler.lang.descr.BaseDescr;
import org.drools.compiler.lang.descr.PatternDescr;
import org.drools.compiler.lang.descr.RuleDescr;
import org.drools.core.util.StringUtils;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.QueryResults;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.engcia.services.RuleUtils.*;


public class WhyNot {
    private final List<RuleDescr> rulesDescr;
    private final KieSession session;
    private final Map<String,String> dynamicQueries;

    public static String getExplanation(KieSession session, String expectedConclusion) {
        WhyNot wn = new WhyNot(session, RuleUtils.getDynamicQueries());
        return wn.getWhyNotExplanation(expectedConclusion);
    }

    private WhyNot(KieSession session, Map<String,String> dynamicQueries) {
        this.session = session;
        this.dynamicQueries = dynamicQueries;

        this.rulesDescr = getRulesDescriptionFromDRL();
    }

    private String getWhyNotExplanation(String expectedConclusion) {
        StringBuffer explanation = new StringBuffer();

        String DRLconclusion = null;
        try {
            DRLconclusion = RuleUtils.convertConstructorToDRL(expectedConclusion);
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }

        try {
            generateExplanation(expectedConclusion, DRLconclusion, explanation,0);
        } catch (Exception e) {
            System.out.println(e.toString());
            System.exit(0);
        }

        return explanation.toString();
    }

    private void generateExplanation(String expectedConclusion, String DRLConclusion, StringBuffer explanation, int level) throws Exception {
        String tabs = StringUtils.repeat("\t", level*2);

        // expectedConclusion is a basic fact
        if (isBasicFact(expectedConclusion, rulesDescr)) {
            explanation.append(tabs);
            explanation.append(DRLConclusion);
            explanation.append(" is a basic fact not defined\n");

            return;
        }

        final String functor = expectedConclusion.split("\\(")[0];
        List<String> rules = getRulesObtainingConclusion(functor, rulesDescr);

        for (String ruleName : rules) {

            if (conclusionFromRuleDoesNotExist(ruleName, functor, DRLConclusion)) {
                explanation.append(tabs);
                explanation.append(DRLConclusion);
                explanation.append(" was not concluded because rule ");
                explanation.append(ruleName);
                explanation.append(" did not fire due to:\n");

                int condNoLocal = 1;
                List<PatternDescr> conditions = getRuleConditions(ruleName, rulesDescr);
                for (PatternDescr patt : conditions) {
                    String drlCondition = patt.getObjectType() + "(" + patt.getDescrs().stream().
                            map(BaseDescr::getText).reduce((s1, s2) -> s1 + " , " + s2).get() + ")";

                    if (conditionIsFalse(drlCondition)) {
                        explanation.append(tabs).append("\t").append("Rule condition ").append(condNoLocal).append(": ").
                                append(drlCondition).append(" is false\n");
                        // convert patt to conclusion (constructor format): conc
                        String conc = convertDRLPatternToConstructor(patt);
                        generateExplanation(conc, drlCondition, explanation,level + 1);
                    }
                    condNoLocal++;
                }
            }
        }
    }

    // Return true if a conclusion from rule's RHS would match 'DRLConclusion' pattern (condition)
    // If rule 'ruleName' was been triggered, a conclusion matching 'DRLConclusion' pattern would be achieved
    private boolean conclusionFromRuleDoesNotExist(String ruleName, String functor, String DRLConclusion) throws Exception {

        // get consequence of rule 'ruleName'
        String rhs = getRuleConsequence(ruleName, rulesDescr);
        // get conclusion(s) with functor 'functor' from rhs of rule 'ruleName'
        List<String> ruleConclusions = getConclusionFromRhs(rhs, functor);
        if (ruleConclusions.size() == 0) { // None of the rule conclusions has 'functor' as functor
            return false;
        }
        boolean exists = false;
        for (String conclusion: ruleConclusions) {
            // Create object from ruleConclusion
            Object fact = createObject(functor, conclusion);

            // Insert object in WM
            FactHandle fh = this.session.insert(fact);

            QueryResults q = null;
            try {
                q = session.getQueryResults(dynamicQueries.get(DRLConclusion)); // DRLConclusion
            } catch (RuntimeException e) {
                throw new Exception("Undefined query: " + DRLConclusion);
            }

            // Remove fact from WM after query check
            this.session.delete(fh);

            if (q.size() > 0) {
                exists = true;
                break;
            }
        }

        return exists;
    }

    private Object createObject(String functor, String conclusion) throws Exception {
        String[] conclusionArgs = conclusion.substring(conclusion.indexOf('(')+1, conclusion.indexOf(')')).
                replaceAll("\\s+","").split(",");

        Class type = Class.forName(FACTS_PACKAGE + "." + functor); // fact classes must be all in the same package

        Constructor[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 0) {
            throw new Exception("Constructor not found: " + FACTS_PACKAGE + "." + functor);
        }
        Object[] arguments = new Object[constructors[0].getParameterCount()];
        Class<?>[] pTypes = constructors[0].getParameterTypes();
        Method method = null;
        try {
            for (int i = 0; i < arguments.length; i++) {
                if (pTypes[i].equals(String.class)) { // String
                    arguments[i] = conclusionArgs[i].replaceAll("\"", "");
                } else if (pTypes[i].equals(int.class) || pTypes[i].equals(float.class) || pTypes[i].equals(double.class)) {
                        method = pTypes[i].getDeclaredMethod("valueOf");
                        arguments[i] = method.invoke(conclusionArgs[i]);
                } else if (pTypes[i].equals(String.class)) { // Enum types
                        method = pTypes[i].getMethod("valueOf", String.class);
                        arguments[i] = method.invoke(null, conclusionArgs[i].substring(conclusionArgs[i].lastIndexOf('.') + 1));
                }
            }
        } catch (IllegalAccessException e) {
            throw new Exception("Forbidden method call: " + method.getName(), e);
        } catch (InvocationTargetException e) {
            throw new Exception("Invoking method by reflection: " + method.getName(), e);
        } catch (NoSuchMethodException e) {
            throw new Exception("Unknown method: " + method.getName(), e);
        }

        Object object = null;
        try {
            object = constructors[0].newInstance(arguments);
        } catch (InstantiationException e) {
            throw new Exception("Cannot instantiate class: " + constructors[0].getName(), e);
        } catch (IllegalAccessException e) {
            throw new Exception("Forbidden access to class: " + constructors[0].getName(), e);
        } catch (InvocationTargetException e) {
            throw new Exception("Invoking constructor by reflection: " + constructors[0].getName(), e);
        }

        return object;
    }

    private boolean conditionIsFalse(String drlCondition) {
        QueryResults q = session.getQueryResults( dynamicQueries.get(drlCondition) );
        return q.size() == 0;
    }

}
