package pl.emmajk.dqrules

import spock.lang.Specification

/** The scenario from section 5 of the task: three records checked by three rules. */
class AcceptanceScenarioSpec extends Specification {

    // The rules are code. The task gives no severities for them, so the spec assumes one of each.
    def catalog = RuleCatalog.builder()
            .register(new Rule('countryBlocked', 'Country is blocked',
                    RuleMetadata.of(RuleStatus.RELEASED, Severity.ERROR), ['country'],
                    { partner -> partner.get('country') == 'ZZ' ? 'blocked' : 'ok' } as RuleLogic,
                    new DecisionMapping([blocked: Decision.INVALID], Decision.NOT_APPLICABLE)))
            .register(new Rule('vatFormat', 'VAT id format',
                    RuleMetadata.of(RuleStatus.RELEASED, Severity.WARNING), ['vatId'],
                    { partner -> partner.get('vatId') instanceof String && partner.get('vatId').length() >= 9 ? 'ok' : 'bad' } as RuleLogic,
                    DecisionMapping.of([ok: Decision.VALID, bad: Decision.INVALID])))
            .register(new Rule('ibanFormat', 'IBAN format',
                    RuleMetadata.of(RuleStatus.RELEASED, Severity.INFO), ['iban', 'country'],
                    { partner ->
                        partner.get('iban') instanceof String && partner.get('country') instanceof String &&
                                partner.get('iban').startsWith(partner.get('country')) ? 'ok' : 'bad'
                    } as RuleLogic,
                    DecisionMapping.of([ok: Decision.VALID, bad: Decision.INVALID])))
            .build()

    def records = Fixtures.records('/fixtures/task-section5-records.json')
    def expectedDecisions = Fixtures.json('/fixtures/task-section5-expected-decisions.json')

    def 'given the task records and rules when calling execute then should return the expected decisions'() {
        when:
        def results = RuleEngine.of(catalog).on(records).execute().results()

        then:
        results.size() == 9
        results.every { it.decision().name() == expectedDecisions[it.recordId()][it.ruleId()] }
    }

    def 'given the task records and rules when calling execute then should return the results record by record in catalog order'() {
        when:
        def results = RuleEngine.of(catalog).on(records).execute().results()

        then:
        results.collect { it.recordId() + '-' + it.ruleId() } == [
                'r1-countryBlocked', 'r1-vatFormat', 'r1-ibanFormat',
                'r2-countryBlocked', 'r2-vatFormat', 'r2-ibanFormat',
                'r3-countryBlocked', 'r3-vatFormat', 'r3-ibanFormat']
    }

    def 'given the task records and rules when calling execute then should give each result the severity of its rule'() {
        given:
        def severities = [countryBlocked: Severity.ERROR, vatFormat: Severity.WARNING, ibanFormat: Severity.INFO]

        when:
        def results = RuleEngine.of(catalog).on(records).execute().results()

        then:
        results.every { it.severity() == severities[it.ruleId()] }
    }

    def 'given the task records and rules when calling execute then should summarize the results by decision and severity'() {
        when:
        def summary = RuleEngine.of(catalog).on(records).execute().summary()

        then:
        summary.results() == 9
        summary.errors() == 0
        summary.byDecision() as Map == [(Decision.VALID): 3L, (Decision.INVALID): 4L, (Decision.REVIEW): 0L,
                                        (Decision.NOT_APPLICABLE): 2L]
        summary.bySeverity() as Map == [(Severity.ERROR): 3L, (Severity.WARNING): 3L, (Severity.INFO): 3L]
    }

    def 'given a rule that fails on a record with a missing field when calling execute then should report the error and the other results'() {
        given:
        def vatLength = new Rule('vatLength', 'VAT id length', RuleMetadata.of(RuleStatus.RELEASED, Severity.WARNING),
                ['vatId'],
                { partner ->
                    if (partner.get('vatId') == null) { throw new IllegalArgumentException('missing field vatId') }
                    partner.get('vatId').length() >= 9 ? 'ok' : 'short'
                } as RuleLogic,
                DecisionMapping.of([ok: Decision.VALID, short: Decision.INVALID]))
        def withFailing = RuleCatalog.builder().registerAll(catalog.rules()).register(vatLength).build()

        when:
        def report = RuleEngine.of(withFailing).on(records).execute()

        then:
        report.results().size() == 11
        report.results().findAll { it.ruleId() != 'vatLength' }
                .every { it.decision().name() == expectedDecisions[it.recordId()][it.ruleId()] }
        report.errors().size() == 1
        report.summary().results() == 11
        report.summary().errors() == 1
        report.errors().first().ruleId() == 'vatLength'
        report.errors().first().recordId() == 'r3'
        report.errors().first().cause() instanceof IllegalArgumentException
        report.errors().first().cause().message == 'missing field vatId'
    }

    def 'given the task records when calling execute then should explain the decisions for r3 with its input values'() {
        when:
        def byRule = RuleEngine.of(catalog).on(records).execute().results()
                .findAll { it.provenance().recordId() == 'r3' }
                .collectEntries { [it.provenance().ruleId(), it.provenance()] }

        then:
        byRule.countryBlocked.value() == 'blocked'
        byRule.vatFormat.value() == 'bad'
        byRule.vatFormat.inputs() == [new Provenance.Input('vatId', null)]
        byRule.ibanFormat.value() == 'bad'
        byRule.ibanFormat.inputs() == [new Provenance.Input('iban', ''), new Provenance.Input('country', 'ZZ')]
    }
}
