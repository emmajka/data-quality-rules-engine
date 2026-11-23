package pl.emmajk.dqrules;

import java.util.List;

/**
 * The outcome of one {@link Run#execute()}: the results and, separately, the errors of the rules
 * that failed. A class with a hidden constructor, not a record, so it can gain more outcomes
 * later without breaking callers.
 */
public final class RunReport {

    private final List<Result> results;
    private final List<RuleError> errors;

    RunReport(List<Result> results, List<RuleError> errors) {
        this.results = List.copyOf(results);
        this.errors = List.copyOf(errors);
    }

    /** Returns the results, record by record and in catalog order within a record. */
    public List<Result> results() {
        return results;
    }

    /** Returns the errors of the rules that failed, in the same order as the results. */
    public List<RuleError> errors() {
        return errors;
    }

    /** Returns only the counts, so logs do not leak field values. */
    @Override
    public String toString() {
        return "RunReport[results=" + results.size() + ", errors=" + errors.size() + "]";
    }
}
