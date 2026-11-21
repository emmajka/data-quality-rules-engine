package pl.emmajk.dqrules;

import java.util.List;

/**
 * The outcome of one {@link Run#execute()}. A class with a hidden constructor, not a record,
 * so it can gain more outcomes later without breaking callers.
 */
public final class RunReport {

    private final List<Result> results;

    RunReport(List<Result> results) {
        this.results = List.copyOf(results);
    }

    /** Returns the results, record by record and in catalog order within a record. */
    public List<Result> results() {
        return results;
    }

    /** Returns only the number of results, so logs do not leak field values. */
    @Override
    public String toString() {
        return "RunReport[results=" + results.size() + "]";
    }
}
