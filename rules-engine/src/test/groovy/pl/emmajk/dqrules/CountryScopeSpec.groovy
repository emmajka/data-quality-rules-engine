package pl.emmajk.dqrules

import spock.lang.Specification

class CountryScopeSpec extends Specification {

    def 'given a null country when calling of then should throw a NullPointerException'() {
        when:
        CountryScope.of(null)

        then:
        thrown(NullPointerException)
    }

    def 'of - dataset tests - blank countries'() {
        when:
        CountryScope.of(country)

        then:
        thrown(IllegalArgumentException)

        where:
        country << ['', '  ']
    }

    def 'appliesTo - dataset tests - world scope'() {
        expect:
        CountryScope.WORLD.appliesTo(country)

        where:
        country << ['DE', '', null, 42]
    }

    def 'appliesTo - dataset tests - country scope'() {
        expect:
        CountryScope.of('DE').appliesTo(country) == expected

        where:
        country || expected
        'DE'    || true
        'FR'    || false
        'de'    || false
        null    || false
        42      || false
    }
}
