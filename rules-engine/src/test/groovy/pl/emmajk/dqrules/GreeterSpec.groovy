package pl.emmajk.dqrules

import spock.lang.Specification

class GreeterSpec extends Specification {

    def 'given no arguments when calling greet then should return Hello, World!'() {
        given:
        def greeter = new Greeter()

        when:
        def result = greeter.greet()

        then:
        result == 'Hello, World!'
    }
}
