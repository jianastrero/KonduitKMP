package dev.jianastrero.journey.ksp.model

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

private const val EXIT_FQN = "dev.jianastrero.journey.annotations.Exit"
private const val PIGGYBACK_FQN = "dev.jianastrero.journey.annotations.Piggyback"

/**
 * Reads repeated KSP annotations by their fully qualified name.
 *
 * KSP handles @Repeatable differently depending on count:
 * - Single annotation: returned directly.
 * - Multiple annotations: wrapped in a generated container whose `value`
 *   argument is a list of the individual annotations.
 *
 * This function handles both cases transparently.
 */
internal fun KSClassDeclaration.getAnnotationsByFqn(fqn: String): List<KSAnnotation> =
    annotations.flatMap { ann ->
        val declFqn = ann.annotationType.resolve().declaration.qualifiedName?.asString()
        when {
            declFqn == fqn -> listOf(ann)
            else -> {
                val containerValue = ann.arguments
                    .firstOrNull { it.name?.asString() == "value" }?.value
                if (containerValue is List<*>) {
                    @Suppress("UNCHECKED_CAST")
                    (containerValue as List<KSAnnotation>).filter { inner ->
                        inner.annotationType.resolve().declaration
                            .qualifiedName?.asString() == fqn
                    }
                } else {
                    emptyList()
                }
            }
        }
    }.toList()

internal fun parseSteps(subclasses: List<KSClassDeclaration>): List<StepModel> =
    subclasses.mapIndexed { index, classDecl ->
        val exits = classDecl.getAnnotationsByFqn(EXIT_FQN).map { ann ->
            val nameArg = ann.arguments.first { it.name?.asString() == "name" }.value as String
            val toType = ann.arguments.first { it.name?.asString() == "to" }.value as KSType
            val targetDecl = toType.declaration as KSClassDeclaration
            ExitModel(
                methodName = nameArg,
                targetClass = targetDecl,
                params = targetDecl.primaryConstructor?.parameters ?: emptyList()
            )
        }

        val piggybacks = classDecl.getAnnotationsByFqn(PIGGYBACK_FQN).map { ann ->
            val id = ann.arguments.first { it.name?.asString() == "id" }.value as String
            val trigger = ann.arguments.firstOrNull { it.name?.asString() == "on" }
                ?.value
                ?.let { it.toString().substringAfterLast('.') }
                ?: "ON_ENTER"
            PiggybackModel(id = id, trigger = trigger)
        }

        StepModel(
            classDecl = classDecl,
            simpleName = classDecl.simpleName.asString(),
            isDataObject = classDecl.classKind == ClassKind.OBJECT,
            isInitial = index == 0,
            isTerminal = exits.isEmpty(),
            exits = exits,
            piggybacks = piggybacks
        )
    }
