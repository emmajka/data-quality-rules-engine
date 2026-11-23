package pl.emmajk.dqrules

import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class RuleErrorSpec extends Specification {

    static final String EMOJI = '😀' // two chars: a surrogate pair

    def error(Map overrides = [:]) {
        def args = [ruleId   : 'vatFormat',
                    recordId : 'r1',
                    errorType: 'java.lang.IllegalStateException',
                    message  : 'boom',
                    origin   : 'com.acme.VatRule.compute(VatRule.java:42)'] + overrides
        new RuleError(args.ruleId, args.recordId, args.errorType, args.message, args.origin)
    }

    static frame(String className) {
        new StackTraceElement(className, 'run', 'Some.java', 7)
    }

    /** A failure whose stack trace has exactly one frame per class name. */
    static failing(String message, String... classNames) {
        def failure = new IllegalStateException(message)
        failure.stackTrace = classNames.collect { frame(it) } as StackTraceElement[]
        failure
    }

    /** The origin text expected for a failure created by {@link #failing}. */
    static trace(String... classNames) {
        classNames.collect { "${it}.run(Some.java:7)" }.join(' <- ')
    }

    def 'given valid components when calling the constructor then should expose them'() {
        when:
        def created = error()

        then:
        created.ruleId() == 'vatFormat'
        created.recordId() == 'r1'
        created.errorType() == 'java.lang.IllegalStateException'
        created.message() == 'boom'
        created.origin() == 'com.acme.VatRule.compute(VatRule.java:42)'
    }

    def 'constructor - dataset tests - null components'() {
        when:
        error((component): null)

        then:
        def e = thrown(NullPointerException)
        e.message == component

        where:
        component << ['ruleId', 'recordId', 'errorType', 'message', 'origin']
    }

    def 'constructor - dataset tests - blank components'() {
        when:
        error((component): text)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains(component)

        where:
        component   | text
        'ruleId'    | ''
        'ruleId'    | '  '
        'recordId'  | ''
        'recordId'  | '  '
        'errorType' | ''
        'errorType' | '  '
    }

    def 'constructor - dataset tests - empty message and origin'() {
        expect:
        error((component): '')."$component"() == ''

        where:
        component << ['message', 'origin']
    }

    def 'given an error with a message when calling toString then should return only the ids and the type'() {
        expect:
        error(message: 'no decision for value DE123').toString() ==
                'RuleError[ruleId=vatFormat, recordId=r1, errorType=java.lang.IllegalStateException]'
    }

    def 'given a failure when calling from then should return an error with the ids, the type and the message'() {
        when:
        def created = RuleError.from('vatFormat', 'r1', failing('boom', 'com.acme.VatRule'))

        then:
        created.ruleId() == 'vatFormat'
        created.recordId() == 'r1'
        created.errorType() == 'java.lang.IllegalStateException'
        created.message() == 'boom'
        created.origin() == trace('com.acme.VatRule')
    }

    def 'given a wrapped failure when calling from then should return an error describing the root cause'() {
        given:
        def root = failing('root message', 'com.acme.Root')
        def wrapper = new RuntimeException('wrapper message', new IllegalArgumentException('middle', root))
        wrapper.stackTrace = [frame('com.acme.Wrapper')] as StackTraceElement[]

        when:
        def created = RuleError.from('r', 'x', wrapper)

        then:
        created.errorType() == 'java.lang.IllegalStateException'
        created.message() == 'root message'
        created.origin() == trace('com.acme.Root')
    }

    def 'given causes that form a cycle when calling from then should return the last cause before the cycle'() {
        given:
        def first = new IllegalStateException('first')
        def second = new IllegalStateException('second', first)
        first.initCause(second)
        // a loop that never ends ignores interrupts, so @Timeout cannot stop it: wait on another thread
        def error = CompletableFuture.supplyAsync { RuleError.from('r', 'x', first) }

        expect:
        error.get(5, TimeUnit.SECONDS).message() == 'second'
    }

    def 'given a failure without a message when calling from then should return an empty message'() {
        expect:
        RuleError.from('r', 'x', new IllegalStateException()).message() == ''
    }

    def 'from - dataset tests - blank messages'() {
        expect:
        RuleError.from('r', 'x', failing(text)).message() == text

        where:
        text << ['  ', '\n']
    }

    def 'from - dataset tests - message length'() {
        expect:
        RuleError.from('r', 'x', failing(text)).message() == expected

        where:
        text                     || expected
        'a' * 499                || 'a' * 499
        'a' * 500                || 'a' * 500
        'a' * 501                || 'a' * 500
        'a' * 5000               || 'a' * 500
        'a' * 499 + EMOJI        || 'a' * 499
        'a' * 498 + EMOJI + 'b'  || 'a' * 498 + EMOJI
    }

    def 'from - dataset tests - origin frames'() {
        expect:
        RuleError.from('r', 'x', failing('m', *classNames)).origin() == expected

        where:
        classNames                                               || expected
        []                                                       || ''
        ['com.acme.A']                                           || trace('com.acme.A')
        ['com.acme.A', 'com.acme.B']                             || trace('com.acme.A', 'com.acme.B')
        ['com.acme.A', 'com.acme.B', 'com.acme.C', 'com.acme.D'] || trace('com.acme.A', 'com.acme.B', 'com.acme.C')
    }
}
