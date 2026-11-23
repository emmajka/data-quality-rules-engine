package pl.emmajk.dqrules;

import java.util.List;

/**
 * The outcome of one {@link Run#execute()}: the results, separately the errors of the rules
 * that failed, and a summary of both. A class with a hidden constructor, not a record, so it can
 * gain more outcomes later without breaking callers.
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

    /** Counts the results and errors on every call, so keep the returned summary if you need it again. */
    public RunSummary summary() {
        return RunSummary.of(results, errors.size());
    }

    /** Returns only the counts, so logs do not leak field values. */
    @Override
    public String toString() {
        return "RunReport[results=" + results.size() + ", errors=" + errors.size() + "]";
    }
}
