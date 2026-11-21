package pl.emmajk.dqrules;

import java.util.Objects;

/**
 * What one rule produced for one record: the decision, the rule's severity and the provenance
 * that explains it.
 *
 * @param provenance which rule, which record, and what the rule read
 * @param decision the decision the rule's mapping gave
 * @param severity the severity of the rule
 */
public record Result(Provenance provenance, Decision decision, Severity severity) {

    /**
     * Validates the components.
     *
     * @throws NullPointerException if any component is null
     */
    public Result {
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(severity, "severity");
    }

    /** Returns the id of the rule that produced this result. */
    public String ruleId() {
        return provenance.ruleId();
    }

    /** Returns the id of the record this result is about. */
    public String recordId() {
        return provenance.recordId();
    }

    /** Returns the ids, decision and severity only, so logs do not leak field values. */
    @Override
    public String toString() {
        return "Result[ruleId=" + provenance.ruleId() + ", recordId=" + provenance.recordId()
                + ", decision=" + decision + ", severity=" + severity + "]";
    }
}
