package org.engcia.services;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.engcia.App;
import org.engcia.model.Appliances;
import org.engcia.model.Justification;
import org.engcia.model.QuestionCatalog;
import org.engcia.model.Questions;
import org.engcia.model.common.CategoricalEvidence;
import org.engcia.model.common.Conclusion;
import org.engcia.model.common.NumericalEvidence;
import org.kie.api.KieServices;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class ExpertEngine {
    private static final Logger LOG = LoggerFactory.getLogger(ExpertEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ExpertEngine INSTANCE = new ExpertEngine();

    private final Map<String, JsonNode> liveConsumption = new LinkedHashMap<>();
    private final List<BiConsumer<String, List<String>>> recommendationListeners = new CopyOnWriteArrayList<>();

    private KieContainer kContainer;
    private KieSession session;
    private TrackingAgendaEventListener agendaEventListener;
    private Map<Integer, Justification> justifications;
    private List<String> conclusionsList;
    private List<String> pendingQuestions;
    private String consumerId;
    private int conclusionCounter;

    public static ExpertEngine get() {
        return INSTANCE;
    }

    public static boolean consumerExists(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        return resourceExists("/org/engcia/consumer" + id.trim() + "_mean.json")
                && resourceExists("/org/engcia/consumer" + id.trim() + "_max.json");
    }

    public static List<String> availableConsumers() {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (consumerExists(String.valueOf(i))) {
                ids.add(String.valueOf(i));
            }
        }
        return ids;
    }

    private static boolean resourceExists(String path) {
        return ExpertEngine.class.getResource(path) != null;
    }

    public synchronized void addRecommendationListener(BiConsumer<String, List<String>> listener) {
        recommendationListeners.add(listener);
    }

    public synchronized void startConsumer(String id) throws IOException {
        if (!consumerExists(id)) {
            throw new IOException("Unknown consumer '" + id + "'. Available: " + availableConsumers());
        }
        this.consumerId = id.trim();
        App.consumerName = this.consumerId;
        resetSession();
        loadProfileFacts();
        resetQuestionLists();
        LOG.info("Started expert session for consumer {}", consumerId);
    }

    public synchronized void resetCurrentConsumer() throws IOException {
        if (consumerId == null) {
            throw new IOException("No consumer selected");
        }
        startConsumer(consumerId);
    }

    private void resetSession() {
        if (session != null) {
            try {
                session.dispose();
            } catch (Exception e) {
                LOG.debug("Error disposing previous session: {}", e.getMessage());
            }
        }
        if (kContainer == null) {
            KieServices ks = KieServices.Factory.get();
            kContainer = ks.getKieClasspathContainer();
        }
        justifications = new TreeMap<>();
        conclusionsList = new ArrayList<>();
        conclusionCounter = 1;
        agendaEventListener = new TrackingAgendaEventListener();
        session = kContainer.newKieSession("ksession");
        session.addEventListener(agendaEventListener);

        App.kContainer = kContainer;
        App.KS = session;
        App.agendaEventListener = agendaEventListener;
        App.justifications = justifications;
        App.conclusionsList = conclusionsList;
    }

    private void resetQuestionLists() {
        pendingQuestions = new ArrayList<>();
        for (QuestionCatalog.QuestionDef def : QuestionCatalog.ordered()) {
            pendingQuestions.add(def.getKey());
        }
        App.questionListCategorical = pendingQuestions.stream()
                .filter(key -> {
                    QuestionCatalog.QuestionDef def = QuestionCatalog.get(key);
                    return def != null && !def.isNumerical();
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        App.questionListNumerical = pendingQuestions.stream()
                .filter(key -> {
                    QuestionCatalog.QuestionDef def = QuestionCatalog.get(key);
                    return def != null && def.isNumerical();
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void loadProfileFacts() throws IOException {
        JsonNode live = liveConsumption.get(consumerId);
        Appliances appliancesMax;
        Appliances appliancesMean;
        if (live != null && looksLikeFullProfile(live)) {
            appliancesMax = MAPPER.treeToValue(live, Appliances.class);
            appliancesMean = appliancesMax;
        } else {
            appliancesMax = readAppliances("consumer" + consumerId + "_max.json");
            appliancesMean = readAppliances("consumer" + consumerId + "_mean.json");
        }

        double maxConsumption = maxOf(appliancesMax.totalConsumption._1, appliancesMax.totalConsumption._0);
        double averageConsumption = (appliancesMean.totalConsumption._1 + appliancesMean.totalConsumption._0) / 2.0;
        double offPeakMean = appliancesMean.totalConsumption._0;
        double wmMax = maxOf(appliancesMax.washingMachine._1, appliancesMax.washingMachine._0);
        double dwMax = maxOf(appliancesMax.dishWasher._1, appliancesMax.dishWasher._0);
        double fridgeMax = maxOf(appliancesMax.refrigerator._1, appliancesMax.refrigerator._0);

        if (live != null && !looksLikeFullProfile(live)) {
            maxConsumption = numberOr(live, "maxKw", maxConsumption);
            averageConsumption = numberOr(live, "meanKw", averageConsumption);
            offPeakMean = numberOr(live, "offPeakMeanKw", offPeakMean);
            wmMax = numberOr(live, "washingMachineMax", wmMax);
            dwMax = numberOr(live, "dishWasherMax", dwMax);
            fridgeMax = numberOr(live, "refrigeratorMax", fridgeMax);
        }

        insertNumerical("Save Contracted Power", 0);
        insertNumerical("Max Consumption", maxConsumption);
        insertNumerical("Best Contracted Power", Calculate.calculateContractedPower());
        insertNumerical("Average Consumption", averageConsumption);
        insertNumerical("Average Consumption Bi-Schedule", offPeakMean);
        insertNumerical("Solar Panel Production", Calculate.calculateSolarPanelProduction());
        insertNumerical("Energy to Sell", Calculate.calculateEnergyToSell());
        insertNumerical("Washing Machine Max", wmMax);
        insertNumerical("Dish washer Max", dwMax);
        insertNumerical("Refrigerator Max", fridgeMax);

        // Only appliances that can be delayed-start programmed (fridge always-on).
        for (String appliance : Arrays.asList("Washing Machine", "Dish washer")) {
            session.insert(new CategoricalEvidence("Appliance", appliance));
        }
    }

    private Appliances readAppliances(String fileName) throws IOException {
        String path = "/org/engcia/" + fileName;
        try (InputStream in = ExpertEngine.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("Missing classpath resource " + path);
            }
            return MAPPER.readValue(in, Appliances.class);
        }
    }

    public synchronized void upsertConsumption(String id, JsonNode payload) throws IOException {
        if (payload == null || payload.isNull()) {
            throw new IOException("Consumption payload is empty");
        }
        String target = (id == null || id.isBlank()) ? consumerId : id.trim();
        if (target == null || target.isBlank()) {
            throw new IOException("No consumer selected for consumption update");
        }
        liveConsumption.put(target, payload);
        if (target.equals(consumerId) && session != null) {
            applyLiveConsumption(payload);
        }
    }

    private void applyLiveConsumption(JsonNode payload) {
        if (looksLikeFullProfile(payload)) {
            try {
                Appliances appliances = MAPPER.treeToValue(payload, Appliances.class);
                updateNumerical("Max Consumption", maxOf(appliances.totalConsumption._1, appliances.totalConsumption._0));
                updateNumerical("Average Consumption", (appliances.totalConsumption._1 + appliances.totalConsumption._0) / 2.0);
                updateNumerical("Average Consumption Bi-Schedule", appliances.totalConsumption._0);
                updateNumerical("Washing Machine Max", maxOf(appliances.washingMachine._1, appliances.washingMachine._0));
                updateNumerical("Dish washer Max", maxOf(appliances.dishWasher._1, appliances.dishWasher._0));
                updateNumerical("Refrigerator Max", maxOf(appliances.refrigerator._1, appliances.refrigerator._0));
            } catch (Exception e) {
                LOG.warn("Could not apply full consumption profile: {}", e.getMessage());
                return;
            }
        } else {
            if (payload.has("maxKw")) {
                updateNumerical("Max Consumption", payload.get("maxKw").asDouble());
            }
            if (payload.has("meanKw")) {
                updateNumerical("Average Consumption", payload.get("meanKw").asDouble());
            }
            if (payload.has("offPeakMeanKw")) {
                updateNumerical("Average Consumption Bi-Schedule", payload.get("offPeakMeanKw").asDouble());
            }
            if (payload.has("washingMachineMax")) {
                updateNumerical("Washing Machine Max", payload.get("washingMachineMax").asDouble());
            }
            if (payload.has("dishWasherMax")) {
                updateNumerical("Dish washer Max", payload.get("dishWasherMax").asDouble());
            }
            if (payload.has("refrigeratorMax")) {
                updateNumerical("Refrigerator Max", payload.get("refrigeratorMax").asDouble());
            }
        }
        updateNumerical("Best Contracted Power", Calculate.calculateContractedPower());
    }

    public synchronized void answer(String description, String value) {
        ensureSession();
        String normalized = value == null ? "" : value.trim();
        if ("yes".equalsIgnoreCase(normalized) || "no".equalsIgnoreCase(normalized)) {
            normalized = normalized.toLowerCase();
        }
        session.insert(new CategoricalEvidence(description, normalized));
        markAnswered(description);
    }

    public synchronized void answerNumerical(String description, double value) {
        ensureSession();
        updateOrInsertNumerical(description, value);
        markAnswered(description);
        if (Questions.INSTALL_SPACE.equals(description)) {
            updateNumerical("Solar Panel Production", Calculate.calculateSolarPanelProduction());
            updateNumerical("Energy to Sell", Calculate.calculateEnergyToSell());
        }
    }

    private void markAnswered(String description) {
        if (pendingQuestions != null) {
            pendingQuestions.remove(description);
        }
        if (App.questionListCategorical != null) {
            App.questionListCategorical.remove(description);
        }
        if (App.questionListNumerical != null) {
            App.questionListNumerical.remove(description);
        }
    }

    public synchronized List<String> recommend() {
        ensureSession();
        conclusionsList.clear();
        App.conclusionsList = conclusionsList;
        conclusionCounter = 1;
        session.fireAllRules();

        @SuppressWarnings("unchecked")
        Collection<Conclusion> conclusions = (Collection<Conclusion>) session.getObjects(new ClassObjectFilter(Conclusion.class));
        How how = new How(justifications);
        for (Conclusion conclusion : conclusions) {
            conclusionsList.add(conclusionCounter++ + "." + how.getHowExplanation(conclusion.getId()));
        }
        List<String> snapshot = new ArrayList<>(conclusionsList);
        for (BiConsumer<String, List<String>> listener : recommendationListeners) {
            try {
                listener.accept(consumerId, snapshot);
            } catch (Exception e) {
                LOG.warn("Recommendation listener failed: {}", e.getMessage());
            }
        }
        return snapshot;
    }

    public synchronized String how(int factId) {
        if (justifications == null) {
            return "";
        }
        return new How(justifications).getHowExplanation(factId);
    }

    public synchronized String whyNot(String expectedConclusion) {
        ensureSession();
        try {
            return WhyNot.getExplanation(session, expectedConclusion);
        } catch (Exception e) {
            LOG.warn("WhyNot failed: {}", e.getMessage());
            return "WhyNot could not explain '" + expectedConclusion + "': " + e.getMessage();
        }
    }

    public synchronized Map<String, Object> getQuestions() {
        List<Map<String, Object>> pending = new ArrayList<>();
        List<String> categorical = new ArrayList<>();
        List<String> numerical = new ArrayList<>();
        if (pendingQuestions != null) {
            for (String key : pendingQuestions) {
                QuestionCatalog.QuestionDef def = QuestionCatalog.get(key);
                if (def == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("key", def.getKey());
                item.put("label", def.getLabel());
                item.put("type", def.getType().name());
                item.put("options", def.getOptions());
                pending.add(item);
                if (def.isNumerical()) {
                    numerical.add(def.getKey());
                } else {
                    categorical.add(def.getKey());
                }
            }
        }
        Map<String, Object> questions = new LinkedHashMap<>();
        questions.put("pending", pending);
        questions.put("categorical", categorical);
        questions.put("numerical", numerical);
        return questions;
    }

    public synchronized String nextQuestion() {
        QuestionCatalog.QuestionDef def = nextQuestionDef();
        return def == null ? null : def.getKey();
    }

    public synchronized QuestionCatalog.QuestionDef nextQuestionDef() {
        if (pendingQuestions == null || pendingQuestions.isEmpty()) {
            return null;
        }
        return QuestionCatalog.get(pendingQuestions.get(0));
    }

    public synchronized boolean isNextQuestionNumerical() {
        QuestionCatalog.QuestionDef def = nextQuestionDef();
        return def != null && def.isNumerical();
    }

    public synchronized boolean hasPendingQuestions() {
        return nextQuestion() != null;
    }

    public synchronized List<String> getConclusions() {
        if (conclusionsList == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(conclusionsList);
    }

    public synchronized String getConsumerId() {
        return consumerId;
    }

    public synchronized boolean hasSession() {
        return session != null;
    }

    private void ensureSession() {
        if (session == null) {
            throw new IllegalStateException("No expert session. Call setConsumer first.");
        }
    }

    private void insertNumerical(String description, double value) {
        session.insert(new NumericalEvidence(description, value));
    }

    private void updateOrInsertNumerical(String description, double value) {
        NumericalEvidence existing = EvidenceHelper.getNumericalEvidence(description);
        if (existing != null) {
            existing.setValue(value);
            session.update(session.getFactHandle(existing), existing);
        } else {
            insertNumerical(description, value);
        }
    }

    private void updateNumerical(String description, double value) {
        updateOrInsertNumerical(description, value);
    }

    private static boolean looksLikeFullProfile(JsonNode node) {
        return node != null && node.has("Total Consumption");
    }

    private static double maxOf(double a, double b) {
        return Math.max(a, b);
    }

    private static double numberOr(JsonNode node, String field, double fallback) {
        if (node != null && node.has(field) && node.get(field).isNumber()) {
            return node.get(field).asDouble();
        }
        return fallback;
    }
}
