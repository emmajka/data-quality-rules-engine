# Design

## Overview and assumptions

- Scope: this version covers the engine, results with provenance, the declarative decision mapping, fault isolation and
  the run summary.
- Assumption: a record is a generic set of named fields, not a typed class (per decision #3). The library does not parse
  JSON. The host parses it and wraps the result in `PartnerRecord`.
- Assumption: a `PartnerRecord` needs a string `id` (per decision #3), and the id must not be blank. Duplicate record
  ids are not detected.
- Assumption: `PartnerRecord` copies the top-level fields but not nested values, so it is only shallowly immutable (per
  decision #3).
- Assumption: a rule's logic returns a short string value and a separate `DecisionMapping` turns it into a decision, so
  the mapping stays data (per decision #5).
- Assumption: a mapping's default is optional, and an unmapped value without a default, or a null value, is an error
  instead of a guessed decision (per decision #6). An empty value is valid and goes through the mapping like any other.
- Assumption: a decision mapping compares a computed value with its keys exactly, so case and surrounding spaces matter
  (per decision #12).
- Assumption: a rule's `inputs` name the fields its logic reads. They are trusted and not verified against the logic
  (per decision #5).
- Assumption: a rule is identified by its id, ids are compared exactly, and registering a duplicate id fails instead of
  replacing the rule (per decision #8).
- Assumption: a country scope is `WORLD` or one exact, case-sensitive country code, matched against the record's
  `country` field (per decision #7).
- Assumption: only `RELEASED` rules whose scope matches the record's country run, and other rules produce no result (per
  decisions #4 and #7).
- Assumption: every result carries provenance with the computed value and the input values, and an absent field shows as
  null like a null field (per decision #11).
- Assumption: a rule fails only by throwing. The engine does not validate records, so a missing field is null and the
  rule decides whether that fails (per decision #16).
- Assumption: a breakdown in the run summary is a count per constant, over all results, and errors are a single count
  (per decision #15).
- Assumption: the task gives no severities for the section 5 rules, so the acceptance test assumes one of each: `ERROR`,
  `WARNING` and `INFO`.

## Public API

- `RuleEngine.of(catalog).on(records).execute().results()`: the engine is immutable and can be shared, `on` captures the
  collection by reference and runs nothing, and `execute` evaluates on every call (per decision #14).
- A rule pairs a `RuleLogic` that returns a value such as `"blocked"` with a `DecisionMapping` such as `blocked →
  INVALID` and a default of `NOT_APPLICABLE`, so the logic never returns a decision (per decisions #5 and #6).
- Rules are built in code and collected with `RuleCatalog.builder()` (per decisions #5 and #8).
- `RunReport` exposes `results()` and, separately, `errors()`. Each `Result` holds the decision, the rule's severity and
  a `Provenance` (per decisions #14 and #11).
- `RunReport` also gives `summary()`, computed on each call: result and error counts and the results by decision and by
  severity, with every constant listed (per decision #15).
- Immutable types and the engine can be shared between threads, builders cannot, and a shared `Run` needs a collection
  that nobody modifies (per decision #10).

## Fault isolation

- A rule that throws an `Exception` on a record is caught for that rule and record. It becomes a `RuleError` in
  `RunReport.errors()` and the run goes on (per decisions #17 and #16).
- A `RuleError` holds the rule id, the record id and the exception the rule threw, so the cause chain and the stack
  trace are available (per decision #17).
- Results and errors are separate lists, and together they cover every applicable (rule, record) pair. A rule skipped by
  status or scope produces neither (per decisions #4, #7 and #17).
- A null record in the collection is a caller error and still throws a `NullPointerException`.

## Seams for host-supplied inputs

- Rules: a host supplies them through `RuleSource`, and a `RuleCatalog.Builder` loads them all-or-nothing (per decision
  #8).
- Rule logic: the host implements `RuleLogic`, which must be stateless and thread-safe (per decision #10).
- Records: the host wraps its data in `PartnerRecord` and passes a `Collection` to `RuleEngine.on` (per decisions #3 and
  #14).

## Trade-offs

- Results and errors are held in memory in a `RunReport` (per decision #14). A `RuleError` keeps the exception itself,
  which takes more memory than a description and can still be changed, so the report is not strictly immutable. Its
  `toString` omits the message, which can hold field values (per decision #17).
- An `Error`, such as a `StackOverflowError` from a runaway rule, is not caught and ends the run (per decision #17).
- A `Provenance` holds the input values of every result, which costs memory and exposes field values, so its `toString`
  prints ids only (per decision #11).
- Rules skipped by status or scope leave no trace in the results (per decisions #4 and #7).
- A missing field and a null field look the same in provenance (per decisions #3 and #11).
- Rule ids are compared exactly, so near-duplicates such as `a` and `A` are not caught (per decision #8).

## What I'd do next

- Bounded memory (F5): `execute` would pass each result and error to a caller-supplied sink instead of collecting them,
  and the summary would come from counters kept during the run. `RuleEngine.on` would take an `Iterable` of records,
  which supersedes decision #14, and the caller would set the batch size.
