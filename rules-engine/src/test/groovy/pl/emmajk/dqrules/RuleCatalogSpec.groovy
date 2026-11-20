package pl.emmajk.dqrules

import spock.lang.Shared
import spock.lang.Specification

class RuleCatalogSpec extends Specification {

    @Shared def ruleA = rule('a')

    static rule(String id, RuleStatus status = RuleStatus.RELEASED) {
        new Rule(id, "Rule ${id}", RuleMetadata.of(status, Severity.ERROR), ['vatId'],
                { partner -> 'ok' } as RuleLogic, DecisionMapping.of([ok: Decision.VALID]))
    }

    def 'given rules of every status when calling build then should return all of them in registration order'() {
        given:
        def rules = [rule('c', RuleStatus.DEPRECATED), rule('a', RuleStatus.RELEASED), rule('b', RuleStatus.DRAFT)]
        def builder = RuleCatalog.builder()
        rules.each { builder.register(it) }

        expect:
        builder.build().rules() == rules
    }

    def 'given no rules when calling build then should return an empty catalog'() {
        expect:
        RuleCatalog.builder().build().rules().isEmpty()
    }

    def 'given a catalog when calling rules then should return an unmodifiable list'() {
        when:
        RuleCatalog.builder().register(ruleA).build().rules().add(rule('b'))

        then:
        thrown(UnsupportedOperationException)
    }

    def 'given a builder changed after build when calling rules then should return the original rules'() {
        given:
        def builder = RuleCatalog.builder().register(ruleA)
        def catalog = builder.build()

        when:
        builder.register(rule('b'))

        then:
        catalog.rules() == [ruleA]
        !catalog.contains('b')
    }

    def 'given a catalog when calling toBuilder then should return a builder starting with its rules'() {
        given:
        def catalog = RuleCatalog.builder().register(ruleA).build()
        def ruleB = rule('b')

        when:
        def extended = catalog.toBuilder().register(ruleB).build()

        then:
        extended.rules() == [ruleA, ruleB]
        catalog.rules() == [ruleA]
    }

    def 'find - dataset tests'() {
        given:
        def catalog = RuleCatalog.builder().register(ruleA).build()

        expect:
        catalog.find(id).orElse(null) == expected

        where:
        id        || expected
        'a'       || ruleA
        'missing' || null
        'A'       || null
    }

    def 'contains - dataset tests'() {
        given:
        def catalog = RuleCatalog.builder().register(ruleA).build()

        expect:
        catalog.contains(id) == expected

        where:
        id        || expected
        'a'       || true
        'missing' || false
        'A'       || false
    }

    def 'find and contains - dataset tests - null id'() {
        given:
        def catalog = RuleCatalog.builder().register(ruleA).build()

        when:
        catalog."$method"(null)

        then:
        thrown(NullPointerException)

        where:
        method << ['find', 'contains']
    }

    def 'register - dataset tests - duplicate ids'() {
        given:
        def builder = RuleCatalog.builder().register(ruleA)

        when:
        builder.register(duplicate)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("'a'")

        where:
        duplicate << [rule('a'), ruleA]
    }

    def 'given a rejected rule when calling build then should return a catalog with the first rule only'() {
        given:
        def builder = RuleCatalog.builder().register(ruleA)

        when:
        builder.register(rule('a'))

        then:
        thrown(IllegalArgumentException)

        when:
        def catalog = builder.build()

        then:
        catalog.rules() == [ruleA]
    }

    def 'given ids that differ only in case when calling register then should register both'() {
        when:
        def catalog = RuleCatalog.builder().register(rule('a')).register(rule('A')).build()

        then:
        catalog.rules().collect { it.id() } == ['a', 'A']
    }

    def 'given a null rule when calling register then should throw a NullPointerException'() {
        when:
        RuleCatalog.builder().register(null)

        then:
        def e = thrown(NullPointerException)
        e.message == 'rule'
    }

    def 'given an iterable of rules when calling registerAll then should register them in iteration order'() {
        given:
        def rules = [rule('b'), rule('a')]
        def iterable = { -> rules.iterator() } as Iterable

        expect:
        RuleCatalog.builder().registerAll(iterable).build().rules() == rules
    }

    def 'registerAll - dataset tests - duplicate ids'() {
        given:
        def builder = RuleCatalog.builder()
        existing.each { builder.register(rule(it)) }

        when:
        builder.registerAll(batch.collect { rule(it) })

        then:
        def e = thrown(IllegalArgumentException)
        expected.every { e.message.count("'${it}'") == 1 }
        ((existing + batch).unique() - expected).every { !e.message.contains("'${it}'") }

        where:
        existing | batch                || expected
        []       | ['a', 'b', 'a']      || ['a']
        []       | ['a', 'a', 'a']      || ['a']
        ['a']    | ['b', 'a']           || ['a']
        ['a']    | ['a', 'b', 'b', 'c'] || ['a', 'b']
    }

    def 'given a batch with a duplicate when calling registerAll then should register none of the rules'() {
        given:
        def builder = RuleCatalog.builder().register(ruleA)

        when:
        builder.registerAll([rule('b'), rule('a')])

        then:
        thrown(IllegalArgumentException)

        when:
        def catalog = builder.build()

        then:
        catalog.rules() == [ruleA]
    }

    def 'given a batch with a null rule when calling registerAll then should throw a NullPointerException and register none'() {
        given:
        def builder = RuleCatalog.builder()

        when:
        builder.registerAll([rule('a'), null])

        then:
        def e = thrown(NullPointerException)
        e.message == 'rule'
        builder.build().rules().isEmpty()
    }

    def 'given a null iterable when calling registerAll then should throw a NullPointerException'() {
        when:
        RuleCatalog.builder().registerAll(null)

        then:
        def e = thrown(NullPointerException)
        e.message == 'rules'
    }

    def 'given an empty iterable when calling registerAll then should leave the registered rules unchanged'() {
        given:
        def builder = RuleCatalog.builder().register(ruleA)

        when:
        builder.registerAll([])

        then:
        builder.build().rules() == [ruleA]
    }

    def 'given registered rules when calling registerAll then should append the new rules after them'() {
        given:
        def builder = RuleCatalog.builder().register(rule('c'))

        when:
        builder.registerAll([rule('b'), rule('a')])

        then:
        builder.build().rules().collect { it.id() } == ['c', 'b', 'a']
    }

    def 'given a source when calling registerFrom then should register its rules'() {
        given:
        def rules = [rule('a'), rule('b')]
        def source = { -> rules } as RuleSource

        expect:
        RuleCatalog.builder().registerFrom(source).build().rules() == rules
    }

    def 'given a null source when calling registerFrom then should throw a NullPointerException'() {
        when:
        RuleCatalog.builder().registerFrom(null)

        then:
        def e = thrown(NullPointerException)
        e.message == 'source'
    }

    def 'given a source returning null when calling registerFrom then should throw a NullPointerException'() {
        given:
        def source = { -> null } as RuleSource

        when:
        RuleCatalog.builder().registerFrom(source)

        then:
        def e = thrown(NullPointerException)
        e.message == 'source returned null rules'
    }

    def 'given a source that throws when calling registerFrom then should propagate the failure and keep the registered rules'() {
        given:
        def source = { -> throw new IllegalStateException('boom') } as RuleSource
        def builder = RuleCatalog.builder().register(ruleA)

        when:
        builder.registerFrom(source)

        then:
        def e = thrown(IllegalStateException)
        e.message == 'boom'
        builder.build().rules() == [ruleA]
    }

    def 'given an iterable failing midway when calling registerAll then should propagate the failure and register none'() {
        given:
        def served = false
        def iterator = [hasNext: { true },
                        next   : {
                            if (served) {
                                throw new IllegalStateException('boom')
                            }
                            served = true
                            rule('a')
                        }] as Iterator
        def iterable = { -> iterator } as Iterable
        def builder = RuleCatalog.builder()

        when:
        builder.registerAll(iterable)

        then:
        def e = thrown(IllegalStateException)
        e.message == 'boom'
        builder.build().rules().isEmpty()
    }
}
