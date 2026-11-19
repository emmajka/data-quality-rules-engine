package pl.emmajk.dqrules

import spock.lang.Specification

class RuleMetadataSpec extends Specification {

    def metadata(Map overrides = [:]) {
        def args = [status    : RuleStatus.RELEASED,
                    severity  : Severity.ERROR,
                    scope     : CountryScope.WORLD,
                    categories: ['tax'] as Set] + overrides
        new RuleMetadata(args.status, args.severity, args.scope, args.categories)
    }

    def 'given valid components when calling the constructor then should expose them'() {
        when:
        def created = new RuleMetadata(RuleStatus.DEPRECATED, Severity.INFO, CountryScope.of('DE'), ['tax'] as Set)

        then:
        created.status() == RuleStatus.DEPRECATED
        created.severity() == Severity.INFO
        created.scope() == CountryScope.of('DE')
        created.categories() == ['tax'] as Set
    }

    def 'given a status and a severity when calling of then should return metadata for every country without categories'() {
        when:
        def created = RuleMetadata.of(RuleStatus.DRAFT, Severity.WARNING)

        then:
        created.status() == RuleStatus.DRAFT
        created.severity() == Severity.WARNING
        created.scope() == CountryScope.WORLD
        created.categories().isEmpty()
    }

    def 'constructor - dataset tests - null components'() {
        when:
        metadata((component): null)

        then:
        def e = thrown(NullPointerException)
        e.message == component

        where:
        component << ['status', 'severity', 'scope', 'categories']
    }

    def 'given a categories set changed after creation when calling categories then should return the original set'() {
        given:
        def source = ['tax'] as Set
        def created = metadata(categories: source)

        when:
        source << 'bank'

        then:
        created.categories() == ['tax'] as Set
    }

    def 'given metadata when calling categories then should return an unmodifiable set'() {
        when:
        metadata().categories().add('x')

        then:
        thrown(UnsupportedOperationException)
    }
}
