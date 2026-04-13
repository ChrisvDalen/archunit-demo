package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * DEMO: Annotation Rules
 *
 * ArchUnit can verify that the right Spring annotations are applied consistently.
 * This prevents bugs like forgetting @Service on a service class so Spring
 * doesn't manage it as a bean.
 *
 * Rules demonstrated:
 *  - All classes in 'service' package must have @Service
 *  - All classes in 'repository' package must have @Repository
 *  - All classes in 'controller' package must have @RestController or @Controller
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class AnnotationRuleTest {

    @ArchTest
    static final ArchRule services_should_be_annotated_with_service =
            classes()
                    .that().resideInAPackage("..service..")
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith(Service.class)
                    .because("Spring must manage service classes — @Service annotation is required");

    @ArchTest
    static final ArchRule repositories_should_be_annotated_with_repository =
            classes()
                    .that().resideInAPackage("..repository..")
                    .and().areInterfaces()
                    .should().beAssignableTo(JpaRepository.class)
                    .because("Spring must manage repository classes — @Repository annotation is required");

    @ArchTest
    static final ArchRule controllers_should_be_annotated_with_rest_controller =
            classes()
                    .that().resideInAPackage("..controller..")
                    .should().beAnnotatedWith(RestController.class)
                    .because("All our controllers are REST controllers and must be annotated with @RestController");
}
