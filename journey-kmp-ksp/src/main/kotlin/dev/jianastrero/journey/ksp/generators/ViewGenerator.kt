package dev.jianastrero.journey.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import dev.jianastrero.journey.ksp.model.StepModel

internal class ViewGenerator(
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
        val fileName = "${journeyName}View"
        val viewClassName = ClassName(packageName, fileName)

        val sealedClass = TypeSpec.classBuilder(fileName)
            .addModifiers(KModifier.SEALED)

        steps.forEach { step ->
            val controllerType = ClassName(packageName, "$journeyName${step.simpleName}Controller")
            val innerClass = TypeSpec.classBuilder(step.simpleName)
                .superclass(viewClassName)

            if (step.isDataObject) {
                innerClass
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addModifiers(KModifier.INTERNAL)
                            .addParameter(ParameterSpec.builder("controller", controllerType).build())
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("controller", controllerType)
                            .initializer("controller")
                            .build()
                    )
            } else {
                val stepType = ClassName(packageName, journeyName)
                    .nestedClass(step.simpleName)

                innerClass
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addModifiers(KModifier.INTERNAL)
                            .addParameter(ParameterSpec.builder("step", stepType).build())
                            .addParameter(ParameterSpec.builder("controller", controllerType).build())
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("step", stepType)
                            .initializer("step")
                            .build()
                    )
                    .addProperty(
                        PropertySpec.builder("controller", controllerType)
                            .initializer("controller")
                            .build()
                    )
            }

            sealedClass.addType(innerClass.build())
        }

        FileSpec.builder(packageName, fileName)
            .addType(sealedClass.build())
            .build()
            .writeTo(
                codeGenerator,
                Dependencies(aggregating = false, journeyDecl.containingFile!!)
            )
    }
}
