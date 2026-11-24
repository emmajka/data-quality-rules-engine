package pl.emmajk.dqrules;

import java.util.Objects;

/**
 * A rule that failed on a record, with the exception it threw. The exception is shared, not copied,
 * so it can still be changed after the run.
 *
 * @param ruleId the id of the failed rule, never blank
 * @param recordId the id of the record it failed on, never blank
 * @param cause the exception the rule threw
 */
public record RuleError(String ruleId, String recordId, Exception cause) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if any component is null
     * @throws IllegalArgumentException if the rule id or record id is blank
     */
    public RuleError {
        requireNotBlank(ruleId, "ruleId");
        requireNotBlank(recordId, "recordId");
        Objects.requireNonNull(cause, "cause");
    }

    private static void requireNotBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    /** Returns the ids and the type of the cause only, because its message can contain field values. */
    @Override
    public String toString() {
        return "RuleError[ruleId=" + ruleId + ", recordId=" + recordId + ", cause=" + cause.getClass().getName() + "]";
    }
}
