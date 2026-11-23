package pl.emmajk.dqrules

import spock.lang.Specification

class RunSummarySpec extends Specification {

    static result(Decision decision, Severity severity = Severity.ERROR) {
        new Result(new Provenance('rule', 'r1', 'ok', [new Provenance.Input('vatId', 'DE1')]), decision, severity)
    }

    def 'given no results and no errors when calling of then should return zero counts for every constant'() {
        when:
        def summary = RunSummary.of([], 0)

        then:
        summary.results() == 0
        summary.errors() == 0
        summary.byDecision().keySet() as List == Decision.values() as List
        summary.byDecision().values().every { it == 0 }
        summary.bySeverity().keySet() as List == Severity.values() as List
        summary.bySeverity().values().every { it == 0 }
    }

    def 'of - dataset tests - counts by decision'() {
        when:
        def summary = RunSummary.of(decisions.collect { result(it) }, 0)

        then:
        summary.results() == decisions.size()
        summary.byDecision().values() as List == expected // in the order VALID, INVALID, REVIEW, NOT_APPLICABLE

        where:
        decisions                                                    || expected
        [Decision.VALID]                                             || [1, 0, 0, 0]
        [Decision.VALID, Decision.INVALID, Decision.VALID]           || [2, 1, 0, 0]
        [Decision.REVIEW, Decision.REVIEW, Decision.REVIEW]          || [0, 0, 3, 0]
        [Decision.NOT_APPLICABLE, Decision.INVALID, Decision.REVIEW] || [0, 1, 1, 1]
    }

    def 'of - dataset tests - counts by severity'() {
        when:
        def summary = RunSummary.of(severities.collect { result(Decision.VALID, it) }, 0)

        then:
        summary.bySeverity().values() as List == expected // in the order ERROR, WARNING, INFO

        where:
        severities                                                 || expected
        [Severity.ERROR]                                           || [1, 0, 0]
        [Severity.INFO, Severity.WARNING, Severity.INFO]           || [0, 1, 2]
        [Severity.ERROR, Severity.ERROR, Severity.WARNING]         || [2, 1, 0]
    }

    def 'given an error count when calling of then should return it apart from the results'() {
        when:
        def summary = RunSummary.of([result(Decision.VALID)], 4)

        then:
        summary.results() == 1
        summary.errors() == 4
    }

    def 'given a summary when calling byDecision or bySeverity then should return unmodifiable maps'() {
        given:
        def summary = RunSummary.of([result(Decision.VALID)], 0)

        when:
        summary."$method"().put(key, 5L)

        then:
        thrown(UnsupportedOperationException)

        where:
        method          | key
        'byDecision'    | Decision.VALID
        'bySeverity'    | Severity.ERROR
    }

    def 'given a summary when calling toString then should return the counts'() {
        expect:
        RunSummary.of([result(Decision.VALID, Severity.WARNING), result(Decision.INVALID)], 1).toString() ==
                'RunSummary[results=2, errors=1, byDecision={VALID=1, INVALID=1, REVIEW=0, NOT_APPLICABLE=0}, ' +
                'bySeverity={ERROR=1, WARNING=1, INFO=0}]'
    }
}
