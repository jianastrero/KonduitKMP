package dev.jianastrero.journey.example

import dev.jianastrero.journey.JourneyStep
import dev.jianastrero.journey.annotations.Exit
import dev.jianastrero.journey.annotations.Journey
import dev.jianastrero.journey.annotations.Piggyback
import dev.jianastrero.journey.annotations.PiggybackTrigger.ON_ENTER
import dev.jianastrero.journey.annotations.Step

// @Journey marks this sealed interface as a journey definition.
// KSP generates LoginStepJourneyHost, LoginStepView, and per-step typed controllers.
@Journey
sealed interface LoginStep : JourneyStep {

    // The first subclass is the initial step — must be a data object (no constructor args required).
    // @Exit("toEnterPassword", ...) makes KSP generate toEnterPassword(email: String)
    // on LoginStepEnterEmailController. That is the only valid forward exit from this step.
    @Step
    @Exit("toEnterPassword", EnterPassword::class)
    data object EnterEmail : LoginStep

    // Data class steps carry data through the journey.
    // The email field is accessible via LoginStepView.EnterPassword.step.email.
    // @Piggyback fires a named side-effect on ON_ENTER; register a handler via PiggybackRegistry.
    @Step
    @Exit("toDone", Done::class)
    @Piggyback("analytics:login_attempted", on = ON_ENTER)
    data class EnterPassword(val email: String) : LoginStep

    // A step with no @Exit is terminal. KSP generates finish() on LoginStepDoneController,
    // which calls onFinish() on the JourneyHost when invoked.
    @Step
    data object Done : LoginStep
}
