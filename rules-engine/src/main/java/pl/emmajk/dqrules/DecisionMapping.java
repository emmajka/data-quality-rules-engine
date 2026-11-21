package pl.emmajk.dqrules;

import java.util.Map;
import java.util.Objects;

/**
 * Turns the value a rule computes into a decision. It is data, so a rule never
 * hard-codes its decision.
 *
 * @param byValue the decision for each known value
 * @param defaultDecision the decision for any other value, or null if there is none
 */
public record DecisionMapping(Map<String, Decision> byValue, Decision defaultDecision) {

    /**
     * Copies the mapping.
     *
     * @throws NullPointerException if the map, a key or a decision in it is null
     */
    public DecisionMapping {
        byValue = Map.copyOf(Objects.requireNonNull(byValue, "byValue"));
    }

    /** Creates a mapping without a default, so an unmapped value is an error. */
    public static DecisionMapping of(Map<String, Decision> byValue) {
        return new DecisionMapping(byValue, null);
    }

    /**
     * Returns the decision for the value, or the default if the value is not mapped.
     * Values are compared exactly, so case and surrounding spaces matter.
     *
     * @throws NullPointerException if {@code value} is null
     * @throws IllegalArgumentException if the value is not mapped and there is no default
     */
    public Decision decide(String value) {
        Objects.requireNonNull(value, "value");
        Decision decision = byValue.get(value);
        if (decision != null) {
            return decision;
        }
        if (defaultDecision == null) {
            throw new IllegalArgumentException("no decision for value '" + value + "' and no default");
        }

        return defaultDecision;
    }
}
