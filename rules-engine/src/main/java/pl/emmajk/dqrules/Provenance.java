package pl.emmajk.dqrules;

import java.util.List;
import java.util.Objects;

/**
 * Explains a result: which rule, which record, the value the rule computed, and the field values
 * it read. It keeps the values, never the record itself.
 *
 * @param ruleId the id of the rule, never blank
 * @param recordId the id of the record, never blank
 * @param value the value the rule's logic computed, possibly empty
 * @param inputs the rule's declared input fields with their values, in declared order, as given by the caller
 */
public record Provenance(String ruleId, String recordId, String value, List<Input> inputs) {

    /**
     * One field the rule read and the value it had, null for a field that is absent or null.
     *
     * @param field the field name, never blank
     * @param value the value of the field in the record
     */
    public record Input(String field, Object value) {

        /**
         * Validates the field name.
         *
         * @throws NullPointerException if {@code field} is null
         * @throws IllegalArgumentException if {@code field} is blank
         */
        public Input {
            requireNotBlank(field, "field");
        }
    }

    /**
     * Validates the components. The inputs list is not copied, so the caller must pass an
     * unmodifiable list without nulls and must not change it afterwards.
     *
     * @throws NullPointerException if any component is null
     * @throws IllegalArgumentException if the rule id or record id is blank
     */
    public Provenance {
        requireNotBlank(ruleId, "ruleId");
        requireNotBlank(recordId, "recordId");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(inputs, "inputs");
    }

    private static void requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /** Returns only the ids, so logs do not leak field values such as IBANs. */
    @Override
    public String toString() {
        return "Provenance[ruleId=" + ruleId + ", recordId=" + recordId + "]";
    }
}
