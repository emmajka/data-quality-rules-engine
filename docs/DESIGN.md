# Design

## Overview and assumptions

- Assumption: a record is a generic set of named fields, not a typed class (per decision #3). The library does not parse JSON. The host parses it and wraps the result in `PartnerRecord`.
- Assumption: a `PartnerRecord` needs a non-blank string `id` (per decision #3). Duplicate ids are not detected.
- Assumption: `PartnerRecord` copies the top-level fields but not nested values, so it is only shallowly immutable (per decision #3).
