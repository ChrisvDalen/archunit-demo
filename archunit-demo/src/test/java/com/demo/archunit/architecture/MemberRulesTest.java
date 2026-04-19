package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaModifier;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * DEMO: Member-Level Rules — Fields and Methods
 *
 * <p>ArchUnit's rule DSL is not limited to class-level checks. You can inspect
 * individual <strong>fields</strong> and <strong>methods</strong> with the same
 * fluent API used for class-level rules.
 *
 * <p>Field rules use {@code fields().that()...should()} / {@code noFields().that()...should()}.
 * Method rules use {@code methods().that()...should()} / {@code noMethods().that()...should()}.
 *
 * <p>Rules demonstrated:
 * <ul>
 *   <li>JPA entity fields must be {@code private} — encapsulate state behind getters/setters.</li>
 *   <li>Spring {@code @Service} fields must be {@code final} — immutable after constructor injection.</li>
 *   <li>Spring {@code @RestController} fields must be {@code final} — same immutability contract.</li>
 *   <li>Service methods must not be {@code synchronized} — Spring singletons should be stateless.</li>
 * </ul>
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class MemberRulesTest {

    // -----------------------------------------------------------------------
    // 1. Field-level rules
    //
    //    fields().that().<predicate>.should().<condition>
    //    noFields().that().<predicate>.should().<condition>
    // -----------------------------------------------------------------------

    /**
     * JPA entity fields must be {@code private}.
     *
     * <p>Exposing entity state via {@code public} fields bypasses all validation
     * and lifecycle hooks. Use getters and setters (or Lombok {@code @Data})
     * to control how the state is read and mutated.
     *
     * <p>Violation example:
     * <pre>{@code
     * @Entity
     * public class Product {
     *     public String name;  // BAD — exposed directly, no control over mutation
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule entity_fields_should_be_private =
            fields()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                    .and().areNotStatic()
                    .should().bePrivate()
                    .because("JPA entity state must be encapsulated — expose it via getters/setters only");

    /**
     * Spring {@code @Service} instance fields must be {@code final}.
     *
     * <p>Constructor injection (the Spring-recommended approach since Spring 4.3)
     * assigns every dependency exactly once in the constructor. Declaring those
     * fields {@code final} makes the guarantee explicit and prevents accidental
     * reassignment later in the class.
     *
     * <p>Violation example:
     * <pre>{@code
     * @Service
     * public class ProductService {
     *     private ProductRepository repo;  // BAD — not final, can be reassigned
     *
     *     public ProductService(ProductRepository repo) {
     *         this.repo = repo;
     *     }
     * }
     * }</pre>
     *
     * <p>Fix:
     * <pre>{@code
     *     private final ProductRepository repo;  // GOOD — immutable after construction
     * }</pre>
     */
    @ArchTest
    static final ArchRule service_fields_should_be_final =
            fields()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                    .and().areNotStatic()
                    .should().beFinal()
                    .because("Constructor-injected service fields must be final to guarantee immutability");

    /**
     * Spring {@code @RestController} instance fields must be {@code final}.
     *
     * <p>Same rationale as {@link #service_fields_should_be_final}: constructor
     * injection assigns dependencies once; {@code final} makes that contract
     * visible in the type system and prevents accidental mutation.
     */
    @ArchTest
    static final ArchRule controller_fields_should_be_final =
            fields()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .and().areNotStatic()
                    .should().beFinal()
                    .because("Constructor-injected controller fields must be final to guarantee immutability");

    // -----------------------------------------------------------------------
    // 2. Method-level rules
    //
    //    methods().that().<predicate>.should().<condition>
    //    noMethods().that().<predicate>.should().<condition>
    // -----------------------------------------------------------------------

    /**
     * Service methods must not be {@code synchronized}.
     *
     * <p>Spring {@code @Service} beans are singletons shared across threads.
     * A {@code synchronized} method is usually a symptom of mutable instance
     * state — which is itself an anti-pattern in Spring services. If shared state
     * is truly required, use {@link java.util.concurrent.atomic.AtomicLong} or
     * similar concurrent primitives, and document the decision explicitly.
     *
     * <p>Violation example:
     * <pre>{@code
     * @Service
     * public class OrderService {
     *     private int counter = 0;
     *
     *     public synchronized int nextId() {  // BAD — mutable state + synchronization
     *         return ++counter;
     *     }
     * }
     * }</pre>
     */
    @ArchTest
    static final ArchRule service_methods_should_not_be_synchronized =
            noMethods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                    .should().haveModifier(JavaModifier.SYNCHRONIZED)
                    .because("Stateless Spring services must not synchronize — "
                            + "synchronized methods indicate mutable shared state, which is an anti-pattern");
}
