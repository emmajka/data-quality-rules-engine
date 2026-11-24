package pl.emmajk.dqrules

import spock.lang.Specification

class RunSpec extends Specification {

    /** Counts every read: forEach, stream and toArray all go through iterator(). */
    static class CountingCollection extends AbstractCollection<PartnerRecord> {
        final List<PartnerRecord> records
        int iterations = 0

        CountingCollection(List<PartnerRecord> records) {
            this.records = records
        }

        @Override
        Iterator<PartnerRecord> iterator() {
            iterations++
            records.iterator()
        }

        @Override
        int size() {
            records.size()
        }
    }

    def 'given records when calling on then should not read them until execute is called'() {
        given:
        def records = new CountingCollection([Fixtures.record(id: 'a')])
        def engine = RuleEngine.of(Fixtures.catalog(Fixtures.rule()))

        when:
        def run = engine.on(records)

        then:
        records.iterations == 0

        when:
        run.execute()

        then:
        records.iterations == 1
    }

    def 'given a run when calling execute twice then should read the records each time and return equal results'() {
        given:
        def records = new CountingCollection([Fixtures.record(id: 'a')])
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on(records)

        when:
        def first = run.execute()
        def second = run.execute()

        then:
        records.iterations == 2
        !first.is(second)
        first.results() == second.results()
    }

    def 'given records changed after on when calling execute then should evaluate the current records'() {
        given:
        def records = [Fixtures.record(id: 'a')]
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on(records)

        when:
        records << Fixtures.record(id: 'b')

        then:
        run.execute().results().collect { it.provenance().recordId() } == ['a', 'b']
    }

    def 'execute - dataset tests - rule statuses'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule(status: status))).on([Fixtures.record(id: 'a')])

        expect:
        run.execute().results().size() == expected

        where:
        status                || expected
        RuleStatus.RELEASED   || 1
        RuleStatus.DRAFT      || 0
        RuleStatus.DEPRECATED || 0
    }

    def 'execute - dataset tests - country scopes'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule(scope: scope))).on([Fixtures.record([id: 'a'] + country)])

        expect:
        run.execute().results().size() == expected

        where:
        scope                 | country         || expected
        CountryScope.WORLD    | [country: 'DE'] || 1
        CountryScope.WORLD    | [:]             || 1
        CountryScope.of('DE') | [country: 'DE'] || 1
        CountryScope.of('DE') | [country: 'FR'] || 0
        CountryScope.of('DE') | [country: 'de'] || 0
        CountryScope.of('DE') | [country: 42]   || 0
        CountryScope.of('DE') | [:]             || 0
    }

    def 'execute - dataset tests - mapping decisions'() {
        given:
        def rule = Fixtures.rule(inputs: ['kind'], logic: { partner -> partner.get('kind') } as RuleLogic,
                mapping: new DecisionMapping([a: Decision.VALID, b: Decision.INVALID, c: Decision.REVIEW],
                        Decision.NOT_APPLICABLE))
        def run = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'x', kind: kind)])

        expect:
        run.execute().results()*.decision() == [expected]

        where:
        kind    || expected
        'a'     || Decision.VALID
        'b'     || Decision.INVALID
        'c'     || Decision.REVIEW
        'other' || Decision.NOT_APPLICABLE
    }

    def 'execute - dataset tests - severities'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule(severity: severity))).on([Fixtures.record(id: 'a')])

        expect:
        run.execute().results()*.severity() == [severity]

        where:
        severity << Severity.values()
    }

    def 'given a rule with inputs when calling execute then should record the rule, the record, the value and the input values'() {
        given:
        def rule = Fixtures.rule(id: 'vatFormat', inputs: ['vatId', 'iban'], logic: { partner -> 'bad' } as RuleLogic,
                mapping: DecisionMapping.of([bad: Decision.INVALID]))
        def run = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'r1', iban: '')])

        when:
        def explanation = run.execute().results().first().provenance()

        then:
        explanation.ruleId() == 'vatFormat'
        explanation.recordId() == 'r1'
        explanation.value() == 'bad'
        explanation.inputs() == [new Provenance.Input('vatId', null), new Provenance.Input('iban', '')]
    }

    def 'given a rule with several inputs when calling execute then should keep the declared order of the inputs'() {
        given:
        def rule = Fixtures.rule(inputs: ['z', 'y', 'x', 'w'])
        def run = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a', w: 1, x: 2, y: 3, z: 4)])

        expect:
        run.execute().results().first().provenance().inputs()*.field() == ['z', 'y', 'x', 'w']
    }

    def 'given several records and rules when calling execute then should return results record by record in catalog order'() {
        given:
        def catalog = Fixtures.catalog(Fixtures.rule(id: 'x'), Fixtures.rule(id: 'y'))
        def run = RuleEngine.of(catalog).on([Fixtures.record(id: 'a'), Fixtures.record(id: 'b')])

        expect:
        run.execute().results().collect { it.provenance().recordId() + '-' + it.provenance().ruleId() } ==
                ['a-x', 'a-y', 'b-x', 'b-y']
    }

    def 'execute - dataset tests - nothing to evaluate'() {
        expect:
        RuleEngine.of(Fixtures.catalog(*rules)).on(records).execute().results().isEmpty()

        where:
        rules              | records
        [Fixtures.rule()]  | []
        []                 | [Fixtures.record(id: 'a')]
    }

    def 'given records with the same id when calling execute then should evaluate each of them'() {
        given:
        def records = [Fixtures.record(id: 'a', vatId: 'x'), Fixtures.record(id: 'a', vatId: 'y')]
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on(records)

        expect:
        run.execute().results().collect { it.provenance().inputs().first().value() } == ['x', 'y']
    }

    def 'given a collection with a null record when calling execute then should throw a NullPointerException'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on([Fixtures.record(id: 'a'), null])

        when:
        run.execute()

        then:
        def e = thrown(NullPointerException)
        e.message == 'record'
    }

    def 'given a result when calling inputs on its provenance then should return an unmodifiable list'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on([Fixtures.record(id: 'a')])

        when:
        run.execute().results().first().provenance().inputs().add(new Provenance.Input('x', 'y'))

        then:
        thrown(UnsupportedOperationException)
    }

    static RuleLogic failingOn(String recordId, Throwable failure) {
        { partner -> if (partner.id() == recordId) { throw failure }; 'ok' } as RuleLogic
    }

    def 'given a rule that fails on one record when calling execute then should report the error and keep the other results'() {
        given:
        def failure = new IllegalStateException('boom')
        def rules = [Fixtures.rule(id: 'x', logic: failingOn('b', failure)), Fixtures.rule(id: 'y')]
        def records = [Fixtures.record(id: 'a'), Fixtures.record(id: 'b'), Fixtures.record(id: 'c')]

        when:
        def report = RuleEngine.of(Fixtures.catalog(*rules)).on(records).execute()

        then:
        report.results().collect { it.recordId() + '-' + it.ruleId() } == ['a-x', 'a-y', 'b-y', 'c-x', 'c-y']
        report.errors().size() == 1
        report.errors().first().ruleId() == 'x'
        report.errors().first().recordId() == 'b'
        report.errors().first().cause().is(failure)
    }

    def 'execute - dataset tests - kinds of failures'() {
        given:
        def rule = Fixtures.rule(logic: { partner ->
            if (value instanceof Throwable) { throw value }
            value
        } as RuleLogic, mapping: DecisionMapping.of([ok: Decision.VALID]))
        def report = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a')]).execute()

        expect:
        report.results().isEmpty()
        report.errors()*.cause()*.getClass()*.name == [type]
        report.errors()*.cause()*.message == [message]

        where:
        value                              || type                              | message
        new IllegalStateException('bad')   || 'java.lang.IllegalStateException' | 'bad'
        'weird'                            || 'java.lang.IllegalArgumentException' | "no decision for value 'weird' and no default"
        null                               || 'java.lang.NullPointerException'  | 'value'
    }

    def 'given every rule failing on every record when calling execute then should report an error for each pair and no result'() {
        given:
        def failure = new IllegalStateException('boom')
        def rules = [Fixtures.rule(id: 'x', logic: { throw failure } as RuleLogic),
                     Fixtures.rule(id: 'y', logic: { throw failure } as RuleLogic)]
        def run = RuleEngine.of(Fixtures.catalog(*rules)).on([Fixtures.record(id: 'a'), Fixtures.record(id: 'b')])

        when:
        def report = run.execute()

        then:
        report.results().isEmpty()
        report.errors().collect { it.recordId() + '-' + it.ruleId() } == ['a-x', 'a-y', 'b-x', 'b-y']
    }

    def 'execute - dataset tests - failing rules that are not applicable'() {
        given:
        def rule = Fixtures.rule(status: status, scope: scope, logic: { throw new IllegalStateException('boom') } as RuleLogic)
        def report = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a', country: 'DE')]).execute()

        expect:
        report.results().isEmpty()
        report.errors().isEmpty()

        where:
        status                | scope
        RuleStatus.DRAFT      | CountryScope.WORLD
        RuleStatus.DEPRECATED | CountryScope.WORLD
        RuleStatus.RELEASED   | CountryScope.of('FR')
    }

    def 'given a rule that throws an Error when calling execute then should propagate it'() {
        given:
        def rule = Fixtures.rule(logic: { throw new OutOfMemoryError('simulated') } as RuleLogic)
        def run = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a')])

        when:
        run.execute()

        then:
        def e = thrown(OutOfMemoryError)
        e.message == 'simulated'
    }

    def 'given a failing rule and a later null record when calling execute then should still throw a NullPointerException'() {
        given:
        def rule = Fixtures.rule(logic: { throw new IllegalStateException('boom') } as RuleLogic)
        def run = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a'), null])

        when:
        run.execute()

        then:
        def e = thrown(NullPointerException)
        e.message == 'record'
    }

    def 'given a failing rule when calling execute twice then should report the same rule and record each time'() {
        given:
        def rule = Fixtures.rule(logic: { throw new IllegalStateException('boom') } as RuleLogic)
        def run = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a')])

        expect:
        run.execute().errors().collect { [it.ruleId(), it.recordId()] } ==
                run.execute().errors().collect { [it.ruleId(), it.recordId()] }
    }

    def 'given a rule that throws an exception whose getMessage fails when calling execute then should keep the other results'() {
        given:
        def rules = [Fixtures.rule(id: 'x', logic: failingOn('b', new Fixtures.BrokenMessage())), Fixtures.rule(id: 'y')]
        def records = [Fixtures.record(id: 'a'), Fixtures.record(id: 'b'), Fixtures.record(id: 'c')]

        when:
        def report = RuleEngine.of(Fixtures.catalog(*rules)).on(records).execute()

        then:
        report.results().collect { it.recordId() + '-' + it.ruleId() } == ['a-x', 'a-y', 'b-y', 'c-x', 'c-y']
        report.errors()*.recordId() == ['b']
        report.errors().first().cause() instanceof Fixtures.BrokenMessage
    }

    def 'given a run without failures when calling execute then should return no errors'() {
        expect:
        RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on([Fixtures.record(id: 'a')]).execute().errors().isEmpty()
    }

    def 'given a run with results and errors when calling execute then should summarize both'() {
        given:
        def rules = [Fixtures.rule(id: 'x', severity: Severity.WARNING, logic: failingOn('b', new IllegalStateException('boom'))),
                     Fixtures.rule(id: 'y', severity: Severity.INFO, mapping: DecisionMapping.of([ok: Decision.REVIEW]))]
        def records = [Fixtures.record(id: 'a'), Fixtures.record(id: 'b'), Fixtures.record(id: 'c')]

        when:
        def summary = RuleEngine.of(Fixtures.catalog(*rules)).on(records).execute().summary()

        then:
        summary.results() == 5
        summary.errors() == 1
        summary.byDecision() as Map == [(Decision.VALID): 2L, (Decision.INVALID): 0L, (Decision.REVIEW): 3L,
                                        (Decision.NOT_APPLICABLE): 0L]
        summary.bySeverity() as Map == [(Severity.ERROR): 0L, (Severity.WARNING): 2L, (Severity.INFO): 3L]
    }

    def 'given rules that are skipped when calling execute then should not count them in the summary'() {
        given:
        def rules = [Fixtures.rule(id: 'x'), Fixtures.rule(id: 'draft', status: RuleStatus.DRAFT),
                     Fixtures.rule(id: 'fr', scope: CountryScope.of('FR'))]
        def run = RuleEngine.of(Fixtures.catalog(*rules)).on([Fixtures.record(id: 'a', country: 'DE')])

        when:
        def summary = run.execute().summary()

        then:
        summary.results() == 1
        summary.errors() == 0
    }

    def 'given a report when calling summary then should match its results and errors'() {
        given:
        def rules = [Fixtures.rule(id: 'x', logic: failingOn('b', new IllegalStateException('boom'))), Fixtures.rule(id: 'y')]
        def records = [Fixtures.record(id: 'a'), Fixtures.record(id: 'b')]

        when:
        def report = RuleEngine.of(Fixtures.catalog(*rules)).on(records).execute()

        then:
        report.summary().results() == report.results().size()
        report.summary().errors() == report.errors().size()
        report.summary().byDecision().values().sum() == report.results().size()
        report.summary().bySeverity().values().sum() == report.results().size()
    }

    def 'given a report when calling summary twice then should return the same counts'() {
        given:
        def rule = Fixtures.rule(logic: failingOn('b', new IllegalStateException('boom')))
        def report = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a'), Fixtures.record(id: 'b')]).execute()

        when:
        def first = report.summary()
        def second = report.summary()

        then:
        first.results() == 1
        first.errors() == 1
        first.toString() == second.toString()
    }

    def 'given a report when calling errors then should return an unmodifiable list'() {
        given:
        def rule = Fixtures.rule(logic: { throw new IllegalStateException('boom') } as RuleLogic)
        def report = RuleEngine.of(Fixtures.catalog(rule)).on([Fixtures.record(id: 'a')]).execute()

        when:
        report.errors().add(null)

        then:
        thrown(UnsupportedOperationException)
    }

    def 'given a report with results and errors when calling toString then should return only the counts'() {
        given:
        def rules = [Fixtures.rule(id: 'x', logic: failingOn('b', new IllegalStateException('secret'))), Fixtures.rule(id: 'y')]
        def report = RuleEngine.of(Fixtures.catalog(*rules)).on([Fixtures.record(id: 'a'), Fixtures.record(id: 'b')]).execute()

        expect:
        report.toString() == 'RunReport[results=3, errors=1]'
    }

    def 'given a report when calling results then should return an unmodifiable list'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on([Fixtures.record(id: 'a')])

        when:
        run.execute().results().add(null)

        then:
        thrown(UnsupportedOperationException)
    }
}
