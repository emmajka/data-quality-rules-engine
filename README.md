# Data-quality rules engine

A Java 21 library that validates business-partner records against data-quality rules.
Task: [docs/task.md](docs/task.md). Design: [docs/DESIGN.md](docs/DESIGN.md). Decisions:
[docs/DECISIONS.md](docs/DECISIONS.md).

The library is the `rules-engine` module.

## Build and test

```bash
make build   # build and test everything
make test    # tests only
```

One test class:

```bash
./gradlew :rules-engine:test --tests 'pl.emmajk.dqrules.AcceptanceScenarioSpec'
```

## Using the library

```java
Rule vatFormat = new Rule("vatFormat", "VAT id format", RuleMetadata.of(RuleStatus.RELEASED, Severity.WARNING),
        List.of("vatId"),
        partner -> partner.get("vatId") instanceof String vatId && vatId.length() >= 9 ? "ok" : "bad",
        DecisionMapping.of(Map.of("ok", Decision.VALID, "bad", Decision.INVALID)));
RuleEngine engine = RuleEngine.of(RuleCatalog.builder().register(vatFormat).build());

RunReport report = engine
        .on(List.of(PartnerRecord.of(Map.of("id", "r1", "vatId", "DE111111111"))))
        .execute();

report.results();  // one Result per rule and record
report.errors();   // rules that threw, one RuleError per rule and record
report.summary();  // counts of results and errors, and of results by decision and severity
```
