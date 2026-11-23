package pl.emmajk.dqrules;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The counts of one run: how many results and errors, and the results by decision and by severity.
 * Rules skipped by status or scope are not counted. A class with a hidden constructor, so the
 * counts always agree with each other.
 */
public final class RunSummary {

    private final long results;
    private final long errors;
    private final Map<Decision, Long> byDecision;
    private final Map<Severity, Long> bySeverity;

    private RunSummary(long results, long errors, Map<Decision, Long> byDecision, Map<Severity, Long> bySeverity) {
        this.results = results;
        this.errors = errors;
        this.byDecision = Collections.unmodifiableMap(byDecision);
        this.bySeverity = Collections.unmodifiableMap(bySeverity);
    }

    /** Counts the given results, with every decision and severity present even when its count is 0. */
    static RunSummary of(List<Result> results, long errors) {
        Map<Decision, Long> byDecision = new EnumMap<>(Decision.class);
        Map<Severity, Long> bySeverity = new EnumMap<>(Severity.class);
        for (Decision decision : Decision.values()) {
            byDecision.put(decision, 0L);
        }
        for (Severity severity : Severity.values()) {
            bySeverity.put(severity, 0L);
        }
        for (Result result : results) {
            byDecision.merge(result.decision(), 1L, Long::sum);
            bySeverity.merge(result.severity(), 1L, Long::sum);
        }

        return new RunSummary(results.size(), errors, byDecision, bySeverity);
    }

    /** Returns the number of results. */
    public long results() {
        return results;
    }

    /** Returns the number of errors, the rules that failed on a record. */
    public long errors() {
        return errors;
    }

    /** Returns the results per decision, in the order of the {@link Decision} constants. */
    public Map<Decision, Long> byDecision() {
        return byDecision;
    }

    /** Returns the results per severity, in the order of the {@link Severity} constants. */
    public Map<Severity, Long> bySeverity() {
        return bySeverity;
    }

    @Override
    public String toString() {
        return "RunSummary[results=" + results + ", errors=" + errors
                + ", byDecision=" + byDecision + ", bySeverity=" + bySeverity + "]";
    }
}
