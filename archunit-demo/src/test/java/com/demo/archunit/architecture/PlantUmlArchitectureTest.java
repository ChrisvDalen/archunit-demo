package com.demo.archunit.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition;

import java.net.URL;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInDiagram;

/**
 * DEMO: PlantUmlArchCondition — Architecture as a Living Diagram
 *
 * <p>ArchUnit can derive and enforce architecture rules directly from a
 * <a href="https://plantuml.com/component-diagram">PlantUML component diagram</a>.
 * This enables <em>architecture-as-documentation</em>: the diagram you draw is not
 * a picture of what the architecture <em>should</em> look like — it IS the enforced,
 * machine-verified specification.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Draw a PlantUML component diagram (see
 *       {@code src/test/resources/architecture.puml}).</li>
 *   <li>Each {@code [Component]} in the diagram maps to a set of Java packages,
 *       identified by its <em>stereotype</em>: {@code <<..packageName..>>}. The
 *       stereotype syntax mirrors ArchUnit's {@code resideInAPackage("..name..")} pattern.</li>
 *   <li>Arrows ({@code -->}) define which components are <em>allowed</em> to depend
 *       on which other components. Any actual Java dependency not represented by an
 *       arrow in the diagram is a <strong>violation</strong>.</li>
 *   <li>ArchUnit reads and parses the diagram at test time using its built-in PlantUML
 *       parser (no external PlantUML rendering library required) and enforces the
 *       allowed dependency set against the real bytecode.</li>
 * </ol>
 *
 * <h2>Benefits over hand-written Java rules</h2>
 * <ul>
 *   <li><strong>No diagram drift.</strong> The code is <em>guaranteed</em> to match the
 *       diagram because the diagram IS the rule. Traditional architecture diagrams in
 *       Confluence diverge from reality within weeks.</li>
 *   <li><strong>Non-developer friendly.</strong> Architects and tech leads can review
 *       and modify allowed dependencies by editing the {@code .puml} file without
 *       touching Java source code.</li>
 *   <li><strong>Minimal change surface.</strong> Adding a new allowed dependency is a
 *       one-line change: draw the arrow in the diagram and commit. No Java file edits,
 *       no re-deployment of rule logic.</li>
 *   <li><strong>Diagram-first design.</strong> Start with the diagram, then write the
 *       code. The architecture test will guide the implementation into the correct shape.</li>
 * </ul>
 *
 * <h2>Diagram file</h2>
 * <p>The PlantUML diagram lives at {@code src/test/resources/architecture.puml} and is
 * loaded from the test classpath at runtime. Editing the diagram immediately changes
 * which dependencies are allowed or forbidden — no Java code changes needed.
 *
 * <h2>Configuration options</h2>
 * <ul>
 *   <li>{@link PlantUmlArchCondition.Configurations#consideringOnlyDependenciesInDiagram()} —
 *       checks only dependencies <em>between components that appear in the diagram</em>.
 *       External library dependencies (Spring, JPA, Java standard library) are ignored,
 *       keeping the rule focused on your own architectural boundaries.</li>
 *   <li>{@link PlantUmlArchCondition.Configurations#consideringAllDependencies()} —
 *       also verifies that no code depends on packages <em>outside</em> the diagram.
 *       Use this for strict module isolation where external libraries must be declared
 *       in the diagram explicitly.</li>
 * </ul>
 *
 * @see PlantUmlArchCondition
 * @see <a href="https://www.archunit.org/userguide/html/000_Index.html#_plantuml_component_diagrams">
 *     ArchUnit user guide — PlantUML component diagrams</a>
 */
@AnalyzeClasses(packages = "com.demo.archunit")
public class PlantUmlArchitectureTest {

    /**
     * The PlantUML component diagram that defines the allowed layer dependencies.
     *
     * <p>Loaded from the test classpath root. The file must reside at
     * {@code src/test/resources/architecture.puml} so that Maven places it on the
     * test classpath during the {@code test-compile} phase.
     *
     * <p>This field is {@code static final} because it is referenced in the rule
     * declaration below, which itself must be a {@code static final} field for
     * the {@code @ArchTest} annotation to work correctly.
     */
    private static final URL ARCHITECTURE_DIAGRAM =
            PlantUmlArchitectureTest.class.getClassLoader().getResource("architecture.puml");

    /**
     * Enforces that all classes respect the dependencies drawn in {@code architecture.puml}.
     *
     * <p>When this test fails, the violation message identifies the exact class that
     * violated the diagram, the dependency it has, and which component boundary was
     * crossed — giving developers precise, actionable feedback.
     *
     * <p>Example violation message when a controller directly accesses a repository:
     * <pre>
     * Class &lt;com.demo.archunit.controller.ProductController&gt; accesses
     * &lt;com.demo.archunit.repository.ProductRepository&gt;
     * which is not allowed according to the diagram architecture.puml
     * (Component [Controller] may not access [Repository] — no arrow exists)
     * </pre>
     *
     * <p>The rule uses {@link PlantUmlArchCondition.Configurations#consideringOnlyDependenciesInDiagram()}
     * so that framework dependencies (Spring annotations, JPA types, etc.) are not
     * checked — only cross-layer dependencies between our own packages matter here.
     */
    @ArchTest
    static final ArchRule classes_should_adhere_to_the_plantuml_diagram =
            classes()
                    .that().resideInAnyPackage(
                            "..controller..",
                            "..service..",
                            "..repository..",
                            "..model..")
                    .should(PlantUmlArchCondition.adhereToPlantUmlDiagram(
                            ARCHITECTURE_DIAGRAM,
                            consideringOnlyDependenciesInDiagram()))
                    .because("architecture.puml is the single source of truth for allowed layer "
                            + "dependencies — the diagram and the code must always agree");
}
