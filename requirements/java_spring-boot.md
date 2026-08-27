# Java & Spring Boot

- Entry point: ./gradlew bootRun bound to system Java; set org.gradle.java.installations.auto-download=false in gradle.properties.
- Logs overwrite to ./logs/<project_name>.log per run; suppress stdout/stderr completely (including Spring banner and Java logs).
- Use src/ layout; configure via env vars, never CLI args.
- Modern Java idioms: use var for local variables, record for immutable data, and switch pattern matching.
- Keep code raw Java by default; reserve Spring exclusively for DI and scheduling.
