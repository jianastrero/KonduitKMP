package dev.jianastrero.journey.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.jianastrero.journey.ksp.model.ExitModel
import dev.jianastrero.journey.ksp.model.PiggybackModel
import dev.jianastrero.journey.ksp.model.StepModel

private val COMPOSABLE = ClassName("androidx.compose.runtime", "Composable")
private val REMEMBER = ClassName("androidx.compose.runtime", "remember")
private val LAUNCHED_EFFECT = ClassName("androidx.compose.runtime", "LaunchedEffect")
private val DISPOSABLE_EFFECT = ClassName("androidx.compose.runtime", "DisposableEffect")
private val REMEMBER_COROUTINE_SCOPE = ClassName("androidx.compose.runtime", "rememberCoroutineScope")
private val MUTABLE_STATE_LIST_OF = ClassName("androidx.compose.runtime", "mutableStateListOf")
private val NAV_DISPLAY = ClassName("androidx.navigation3.ui", "NavDisplay")
private val NAV_ENTRY = ClassName("androidx.navigation3.runtime", "NavEntry")
private val JOURNEY_STEP = ClassName("dev.jianastrero.journey", "JourneyStep")
private val NAVIGATE_TO = ClassName("dev.jianastrero.journey", "navigateTo")
private val LOCAL_PIGGYBACK_REGISTRY = ClassName("dev.jianastrero.journey", "LocalPiggybackRegistry")
private val LAUNCH = MemberName("kotlinx.coroutines", "launch")

internal class HostGenerator(
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
        val fileName = "${journeyName}JourneyHost"
        val viewType = ClassName(packageName, "${journeyName}View")
        val contentParam = ParameterSpec.builder(
            "content",
            LambdaTypeName.get(
                parameters = listOf(ParameterSpec.unnamed(viewType)),
                returnType = UNIT
            ).copy(annotations = listOf(AnnotationSpec.builder(COMPOSABLE).build()))
        ).build()
        val onFinishParam = ParameterSpec.builder("onFinish", LambdaTypeName.get(returnType = UNIT))
            .defaultValue("{}")
            .build()
        val hostFun = FunSpec.builder(fileName)
            .addAnnotation(COMPOSABLE)
            .addParameter(onFinishParam)
            .addParameter(contentParam)
            .addCode(buildHostBody(steps, packageName, journeyName))
            .build()
        FileSpec.builder(packageName, fileName)
            .addFunction(hostFun)
            .build()
            .writeTo(codeGenerator, Dependencies(aggregating = false, journeyDecl.containingFile!!))
    }

    private fun buildHostBody(steps: List<StepModel>, packageName: String, journeyName: String): CodeBlock {
        val journeyClass = ClassName(packageName, journeyName)
        val viewClass = ClassName(packageName, "${journeyName}View")
        val initialStep = steps.first()
        val hasExitPiggybacks = steps.any { s -> s.piggybacks.any { it.trigger == "ON_EXIT" } }

        return CodeBlock.builder()
            .addStatement(
                "val backStack = %T { %T<%T>(%T.%L) }",
                REMEMBER,
                MUTABLE_STATE_LIST_OF,
                JOURNEY_STEP,
                journeyClass,
                initialStep.simpleName
            )
            .addStatement("val piggybackRegistry = %T.current", LOCAL_PIGGYBACK_REGISTRY)
            .apply { if (hasExitPiggybacks) addStatement("val scope = %T()", REMEMBER_COROUTINE_SCOPE) }
            .add("\n")
            .beginControlFlow(
                "%T(backStack = backStack, onBack = { backStack.removeLastOrNull() }) { step ->",
                NAV_DISPLAY
            )
            .beginControlFlow("when (step)")
            .apply { steps.forEach { add(buildStepBranch(it, journeyClass, viewClass, journeyName, packageName)) } }
            .addStatement("else -> %T(step) {}", NAV_ENTRY)
            .endControlFlow()
            .endControlFlow()
            .build()
    }

    private fun buildStepBranch(
        step: StepModel,
        journeyClass: ClassName,
        viewClass: ClassName,
        journeyName: String,
        packageName: String
    ): CodeBlock {
        val controllerType = ClassName(packageName, "$journeyName${step.simpleName}Controller")
        return CodeBlock.builder()
            .beginControlFlow("is %T.%L ->", journeyClass, step.simpleName)
            .beginControlFlow("%T(step)", NAV_ENTRY)
            .add(buildEnterPiggybacks(step.piggybacks, step.simpleName, journeyName))
            .add(buildExitPiggybacks(step.piggybacks, step.simpleName, journeyName))
            .add(buildControllerBlock(step, controllerType, journeyClass, viewClass))
            .endControlFlow()
            .endControlFlow()
            .build()
    }

    private fun buildEnterPiggybacks(piggybacks: List<PiggybackModel>, stepId: String, journeyId: String): CodeBlock {
        val enters = piggybacks.filter { it.trigger == "ON_ENTER" }
        if (enters.isEmpty()) return CodeBlock.builder().build()
        return CodeBlock.builder()
            .beginControlFlow("%T(step)", LAUNCHED_EFFECT)
            .apply {
                enters.forEach {
                    addStatement(
                        "piggybackRegistry?.fire(%S, stepId = %S, journeyId = %S)",
                        it.id,
                        stepId,
                        journeyId
                    )
                }
            }
            .endControlFlow()
            .build()
    }

    private fun buildExitPiggybacks(piggybacks: List<PiggybackModel>, stepId: String, journeyId: String): CodeBlock {
        val exits = piggybacks.filter { it.trigger == "ON_EXIT" }
        if (exits.isEmpty()) return CodeBlock.builder().build()
        return CodeBlock.builder()
            .beginControlFlow("%T(step)", DISPOSABLE_EFFECT)
            .beginControlFlow("onDispose")
            .apply {
                exits.forEach {
                    addStatement(
                        "scope.%M { piggybackRegistry?.fire(%S, stepId = %S, journeyId = %S) }",
                        LAUNCH,
                        it.id,
                        stepId,
                        journeyId
                    )
                }
            }
            .endControlFlow()
            .endControlFlow()
            .build()
    }

    private fun buildControllerBlock(
        step: StepModel,
        controllerType: ClassName,
        journeyClass: ClassName,
        viewClass: ClassName
    ): CodeBlock {
        return CodeBlock.builder()
            .beginControlFlow("val controller = %T(backStack)", REMEMBER)
            .beginControlFlow("object : %T", controllerType)
            .apply { step.exits.forEach { add(buildExitMethod(it, journeyClass)) } }
            .beginControlFlow("override fun back()")
            .addStatement("backStack.removeLastOrNull()")
            .endControlFlow()
            .apply { if (step.isTerminal) add(buildFinishMethod()) }
            .endControlFlow()
            .endControlFlow()
            .add(buildContentCall(step, viewClass))
            .build()
    }

    private fun buildExitMethod(exit: ExitModel, journeyClass: ClassName): CodeBlock {
        val targetFqSimple = exit.targetClass.simpleName.asString()
        val isTargetObject = exit.targetClass.classKind == ClassKind.OBJECT
        return if (exit.params.isEmpty()) {
            val targetExpr = if (isTargetObject) "%T.%L" else "%T.%L()"
            CodeBlock.builder()
                .beginControlFlow("override fun %L()", exit.methodName)
                .addStatement("%T(backStack, $targetExpr)", NAVIGATE_TO, journeyClass, targetFqSimple)
                .endControlFlow()
                .build()
        } else {
            val block = CodeBlock.builder()
            block.add("override fun %L(", exit.methodName)
            exit.params.forEachIndexed { i, param ->
                if (i > 0) block.add(", ")
                block.add("%L: %T", param.name!!.asString(), param.type.resolve().toTypeName())
            }
            val argList = exit.params.joinToString(", ") { it.name!!.asString() }
            block.beginControlFlow(")")
            block.addStatement("%T(backStack, %T.%L(%L))", NAVIGATE_TO, journeyClass, targetFqSimple, argList)
            block.endControlFlow()
            block.build()
        }
    }

    private fun buildFinishMethod(): CodeBlock =
        CodeBlock.builder()
            .beginControlFlow("override fun finish()")
            .addStatement("onFinish()")
            .endControlFlow()
            .build()

    private fun buildContentCall(step: StepModel, viewClass: ClassName): CodeBlock =
        if (step.isDataObject) {
            CodeBlock.of("content(%T.%L(controller))\n", viewClass, step.simpleName)
        } else {
            CodeBlock.of("content(%T.%L(step, controller))\n", viewClass, step.simpleName)
        }
}
