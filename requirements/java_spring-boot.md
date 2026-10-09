# Java & Spring Boot

- **Entry Point & Tooling**: `./gradlew bootRun` using system JVM (`java -version`). No pinned Gradle toolchains. If system JDK > Spring Boot max, set `options.release` in `build.gradle` and define `springBoot.mainClass`.
- **Gradle**: Enable `org.gradle.configuration-cache=true` in `gradle.properties`. Ensure `./gradlew test` and `./gradlew bootRun` pass with it active.
- **Code Clarity**: Favor simple, sequential logic over deep inheritance, single-impl interfaces, or generic abstractions. Keep packages flat.
- **Idioms & DTOs**: Use `var` when type is obvious, `record` (or plain classes) for immutable DTOs, and `switch` pattern matching over `if-else` chains.
- **Spring Scope**: Limit Spring strictly to DI and scheduled tasks. Core logic remains plain Java.
- **Config**: Read settings from env vars or `application.yml`/`application.properties` with fallbacks. No CLI args.
- **Logging**: Use SLF4J. Output to `stdout`/`stderr`. Silence Spring banner and noisy framework logs.
- **Formatting**: Do not let strict lint rules override clarity.
