package pl.emmajk.dqrules

import spock.lang.Specification

class RuleErrorSpec extends Specification {

    def cause = new IllegalStateException('boom')

    def error(Map overrides = [:]) {
        def args = [ruleId: 'vatFormat', recordId: 'r1', cause: cause] + overrides
        new RuleError(args.ruleId, args.recordId, args.cause)
    }

    def 'given valid components when calling the constructor then should expose them'() {
        when:
        def created = error()

        then:
        created.ruleId() == 'vatFormat'
        created.recordId() == 'r1'
        created.cause().is(cause)
    }

    def 'constructor - dataset tests - null components'() {
        when:
        error((component): null)

        then:
        def e = thrown(NullPointerException)
        e.message == component

        where:
        component << ['ruleId', 'recordId', 'cause']
    }

    def 'constructor - dataset tests - blank ids'() {
        when:
        error((component): text)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(component)

        where:
        component  | text
        'ruleId'   | ''
        'ruleId'   | '  '
        'recordId' | ''
        'recordId' | '  '
    }

    def 'given an error with a message when calling toString then should return only the ids and the type of the cause'() {
        expect:
        error(cause: new IllegalStateException('no decision for value DE123')).toString() ==
                'RuleError[ruleId=vatFormat, recordId=r1, cause=java.lang.IllegalStateException]'
    }

    def 'given a cause whose getMessage throws when calling toString then should still return the text'() {
        expect:
        error(cause: new Fixtures.BrokenMessage()).toString().startsWith('RuleError[ruleId=vatFormat')
    }
}
