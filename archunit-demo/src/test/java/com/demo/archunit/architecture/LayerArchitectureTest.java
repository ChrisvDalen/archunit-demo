package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/**
 * DEMO: Layer Architecture Rules
 *
 * This test enforces that our application respects a strict layered architecture:
 *
 *   Controller  →  Service  →  Repository  →  Model
 *
 * Rules enforced:
 *  - Controllers may only be accessed by nobody (they are entry points)
 *  - Services may only be accessed by Controllers
 *  - Repositories may only be accessed by Services
 *  - Models can be accessed by anyone
 *
 * If a developer accidentally injects a Repository directly into a Controller,
 * this test will FAIL and explain exactly why.
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class LayerArchitectureTest {

    @ArchTest
    static final ArchRule layered_architecture_is_respected =
            Architectures.layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Controller").definedBy("com.demo.archunit.controller..")
                    .layer("Service").definedBy("com.demo.archunit.service..")
                    .layer("Repository").definedBy("com.demo.archunit.repository..")
                    .layer("Model").definedBy("com.demo.archunit.model..")

                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                    .whereLayer("Model").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository");
}
