# Java & Spring Boot

- Entry point: `./gradlew bootRun`; use the system default JVM (same as `java -version`), not a pinned Gradle toolchain. When the system JDK is newer than Spring Boot supports, set `options.release` in `build.gradle` to Spring Boot's max supported Java version and set `springBoot.mainClass` explicitly.
- Gradle configuration cache enabled in `gradle.properties` (`org.gradle.configuration-cache=true`); `./gradlew test` and `./gradlew bootRun` succeed with it on.
- Logs overwrite to ./logs/<project_name>.log per run; suppress stdout/stderr completely (including Spring banner and Java logs).
- Use src/ layout; configure via env vars, never CLI args.
- Modern Java idioms: use var for local variables, record for immutable data, and switch pattern matching.
- Keep code raw Java by default; reserve Spring exclusively for DI and scheduling.
