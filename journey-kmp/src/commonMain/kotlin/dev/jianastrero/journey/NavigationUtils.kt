package dev.jianastrero.journey

import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Navigates to [target] within the [backStack] using pop-or-push logic:
 * - If [target]'s class is already in the stack, pop down to that entry.
 * - Otherwise push [target] onto the stack.
 */
fun navigateTo(backStack: SnapshotStateList<JourneyStep>, target: JourneyStep) {
    val existingIndex = backStack.indexOfFirst { it::class == target::class }
    if (existingIndex >= 0) {
        while (backStack.size > existingIndex + 1) {
            backStack.removeLastOrNull()
        }
    } else {
        backStack.add(target)
    }
}
