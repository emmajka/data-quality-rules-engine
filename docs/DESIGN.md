# Design

## Overview and assumptions

- Assumption: a record is a generic set of named fields, not a typed class (per decision #3). The library does not parse JSON. The host parses it and wraps the result in `PartnerRecord`.
- Assumption: a `PartnerRecord` needs a string `id` (per decision #3), and the id must not be blank. Duplicate record ids are not detected.
- Assumption: `PartnerRecord` copies the top-level fields but not nested values, so it is only shallowly immutable (per decision #3).
- Assumption: a rule is identified by its id, ids are compared exactly, and registering a duplicate id fails instead of replacing the rule (per decision #8).

## Seams for host-supplied inputs

- Rules: a host supplies them through `RuleSource`, and a `RuleCatalog.Builder` loads them all-or-nothing (per decision #8).
