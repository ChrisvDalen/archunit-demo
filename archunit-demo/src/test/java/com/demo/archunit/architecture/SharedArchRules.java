package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

/**
 * A reusable rule library that can be imported into any ArchUnit test class.
 *
 * <p>This class demonstrates one of ArchUnit's most underused features:
 * <strong>shared rule libraries</strong>. Rather than copy-pasting {@code @ArchTest}
 * rules across dozens of test classes or microservice repositories, define them once
 * here and import them with a single line:
 *
 * <pre>{@code
 * @AnalyzeClasses(packages = "com.your.base.package")
 * class MyArchTest {
 *
 *     // Imports ALL @ArchTest fields from SharedArchRules into this test class:
 *     @ArchTest
 *     static final ArchTests SHARED_RULES = ArchTests.in(SharedArchRules.class);
 * }
 * }</pre>
 *
 * <p>Key properties of this pattern:
 * <ul>
 *   <li>This class carries <strong>no {@code @AnalyzeClasses} annotation</strong> and no
 *       JUnit 5 {@code @ExtendWith} — it is a plain Java class, not a test class itself.</li>
 *   <li>ArchUnit discovers the {@code @ArchTest} fields via {@link ArchRules#in(Class)}
 *       and registers them as if they had been declared directly in the importing class.</li>
 *   <li>Any update to this class is automatically reflected everywhere it is imported —
 *       no copy-paste drift, no forgotten updates.</li>
 * </ul>
 *
 * <p><strong>Multi-module / multi-repo tip:</strong> Publish this class (and any companion
 * classes) as a Maven artifact with a {@code test-jar} classifier. Every microservice can
 * then declare a single {@code <scope>test</scope>} dependency and get all organisational
 * architecture standards for free. See {@code README.md} for the full setup.
 *
 * <p>The rules in this library are intentionally <em>general-purpose coding hygiene</em>
 * rules that apply to every Java project, regardless of domain or framework.
 *
 * @see ArchTests#in(Class)
 * @see CodingRulesTest for an example of how to import this library
 */
public final class SharedArchRules {

    private SharedArchRules() {
        // Utility class — not instantiable.
    }

    /**
     * No class should write to {@code System.out} or {@code System.err} directly.
     *
     * <p>Standard stream access is invisible to log-level filtering, cannot be
     * correlated with request IDs or MDC context, and pollutes test output.
     * Use SLF4J (backed by Logback or Log4j2) for all logging.
     */
    @ArchTest
    static final ArchRule no_standard_streams =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /**
     * No class should throw {@link Exception}, {@link RuntimeException} or {@link Error} directly.
     *
     * <p>Generic exceptions carry no semantic information for callers. Throw a
     * specific, named exception type so that catch-sites can distinguish error
     * categories and callers know exactly what to handle.
     *
     * <p>Violation example:
     * <pre>{@code
     * // BAD — forbidden by this rule
     * throw new RuntimeException("something went wrong");
     *
     * // GOOD — specific and self-documenting
     * throw new ProductNotFoundException(id);
     * }</pre>
     */
    @ArchTest
    static final ArchRule no_generic_exceptions =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /**
     * No class should use {@link java.util.logging.Logger java.util.logging} (JUL).
     *
     * <p>JUL has an awkward API and poor interoperability with the SLF4J ecosystem.
     * Use SLF4J with Logback or Log4j2 — they provide structured logging, MDC context
     * propagation, and seamless integration with Spring Boot's logging auto-configuration.
     */
    @ArchTest
    static final ArchRule no_java_util_logging =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    /**
     * No class should use Spring's {@code @Autowired} annotation on fields.
     *
     * <p>Field injection is a Spring anti-pattern: it hides dependencies, prevents
     * {@code final} fields, and forces use of a running Spring context in unit tests.
     * Constructor injection is the Spring team's own recommendation since Spring 4.3.
     *
     * <p>This rule is provided as a built-in by {@link GeneralCodingRules} since
     * ArchUnit 1.0 — no custom condition required.
     *
     * <p>Violation example:
     * <pre>{@code
     * // BAD — forbidden by this rule
     * @Autowired
     * private ProductRepository repository;
     *
     * // GOOD — constructor injected, can be final, easy to unit test
     * private final ProductRepository repository;
     *
     * public ProductService(ProductRepository repository) {
     *     this.repository = repository;
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule no_field_injection =
            GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    /**
     * No class should use deprecated API.
     *
     * <p>Calls to {@code @Deprecated} members are a maintenance liability: they may
     * be removed in a future library version, and they signal that a better alternative
     * already exists. Keeping this rule green means the codebase proactively migrates
     * off deprecated APIs rather than accumulating technical debt.
     */
    @ArchTest
    static final ArchRule no_deprecated_api =
            GeneralCodingRules.DEPRECATED_API_SHOULD_NOT_BE_USED;

    /**
     * No class should use old {@code java.util.Date} or {@code java.util.Calendar}.
     *
     * <p>{@code Date} and {@code Calendar} are mutable, not thread-safe, and have
     * a notoriously confusing API (months are 0-indexed, years offset from 1900, etc.).
     * Java 8 introduced {@code java.time.*} (JSR-310) which is immutable, thread-safe,
     * and far more expressive. Use {@link java.time.LocalDate}, {@link java.time.Instant},
     * or {@link java.time.ZonedDateTime} instead.
     */
    @ArchTest
    static final ArchRule no_old_date_and_time_classes =
            GeneralCodingRules.OLD_DATE_AND_TIME_CLASSES_SHOULD_NOT_BE_USED;
}
