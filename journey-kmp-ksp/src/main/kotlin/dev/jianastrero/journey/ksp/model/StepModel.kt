package dev.jianastrero.journey.ksp.model

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

internal data class ExitModel(
    val methodName: String,
    val targetClass: KSClassDeclaration,
    val params: List<KSValueParameter>
)

internal data class PiggybackModel(
    val id: String,
    val trigger: String // "ON_ENTER" or "ON_EXIT"
)

internal data class StepModel(
    val classDecl: KSClassDeclaration,
    val simpleName: String,
    val isDataObject: Boolean,
    val isInitial: Boolean,
    val isTerminal: Boolean,
    val exits: List<ExitModel>,
    val piggybacks: List<PiggybackModel>
)
