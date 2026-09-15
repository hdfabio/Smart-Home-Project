package org.engcia.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Questionnaire definitions: Drools fact key, user-facing label (with units), and selectable options.
 */
public final class QuestionCatalog {

    public enum AnswerType {
        CHOICE,
        NUMBER,
        NUMBER_CHOICE
    }

    public static final class QuestionDef {
        private final String key;
        private final String label;
        private final AnswerType type;
        private final List<String> options;

        public QuestionDef(String key, String label, AnswerType type, List<String> options) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(options));
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public AnswerType getType() {
            return type;
        }

        public List<String> getOptions() {
            return options;
        }

        public boolean isNumerical() {
            return type == AnswerType.NUMBER || type == AnswerType.NUMBER_CHOICE;
        }
    }

    private static final List<String> YES_NO = Arrays.asList("yes", "no");
    private static final List<String> EFFICIENCY_FULL = Arrays.asList("A+++", "A++", "A+", "A", "B", "C", "D");
    private static final List<String> EFFICIENCY_FRIDGE = Arrays.asList("A+++", "A++", "A+", "A", "B", "C");

    private static final Map<String, QuestionDef> BY_KEY = new LinkedHashMap<>();
    private static final List<QuestionDef> ORDER = new ArrayList<>();

    static {
        add(new QuestionDef(Questions.BI_SHEDULE,
                "Do you already have a dual-rate tariff / bi-schedule?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef(Questions.INVEST_RENEWABLE_ENERGY,
                "Are you willing to invest in renewable energy?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef(Questions.SELL_ENERGY,
                "Are you willing to sell surplus energy to neighbours / communities?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef(Questions.SWITCH_APPLICANCES,
                "Are you willing to replace appliances (washing machine, dishwasher, fridge)?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef("Dish washer Efficiency",
                "Dishwasher energy efficiency class",
                AnswerType.CHOICE, EFFICIENCY_FULL));
        add(new QuestionDef("Dish washer is programmable",
                "Is the dishwasher programmable (delayed start)?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef("Washing Machine Efficiency",
                "Washing machine energy efficiency class",
                AnswerType.CHOICE, EFFICIENCY_FULL));
        add(new QuestionDef("Washing Machine is programmable",
                "Is the washing machine programmable (delayed start)?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef("Refrigerator Efficiency",
                "Refrigerator energy efficiency class",
                AnswerType.CHOICE, EFFICIENCY_FRIDGE));
        add(new QuestionDef(Questions.LOCOMOTION,
                "Is your main transport fossil-fuelled (petrol/diesel)?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef(Questions.SWITCH_LOCOMOTION,
                "Are you willing to switch transport?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef(Questions.SLOW_CHARGE,
                "If you get an EV, would you allow slow charging?",
                AnswerType.CHOICE, YES_NO));
        add(new QuestionDef("Contracted Power",
                "Current contracted power (kW)",
                AnswerType.NUMBER_CHOICE, contractedPowerOptions()));
        add(new QuestionDef(Questions.INSTALL_SPACE,
                "Available roof/ground area for solar panels (m²)",
                AnswerType.NUMBER, null));
        add(new QuestionDef(Questions.DISTANCE,
                "Average daily distance travelled (km)",
                AnswerType.NUMBER, null));
    }

    private QuestionCatalog() {
    }

    private static void add(QuestionDef def) {
        BY_KEY.put(def.getKey(), def);
        ORDER.add(def);
    }

    private static List<String> contractedPowerOptions() {
        List<String> options = new ArrayList<>();
        for (ContractedPower tier : ContractedPower.values()) {
            options.add(String.valueOf(tier.getValue()));
        }
        return options;
    }

    public static List<QuestionDef> ordered() {
        return Collections.unmodifiableList(ORDER);
    }

    public static QuestionDef get(String key) {
        return BY_KEY.get(key);
    }

    public static String labelOf(String key) {
        QuestionDef def = BY_KEY.get(key);
        return def == null ? key : def.getLabel();
    }
}
