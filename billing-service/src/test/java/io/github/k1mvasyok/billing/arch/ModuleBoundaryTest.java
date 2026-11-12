package io.github.k1mvasyok.billing.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Границы модулей из ADR 0001 и CLAUDE.md. Модуль виден снаружи только через свой пакет api:
 * до его внутренних классов (domain, app, infra, web) никто извне дотянуться не может.
 * shared ничего не знает о модулях, domain — чистый Java без Spring.
 * demo — потребитель модулей, вне предметной схемы: ходит к ним только через api.
 */
class ModuleBoundaryTest {

    private static final String ROOT = "io.github.k1mvasyok.billing";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private static final String[] MODULES = {"customers", "contracts", "invoicing"};

    /** Все пакеты верхнего уровня. Новый пакет сюда добавляют осознанно, и его границы проверяются. */
    private static final Set<String> TOP_LEVEL_PACKAGES = Set.of("customers", "contracts", "invoicing", "shared", "demo");

    /** Защита от пустого сканирования: правила проходят на пустом наборе, если классы не импортированы. */
    @Test
    void scansClassesOfEveryModule() {
        assertThat(CLASSES)
                .extracting(JavaClass::getName)
                .contains(
                        "io.github.k1mvasyok.billing.customers.domain.Customer",
                        "io.github.k1mvasyok.billing.contracts.domain.Contract",
                        "io.github.k1mvasyok.billing.invoicing.domain.Invoice",
                        "io.github.k1mvasyok.billing.shared.period.ValidityPeriod");
    }

    @Test
    void everyTopLevelPackageIsKnown() {
        Set<String> found = CLASSES.stream()
                .map(JavaClass::getPackageName)
                .filter(name -> name.startsWith(ROOT + "."))
                .map(name -> name.substring(ROOT.length() + 1).split("\\.")[0])
                .collect(Collectors.toSet());

        assertThat(found).containsExactlyInAnyOrderElementsOf(TOP_LEVEL_PACKAGES);
    }

    /** Классы вне модуля и вне shared видят модуль только через его api. */
    @Test
    void modulesAreReachedOnlyThroughTheirApi() {
        for (String module : MODULES) {
            noClasses()
                    .that().resideOutsideOfPackages("..%s..".formatted(module), "..shared..", ROOT)
                    .should().dependOnClassesThat(internalsOf(module))
                    .check(CLASSES);
        }
    }

    /** demo ходит к модулям только через api, без app, domain, infra и web. */
    @Test
    void demoUsesModulesOnlyThroughApi() {
        for (String module : MODULES) {
            noClasses()
                    .that().resideInAPackage("..demo..")
                    .should().dependOnClassesThat(internalsOf(module))
                    .check(CLASSES);
        }
    }

    @Test
    void sharedKnowsNothingAboutModules() {
        noClasses()
                .that().resideInAPackage("..shared..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..customers..", "..contracts..", "..invoicing..")
                .check(CLASSES);
    }

    @Test
    void domainDoesNotUseSpring() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(CLASSES);
    }

    /** Всё в пакете модуля, кроме его api: то, что снаружи знать не должно. */
    private static DescribedPredicate<JavaClass> internalsOf(String module) {
        return JavaClass.Predicates.resideInAPackage("..%s..".formatted(module))
                .and(DescribedPredicate.not(JavaClass.Predicates.resideInAPackage("..%s.api..".formatted(module))));
    }
}
