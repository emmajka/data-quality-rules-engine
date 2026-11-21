package pl.emmajk.dqrules

import groovy.json.JsonSlurper

/** Builds rules, catalogs and records for the specs, and reads the JSON fixtures in the test resources. */
class Fixtures {

    static Rule rule(Map overrides = [:]) {
        def args = [id        : 'rule',
                    label     : 'Rule',
                    status    : RuleStatus.RELEASED,
                    severity  : Severity.ERROR,
                    scope     : CountryScope.WORLD,
                    categories: [] as Set,
                    inputs    : ['vatId'],
                    logic     : { partner -> 'ok' } as RuleLogic,
                    mapping   : DecisionMapping.of([ok: Decision.VALID])] + overrides
        new Rule(args.id, args.label, new RuleMetadata(args.status, args.severity, args.scope, args.categories),
                args.inputs, args.logic, args.mapping)
    }

    static RuleCatalog catalog(Rule... rules) {
        RuleCatalog.builder().registerAll(rules.toList()).build()
    }

    static PartnerRecord record(Map fields) {
        PartnerRecord.of(fields)
    }

    static Object json(String resource) {
        def stream = Fixtures.getResourceAsStream(resource)
        if (stream == null) {
            throw new IllegalStateException("missing test resource ${resource}")
        }
        stream.withCloseable { new JsonSlurper().parse(new InputStreamReader(it, 'UTF-8')) }
    }

    static List<PartnerRecord> records(String resource) {
        json(resource).collect { PartnerRecord.of(it as Map) }
    }
}
