package org.engcia.services;

import org.drools.compiler.compiler.DrlParser;
import org.drools.compiler.compiler.DroolsParserException;
import org.drools.compiler.lang.descr.*;
import org.kie.internal.builder.conf.LanguageLevelOption;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RuleUtils {
    public static final String FACTS_PACKAGE = "org.engcia.model.common"; // package where reside classes used to define facts
    private static final List<String> DRL_PATHS; // paths of drl files included in the project
    private final Logger LOG = LoggerFactory.getLogger(RuleUtils.class);
    private static final Map<String,String> dynamicQueries = new HashMap<>();
    private static final List<RuleDescr> ruleDescr;

    static {
        String baseDir = System.getProperty("user.dir");
        DRL_PATHS = findDrlFiles(new File(baseDir + "/src"));
        ruleDescr = getRulesDescriptionFromDRL();
    }

    private static List<String> findDrlFiles(File baseDir) {
        ArrayList<String> lst = new ArrayList<String>();
        findFile(baseDir, lst);
        return lst;
    }

    private static void findFile(File file, List<String> lst) {
        final String name = "drl";
        File[] list = file.listFiles();
        if(list!=null)
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findFile(fil, lst);
                }
                else if (fil.getName().endsWith(name.toLowerCase())) {
                    lst.add(fil.getParentFile() + "/" + fil.getName());
                }
            }
    }

    public static Map<String,String> getDynamicQueries() {
        return dynamicQueries;
    }

    public static List<RuleDescr> getRulesDescriptionFromDRL() {
        String drl;
        StringBuffer drlBuffer = new StringBuffer();

        try {
            for (String path: DRL_PATHS) {
                drlBuffer.append(new String(Files.readAllBytes(Paths.get(path)), Charset.defaultCharset()));
            }
            drl = drlBuffer.toString();
        } catch (IOException e) {
            throw new RuntimeException("File not found", e);
        }

        DrlParser parser = new DrlParser(LanguageLevelOption.DRL6);
        PackageDescr pkgDescr;
        try {
            pkgDescr = parser.parse(null, drl);
        } catch (DroolsParserException e) {
            throw new RuntimeException("DRL parse error", e);
        } catch (NullPointerException e) {
            throw new RuntimeException("Path incorrectly defined: ", e);
        }

        if (pkgDescr == null) {
            throw new RuntimeException("Path incorrectly defined: ");
        }
        return pkgDescr.getRules();
    }

    public static List<PatternDescr> getRuleConditions(String ruleName, List<RuleDescr> rulesDescr) {
        List<PatternDescr> lst = new ArrayList<>(rulesDescr.stream().
                filter(r -> r.getName().equals(ruleName)).
                map(RuleDescr::getLhs).findFirst().get().getAllPatternDescr());
        return lst;
    }

    public static List<String> getRulesObtainingConclusion(String conclusion, List<RuleDescr> rulesDescr) {
        String regex = "(?s).*" + conclusion + "\\(" + ".*" + "\\)" + ".*";
        List<String> lst = rulesDescr.stream().
                filter(r -> r.getConsequence().toString().
                        replaceAll("\\s+","").
                        matches(regex)).
                        map(RuleDescr::getName).collect(Collectors.toList());
        return lst;

    }

    public static Set<String> getAllRuleConditionsList(List<RuleDescr> rulesDescr) {
        Set<String> set = rulesDescr.stream().
                map(RuleDescr::getLhs).
                map(AndDescr::getAllPatternDescr).
                flatMap(List::stream).
                map(p -> p.getObjectType() + "(" + p.getDescrs().stream().
                        map(BaseDescr::getText).
                        reduce((s1,s2) -> s1 + " , " + s2).get() + ")").
                collect(Collectors.toSet());
        return set;
    }

    public static Set<String> getAllRuleActionsList(List<RuleDescr> rulesDescr) {
        Set<String> set = rulesDescr.stream().
                map(RuleDescr::getConsequence).
                map(Object::toString).
                map(RuleUtils::getConstructorCalls).flatMap(Set::stream).
                collect(Collectors.toSet());
        return set;
    }

    private static Set<String> getConstructorCalls(String consequent) {
        Set<String> set = new HashSet();
        Pattern pattern = Pattern.compile("(?<=new\\s).*?(?=;)");
        Matcher matcher = pattern.matcher(consequent);
        while (matcher.find()) {
            String str = matcher.group().replaceAll(" ", "");
            set.add(str);
        }
        return set;
    }

    private static String getImportsString() {
        Reflections reflections = new Reflections(FACTS_PACKAGE, new SubTypesScanner(false));
        String str1;
        String str2;
        try {
            Set<Class> setObjects = new HashSet<>(reflections.getSubTypesOf(Object.class));
            str1 = setObjects.stream().map(Class::getName).collect(Collectors.joining(";\nimport ", "import ", ";\n"));
            Set<Class> setEnums = new HashSet<>(reflections.getSubTypesOf(Enum.class));
            str2 = setEnums.stream().map(Class::getName).collect(Collectors.joining(";\nimport ", "import ", ";\n"));
        } catch (Exception e) {
            throw new RuntimeException("FACTS_PACKAGE incorrectly defined: " + FACTS_PACKAGE, e);
        }

        return str1 + str2;
    }

    public static String generateQueries() {
        Set<String> condSet = getAllRuleConditionsList(ruleDescr);

        Set<String> concSet = getAllRuleActionsList(ruleDescr);
        Set<String> consSet = concSet.stream().map(c -> {
            try {
                return convertConstructorToDRL(c);
            } catch (Exception e) {
                System.out.println(e.toString());
                System.exit(0);
            }
            return null;
        }).collect(Collectors.toSet());

        condSet.addAll(consSet);

        StringBuffer drl = new StringBuffer(getImportsString() + "\n" );
        int n = 0;
        for (String c: condSet) {
            String queryName = "confirmCondition" + ++n;
            drl.append("query ").append(queryName).append("\n").append("\t").append(c).append("\n").
                    append("end").append("\n");
            dynamicQueries.put(c,queryName);
        }

        return drl.toString();
    }

    public static boolean isBasicFact(String Fact, List<RuleDescr> rulesDescr) {
        // Search 'Fact' in rules' RHS; if none occurrence is found, return true

        // Get fact name (functor) from 'Fact':
        final String functor = Fact.replaceAll("\\s+","").split("\\(")[0];
        return !rulesDescr.stream().
                map(RuleDescr::getConsequence).
                map(c -> c.toString()).
                anyMatch(s -> s.contains(functor));
    }

    public static String getRuleConsequence(String ruleName, List<RuleDescr> rulesDescr) {
        String rhs = rulesDescr.stream().
                filter(r -> r.getName().equals(ruleName)).
                map(r -> r.getConsequence().toString()).
                findFirst().orElse("");
        return rhs;
    }

    public static List<String> getConclusionFromRhs(String rhs, String functor) {
        return getAllMatches(rhs, functor + "\\(.*\\)");
    }

    private static List<String> getAllMatches(String text, String regex) {
        List<String> matches = new ArrayList<String>();
        Matcher m = Pattern.compile("(?=(" + regex + "))").matcher(text);
        while(m.find()) {
            matches.add(m.group(1));
        }
        return matches;
    }

    public static String convertDRLPatternToConstructor(PatternDescr patt) throws Exception {
        final String objectType = patt.getObjectType();
        StringBuilder conclusion = new StringBuilder(objectType + "(");

        Map<String,String> map = patt.getDescrs().stream().
                map(BaseDescr::getText).
                map(s -> s.replaceAll("\\s+","")).
                collect(Collectors.toMap(k->k.split("[=:<>]{1,3}")[0],v->v.split("[=:<>]{1,3}")[1]));

        // Create constructor call as a string
        String[] parameters = null;
        Class type = Class.forName(FACTS_PACKAGE + "." + objectType); // fact classes must be all in the same package
        parameters = getConstructorParameters(type); // fact classes must be all in the same package

        if (parameters != null)
            conclusion.append(Arrays.stream(parameters).map(map::get).reduce((p1, p2) -> p1 + "," + p2).get());

        conclusion.append(")");

        return conclusion.toString();
    }

    public static String convertConstructorToDRL(String constructor) throws Exception {
        String objectType = constructor.substring(0, constructor.indexOf('(')).
                replaceAll("\\s+","");
        StringBuffer DRL = new StringBuffer(objectType);
        String[] constructorArgs = constructor.substring(constructor.indexOf('(')+1, constructor.indexOf(')')).
                replaceAll("\\s+","").split(",");
        String[] parameters = null;
        Class type = null;
        try {
            type = Class.forName(FACTS_PACKAGE + "." + objectType);
        } catch (ClassNotFoundException e) {
            throw new Exception("Unknown class: " + FACTS_PACKAGE + "." + objectType +
                    "\nCheck FACTS_PACKAGE constant: " + FACTS_PACKAGE +
                    "\nCheck conclusion functor: " + objectType, e);
        }
        parameters = getConstructorParameters(type); // fact classes must be all in the same package

        if (parameters != null) {
            if (parameters.length != constructorArgs.length) {
                throw new Exception("Invalid number of parameters in conclusion " + objectType);
            }
            Stream<String> stream = Stream.empty();
            int i=0;
            for (String par: parameters) {
                stream = Stream.concat(stream, Stream.of(par + " == " + constructorArgs[i++]));
            }
            String args = stream.reduce((s1,s2)->s1 + " , " + s2).get();

            DRL.append("(").append(args).append(")");
        }

        return DRL.toString();
    }

    private static String[] getConstructorParameters(Class c) throws Exception {
        Constructor[] allConstructors = c.getConstructors();
        if (allConstructors.length == 0) {
            throw new Exception("Undefined constructor");
        }
        Parameter[] params = allConstructors[0].getParameters();
        return Arrays.stream(params).map(Parameter::getName).toArray(String[]::new);
    }

    private static String getCondElem(String elements, int pos) {
        return elements.split("[=:<>]{1,3}]")[pos]; // occurs at least 1 time but less than 3 times
    }

    public static boolean existRule(String ruleName, List<RuleDescr> rulesDescr) {
        return rulesDescr.stream().map(RuleDescr::getName).anyMatch(r -> r.equals(ruleName));
    }

    public static Optional<RuleDescr> getRuleDescr(String ruleName, List<RuleDescr> rulesDescr) {
        return rulesDescr.stream().filter(rule -> rule.getName().equals(ruleName)).findFirst();
    }

}
