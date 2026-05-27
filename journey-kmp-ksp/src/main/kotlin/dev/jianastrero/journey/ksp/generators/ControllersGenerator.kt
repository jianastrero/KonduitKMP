package dev.jianastrero.journey.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.jianastrero.journey.ksp.model.StepModel

private val STEP_CONTROLLER_CLASS = ClassName("dev.jianastrero.journey", "StepController")

internal class ControllersGenerator(
    private val codeGenerator: CodeGenerator,
    @Suppress("UnusedPrivateProperty")
    private val logger: KSPLogger
) {
    fun generate(
        journeyDecl: KSClassDeclaration,
        steps: List<StepModel>,
        packageName: String,
        journeyName: String
    ) {
        val fileName = "${journeyName}Controllers"
        val fileSpec = FileSpec.builder(packageName, fileName)

        steps.forEach { step ->
            val controllerName = "$journeyName${step.simpleName}Controller"

            val interfaceSpec = TypeSpec.interfaceBuilder(controllerName)
                .addSuperinterface(STEP_CONTROLLER_CLASS)

            step.exits.forEach { exit ->
                val funcSpec = FunSpec.builder(exit.methodName)
                    .addModifiers(KModifier.ABSTRACT)

                exit.params.forEach { param ->
                    funcSpec.addParameter(
                        param.name!!.asString(),
                        param.type.resolve().toTypeName()
                    )
                }

                interfaceSpec.addFunction(funcSpec.build())
            }

            if (step.isTerminal) {
                interfaceSpec.addFunction(
                    FunSpec.builder("finish")
                        .addModifiers(KModifier.ABSTRACT)
                        .build()
                )
            }

            fileSpec.addType(interfaceSpec.build())
        }

        fileSpec.build().writeTo(
            codeGenerator,
            Dependencies(aggregating = false, journeyDecl.containingFile!!)
        )
    }
}
