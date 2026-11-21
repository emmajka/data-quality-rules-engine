package pl.emmajk.dqrules

import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RuleEngineSpec extends Specification {

    def 'given a null catalog when calling of then should throw a NullPointerException'() {
        when:
        RuleEngine.of(null)

        then:
        def e = thrown(NullPointerException)
        e.message == 'catalog'
    }

    def 'given a null collection when calling on then should throw a NullPointerException'() {
        given:
        def engine = RuleEngine.of(Fixtures.catalog(Fixtures.rule()))

        when:
        engine.on(null)

        then:
        def e = thrown(NullPointerException)
        e.message == 'records'
    }

    def 'given a run shared by several threads when calling execute then should return the same results in every thread'() {
        given:
        def engine = RuleEngine.of(Fixtures.catalog(
                Fixtures.rule(id: 'x'), Fixtures.rule(id: 'y', scope: CountryScope.of('DE'))))
        def records = (1..200).collect { Fixtures.record(id: 'r' + it, country: it % 2 == 0 ? 'DE' : 'FR') }
        def run = engine.on(records)
        def expected = run.execute().results()
        def pool = Executors.newFixedThreadPool(8)
        def start = new CountDownLatch(1)

        when:
        def futures = (1..8).collect { pool.submit({ start.await(); run.execute().results() } as Callable) }
        start.countDown()
        def all = futures.collect { it.get(10, TimeUnit.SECONDS) }

        then:
        all.size() == 8
        all.every { it == expected }

        cleanup:
        pool.shutdownNow()
    }
}
