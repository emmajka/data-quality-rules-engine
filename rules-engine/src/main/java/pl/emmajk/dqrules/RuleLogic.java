package pl.emmajk.dqrules;

/**
 * Computes the value of a rule from one record, such as {@code "ok"} or {@code "blocked"}.
 * The rule's {@link DecisionMapping} turns that value into a decision.
 *
 * <p>One instance is shared by all records, so it must be stateless, thread-safe and
 * read only the record. It should return a short label, not raw field data.
 */
@FunctionalInterface
public interface RuleLogic {

    /**
     * Returns the value for the record. May throw an unchecked exception when the rule cannot be evaluated.
     *
     * @param partner the record to read
     * @return the value, never null
     */
    String compute(PartnerRecord partner);
}
