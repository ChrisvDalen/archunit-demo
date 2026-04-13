package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DEMO: Naming Convention Rules
 *
 * ArchUnit can enforce that your team follows consistent naming conventions.
 * These rules will catch naming mistakes at build time — not code review time.
 *
 * Examples demonstrated:
 *  - Classes in the 'controller' package must be named "...Controller"
 *  - Classes in the 'service' package must be named "...Service"
 *  - Classes in the 'repository' package must be named "...Repository"
 *  - Controllers must live in the 'controller' package (and nowhere else)
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class NamingConventionTest {

    // -----------------------------------------------------------------------
    // 1. Package → Name rules
    // "If you are IN this package, your class name MUST end with..."
    // -----------------------------------------------------------------------

    @ArchTest
    static final ArchRule controllers_should_be_named_correctly =
            classes()
                    .that().resideInAPackage("..controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Every class in the controller package should be named *Controller");

    @ArchTest
    static final ArchRule services_should_be_named_correctly =
            classes()
                    .that().resideInAPackage("..service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Every class in the service package should be named *Service");

    @ArchTest
    static final ArchRule repositories_should_be_named_correctly =
            classes()
                    .that().resideInAPackage("..repository..")
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Every class in the repository package should be named *Repository");

    // -----------------------------------------------------------------------
    // 2. Name → Package rules
    // "If your name ends with ..., you MUST live in the correct package"
    // -----------------------------------------------------------------------

    @ArchTest
    static final ArchRule classes_named_controller_should_be_in_controller_package =
            classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..controller..")
                    .because("*Controller classes must live in the controller package");

    @ArchTest
    static final ArchRule classes_named_service_should_be_in_service_package =
            classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .should().resideInAPackage("..service..")
                    .because("*Service classes must live in the service package");

    @ArchTest
    static final ArchRule classes_named_repository_should_be_in_repository_package =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().resideInAPackage("..repository..")
                    .because("*Repository classes must live in the repository package");

    // -----------------------------------------------------------------------
    // 3. Negative rule — just for demo: show what a violation looks like
    // -----------------------------------------------------------------------

    @ArchTest
    static final ArchRule no_classes_should_be_named_Manager =
            noClasses()
                    .should().haveSimpleNameEndingWith("Manager")
                    .because("We use 'Service' not 'Manager' — please rename to *Service");
}
