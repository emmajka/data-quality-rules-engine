package pl.emmajk.dqrules

import spock.lang.Specification

class ProvenanceInputSpec extends Specification {

    def 'given a field and a value when calling the constructor then should expose them'() {
        when:
        def created = new Provenance.Input('iban', 'DE123')

        then:
        created.field() == 'iban'
        created.value() == 'DE123'
    }

    def 'given a null field when calling the constructor then should throw a NullPointerException'() {
        when:
        new Provenance.Input(null, 'DE123')

        then:
        def e = thrown(NullPointerException)
        e.message == 'field'
    }

    def 'constructor - dataset tests - blank fields'() {
        when:
        new Provenance.Input(text, 'DE123')

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains('field')

        where:
        text << ['', '  ']
    }

    def 'given no value when calling the constructor then should keep the null value'() {
        when:
        def created = new Provenance.Input('vatId', null)

        then:
        created.field() == 'vatId'
        created.value() == null
    }
}
