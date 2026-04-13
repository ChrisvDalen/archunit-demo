# ArchUnit Demo — Spring Boot 4 + Java 25

A demo project for showing your team how **ArchUnit** works: architecture rules as
unit tests, enforced at every build.

---

## Stack

| Tool        | Version |
|-------------|---------|
| Spring Boot | 4.0.5   |
| Java        | 25      |
| ArchUnit    | 1.4.1   |
| JUnit       | 5       |
| Database    | H2 (in-memory) |

---

## Project Structure

```
src/main/java/com/demo/archunit/
├── controller/   ProductController.java    ← REST layer
├── service/      ProductService.java       ← Business logic layer
├── repository/   ProductRepository.java    ← Data access layer
└── model/        Product.java              ← Domain model

src/test/java/com/demo/archunit/architecture/
├── LayerArchitectureTest.java   ← Enforces layer dependency direction
├── NamingConventionTest.java    ← Enforces *Controller, *Service, *Repository naming
├── AnnotationRuleTest.java      ← Enforces @Service, @Repository, @RestController
└── DependencyRuleTest.java      ← No upward deps, no cyclic deps
```

---

## Running the tests

```bash
mvn test
```

All 4 test classes run as normal JUnit 5 tests. Every `@ArchTest` field is a rule.

---

## Demo Script

### Step 1 — Show the tests pass cleanly
```bash
mvn test
# → BUILD SUCCESS
```

### Step 2 — Introduce a violation: wrong layer dependency
In `ProductController.java`, inject `ProductRepository` directly (bypassing the service):
```java
// Add to ProductController:
private final ProductRepository productRepository; // ← violation!
```
Run tests again:
```bash
mvn test
# LayerArchitectureTest FAILS with a clear message:
#   "Repository was accessed by Controller, but Repository may only be accessed by Service"
```

### Step 3 — Introduce a naming violation
Rename `ProductService.java` to `ProductManager.java` (and update the class name).
```bash
mvn test
# NamingConventionTest FAILS:
#   "classes in package 'service' should have name ending with 'Service'"
# Also:
#   "no classes should have name ending with 'Manager'"
```

### Step 4 — Introduce an annotation violation
Remove `@Service` from `ProductService`.
```bash
mvn test
# AnnotationRuleTest FAILS:
#   "classes in 'service' package should be annotated with @Service"
```

### Step 5 — Restore everything and show clean build
```bash
mvn test
# → BUILD SUCCESS — architecture is protected!
```

---

## Key Concepts Shown

| ArchUnit Feature            | Test class                  |
|-----------------------------|-----------------------------|
| `layeredArchitecture()`     | `LayerArchitectureTest`     |
| `classes().that()...should()` | `NamingConventionTest`    |
| `beAnnotatedWith()`         | `AnnotationRuleTest`        |
| `noClasses().should()`      | `DependencyRuleTest`        |
| `SlicesRuleDefinition` (cycles) | `DependencyRuleTest`    |

---

## Why ArchUnit?

- Rules are **code** — they live in version control alongside your application
- Violations are caught at **build time**, not code review time
- No external tools or agents required — just a test dependency
- Rules are self-documenting via `.because("...")` messages
