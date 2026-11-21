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

    def 'given a report when calling results then should return an unmodifiable list'() {
        given:
        def run = RuleEngine.of(Fixtures.catalog(Fixtures.rule())).on([Fixtures.record(id: 'a')])

        when:
        run.execute().results().add(null)

        then:
        thrown(UnsupportedOperationException)
    }
}
