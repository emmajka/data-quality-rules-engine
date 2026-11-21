# Design decisions

## #1 Two Gradle modules: library and example API
- status: accepted
- context: The task asks for a reusable library plus an example HTTP API that uses it.
- decision: `rules-engine` (library) and `example-api` (application, depends on the library). Shared build settings live in the root `subprojects {}` block.
- consequences: The example API's dependencies stay out of the library's classpath.

## #2 Java 21 via the Gradle toolchain
- status: accepted
- context: The task specifies Java 21 (or later), while the JDK launching Gradle can be newer.
- decision: Set `java.toolchain.languageVersion = 21` for all modules, so compilation and tests run on JDK 21 whatever JDK launches Gradle.
- consequences: A full JDK 21 must be installed locally, there is no toolchain auto-download plugin.

## #3 Records are generic fields wrapped in PartnerRecord
- status: accepted
- context: The task says a record is "supplied as a JSON object" but not which Java type the library takes, and records vary in shape (a missing field is a possible fault, F3).
- decision: The core takes a `PartnerRecord` (shallowly immutable), built from a `Map` of named fields that must include a string `id`, and has no JSON dependency. The host parses its JSON and wraps it.
- consequences: Hosts adapt JSON, typed objects or rows with one conversion; `get` returns null for a field that is absent or null. Nested values are not copied, so a record is only shallowly immutable.

## #4 Only RELEASED rules run, enforced by the engine
- status: accepted
- context: F6 says "Whatever the filters select, the engine runs `RELEASED` rules only", but not what happens to the other rules.
- decision: The engine always skips rules that are not `RELEASED`, whatever the caller's filters select. A skipped rule produces no Result and no error.
- consequences: A status filter can only narrow the `RELEASED` rules, so a filter for `DRAFT` or `DEPRECATED` runs nothing. Skipped rules are not counted in the run summary.

## #5 A Rule is a record with embedded logic and a separate mapping
- status: accepted
- context: F2 asks for the decision to come from a declarative mapping, while the logic that computes the value has to be code.
- decision: `Rule` is a record of an id, a label, `RuleMetadata` (categories are free-form strings), the `inputs`, a `RuleLogic` that reads a `PartnerRecord` (per decision #3) and returns a String value, and a `DecisionMapping` that turns the value into a decision. It is immutable as long as the logic is stateless, and the logic never returns a decision.
- consequences: Mapping and metadata are data but the logic is opaque code, so loading a whole catalog from data would need a registry of logics, which is future work. The `inputs` list must not be empty, names the fields the logic reads for provenance, and is trusted, not verified against the logic.

## #6 A decision mapping has an optional default, an unmapped value without one is an error
- status: accepted
- context: The task says a mapping has a default fallback, but its fixture table lists a default only for `countryBlocked`.
- decision: `DecisionMapping`, the mapping part of a rule (per decision #5), maps computed values to decisions and has an optional default. A value that is unmapped without a default, or a null value, is an error and is never guessed into a decision.
- consequences: `decide` throws `IllegalArgumentException` for an unmapped value and `NullPointerException` for a null one, which the engine is to report as rule errors (F3). A mapping with no entries and no default is accepted and fails on every value.

## #7 Country scope is WORLD or one exact country code
- status: accepted
- context: The task says a rule has an optional country scope and applies when its scope is `WORLD` or equals the record's country, but not how the values are compared.
- decision: `CountryScope`, part of the rule metadata (per decision #5), is `WORLD` or one non-blank country code, compared exactly and case-sensitively with the record's `country` value. A record whose country is missing or not a string matches only `WORLD` rules.
- consequences: A rule is always given a scope, with `WORLD` meaning no restriction. A rule skipped by scope produces no Result, like a non-`RELEASED` rule (per decision #4), and unlike the `NOT_APPLICABLE` decision, which only a mapping can produce.

## #8 The rule catalog is an immutable collection of rules built by registration
- status: accepted
- context: F1 needs a catalog of rules, the task names catalog sources as a seam, F6 filters by status, and a rule's stable id must identify it.
- decision: `RuleCatalog` is an immutable collection of rules (per decision #5) of every status, in registration order, built with a `RuleCatalog.Builder`; the `RELEASED` guard stays with the engine (per decision #4). Registering a rule whose id is already registered fails with an `IllegalArgumentException`, and `registerAll` registers all rules or none and reports every duplicate id at once.
- consequences: Hosts supply rules through the `RuleSource` seam or register them directly, and a catalog is safe to share across runs and threads as long as the rule logic is stateless (per decision #5). Ids are compared exactly and the catalog is held in memory, so a catalog of millions of rules would need indexing and rules as data.

## #9 The engine is an immutable singleton that prepares deferred, repeatable runs
- status: accepted
- context: F1 needs an engine that evaluates rules against a batch of records, which hosts such as an HTTP API embed as one shared instance.
- decision: `RuleEngine` is immutable and built from a `RuleCatalog` (per decision #8). `engine.on(records)` captures the `Collection<PartnerRecord>` by reference and runs nothing, and `Run.execute()` evaluates the applicable rules (per decisions #4 and #7) on every call and returns an immutable `RunReport`.
- consequences: The records are read when `execute()` is called, so the caller must not change the collection meanwhile. Until fault isolation exists, a failing rule propagates out of `execute()`, and `RunReport` has only `results()`.

## #10 Immutable types and the engine are thread-safe, working objects are confined to one thread
- status: accepted
- context: The task says hosts such as an HTTP API embed the library but does not mention thread-safety, so a shared engine needs an explicit policy.
- decision: Immutable types and `RuleEngine` are safe to share across threads, given stateless rule logic (per decision #5), and a type documents thread-safety only where it is conditional or absent. Builders are not thread-safe, and the caller owns the thread-safety of what it passes in, such as the record collection and the rule logic.
- consequences: The library adds no locking, so a caller that shares a `Run` must not modify its collection. A concurrent test of the shared run guards the claim.

## #11 A result carries provenance on every outcome, with the computed value and the input values, never the record
- status: accepted
- context: The task says each result carries provenance showing which rule, which record and which fields and values the rule read, without fixing its shape.
- decision: `Result` holds the decision, the rule's severity and a nested `Provenance` of rule id, record id, the value the rule computed (our addition) and the declared input fields with their values, on every result whatever its decision. It never references the record, and its `toString` prints ids, decision and severity only, so field values such as IBANs stay out of logs.
- consequences: An absent field and a null field both appear as null, and the inputs are trusted to be the fields the logic reads (per decision #5). The inputs list is not copied, so a caller building a `Provenance` passes an unmodifiable list without nulls.

## #12 Mapping values are compared exactly
- status: accepted
- context: The task says a mapping turns a computed value into a decision but not how values are compared.
- decision: `DecisionMapping` compares a computed value with its keys exactly, so case and surrounding spaces matter (per decision #6). A value that differs only in those is unmapped, so it takes the default or is an error without one.
- consequences: A rule must return the same labels its mapping uses, and nothing normalizes values.
