package dev.jianastrero.journey.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import dev.jianastrero.journey.ksp.generators.ControllersGenerator
import dev.jianastrero.journey.ksp.generators.HostGenerator
import dev.jianastrero.journey.ksp.generators.ViewGenerator
import dev.jianastrero.journey.ksp.model.parseSteps

private const val JOURNEY_ANNOTATION_FQN = "dev.jianastrero.journey.annotations.Journey"
private const val JOURNEY_STEP_FQN = "dev.jianastrero.journey.JourneyStep"

internal class JourneySymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(JOURNEY_ANNOTATION_FQN)
            .filterIsInstance<KSClassDeclaration>()

        val deferred = mutableListOf<KSAnnotated>()

        symbols.forEach { classDecl ->
            if (!classDecl.validate()) {
                deferred.add(classDecl)
                return@forEach
            }
            processJourneyInterface(classDecl)
        }

        return deferred
    }

    private fun processJourneyInterface(classDecl: KSClassDeclaration) {
        if (!validateInterface(classDecl)) return

        val subclasses = classDecl.declarations.filterIsInstance<KSClassDeclaration>().toList()
        if (!validateSubclasses(classDecl, subclasses)) return

        val packageName = classDecl.packageName.asString()
        val journeyName = classDecl.simpleName.asString()
        val steps = parseSteps(subclasses)

        ControllersGenerator(codeGenerator, logger).generate(classDecl, steps, packageName, journeyName)
        ViewGenerator(codeGenerator, logger).generate(classDecl, steps, packageName, journeyName)
        HostGenerator(codeGenerator, logger).generate(classDecl, steps, packageName, journeyName)
    }

    private fun validateInterface(classDecl: KSClassDeclaration): Boolean {
        val isSealedInterface = classDecl.classKind == ClassKind.INTERFACE && Modifier.SEALED in classDecl.modifiers
        if (!isSealedInterface) {
            logger.error(
                "@Journey must annotate a sealed interface, found ${classDecl.classKind} ${classDecl.modifiers}",
                classDecl
            )
            return false
        }
        val implementsJourneyStep = classDecl.superTypes.any { superType ->
            superType.resolve().declaration.qualifiedName?.asString() == JOURNEY_STEP_FQN
        }
        if (!implementsJourneyStep) {
            logger.error(
                "@Journey interface '${classDecl.simpleName.asString()}' must extend JourneyStep",
                classDecl
            )
            return false
        }
        return true
    }

    private fun validateSubclasses(
        journeyDecl: KSClassDeclaration,
        subclasses: List<KSClassDeclaration>
    ): Boolean {
        if (subclasses.isEmpty()) {
            logger.error("@Journey interface must have at least one @Step subclass", journeyDecl)
            return false
        }
        val initialClass = subclasses.first()
        if (initialClass.classKind != ClassKind.OBJECT) {
            logger.error(
                "The first @Step '${initialClass.simpleName.asString()}' must be a data object " +
                    "because it is the initial step and must be instantiable without arguments.",
                initialClass
            )
            return false
        }
        return true
    }
}
