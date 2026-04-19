# ArchUnit Demo — Spring Boot + Java 21

A demo project showing your team how **ArchUnit** works: architecture rules as
unit tests, enforced at every build. No external tools, no extra agents — just a
test dependency that fails the build when someone violates the agreed structure.

---

## Stack

| Tool        | Version        |
|-------------|----------------|
| Java        | 21             |
| Spring Boot | 4.0.5          |
| ArchUnit    | 1.4.1          |
| JUnit       | 5              |
| Database    | H2 (in-memory) |
| Build       | Maven          |

---

## Application Structure

The demo app is a minimal product catalogue with a classic 3-tier layered
architecture:

```
src/main/java/com/demo/archunit/
├── controller/   ProductController.java    ← REST endpoints  (@RestController)
├── service/      ProductService.java       ← Business logic  (@Service)
├── repository/   ProductRepository.java    ← Data access     (@Repository / JpaRepository)
└── model/        Product.java              ← Domain entity   (@Entity)
```

Dependency direction is strictly top-down:

```
Controller  →  Service  →  Repository  →  Model
```

---

## Architecture Tests

All tests live in `src/test/java/com/demo/archunit/architecture/` and run as
normal JUnit 5 tests via `mvn test`.

### 1. `LayerArchitectureTest`
Uses ArchUnit's `layeredArchitecture()` DSL to declare which layer may call
which. Any upward or skip-level dependency fails the build immediately with a
human-readable message.

### 2. `NamingConventionTest`
Enforces naming discipline in both directions:
- Classes in the `controller` package must end with `Controller` (and vice versa)
- Classes in the `service` package must end with `Service` (and vice versa)
- Classes in the `repository` package must end with `Repository` (and vice versa)
- No class may be named `*Manager` (explicit negative rule)

### 3. `AnnotationRuleTest`
Ensures Spring stereotypes are always present:
- Every service class must carry `@Service`
- Every controller class must carry `@RestController`
- Every repository interface must extend `JpaRepository`

### 4. `DependencyRuleTest`
Guards against upward and circular dependencies using `noClasses().should()` and
`SlicesRuleDefinition.slices().cycle()` rules.

### 5. `CodingRulesTest`
Two advanced ArchUnit patterns in one class:

**Shared rule library** — imports `ArchTests.in(SharedArchRules.class)`, a
reusable rule set that can be shared across multiple repositories. The shared
rules ban:
- `System.out` / `System.err` usage (use a logger)
- Throwing generic `Exception` or `RuntimeException`
- `java.util.logging` (use SLF4J)
- `@Autowired` field injection (constructor injection only)
- Calls to deprecated APIs
- Legacy `java.util.Date` / `java.util.Calendar`

**Custom condition** — a bespoke `ArchCondition` that limits constructor
parameter count to 5, enforcing the Single Responsibility Principle on Spring
beans.

### 6. `FreezingArchRuleTest`
Demonstrates how to adopt ArchUnit on a messy, existing codebase without
immediately turning the build red.

`FreezingArchRule.freeze(rule)` works in two phases:
1. **First run** — all current violations are recorded to
   `src/test/resources/archunit_store/`. Commit these files so every developer
   shares the same baseline.
2. **Subsequent runs** — only *new* violations (not in the store) fail the
   build. Fix violations incrementally; they auto-clear from the store once
   resolved.

### 7. `PlantUmlArchitectureTest`
Reads `src/test/resources/architecture.puml` at test time and enforces whatever
dependency arrows are drawn in the diagram:

```plantuml
[Controller] --> [Service]
[Controller] --> [Model]
[Service]    --> [Repository]
[Service]    --> [Model]
[Repository] --> [Model]
```

The diagram *is* the enforced spec — no drift possible. Non-developers can
propose architecture changes by editing the diagram file.

---

## Running the Tests

```bash
mvn test
# → BUILD SUCCESS  (all 24 architecture tests pass)
```

---

## Demo Script

### Step 1 — Show a clean build
```bash
mvn test
# → BUILD SUCCESS
```

### Step 2 — Layer violation
In `ProductController.java`, inject `ProductRepository` directly:
```java
private final ProductRepository productRepository; // ← bypass the service layer
```
```bash
mvn test
# LayerArchitectureTest FAILS:
#   "Repository was accessed by Controller,
#    but Repository may only be accessed by Service"
```

### Step 3 — Naming violation
Rename `ProductService` to `ProductManager`:
```bash
mvn test
# NamingConventionTest FAILS:
#   "classes in package 'service' should have name ending with 'Service'"
#   "no class should have name ending with 'Manager'"
```

### Step 4 — Annotation violation
Remove `@Service` from `ProductService`:
```bash
mvn test
# AnnotationRuleTest FAILS:
#   "classes in 'service' package should be annotated with @Service"
```

### Step 5 — Restore and celebrate
```bash
mvn test
# → BUILD SUCCESS — architecture is protected!
```

---

## Why ArchUnit?

| Problem | ArchUnit solution |
|---|---|
| Architecture decisions get lost | Rules live in version control like any other code |
| Violations sneak through code review | Caught at build time — before merge |
| Diagrams drift from reality | `PlantUmlArchitectureTest` makes the diagram the test |
| Legacy code has too many violations to fix at once | `FreezingArchRule` lets you adopt rules incrementally |
| Same standards across many repos | Package `SharedArchRules` as a library and share it |

---

## Key ArchUnit APIs Used

| API | Where |
|-----|-------|
| `layeredArchitecture()` | `LayerArchitectureTest` |
| `classes().that()…should()` | `NamingConventionTest` |
| `beAnnotatedWith()` | `AnnotationRuleTest` |
| `noClasses().should()` / `SlicesRuleDefinition` | `DependencyRuleTest` |
| `ArchTests.in()` · `ArchCondition` | `CodingRulesTest` |
| `FreezingArchRule.freeze()` | `FreezingArchRuleTest` |
| `classes().should().adhereToPlantUmlDiagram()` | `PlantUmlArchitectureTest` |
