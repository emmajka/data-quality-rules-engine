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
