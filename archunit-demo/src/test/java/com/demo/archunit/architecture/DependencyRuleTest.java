package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DEMO: Dependency Direction and Cycle Rules
 *
 * These tests enforce the direction dependencies must flow and check for
 * circular dependencies — one of the most common architecture problems.
 *
 * Rules demonstrated:
 *  - Repositories must never depend on Services (no upward dependency)
 *  - Services must never depend on Controllers (no upward dependency)
 *  - No circular dependencies between any slices (packages)
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class DependencyRuleTest {

    // -----------------------------------------------------------------------
    // 1. Dependency direction rules
    //    Lower layers must NOT depend on higher layers
    // -----------------------------------------------------------------------

    @ArchTest
    static final ArchRule repositories_should_not_depend_on_services =
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .because("Repositories are a low-level layer — they must not know about Services");

    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers =
            noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Services must not know about Controllers — that would create an upward dependency");

    @ArchTest
    static final ArchRule repositories_should_not_depend_on_controllers =
            noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Repositories must not know about Controllers");

    // -----------------------------------------------------------------------
    // 2. No cyclic dependencies between packages
    //    ArchUnit will detect A→B→C→A style cycles automatically
    // -----------------------------------------------------------------------

    @ArchTest
    static final ArchRule no_cycles_between_slices =
            SlicesRuleDefinition.slices()
                    .matching("com.demo.archunit.(*)..")
                    .should().beFreeOfCycles()
                    .because("Cyclic dependencies make code hard to understand, test and refactor");
}
