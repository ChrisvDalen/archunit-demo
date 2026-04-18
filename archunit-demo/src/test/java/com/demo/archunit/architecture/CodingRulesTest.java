package com.demo.archunit.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * DEMO: Coding Rules — Shared Libraries and Custom Conditions
 *
 * <p>This test class showcases two complementary ArchUnit features that are rarely
 * demonstrated but extremely valuable in real projects:
 *
 * <ol>
 *   <li><strong>Shared rule libraries via {@link ArchTests#in(Class)}</strong> — import
 *       all {@code @ArchTest} rules from {@link SharedArchRules} with a single field
 *       declaration. No copy-paste, no drift, and updates propagate automatically.</li>
 *
 *   <li><strong>Custom {@link ArchCondition}</strong> — extend the built-in DSL with
 *       your own reusable conditions when no existing combinator can express the rule.
 *       Conditions have full access to the ArchUnit Java class model: fields, methods,
 *       constructors, annotations, call sites, and more.</li>
 * </ol>
 *
 * <p>The custom condition here enforces the <em>Single Responsibility Principle</em>
 * at the constructor level: a Spring bean with more than {@value #MAX_DEPENDENCIES}
 * constructor parameters is a strong signal that the class is doing too much and
 * should be split. No built-in ArchUnit rule captures this; it requires a custom
 * condition that inspects constructor parameter counts at the bytecode level.
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class CodingRulesTest {

    /**
     * Maximum number of constructor dependencies allowed for a Spring bean.
     *
     * <p>This threshold is a heuristic for the Single Responsibility Principle.
     * More than five injected dependencies is a strong signal that a class has
     * grown too large and should be decomposed. Adjust to match your team's
     * agreed-upon standard.
     */
    private static final int MAX_DEPENDENCIES = 5;

    // -------------------------------------------------------------------------
    // 1. Shared rule library import
    //
    //    ArchRules.in(SharedArchRules.class) scans that class for @ArchTest fields
    //    and registers every rule it finds as if it were declared here directly.
    //    In a multi-module build, SharedArchRules lives in a shared test-jar
    //    and all microservices consume it with this one-liner.
    // -------------------------------------------------------------------------

    /**
     * Imports the full set of general coding hygiene rules from {@link SharedArchRules}.
     *
     * <p>Rules imported:
     * <ul>
     *   <li>No {@code System.out} / {@code System.err} access</li>
     *   <li>No throwing generic {@code Exception} / {@code RuntimeException}</li>
     *   <li>No {@code java.util.logging} usage</li>
     *   <li>No {@code @Autowired} field injection</li>
     *   <li>No deprecated API calls</li>
     *   <li>No legacy {@code Date} / {@code Calendar} usage</li>
     * </ul>
     *
     * <p>This is the <strong>hidden gem</strong>: one field declaration replaces
     * six separate {@code @ArchTest} methods. In a large organisation with 20+ repos,
     * this pattern means architectural standards are updated in one place and
     * automatically enforced everywhere within minutes of the next CI run.
     *
     * @see SharedArchRules
     */
    @ArchTest
    static final ArchTests SHARED_CODING_RULES = ArchTests.in(SharedArchRules.class);

    // -------------------------------------------------------------------------
    // 2. Custom ArchCondition — SRP dependency count limiter
    //
    //    Demonstrates how to write a custom condition when the built-in DSL
    //    cannot express the rule. The condition inspects constructor parameter
    //    lists — something no standard combinator covers.
    // -------------------------------------------------------------------------

    /**
     * All Spring-managed beans must not exceed {@value #MAX_DEPENDENCIES} constructor
     * dependencies, as a heuristic guard against Single Responsibility Principle violations.
     *
     * <p>Rationale: a class that needs six or more collaborators to function is almost
     * certainly doing more than one thing. Breaking it apart produces smaller, more
     * testable, and more comprehensible classes. This rule makes the violation visible
     * at build time rather than waiting for it to be noticed in code review.
     *
     * <p>The condition is implemented with a custom {@link ArchCondition} because the
     * built-in ArchUnit DSL has no combinator for "constructor has at most N parameters".
     * Writing a custom condition requires three steps:
     * <ol>
     *   <li>Extend {@code ArchCondition<T>} with the domain type to inspect
     *       (here {@code JavaClass}).</li>
     *   <li>Override {@link ArchCondition#check} to inspect the object and emit
     *       {@link SimpleConditionEvent#violated} events for each breach found.</li>
     *   <li>Pass the condition instance to {@code .should(...)} in the rule DSL.</li>
     * </ol>
     *
     * @see #atMostNConstructorDependencies(int)
     */
    @ArchTest
    static final ArchRule spring_beans_must_not_exceed_max_dependencies =
            classes()
                    .that().areAnnotatedWith(Service.class)
                    .or().areAnnotatedWith(Repository.class)
                    .or().areAnnotatedWith(RestController.class)
                    .should(atMostNConstructorDependencies(MAX_DEPENDENCIES))
                    .because("A bean with more than " + MAX_DEPENDENCIES + " dependencies likely violates "
                            + "the Single Responsibility Principle — consider splitting the class");

    /**
     * Creates a custom {@link ArchCondition} that passes only when a class has no
     * constructor whose parameter count exceeds {@code max}.
     *
     * <p>The condition iterates over every constructor declared by the class. For
     * each constructor whose parameter count exceeds the threshold, it emits a
     * {@link SimpleConditionEvent#violated} event carrying the constructor as the
     * corresponding domain object (so the violation message includes the exact
     * constructor signature and source location).
     *
     * <p>Using the constructor as the event's corresponding object — rather than the
     * class — gives ArchUnit the information it needs to print the precise line in
     * the violation report, matching the level of detail engineers expect from a
     * compiler error.
     *
     * @param max the inclusive maximum number of constructor parameters allowed
     * @return a reusable {@link ArchCondition} asserting the parameter count constraint
     */
    private static ArchCondition<JavaClass> atMostNConstructorDependencies(int max) {
        return new ArchCondition<>("have at most " + max + " constructor dependencies") {

            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaConstructor constructor : javaClass.getConstructors()) {
                    int paramCount = constructor.getParameterTypes().size();
                    if (paramCount > max) {
                        events.add(SimpleConditionEvent.violated(
                                constructor,
                                String.format(
                                        "Constructor of '%s' has %d dependencies (max allowed: %d) "
                                                + "— consider splitting responsibilities into smaller classes",
                                        javaClass.getSimpleName(),
                                        paramCount,
                                        max)));
                    }
                }
            }
        };
    }
}
