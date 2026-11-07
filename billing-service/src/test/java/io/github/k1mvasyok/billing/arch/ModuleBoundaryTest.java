package io.github.k1mvasyok.billing.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Границы модулей из ADR 0001 и CLAUDE.md: модуль ходит к другому модулю только через его api,
 * shared ничего не знает о модулях, domain — чистый Java без Spring.
 */
class ModuleBoundaryTest {

    private static final String ROOT = "io.github.k1mvasyok.billing";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    private static final String[] MODULES = {"customers", "contracts", "invoicing"};

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
    void modulesUseEachOtherOnlyThroughApi() {
        for (String module : MODULES) {
            for (String other : MODULES) {
                if (module.equals(other)) {
                    continue;
                }
                ArchRule rule = noClasses()
                        .that().resideInAPackage("..%s..".formatted(module))
                        .should().dependOnClassesThat().resideInAnyPackage(
                                "..%s.domain..".formatted(other),
                                "..%s.app..".formatted(other),
                                "..%s.infra..".formatted(other),
                                "..%s.web..".formatted(other))
                        .as("%s must not depend on internals of %s".formatted(module, other));
                rule.check(CLASSES);
            }
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
}
