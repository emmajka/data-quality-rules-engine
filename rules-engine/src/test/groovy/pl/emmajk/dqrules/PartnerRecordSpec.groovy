package pl.emmajk.dqrules

import spock.lang.Specification

class PartnerRecordSpec extends Specification {

    def partner = PartnerRecord.of([id: 'r1', vatId: 'DE111', iban: '', note: null])

    def 'given a null map when calling of then should throw a NullPointerException'() {
        when:
        PartnerRecord.of(null)

        then:
        thrown(NullPointerException)
    }

    def 'of - dataset tests - invalid ids'() {
        when:
        PartnerRecord.of(fields)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("'id'")

        where:
        fields << [[:], [id: null], [id: ''], [id: '  '], [id: 42]]
    }

    def 'given a source map changed after creation when calling get then should return the original value'() {
        given:
        def source = [id: 'r1', vatId: 'DE111']
        def created = PartnerRecord.of(source)

        when:
        source.vatId = 'changed'

        then:
        created.get('vatId') == 'DE111'
    }

    def 'given a nested list changed after creation when calling get then should return the changed list'() {
        given:
        def tags = ['a']
        def created = PartnerRecord.of([id: 'r1', tags: tags])

        when:
        tags << 'b'

        then:
        created.get('tags') == ['a', 'b']
    }

    def 'given fields with an id when calling id then should return the id'() {
        expect:
        partner.id() == 'r1'
    }

    def 'get - dataset tests'() {
        expect:
        partner.get(field) == expected

        where:
        field    || expected
        'id'     || 'r1'
        'vatId'  || 'DE111'
        'iban'   || ''
        'note'   || null
        'absent' || null
    }

    def 'given a null field name when calling get then should throw a NullPointerException'() {
        when:
        partner.get(null)

        then:
        thrown(NullPointerException)
    }

    def 'given a record with fields when calling toString then should return only the id'() {
        expect:
        partner.toString() == 'PartnerRecord[id=r1]'
    }
}
