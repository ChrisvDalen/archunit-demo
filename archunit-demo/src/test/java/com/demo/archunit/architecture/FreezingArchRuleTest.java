package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * DEMO: FreezingArchRule — Incremental Architecture Adoption for Legacy Codebases
 *
 * <p>One of ArchUnit's most powerful but least-known features solves a very real problem:
 * <em>"How do I introduce architecture rules into an existing codebase that already has
 * violations?"</em>
 *
 * <p>Without freezing, adding a rule that the codebase already violates makes the build
 * red on day one — so teams abandon the idea. {@link FreezingArchRule} breaks that deadlock.
 *
 * <h2>How FreezingArchRule works</h2>
 * <ol>
 *   <li><strong>First run (no store file):</strong> ArchUnit evaluates the wrapped rule,
 *       collects all current violations, and writes them to a text file in the
 *       <em>violation store</em> ({@code src/test/resources/archunit_store/} in this project).
 *       The build passes — existing violations are "frozen".</li>
 *   <li><strong>Subsequent runs:</strong> ArchUnit evaluates the rule again, then compares
 *       the results against the frozen set. <strong>Only violations that are NOT in the
 *       frozen set cause a test failure.</strong> All previously frozen violations are
 *       silently accepted.</li>
 *   <li><strong>Gradual improvement:</strong> When a developer fixes a frozen violation,
 *       ArchUnit automatically removes that entry from the store file. The rule becomes
 *       progressively stricter over time with zero additional configuration.</li>
 *   <li><strong>New violations are caught immediately:</strong> Any dependency or naming
 *       mistake introduced after the freeze is not in the store, so it fails the build
 *       right away — preventing the codebase from getting worse while the team cleans
 *       up the existing mess.</li>
 * </ol>
 *
 * <h2>Committing the store to git</h2>
 * <p>The violation store files in {@code src/test/resources/archunit_store/} <strong>must
 * be committed to git</strong>. This shared baseline ensures that every developer and
 * every CI environment agrees on which violations are "known" vs. "newly introduced".
 * Without committed store files, each machine would re-freeze on its first run and
 * never catch new violations.
 *
 * <h2>Configuration</h2>
 * <p>The violation store is configured in
 * {@code src/test/resources/archunit.properties}:
 * <pre>
 * freeze.store.default.path=src/test/resources/archunit_store
 * freeze.store.default.allowStoreCreation=true
 * freeze.store.default.allowStoreUpdate=true
 * </pre>
 *
 * <p>In CI pipelines, consider setting {@code allowStoreUpdate=false} to prevent
 * accidentally "silencing" new violations by re-freezing them. Require an explicit
 * human decision to update the frozen baseline.
 *
 * <h2>What can be frozen?</h2>
 * <p>{@link FreezingArchRule#freeze(ArchRule)} accepts <strong>any</strong> {@link ArchRule},
 * including layered architecture rules, naming convention rules, annotation rules,
 * dependency direction rules, and custom conditions. Wrap whichever rules your legacy
 * codebase currently violates.
 *
 * @see FreezingArchRule
 * @see <a href="https://www.archunit.org/userguide/html/000_Index.html#_freezing_arch_rules">
 *     ArchUnit user guide — Freezing arch rules</a>
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class FreezingArchRuleTest {

    /**
     * The layer architecture rule wrapped in a {@link FreezingArchRule}.
     *
     * <p>In this demo project the code is already clean, so the violation store for
     * this rule will be created as an empty file on the first run — meaning any new
     * violation will immediately fail the build.
     *
     * <p>In a real legacy project, the first run might discover dozens or hundreds of
     * existing layer violations and freeze them all. The team can then fix them
     * incrementally over multiple sprints without the build going red on day one.
     *
     * <p>The rule wrapped here is identical to the one in {@link LayerArchitectureTest}.
     * In a production setup you would <em>replace</em> the plain rule with its frozen
     * equivalent — not add a second copy. Both are shown here purely for demonstration.
     */
    @ArchTest
    static final ArchRule frozen_layer_architecture =
            FreezingArchRule.freeze(
                    Architectures.layeredArchitecture()
                            .consideringAllDependencies()
                            .layer("Controller").definedBy("com.demo.archunit.controller..")
                            .layer("Service").definedBy("com.demo.archunit.service..")
                            .layer("Repository").definedBy("com.demo.archunit.repository..")
                            .layer("Model").definedBy("com.demo.archunit.model..")
                            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                            .whereLayer("Model").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository")
                            .because("Demonstrates FreezingArchRule: on first run current violations are frozen; "
                                    + "only NEW violations introduced after the freeze will fail the build"));
}
