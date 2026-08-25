# Java & Spring Boot

- `./gradlew bootRun` default build/run command; configured to use system default Java.
- Unless overridden by master requirements, Java logs to `./logs/<project_name>.log`; overwritten on each run; no log output sent to stdout/stderr.
- Unless overridden by master requirements, Spring Boot banner/version and Java logs on stdout are suppressed.
- Toolchains, src/ layout used.
- Env vars for config, not CLI args.
- Local variable type inference: `var`; `record` for immutable data; `switch` pattern matching.
- Raw Java preferred; Spring for dependency injection, scheduling.
