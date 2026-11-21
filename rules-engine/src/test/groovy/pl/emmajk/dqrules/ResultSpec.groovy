package pl.emmajk.dqrules

import spock.lang.Specification

class ResultSpec extends Specification {

    def explanation = new Provenance('vatFormat', 'r1', 'ok', [new Provenance.Input('vatId', 'DE111111111')])

    def result(Map overrides = [:]) {
        def args = [provenance: explanation, decision: Decision.VALID, severity: Severity.WARNING] + overrides
        new Result(args.provenance, args.decision, args.severity)
    }

    def 'given valid components when calling the constructor then should expose them'() {
        when:
        def created = result()

        then:
        created.provenance().is(explanation)
        created.decision() == Decision.VALID
        created.severity() == Severity.WARNING
    }

    def 'constructor - dataset tests - null components'() {
        when:
        result((component): null)

        then:
        def e = thrown(NullPointerException)
        e.message == component

        where:
        component << ['provenance', 'decision', 'severity']
    }

    def 'ruleId and recordId - dataset tests'() {
        expect:
        result()."$method"() == expected

        where:
        method     || expected
        'ruleId'   || 'vatFormat'
        'recordId' || 'r1'
    }

    def 'given a result with field values when calling toString then should return only the ids, decision and severity'() {
        expect:
        result().toString() == 'Result[ruleId=vatFormat, recordId=r1, decision=VALID, severity=WARNING]'
    }
}
