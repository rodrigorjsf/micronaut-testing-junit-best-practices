# Project: micronaut-testing-junit-best-practices

Micronaut 4.10.7 application demonstrating testing best practices with JUnit 5.

## Tech Stack
- **Java 25** (LTS)
- **Micronaut 4.10.7** with Micronaut Data JDBC, Serde, Validation, OpenAPI
- **Maven** (micronaut-maven-plugin)
- **JUnit 5** + AssertJ + Testcontainers
- **PostgreSQL** (production and tests via Testcontainers)

## Build & Test
```bash
./mvnw compile          # Compile
./mvnw test             # Run tests (requires Docker for Testcontainers)
./mvnw package          # Build JAR
./mvnw mn:run           # Run application
```

## Project Structure
- `src/main/java/com/example/` — Application code (controllers, services, repositories, entities, models)
- `src/test/java/com/example/` — JUnit 5 tests
  - `AbstractIntegrationTest` — Base test class with @MicronautTest, TestPropertyProvider, leakage detection
  - `AbstractServerTest` — Extends above, adds HttpClient for controller tests
  - `fixtures/` — AuthorFixture, BookFixture interfaces with default helper methods
  - `PostgresqlTestContainer` — Singleton Testcontainers PostgreSQL container

## Key Patterns
- `javax.*` → `jakarta.*` (Micronaut 4 migration)
- `@Serdeable` on DTOs for micronaut-serde-jackson
- `TestPropertyProvider` for dynamic Testcontainers properties
- `@Requires(property = "spec.name")` for conditional mock bean activation
- Leakage detection via `@AfterEach` assertion on repository counts
