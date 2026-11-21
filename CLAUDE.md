# CLAUDE.md

## Project
- a reusable Java library that validates business-partner records against a catalog of data-quality rules, plus a small
  example HTTP API.
- full task description (requirements F1-F6, deliverables, evaluation criteria) can be found in `docs/task.md` file.
  Never edit it.
- design decisions live in `docs/DECISIONS.md`. `docs/DESIGN.md` (a deliverable) is derived from them and must stay
  consistent with them.
- priorities (time-boxed, a smaller slice done well beats a broad sketch): F1-F4 correct, then F5, then F6, then the
  example API. Keep `DESIGN.md` up to date along the way.
- all code, docs, tests and commit messages are in English.

## Design decisions (docs/DECISIONS.md)
- `docs/DECISIONS.md` contains numbered design decisions, indexed from 1. Numbers are never reused or renumbered.
- entry format: `title`, `status` (`accepted` or `superseded by #N`), `context` (optional), `decision` (mandatory),
  `consequences` (optional). Each of `context`, `decision` and `consequences` is at most 2 lines (about 240 characters),
  so an entry is at most about 10 lines.
- follow only decisions with status `accepted`, never a `superseded` one.
- decisions are immutable: never edit, renumber or delete one, whatever the reason. A change is a new decision that
  supersedes the old one, which only gets the status `superseded by #N`. The new decision restates what stays valid,
  because superseded decisions are not followed. Adding or superseding a decision needs my approval.
- if a decision looks wrong, incomplete, or conflicts with the code or another decision: stop and inform me. Propose a
  new or superseding entry and wait for my approval.
- a new decision that relates to an existing one should say so, for instance `per decision #N`.
- with every decision change, verify it against `README.md` and `docs/DESIGN.md`, inform me about any mismatch and
  propose a fix.
- ambiguities in the task are raised as proposed decisions, not resolved silently.

## Stack
- Java 21 via the Gradle toolchain. The task specifies Java 21 (or later).
- Gradle with the wrapper (`./gradlew`). The wrapper, including `gradle/wrapper/gradle-wrapper.jar`, is committed.
- test with Spock and Groovy, in `<module>/src/test/groovy`. Use the standard Gradle setup (core `groovy` plugin, JUnit
  Platform).
- Gradle core plugins (`java-library`, `groovy`, `application`) are fine. Any other plugin, framework, library or
  dependency needs my approval first.
- keep dependencies minimal. That includes the example API's choices (HTTP, JSON): ask before adding any.
- commands: full verification `./gradlew clean build` (or `make build`), tests only `make test`, single test `./gradlew
  test --tests '<fully.qualified.Spec>'`.

## Design
- keep it simple, keep the public API minimal.
- no abstractions without a concrete need. Seams are a concrete need where the task names them (catalog source, rule
  selection with composable filters, host-supplied inputs) or where streaming implies them (record input, result and
  error output).
- data models, value objects and containers (records, results, rules, the catalog) are immutable: Java records, enums,
  sealed types, final fields and unmodifiable collections. Use a final class instead of a Java record only when the
  representation must stay hidden (like `PartnerRecord`).
- mutable state is fine in short-lived working objects (builders, the engine's per-run counters). Keep it private, never
  share it between runs or threads, and hand out an immutable snapshot (like the run summary).

## In-code documentation
- keep in-code documentation short and concise, use simple vocabulary. Explain why, not what.
- Javadoc on every public class, public method and important constant, 1-2 lines of description. Add `@throws` or a
  contract note (nullability, thread-safety, who closes a stream) only where it matters.
- do not document every single piece of code, add documentation only if the code needs more context.
- with every method change, if an in-code documentation exists verify changes against it, analyze if in-code
  documentation requires an update, update if necessary.

## Workflow
- plan before code, wait for my approval. Plans state the trade-offs of non-obvious choices, I have to defend them in
  the interview.
- propose changes to this file, do not edit it on your own.
- order: approved plan, failing tests, implementation, green build.
- one concern per change, sized to a single commit. After each change propose a commit message and the list of files.
- verify changes and their correctness before saying they work or are done. Compiling and running `./gradlew clean
  build` are mandatory steps. Report failures as they are.
- if something is ambiguous, ask, do not guess. Batch questions together.
- no unrelated refactors or "improvements" unless I ask. That includes tests, for instance if a test class becomes too
  big ask before attempting to split it.

## Testing
- write tests using Spock and Groovy, with `given/when/then/expect/where` blocks.
- keep tests short and concise, limit single test to 50 lines of code at most.
- exception tests: `given ${argument description} when calling ${method name without arg list} then should throw
  ${exception name}`, as an example: `given a null record when calling validate then should throw a
  NullPointerException` (illustrative).
- return value tests: `given ${argument description} when calling ${method name without arg list} then should return
  ${result description}`.
- dataset tests: `${method name} - dataset tests`. If a single method has multiple dataset tests differentiate them by
  adding suffix `- ${scenario description}`. As an example: `validate - dataset tests - success scenarios`.
- use dataset tests only if they can be kept short and concise, and only if there are at least 2 scenarios.
- test successful paths and failure/exception paths. Attempt to find edge cases, make scenarios realistic.
- prefer simple fakes over mocks.
- tests we need, derived from the task (fixtures live in `<module>/src/test/resources`). The task itself requires only
  the section 5 scenario, a fault-isolation demo, and tests with run instructions:
  - the `docs/task.md` section 5 scenario as an acceptance test (3 records x 3 rules),
  - fault isolation: a failing rule or record does not stop the run, results and errors are both reported,
  - bounded memory: a lazily generated large input with a counting sink, no accumulation,
  - filter composition and the "only `RELEASED` rules run" invariant,
  - run summary counts by decision and by severity,
  - the example HTTP API.

## Git
- never run commit, push or stage (`git add`) by yourself, never rewrite history.
- never run other state-changing git commands: `reset`, `checkout --`, `stash`, `clean`, branch switching.
- leave changes uncommitted for my review, I will commit and push manually.

## Docs
- all md files, this one included: hard-wrap lines at 120 characters and indent continuation lines under their bullet,
  so nothing needs horizontal scrolling.
- `README.md`: how to build, run the tests and run the example API. It may only claim what the code and tests actually
  do.
- `docs/DESIGN.md` is the reviewer-facing design document, a task deliverable. Keep it short, it is written in my voice.
  - sections: Overview and assumptions, Public API, Seams for host-supplied inputs, Trade-offs. Add a section, for
    example fault isolation or bounded memory, only once that part is implemented.
  - it describes only what was done and decided: no future work, no notes about what is not implemented.
  - it is derived from `accepted` decisions only. Refer to them as `per decision #N`, do not repeat their reasoning.
  - it may only claim what the code and tests actually do.
  - never edit it on your own. After a change, name the affected section and propose the text, apply it only after my
    approval.
- state every assumption explicitly, the task asks for it.
