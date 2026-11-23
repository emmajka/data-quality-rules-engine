package pl.emmajk.dqrules

import spock.lang.Specification

class RuleSpec extends Specification {

    def rule(Map overrides = [:]) {
        def args = [id      : 'vatFormat',
                    label   : 'VAT ID format',
                    metadata: RuleMetadata.of(RuleStatus.RELEASED, Severity.ERROR),
                    inputs  : ['vatId'],
                    logic   : { partner -> 'ok' } as RuleLogic,
                    mapping : DecisionMapping.of([ok: Decision.VALID])] + overrides
        new Rule(args.id, args.label, args.metadata, args.inputs, args.logic, args.mapping)
    }

    def 'given valid components when calling the constructor then should expose them'() {
        given:
        def metadata = RuleMetadata.of(RuleStatus.DRAFT, Severity.WARNING)
        def logic = { partner -> 'ok' } as RuleLogic
        def mapping = DecisionMapping.of([ok: Decision.VALID])

        when:
        def created = new Rule('vatFormat', 'VAT ID format', metadata, ['vatId'], logic, mapping)

        then:
        created.id() == 'vatFormat'
        created.label() == 'VAT ID format'
        created.metadata().is(metadata)
        created.inputs() == ['vatId']
        created.logic().is(logic)
        created.mapping().is(mapping)
    }

    def 'constructor - dataset tests - null components'() {
        when:
        rule((component): null)

        then:
        def e = thrown(NullPointerException)
        e.message == component

        where:
        component << ['id', 'label', 'metadata', 'inputs', 'logic', 'mapping']
    }

    def 'constructor - dataset tests - blank texts'() {
        when:
        rule((component): text)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(component)

        where:
        component | text
        'id'      | ''
        'id'      | '  '
        'label'   | ''
        'label'   | '  '
    }

    def 'given empty inputs when calling the constructor then should throw an IllegalArgumentException'() {
        when:
        rule(inputs: [])

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('inputs')
    }

    def 'constructor - dataset tests - blank input names'() {
        when:
        rule(inputs: inputs)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('inputs')

        where:
        inputs << [[''], ['  '], ['vatId', ' ']]
    }

    def 'given an inputs list changed after creation when calling inputs then should return the original list'() {
        given:
        def source = ['vatId']
        def created = rule(inputs: source)

        when:
        source << 'iban'

        then:
        created.inputs() == ['vatId']
    }

    def 'given a rule when calling inputs then should return an unmodifiable list'() {
        when:
        rule().inputs().add('x')

        then:
        thrown(UnsupportedOperationException)
    }
}
