package pl.emmajk.dqrules;

import java.util.List;
import java.util.Objects;

/**
 * A named data-quality check: logic that computes a value, and a mapping from that value
 * to a decision, plus the metadata that describes it.
 *
 * @param id the stable id, never blank
 * @param label the human-readable name, never blank
 * @param metadata the lifecycle state, severity, country scope and categories
 * @param inputs the names of the fields the logic reads, at least one, declared so a decision can be explained;
 *               they are not verified against the logic
 * @param logic computes the value for a record
 * @param mapping turns the value into a decision
 */
public record Rule(
        String id,
        String label,
        RuleMetadata metadata,
        List<String> inputs,
        RuleLogic logic,
        DecisionMapping mapping) {

    /**
     * Validates the components and copies the inputs.
     *
     * @throws NullPointerException if any component is null, or the inputs hold a null
     * @throws IllegalArgumentException if the id or label is blank, or the inputs are empty or hold a blank name
     */
    public Rule {
        requireNotBlank(id, "id");
        requireNotBlank(label, "label");
        Objects.requireNonNull(metadata, "metadata");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be empty");
        }
        if (inputs.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("inputs must not contain a blank name");
        }
        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(mapping, "mapping");
    }

    private static void requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
