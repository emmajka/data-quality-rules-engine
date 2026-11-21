package pl.emmajk.dqrules

import spock.lang.Specification

class ProvenanceSpec extends Specification {

    static input(String field, Object value) {
        new Provenance.Input(field, value)
    }

    def provenance(Map overrides = [:]) {
        def args = [ruleId  : 'vatFormat',
                    recordId: 'r1',
                    value   : 'ok',
                    inputs  : [input('vatId', 'DE111111111')]] + overrides
        new Provenance(args.ruleId, args.recordId, args.value, args.inputs)
    }

    def 'given valid components when calling the constructor then should expose them'() {
        when:
        def created = provenance()

        then:
        created.ruleId() == 'vatFormat'
        created.recordId() == 'r1'
        created.value() == 'ok'
        created.inputs() == [input('vatId', 'DE111111111')]
    }

    def 'constructor - dataset tests - null components'() {
        when:
        provenance((component): null)

        then:
        def e = thrown(NullPointerException)
        e.message == component

        where:
        component << ['ruleId', 'recordId', 'value', 'inputs']
    }

    def 'constructor - dataset tests - blank ids'() {
        when:
        provenance((component): text)

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

    def 'constructor - dataset tests - blank values'() {
        expect:
        provenance(value: text).value() == text

        where:
        text << ['', '  ']
    }

    def 'given an inputs list when calling the constructor then should keep the given list without copying it'() {
        given:
        def source = [input('vatId', 'DE1')]

        expect:
        provenance(inputs: source).inputs().is(source)
    }

    def 'given provenance with field values when calling toString then should return only the ids'() {
        expect:
        provenance(inputs: [input('iban', 'DE123')]).toString() == 'Provenance[ruleId=vatFormat, recordId=r1]'
    }
}
