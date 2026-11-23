package pl.emmajk.dqrules

import spock.lang.Specification

class DecisionMappingSpec extends Specification {

    def mapping = new DecisionMapping([blocked: Decision.INVALID], Decision.NOT_APPLICABLE)

    def 'decide - dataset tests - mapped and default values'() {
        expect:
        mapping.decide(value) == expected

        where:
        value     || expected
        'blocked' || Decision.INVALID
        'ok'      || Decision.NOT_APPLICABLE
        ''        || Decision.NOT_APPLICABLE
    }

    def 'decide - dataset tests - values that differ from a key only in case or spacing'() {
        expect:
        mapping.decide(value) == Decision.NOT_APPLICABLE

        where:
        value << ['BLOCKED', 'blocked ', ' blocked']
    }

    def 'given a map when calling of then should return a mapping without a default'() {
        expect:
        DecisionMapping.of([ok: Decision.VALID]).defaultDecision() == null
    }

    def 'given an unmapped value and no default when calling decide then should throw an IllegalArgumentException'() {
        given:
        def noDefault = DecisionMapping.of([ok: Decision.VALID, bad: Decision.INVALID])

        when:
        noDefault.decide('other')

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("'other'")
    }

    def 'given a mapping without entries and without a default when calling decide then should throw an IllegalArgumentException'() {
        given:
        def empty = DecisionMapping.of([:])

        when:
        empty.decide('x')

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("'x'")
    }

    def 'given a null value when calling decide then should throw a NullPointerException'() {
        when:
        mapping.decide(null)

        then:
        def e = thrown(NullPointerException)
        e.message == 'value'
    }

    def 'given a null map when calling the constructor then should throw a NullPointerException'() {
        when:
        new DecisionMapping(null, null)

        then:
        thrown(NullPointerException)
    }

    def 'constructor - dataset tests - null entries'() {
        when:
        new DecisionMapping(byValue, null)

        then:
        thrown(NullPointerException)

        where:
        byValue << [[(null): Decision.VALID], [ok: null]]
    }

    def 'given a source map changed after creation when calling decide then should return the original decision'() {
        given:
        def source = [ok: Decision.VALID]
        def created = DecisionMapping.of(source)

        when:
        source.ok = Decision.INVALID

        then:
        created.decide('ok') == Decision.VALID
    }
}
