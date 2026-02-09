# Conversion Guide: Groovy/Spock to Java/JUnit 5 with Micronaut 4

This document details **everything** that needed to be adapted, changed, and added to convert the tests
in this project from Groovy/Spock to Java/JUnit 5, while simultaneously migrating from Micronaut 2 to Micronaut 4.

> **Original project**: [ilopmar/micronaut-testing-best-practices](https://github.com/ilopmar/micronaut-testing-best-practices)

---

## Table of Contents

1. [Conversion Overview](#1-conversion-overview)
2. [Base Class Hierarchy](#2-base-class-hierarchy)
3. [Testcontainers and TestPropertyProvider](#3-testcontainers-and-testpropertyprovider)
4. [Groovy Traits to Java Interfaces](#4-groovy-traits-to-java-interfaces)
5. [Data Leakage Detector](#5-data-leakage-detector)
6. [Global Service Mocking with @Requires](#6-global-service-mocking-with-requires)
7. [Per-Test Mocking with @Primary and @Requires by Spec Name](#7-per-test-mocking-with-primary-and-requires-by-spec-name)
8. [Secondary Embedded Server for External API Mocking](#8-secondary-embedded-server-for-external-api-mocking)
9. [Parameterized Tests: Spock where/then to @ParameterizedTest](#9-parameterized-tests-spock-wherethen-to-parameterizedtest)
10. [@MicronautTest(transactional = false) — The Why](#10-micronauttesttransactional--false--the-why)
11. [@ExecuteOn(TaskExecutors.BLOCKING) — Event Loop Blocking](#11-executeontaskexecutorsblocking--event-loop-blocking)
12. [Package Visibility in Java vs Groovy](#12-package-visibility-in-java-vs-groovy)
13. [AssertJ vs JUnit Assertions on Java 25](#13-assertj-vs-junit-assertions-on-java-25)
14. [javax to jakarta Migration](#14-javax-to-jakarta-migration)
15. [@Serdeable and micronaut-serde](#15-serdeable-and-micronaut-serde)
16. [@JsonNaming vs @JsonProperty](#16-jsonnaming-vs-jsonproperty)
17. [Test Dependencies in pom.xml](#17-test-dependencies-in-pomxml)
18. [Conversion Map: File by File](#18-conversion-map-file-by-file)

---

## 1. Conversion Overview

### Friendly Explanation

Imagine you have a test suite written in Groovy with Spock — a very expressive language
where tests read almost like English sentences. Now you need to rewrite everything in plain Java with
JUnit 5. The good news: the concepts are the same. The bad news: Java is more verbose and lacks
some of Groovy's syntactic conveniences.

| Before (Groovy/Spock)                   | After (Java/JUnit 5)                             |
|-----------------------------------------|--------------------------------------------------|
| `class FooSpec extends Specification`   | `class FooTest extends AbstractIntegrationTest`  |
| Traits (`implements PostgresqlFixture`) | Interfaces with default methods                  |
| `setup()` / `cleanup()`                | `@BeforeEach` / `@AfterEach`                     |
| `setupSpec()` / `cleanupSpec()`        | `@BeforeAll` / `@AfterAll`                       |
| `where:` / `then:` blocks              | `@ParameterizedTest` + `@MethodSource`           |
| `thrown(Exception)`                     | `assertThrows(Exception.class, () -> ...)`       |
| `@Unroll`                              | `@ParameterizedTest(name = "...")`               |
| `response.status == HttpStatus.OK`     | `assertEquals(HttpStatus.OK, response.status())` |

### Technical Deep Dive

The Spock Framework operates on the Groovy AST (Abstract Syntax Tree), transforming the `given:/when:/then:`
blocks into JUnit-compatible bytecode at compile time. When migrating to plain JUnit 5, we lose this
transformation and need to express each test phase explicitly.

JUnit 5 uses the `Extension` model instead of inheritance like Spock (which uses `Specification`).
Micronaut integrates via `MicronautJunit5Extension`, which intercepts the test lifecycle to
initialize the `ApplicationContext`, perform dependency injection, and control transactions.

---

## 2. Base Class Hierarchy

### Friendly Explanation

In the original project, there were several base "Specifications" in Groovy that added functionality
via inheritance and traits. We condensed everything into two abstract classes:

```
AbstractIntegrationTest          <-- everything that needs a database
    |
    +-- AbstractServerTest       <-- everything that also needs an HTTP client
```

If your test only needs **the database** (repositories, services), extend `AbstractIntegrationTest`.
If it needs to **make HTTP calls** to the embedded server, extend `AbstractServerTest`.

### Technical Deep Dive

```java
// AbstractIntegrationTest.java
@Testcontainers(disabledWithoutDocker = true)
@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest implements TestPropertyProvider {

    @Inject
    protected AuthorRepository authorRepository;

    @Inject
    protected BookRepository bookRepository;

    // Getters required for fixture interfaces to access repositories
    public AuthorRepository getAuthorRepository() {
        return authorRepository;
    }

    public BookRepository getBookRepository() {
        return bookRepository;
    }

    @Override
    @NonNull
    public Map<String, String> getProperties() {
        PostgresqlTestContainer.init();
        Map<String, String> props = new HashMap<>();
        props.put("datasources.default.url", PostgresqlTestContainer.getJdbcUrl());
        props.put("datasources.default.username", PostgresqlTestContainer.getUsername());
        props.put("datasources.default.password", PostgresqlTestContainer.getPassword());
        props.put("datasources.default.schema-generate", "CREATE_DROP");
        props.put("datasources.default.dialect", "POSTGRES");
        props.put("mockSecurityService", String.valueOf(mockSecurityServiceEnabled()));
        String specName = getSpecName();
        if (specName != null) {
            props.put("spec.name", specName);
        }
        return props;
    }

    protected boolean mockSecurityServiceEnabled() {
        return true;
    }

    protected String getSpecName() {
        return null;
    }

    @AfterEach
    void checkLeakage() { /* ... */ }
}
```

```java
// AbstractServerTest.java
public abstract class AbstractServerTest extends AbstractIntegrationTest {

    @Inject
    @Client("/")
    protected HttpClient httpClient;

    protected BlockingHttpClient getClient() {
        return httpClient.toBlocking();
    }
}
```

**Why `public abstract`?** In Groovy, the default visibility is `public`. In Java, the default
visibility is *package-private*. Since test classes live in different sub-packages
(`com.example.controllers`, `com.example.services`, etc.) and the base classes live in `com.example`,
they **must** be `public` to be visible. The same applies to fields and methods that need to be
accessed — `protected` at minimum.

**Why `TestPropertyProvider`?** `TestPropertyProvider` is a Micronaut Test interface that
allows providing configuration properties before the `ApplicationContext` is created. The
`getProperties()` method is called **before** context initialization, which lets us pass the
dynamic Testcontainers URL (only known at runtime). This replaces the Groovy configuration
"fixtures" (`ConfigurationFixture`, `PostgresqlFixture`).

**Why `@Testcontainers(disabledWithoutDocker = true)`?** So that if the test suite is executed
in an environment where Docker is not running, the tests that depend on Docker images will be
skipped instead of failing.

**Why `@TestInstance(Lifecycle.PER_CLASS)`?** `TestPropertyProvider` is a non-static interface.
By default, JUnit 5 creates a **new instance** of the test class for each `@Test` method. But
`getProperties()` needs to be called on the instance that will receive injection — the same instance
that executes the tests. `PER_CLASS` ensures a single instance is created for the entire class,
allowing `getProperties()` to work correctly.

> **Rule**: Every class that implements `TestPropertyProvider` in Micronaut **requires**
> `@TestInstance(Lifecycle.PER_CLASS)`. Without it, you'll see cryptic errors like
> "No bean of type TestPropertyProvider found".

---

## 3. Testcontainers and TestPropertyProvider

### Friendly Explanation

Instead of needing PostgreSQL installed locally, we use Testcontainers to spin up a
Docker container with Postgres automatically. The container is created once and shared across
all tests (Singleton pattern) to avoid the ~3s startup overhead per test class.

### Technical Deep Dive

```java
public final class PostgresqlTestContainer {

    private static PostgreSQLContainer<?> container;

    public static void init() {
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withReuse(true)
                    .withLabel("com.example.demo", "postgresqlTestcontainersReuse");
            container.start();
        }
    }

    public static String getJdbcUrl() {
        return container.getJdbcUrl();
    }

    public static String getUsername() {
        return container.getUsername();
    }

    public static String getPassword() {
        return container.getPassword();
    }
}
```

**Manual Singleton vs `@Testcontainers`**: We chose not to use `@Testcontainers` +
`@Container` from JUnit Jupiter because those annotations control the lifecycle per test class.
With the manual singleton, **a single container** serves all classes. This matters because:

1. Micronaut creates a separate `ApplicationContext` for each test class (each `@MicronautTest`).
2. Each context receives the container URL via `TestPropertyProvider.getProperties()`.
3. The `schema-generate: CREATE_DROP` in `getProperties()` makes Micronaut recreate the tables
   for each new context — ensuring schema isolation.

**`withReuse(true)`**: Allows the container to survive between Maven runs if
`testcontainers.reuse.enable=true` is set in `~/.testcontainers.properties`. In CI environments,
this is ignored and a new container is created.

---

## 4. Groovy Traits to Java Interfaces

### Friendly Explanation

In Groovy, *traits* allow "mixing in" functionality into a class without multiple inheritance.
The original project used traits like `AuthorFixture`, `BookFixture`, `RepositoriesFixture`, etc.

In Java, we don't have traits, but we have **interfaces with default methods** (since Java 8).
The concept is identical: the interface defines utility methods with default implementations,
and the test class simply does `implements AuthorFixture`.

### Technical Deep Dive

```java
// Fixture interface — equivalent to the Groovy trait
public interface AuthorFixture {

    // This abstract method is the "contract" — implementors must provide the repository.
    // In practice, AbstractIntegrationTest already provides it via getAuthorRepository().
    AuthorRepository getAuthorRepository();

    default AuthorEntity saveAuthor(String name) {
        return getAuthorRepository().save(new AuthorEntity(name));
    }

    default AuthorEntity saveAuthor() {
        return saveAuthor("author name");
    }

    default CreateAuthorRequest createAuthorRequest(String name) {
        return new CreateAuthorRequest(name);
    }

    default CreateAuthorRequest createAuthorRequest() {
        return createAuthorRequest("Stephen King");
    }
}
```

**How does resolution work?** When `AuthorControllerTest extends AbstractServerTest implements AuthorFixture`:

1. `AuthorFixture` requires the method `getAuthorRepository()`.
2. `AbstractIntegrationTest` (grandparent class) defines `public AuthorRepository getAuthorRepository()`.
3. The Java compiler resolves the implementation via inheritance — the contract is satisfied.

This pattern is the closest Java equivalent to Groovy traits. The difference is that in Groovy,
traits can have state (fields). In Java, interfaces can only have constants (`static final`).
That's why the repositories live in the base class, and the interfaces only *consume* them via
abstract methods.

**Why `public` on the getters?** Interface default methods are `public` by definition.
The `getAuthorRepository()` method in the base class also needs to be `public` to satisfy
the interface contract. If it were `protected` or package-private, the compiler would complain.

---

## 5. Data Leakage Detector

### Friendly Explanation

The "leakage detector" is an `@AfterEach` that checks if the database is empty after each test.
If a test forgets to clean up its data, the detector fails and reports which type of data leaked.
This ensures isolation between tests — no test depends on data from another.

### Technical Deep Dive

```java
@AfterEach
void checkLeakage() {
    assertThat(bookRepository.count())
            .describedAs("Book leakage detected - ensure test cleans up its data")
            .isZero();
    assertThat(authorRepository.count())
            .describedAs("Author leakage detected - ensure test cleans up its data")
            .isZero();
}
```

**Why manual cleanup instead of rollback?** Because we use `@MicronautTest(transactional = false)`.
Without a test-managed transaction, each operation is committed immediately. Each test is
responsible for calling `repository.deleteAll()` at the end. This is **intentional** — it simulates
the real application behavior (see section 10 for why).

**Deletion order**: When there are foreign keys, delete `bookRepository.deleteAll()` **before**
`authorRepository.deleteAll()`. Otherwise, the database rejects the deletion due to FK violation.

---

## 6. Global Service Mocking with @Requires

### Friendly Explanation

Most tests don't need the real security service — it would be inconvenient to have to
pass `username=admin` in every request just for the test to pass. So we created a `MockSecurityService`
that always returns `true` (access allowed).

The trick is: this mock is only activated when the property `mockSecurityService=true`. Most
tests inherit this property from the base class. When a test needs the real service (like
`AuthorControllerFindAuthorWithSecurityTest`), it simply overrides the method:

```java
@Override
protected boolean mockSecurityServiceEnabled() {
    return false;
}
```

### Technical Deep Dive

```java
@Primary
@Singleton
@Requires(env = Environment.TEST)
@Requires(property = "mockSecurityService", value = StringUtils.TRUE)
public class MockSecurityService implements SecurityService {

    @Override
    public boolean canUserAccess(@Nullable String username) {
        return true;
    }
}
```

**How it works under the hood:**

1. `@Requires(env = Environment.TEST)` — The bean is only registered when the environment is `test`.
   Micronaut detects the `test` environment automatically when running via `@MicronautTest`.

2. `@Requires(property = "mockSecurityService", value = "true")` — The bean is only registered
   when this property is present and equals `"true"`. The `TestPropertyProvider` in the base
   class provides this property.

3. `@Primary` — When there are two beans of the same type (`SecurityServiceImpl` and `MockSecurityService`),
   `@Primary` ensures the mock takes priority in injection.

**Container decision flow:**

```
ApplicationContext initializing...
  |
  +-- SecurityServiceImpl (@Singleton) -> always registered
  |
  +-- MockSecurityService (@Primary @Singleton)
  |       +-- @Requires(env=TEST) -> are we in test? YES
  |       +-- @Requires(property="mockSecurityService"="true") -> property exists and is "true"?
  |                                                                YES (for most tests)
  |
  +-- Injection of SecurityService -> @Primary MockSecurityService wins
```

When `mockSecurityServiceEnabled()` returns `false`, the property is `"false"`, the second
`@Requires` fails, `MockSecurityService` is not registered, and `SecurityServiceImpl` is the only
candidate.

**For future development**: This pattern is very useful when you want a default behavior
for tests but need the real one in some cases. Each test class generates its own
`ApplicationContext`, so the properties of one class don't affect another.

---

## 7. Per-Test Mocking with @Primary and @Requires by Spec Name

### Friendly Explanation

Sometimes you want to replace a bean **only in a specific test**. For example, to simulate that
`AuthorService` throws an exception. To do this, we create a mock as an inner class of the test with
`@Requires(property = "spec.name", value = "TestName")`.

### Technical Deep Dive

```java
class AuthorControllerMockServiceTest extends AbstractServerTest implements AuthorFixture {

    @Override
    protected String getSpecName() {
        return "AuthorControllerMockServiceTest";
    }

    @Test
    void createAuthorFailsWhenServiceThrows() {
        HttpRequest<?> request = HttpRequest.POST("/authors", createAuthorRequest());

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
                () -> getClient().exchange(request, Author.class));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    @Singleton
    @Primary
    @Requires(property = "spec.name", value = "AuthorControllerMockServiceTest")
    static class AuthorServiceMock implements AuthorService {

        @Override
        public Author saveAuthor(String name) {
            throw new RuntimeException("There was an error saving the author.");
        }

        @Override
        public void addBookToAuthor(SaveBook saveBook) {
        }

        @Override
        public Optional<Author> findAuthorByName(String name) {
            return Optional.empty();
        }
    }
}
```

**Why inner `static` class?** Because Micronaut scans the classpath and finds the bean.
The inner class must be `static` so that Micronaut can instantiate it without an instance
of the outer class (which is the test itself).

**Activation scope**: `getSpecName()` returns `"AuthorControllerMockServiceTest"`, which is
passed as the `spec.name` property via `TestPropertyProvider.getProperties()`. Since each test
class has its own `ApplicationContext`, the mock is only activated in this specific test.

**For future development**: If you create more inline mocks, remember:

- Always return the corresponding `getSpecName()`.
- Use `@Primary` to ensure priority over the real bean.
- Use `@Requires(property = "spec.name", value = "...")` to limit the scope.
- The inner class **must** be `static`.

---

## 8. Secondary Embedded Server for External API Mocking

### Friendly Explanation

The `MovieController` calls an external API (OMDB) to search for movies. In tests, we don't want
to depend on a real API. The solution: spin up a **second HTTP server** inside the test that
responds as if it were the OMDB API, and configure the application to point to it.

### Technical Deep Dive

```java
class MovieControllerTest extends AbstractServerTest {

    private final int omdbPort = SocketUtils.findAvailableTcpPort();
    private EmbeddedServer omdbServer;

    @Override
    @NonNull
    public Map<String, String> getProperties() {
        Map<String, String> props = super.getProperties();
        props.put("omdb.base-url", "http://localhost:" + omdbPort);
        return props;
    }

    @BeforeAll
    void startOmdbMock() {
        PostgresqlTestContainer.init();
        Map<String, Object> config = new HashMap<>();
        config.put("micronaut.server.port", omdbPort);
        config.put("spec.name", "MovieControllerTest");
        config.put("datasources.default.url", PostgresqlTestContainer.getJdbcUrl());
        config.put("datasources.default.username", PostgresqlTestContainer.getUsername());
        config.put("datasources.default.password", PostgresqlTestContainer.getPassword());
        config.put("datasources.default.schema-generate", "NONE");  // CRITICAL!
        config.put("mockSecurityService", "true");
        omdbServer = ApplicationContext.run(EmbeddedServer.class, config);
    }

    @AfterAll
    void stopOmdbMock() {
        if (omdbServer != null) omdbServer.close();
    }

    @Controller("/")
    @Requires(property = "spec.name", value = "MovieControllerTest")
    static class OmdbMock {
        @Get
        String findMovie(@QueryValue("t") String title) {
            return """
                    {"Title":"Star Wars: Episode IV - A New Hope","Year":"1977",...}""";
        }
    }
}
```

**`schema-generate: NONE` — The most dangerous pitfall:**

The secondary embedded server creates its own `ApplicationContext`, which loads the application's
`application.yml`. If `application.yml` has `schema-generate: CREATE_DROP`, the second server
will **DROP and CREATE** the tables on the same PostgreSQL database the main server is using.
Result: the tables disappear for the main server, and all tests fail with
`relation "author" does not exist`.

The solution is to explicitly configure `schema-generate: NONE` on the second server:

```java
config.put("datasources.default.schema-generate", "NONE");
```

**`SocketUtils.findAvailableTcpPort()`**: Finds a random available TCP port to avoid
conflicts. This is necessary because the main server already occupies a port, and if two tests
ran in parallel on the same port, there would be a conflict.

**Why does the mock need database configuration?** The second `ApplicationContext` scans the
classpath and tries to instantiate all beans — including repositories that need a datasource.
Without the database properties, the context would fail on initialization. The alternative would be
to configure scanning to exclude packages, but this would be more complex and fragile.

---

## 9. Parameterized Tests: Spock where/then to @ParameterizedTest

### Friendly Explanation

In Spock, you would write:

```groovy
def "save author with invalid name fails"() {
    when:
    authorService.saveAuthor(name)
    then:
    thrown(ConstraintViolationException)
    where:
    name << ["", null]
}
```

In JUnit 5, we use `@ParameterizedTest` with `@MethodSource`:

```java
@ParameterizedTest(name = "saveAuthor(\"{0}\") triggers ConstraintViolationException")
@MethodSource("saveAuthorInvalidArgs")
void saveAuthorTriggersConstraintViolation(String name, String field, String errorMessage) {
    ConstraintViolationException ex = assertThrows(ConstraintViolationException.class,
            () -> authorService.saveAuthor(name));

    assertThat(ex.getConstraintViolations())
            .anyMatch(v -> v.getPropertyPath().toString().contains(field));
}

static Stream<Arguments> saveAuthorInvalidArgs() {
    return Stream.of(
            Arguments.of("", "saveAuthor.name", "must not be blank"),
            Arguments.of(null, "saveAuthor.name", "must not be blank")
    );
}
```

### Technical Deep Dive

**The source method must be `static`**: Because by default JUnit 5 uses `Lifecycle.PER_METHOD`...
but wait — we use `PER_CLASS`! With `PER_CLASS`, the source method could be non-static.
However, keeping it `static` is safer and more conventional, and works in both modes.

**`@ParameterizedTest(name = "...")`**: The `name` attribute uses `{0}`, `{1}`, etc. to reference
the parameters. This generates descriptive names in the test report, similar to Spock's `@Unroll`.

**`@MethodSource` vs `@CsvSource`**: We use `@MethodSource` because we need to pass `null` as
an argument, which is not possible with `@CsvSource` (which works with Strings). Additionally,
`@MethodSource` allows passing complex objects like `SaveBook`.

---

## 10. @MicronautTest(transactional = false) — The Why

### Friendly Explanation

This was the hardest bug to diagnose. The scenario:

1. The test calls `POST /authors` via HTTP.
2. The server receives the request, saves the author to the database, and **commits the transaction**.
3. The test verifies the response — all OK.
4. The `@AfterEach` (leakage detector) tries to count authors to verify cleanup.
5. **But if `transactional = true`**: the framework wraps the test in a transaction that does a **rollback**
   at the end. The cleanup (`authorRepository.deleteAll()`) runs inside this transaction that will be discarded.
   Result: the data from step 2 remains in the database, and the leakage detector sees "leaked" data.

### Technical Deep Dive

When `@MicronautTest(transactional = true)` (the default):

```
Test thread (transaction A):
  1. test method starts -> transaction A opens
  2. httpClient.exchange(POST /authors) -> HTTP request sent

Server thread (transaction B):
  3. AuthorController receives request
  4. AuthorService.saveAuthor() -> transaction B opens
  5. authorRepository.save() -> INSERT executed
  6. transaction B COMMIT -> data persisted in database

Test thread (transaction A, continuing):
  7. response received, assertions OK
  8. authorRepository.deleteAll() -> DELETE executed inside transaction A
  9. @AfterEach checkLeakage() -> SELECT COUNT(*) inside transaction A (sees 0 — correct locally)
  10. transaction A ROLLBACK -> the DELETE from step 8 is reverted
  11. The data from step 5 REMAINS in the database -> next test sees "leaked" data
```

When `@MicronautTest(transactional = false)`:

```
Test thread (no managed transaction):
  1. test method starts (no transaction)
  2. httpClient.exchange(POST /authors)

Server thread (transaction B):
  3-6. Same flow, COMMIT

Test thread (continuing):
  7. response received, assertions OK
  8. authorRepository.deleteAll() -> DELETE executed and COMMITTED immediately
  9. @AfterEach checkLeakage() -> SELECT COUNT(*) returns 0 (data actually deleted)
```

The inverse problem also occurred: `AuthorControllerFindAuthorTest` called `saveAuthor()` in
the test to create setup data, but with `transactional = true`, this INSERT stayed in an
**uncommitted** transaction — invisible to the server (which uses a different connection/transaction).
Result: the server couldn't find the author and returned 404.

**Practical rule**: If your tests make HTTP calls to the embedded server, **always** use
`transactional = false`. HTTP tests are inherently multi-transaction (test vs server).
Only use `transactional = true` in unit/integration tests that don't go through the HTTP layer.

---

## 11. @ExecuteOn(TaskExecutors.BLOCKING) — Event Loop Blocking

### Friendly Explanation

In Micronaut 2, you could make blocking HTTP calls inside a controller without any issues.
In Micronaut 4, the framework detects this and throws an exception:

> "You are trying to run a BlockingHttpClient operation on a netty event loop thread."

The solution: annotate the controller with `@ExecuteOn(TaskExecutors.BLOCKING)` so it runs on
a thread from a separate pool, where blocking is allowed.

### Technical Deep Dive

Netty uses an event loop model: a small number of threads (usually = number of CPUs)
processes **all** requests in a non-blocking fashion. If one of these threads blocks (waiting
for synchronous I/O), all requests on that event loop are stalled.

In Micronaut 2, there was no detection for this. In Micronaut 4, detection is active by default
(`micronaut.netty.event-loops.default.blocking-allowed=false`).

```java
@Controller("/movies")
@ExecuteOn(TaskExecutors.BLOCKING)
public class MovieController {
    // The OmdbClient makes a synchronous HTTP call (BlockingHttpClient).
    // With @ExecuteOn, the handler runs on the "blocking" thread pool (Executors.newCachedThreadPool),
    // freeing the event loop.
}
```

**Alternatives:**

- Use the reactive `HttpClient` (returning `Mono<Movie>` or `Publisher<Movie>`) — more performant
  but requires rewriting the logic.
- Disable the check via config — **not recommended**, it masks performance issues.

**For future development**: Any controller that calls a `BlockingHttpClient`, does synchronous
I/O (JDBC, file I/O, etc.), or calls `Thread.sleep()`, should use `@ExecuteOn(TaskExecutors.BLOCKING)`.

---

## 12. Package Visibility in Java vs Groovy

### Friendly Explanation

In Groovy, everything is `public` by default. In Java, everything without a modifier is
*package-private* (visible only within the same package). This caused compilation errors when a
test class in `com.example.controllers` tried to extend a base class in `com.example`.

### Technical Deep Dive

| Element                        | Groovy (implicit)  | Java (required)         |
|--------------------------------|--------------------|-------------------------|
| `AbstractIntegrationTest`      | `public`           | `public`                |
| `AbstractServerTest`           | `public`           | `public`                |
| `authorRepository` (field)     | `public`           | `protected`             |
| `bookRepository` (field)       | `public`           | `protected`             |
| `getClient()` (method)         | `public`           | `protected` or `public` |
| `mockSecurityServiceEnabled()` | `public`           | `protected`             |

**Typical error that would appear:**

```
error: AbstractIntegrationTest is not public in com.example;
       cannot be accessed from outside package
```

---

## 13. AssertJ vs JUnit Assertions on Java 25

### Friendly Explanation

AssertJ is great for fluent assertions, but on Java 25 there's an ambiguity: when you
write `assertThat(response.status())`, the compiler can't decide whether `HttpStatus` should
use `assertThat(T)` (generic) or `assertThat(CharSequence)` (because `HttpStatus` implements
`CharSequence` in some versions). The compiler fails with:

```
error: reference to assertThat is ambiguous
```

The solution: use `assertEquals` from JUnit 5 to compare `HttpStatus`, and keep `assertThat` for
everything else.

### Technical Deep Dive

The issue occurs because Java 25 changed overload resolution rules for generic methods.
AssertJ has:

```java
public static <T> ObjectAssert<T> assertThat(T actual)

public static AbstractStringAssert<?> assertThat(CharSequence actual)
```

If `HttpStatus` is treated by the compiler as potentially compatible with `CharSequence`
(via boxing, coercion, or changes in Java 25's type inference), the ambiguity appears.

**Rule adopted in this project:**

- `assertEquals(HttpStatus.OK, response.status())` — for comparing HTTP status
- `assertThat(author.getName()).isEqualTo("...")` — for everything else

---

## 14. javax to jakarta Migration

### Friendly Explanation

Micronaut 4 migrated from `javax.*` to `jakarta.*`, following the Java EE to Jakarta EE transition.
Basically, swap the prefix on all imports.

### Technical Deep Dive

| Before (Micronaut 2)              | After (Micronaut 4)                     |
|-----------------------------------|-----------------------------------------|
| `javax.inject.Singleton`          | `jakarta.inject.Singleton`              |
| `javax.inject.Inject`             | `jakarta.inject.Inject`                 |
| `javax.persistence.*`             | `jakarta.persistence.*`                 |
| `javax.validation.*`              | `jakarta.validation.*`                  |
| `javax.transaction.Transactional` | `jakarta.transaction.Transactional`     |
| `javax.annotation.Nullable`       | `io.micronaut.core.annotation.Nullable` |

**Watch out for `@Nullable`**: `javax.annotation.Nullable` did **not** migrate to `jakarta.annotation.Nullable`.
In Micronaut 4, use `io.micronaut.core.annotation.Nullable`. This is a common mistake because the
"swap javax for jakarta" logic doesn't apply here.

---

## 15. @Serdeable and micronaut-serde

### Friendly Explanation

Micronaut 4 uses `micronaut-serde` for JSON serialization (instead of pure Jackson with reflection).
Every class that needs to be serialized/deserialized requires the `@Serdeable` annotation. Without it,
you'll see:

```
No serializable introspection present for type [Movie].
Consider adding Serdeable or @Introspected to type [Movie].
```

### Technical Deep Dive

The `micronaut-serde-processor` generates serializers at compile time. To do this, it needs
to know which classes to process — `@Serdeable` is the marker.

Classes annotated in this project:

- `Author`, `Book` (model DTOs)
- `Movie` (OMDB DTO)
- `CreateAuthorRequest` (request body)
- `SaveBook` (service DTO)

Entities (`AuthorEntity`, `BookEntity`) do **not** need `@Serdeable` because they are converted
manually to DTOs before being returned as JSON.

---

## 16. @JsonNaming vs @JsonProperty

### Friendly Explanation

The `Movie` DTO received JSON with PascalCase fields (`"Title"`, `"Year"`) from the OMDB API.
In Micronaut 2, we used `@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)` to
map automatically. In Micronaut 4 with `micronaut-serde`, this **doesn't work** — the annotation
processor doesn't recognize `@JsonNaming`.

The solution: use `@JsonProperty("Title")` and `@JsonProperty("Year")` on each field.

### Technical Deep Dive

The `micronaut-serde-processor` processes annotations at **compile time**. It recognizes
specific Jackson annotations (`@JsonProperty`, `@JsonIgnore`, etc.) but does **not** recognize
`@JsonNaming` because that annotation affects the naming strategy globally at runtime — something
the compile-time processor cannot resolve statically.

Compilation error that appeared:

```
java.lang.IllegalArgumentException: The argument does not represent an annotation type: JsonNaming
```

---

## 17. Test Dependencies in pom.xml

### Friendly Explanation

The required test dependencies and why each one is needed:

| Dependency                     | Purpose                                                |
|--------------------------------|--------------------------------------------------------|
| `micronaut-test-junit5`        | Integrates `@MicronautTest` with JUnit 5               |
| `junit-jupiter-api`            | `@Test`, `@BeforeEach`, etc. annotations               |
| `junit-jupiter-engine`         | Engine that discovers and executes JUnit 5 tests       |
| `junit-jupiter-params`         | `@ParameterizedTest`, `@MethodSource`, `@CsvSource`    |
| `assertj-core`                 | Fluent assertions (`assertThat(x).isEqualTo(y)`)       |
| `testcontainers`               | Framework for Docker containers in tests               |
| `testcontainers:junit-jupiter` | Testcontainers + JUnit 5 integration (`@Testcontainers`)|
| `testcontainers:postgresql`    | Pre-configured PostgreSQL container                    |

### Technical Deep Dive

In Micronaut 2 with Groovy, test dependencies included `spock-core`, `micronaut-test-spock`,
and the Groovy compiler. With JUnit 5, we replaced them with:

- **`micronaut-test-junit5`** instead of `micronaut-test-spock`: Provides the
  `MicronautJunit5Extension` that manages the `ApplicationContext` in the JUnit lifecycle.
- **`junit-jupiter-params`** is an **extra** dependency that didn't exist in the Spock equivalent.
  Spock has native `where:` blocks; in JUnit 5, parameterized tests require this separate module.
- **`assertj-core`** is **optional** — JUnit 5 has `Assertions.assertEquals()` etc. But AssertJ
  provides more descriptive error messages and a more fluent API, so we kept it.

---

## 18. Conversion Map: File by File

### Groovy (originals) -> Java (converted)

**Test infrastructure:**

| Groovy                                   | Java                                      | What changed                          |
|------------------------------------------|-------------------------------------------|---------------------------------------|
| `fixtures/ConfigurationFixture.groovy`   | Absorbed into `AbstractIntegrationTest`   | Properties via `TestPropertyProvider` |
| `fixtures/PostgresqlFixture.groovy`      | `PostgresqlTestContainer.java`            | Manual singleton instead of trait     |
| `fixtures/RepositoriesFixture.groovy`    | Absorbed into `AbstractIntegrationTest`   | `@Inject` fields in base class       |
| `fixtures/AuthorFixture.groovy`          | `fixtures/AuthorFixture.java`             | Trait -> Interface + default methods  |
| `fixtures/BookFixture.groovy`            | `fixtures/BookFixture.java`               | Trait -> Interface + default methods  |
| `ApplicationContextSpecification.groovy` | `AbstractIntegrationTest.java`            | Specification -> abstract class       |
| `EmbeddedServerSpecification.groovy`     | `AbstractServerTest.java`                 | Adds HttpClient                       |
| `OmdbEmbeddedServerSpecification.groovy` | Absorbed into `MovieControllerTest`       | Inline secondary server               |
| `Postgresql.groovy`                      | `PostgresqlTestContainer.java`            | Singleton companion class             |
| `LeakageDetector.groovy`                 | `@AfterEach` in `AbstractIntegrationTest` | Trait -> method in base class         |
| `security/MockSecurityService.groovy`    | `security/MockSecurityService.java`       | Same logic, javax -> jakarta          |

**Tests:**

| Groovy                                                          | Java                                                          |
|-----------------------------------------------------------------|---------------------------------------------------------------|
| `controllers/AuthorControllerSpec.groovy`                       | `controllers/AuthorControllerTest.java`                       |
| `controllers/AuthorControllerFindAuthorSpec.groovy`             | `controllers/AuthorControllerFindAuthorTest.java`             |
| `controllers/AuthorControllerFindAuthorWithSecuritySpec.groovy` | `controllers/AuthorControllerFindAuthorWithSecurityTest.java` |
| `controllers/AuthorControllerMockServiceSpec.groovy`            | `controllers/AuthorControllerMockServiceTest.java`            |
| `controllers/MovieControllerSpec.groovy`                        | `controllers/MovieControllerTest.java`                        |
| `repositories/AuthorRepositorySpec.groovy`                      | `repositories/AuthorRepositoryTest.java`                      |
| `repositories/BookRepositorySpec.groovy`                        | `repositories/BookRepositoryTest.java`                        |
| `services/AuthorServiceSpec.groovy`                             | `services/AuthorServiceTest.java`                             |
| `services/AuthorServiceImplConstraintsSpec.groovy`              | `services/AuthorServiceImplConstraintsTest.java`              |
| `services/SaveBookConstraintsSpec.groovy`                       | `services/SaveBookConstraintsTest.java`                       |
| `omdb/OmdbClientSpec.groovy`                                    | `omdb/OmdbClientTest.java`                                    |
| `openapi/OpenApiSpec.groovy`                                    | `openapi/OpenApiTest.java`                                    |

---

## Summary: Checklist for Future Development

When creating new tests in this project, remember:

- [ ] Extend `AbstractIntegrationTest` (database) or `AbstractServerTest` (database + HTTP)
- [ ] Use `transactional = false` (inherited from the base class) for HTTP tests
- [ ] Clean up data at the end of each test (`repository.deleteAll()`) in the correct order (children before parents)
- [ ] Use `implements AuthorFixture, BookFixture` for creation utility methods
- [ ] For global mocks: `@Primary` + `@Requires(property/env)` in a separate class
- [ ] For per-test mocks: `@Primary` + `@Requires(property = "spec.name")` as inner `static` class + override `getSpecName()`
- [ ] For secondary mock servers: always `schema-generate: NONE` and `SocketUtils.findAvailableTcpPort()`
- [ ] Controllers with blocking HTTP calls: `@ExecuteOn(TaskExecutors.BLOCKING)`
- [ ] New DTOs that go through JSON: annotate with `@Serdeable`
- [ ] Comparing `HttpStatus`: use `assertEquals()` instead of `assertThat()` (Java 25 ambiguity)
- [ ] Imports: `jakarta.*` (never `javax.*`), `io.micronaut.core.annotation.Nullable` (never `javax.annotation.Nullable`)
