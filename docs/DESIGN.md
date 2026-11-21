# Design

## Overview and assumptions

- Scope: this version covers the engine, results with provenance and the declarative decision mapping.
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
- Assumption: the task gives no severities for the section 5 rules, so the acceptance test assumes one of each: `ERROR`,
  `WARNING` and `INFO`.

## Public API

- `RuleEngine.of(catalog).on(records).execute().results()`: the engine is immutable and can be shared, `on` captures the
  collection by reference and runs nothing, and `execute` evaluates on every call (per decision #9).
- A rule pairs a `RuleLogic` that returns a value such as `"blocked"` with a `DecisionMapping` such as `blocked →
  INVALID` and a default of `NOT_APPLICABLE`, so the logic never returns a decision (per decisions #5 and #6).
- Rules are built in code and collected with `RuleCatalog.builder()` (per decisions #5 and #8).
- `RunReport` exposes `results()`, and each `Result` holds the decision, the rule's severity and a `Provenance` (per
  decisions #9 and #11).
- Immutable types and the engine can be shared between threads, builders cannot, and a shared `Run` needs a collection
  that nobody modifies (per decision #10).

## Seams for host-supplied inputs

- Rules: a host supplies them through `RuleSource`, and a `RuleCatalog.Builder` loads them all-or-nothing (per decision
  #8).
- Rule logic: the host implements `RuleLogic`, which must be stateless and thread-safe (per decision #10).
- Records: the host wraps its data in `PartnerRecord` and passes a `Collection` to `RuleEngine.on` (per decisions #3 and
  #9).

## Trade-offs

- Results are held in memory in a `RunReport`, and a failing rule propagates out of `Run.execute()` (per decision #9).
- A `Provenance` holds the input values of every result, which costs memory and exposes field values, so its `toString`
  prints ids only (per decision #11).
- Rules skipped by status or scope leave no trace in the results (per decisions #4 and #7).
- A missing field and a null field look the same in provenance (per decisions #3 and #11).
- Rule ids are compared exactly, so near-duplicates such as `a` and `A` are not caught (per decision #8).
