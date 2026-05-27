package dev.jianastrero.journey

import androidx.compose.runtime.compositionLocalOf

fun interface PiggybackHandler {
    suspend fun fire(stepId: String, journeyId: String)
}

class PiggybackRegistry {
    private val handlers = mutableMapOf<String, PiggybackHandler>()

    fun register(id: String, handler: PiggybackHandler) {
        handlers[id] = handler
    }

    fun register(id: String, block: suspend (stepId: String, journeyId: String) -> Unit) {
        handlers[id] = PiggybackHandler { stepId, journeyId -> block(stepId, journeyId) }
    }

    suspend fun fire(id: String, stepId: String = "", journeyId: String = "") {
        handlers[id]?.fire(stepId, journeyId)
    }
}

val LocalPiggybackRegistry = compositionLocalOf<PiggybackRegistry?> { null }
