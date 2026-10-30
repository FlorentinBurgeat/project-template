package com.template

import com.tngtech.archunit.core.domain.JavaCall
import com.tngtech.archunit.core.domain.properties.HasName
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class HexagonalArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.template")

    @Test
    fun `domain model depends on neither logic nor port`() {
        noClasses().that().resideInAPackage("..domain..model..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..logic..", "..domain..port..")
            .check(classes)
    }

    @Test
    fun `domain logic does not depend on ports`() {
        noClasses().that().resideInAPackage("..domain..logic..")
            .should().dependOnClassesThat().resideInAPackage("..domain..port..")
            .check(classes)
    }

    @Test
    fun `domain is free of frameworks and outer layers`() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "org.jetbrains.exposed..", "com.fasterxml..",
                "..application..", "..adapters.."
            )
            .check(classes)
    }

    @Test
    fun `application does not depend on adapters`() {
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapters..")
            .check(classes)
    }

    @Test
    fun `web and persistence adapters do not depend on each other`() {
        noClasses().that().resideInAPackage("..adapters.web..")
            .should().dependOnClassesThat().resideInAPackage("..adapters.persistence..")
            .check(classes)
        noClasses().that().resideInAPackage("..adapters.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..adapters.web..")
            .check(classes)
    }

    @Test
    fun `only the persistence adapter rebuilds values without checking them`() {
        noClasses().that().resideOutsideOfPackage("..adapters.persistence..")
            .should().callMethodWhere(
                JavaCall.Predicates.target(HasName.Predicates.nameStartingWith("reconstituer"))
            )
            .check(classes)
    }
}
