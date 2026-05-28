# JourneyKMP

A type-safe, annotation-driven navigation library for Kotlin Multiplatform (Android & iOS), built on top of [Navigation3](https://developer.android.com/jetpack/compose/navigation3).

You define a journey as a sealed interface. KSP reads it at build time and generates a typed controller per step, a sealed view class, and a ready-to-use Compose host. Screens can only navigate to the exits you declared — nothing more.

---

## Installation

> TODO

---

## Usage

### 1. Define a journey

Create a sealed interface in `commonMain`, extend `JourneyStep`, and annotate it with `@Journey`. Each subclass is a step; `@Exit` declares where that step can navigate to.

```kotlin
import dev.jianastrero.journey.JourneyStep
import dev.jianastrero.journey.annotations.*
import dev.jianastrero.journey.annotations.PiggybackTrigger.ON_ENTER

@Journey
sealed interface LoginStep : JourneyStep {

    // First subclass = initial step. Must be a data object (no constructor args).
    // @Exit generates toEnterPassword(email: String) on this step's controller.
    @Step
    @Exit("toEnterPassword", EnterPassword::class)
    data object EnterEmail : LoginStep

    // Data class steps carry data through the journey.
    // @Piggyback fires a named side-effect when this step is entered.
    @Step
    @Exit("toDone", Done::class)
    @Piggyback("analytics:login_attempted", on = ON_ENTER)
    data class EnterPassword(val email: String) : LoginStep

    // No @Exit = terminal step. KSP generates finish() on this step's controller.
    @Step
    data object Done : LoginStep
}
```

**Annotation reference**

| Annotation | Purpose |
|---|---|
| `@Journey` | Marks the sealed interface; triggers KSP code generation |
| `@Step` | Marks a sealed subclass as a navigation step |
| `@Exit("name", To::class)` | Generates `fun name(...)` on the controller; parameters come from `To`'s primary constructor |
| `@Piggyback("id", on = ...)` | Fires a named side-effect on `ON_ENTER` (default) or `ON_EXIT` |

**Rules enforced at build time**
- The interface must extend `JourneyStep`
- The first subclass must be a `data object` (it is the initial step and requires no arguments)
- A step with no `@Exit` is terminal and receives `fun finish()` on its controller

---

### 2. What gets generated

For a journey named `LoginStep`, KSP produces three files in the same package:

| Generated file | Contents |
|---|---|
| `LoginStepControllers.kt` | One interface per step, e.g. `LoginStepEnterEmailController`. Each extends `StepController` (providing `back()`) and exposes only the exits declared for that step. |
| `LoginStepView.kt` | `sealed class LoginStepView` with one inner class per step. Data class steps include their fields; all steps include their controller. |
| `LoginStepJourneyHost.kt` | `@Composable fun LoginStepJourneyHost(onFinish, content)` wired to Nav3's `NavDisplay`. |

---

### 3. Drop in the host

Place `LoginStepJourneyHost` where the journey starts. The `content` lambda receives a sealed `LoginStepView` — use a `when` block to render the right screen for each state:

```kotlin
@Composable
fun App() {
    LoginStepJourneyHost(onFinish = { /* journey complete — navigate away */ }) { view ->
        when (view) {
            is LoginStepView.EnterEmail ->
                EnterEmailScreen(view.controller)

            is LoginStepView.EnterPassword ->
                EnterPasswordScreen(view.step.email, view.controller)

            is LoginStepView.Done ->
                DoneScreen(view.controller)
        }
    }
}
```

The compiler enforces exhaustiveness on `view` — no `else` branch needed and no step can be forgotten. Back navigation (system gesture / button) is handled automatically.

---

### 4. Implement your screens

Each screen receives its typed controller. The only navigation methods available are the ones declared via `@Exit` for that step:

```kotlin
@Composable
fun EnterEmailScreen(controller: LoginStepEnterEmailController) {
    var email by remember { mutableStateOf("") }
    // ...
    Button(onClick = { controller.toEnterPassword(email) }) { Text("Continue") }
}

@Composable
fun EnterPasswordScreen(email: String, controller: LoginStepEnterPasswordController) {
    // email was forwarded from the previous step — no extra state needed
    // ...
    Button(onClick = { controller.toDone() }) { Text("Log in") }
    Button(onClick = { controller.back() }) { Text("Back") }
}

@Composable
fun DoneScreen(controller: LoginStepDoneController) {
    // ...
    Button(onClick = { controller.finish() }) { Text("Continue to app") }
}
```

---

### 5. Piggybacks (optional)

A piggyback is a named side-effect — analytics, logging, an API call — that fires automatically when a step is entered or exited. Register handlers by providing a `PiggybackRegistry` through the composition:

```kotlin
@Composable
fun App() {
    val registry = remember {
        PiggybackRegistry().apply {
            register("analytics:login_attempted") { stepId, journeyId ->
                analytics.track("login_attempted", mapOf("step" to stepId))
            }
        }
    }

    CompositionLocalProvider(LocalPiggybackRegistry provides registry) {
        LoginStepJourneyHost(onFinish = { /* ... */ }) { view ->
            // ...
        }
    }
}
```

If no registry is provided, piggyback calls are silently ignored.

---

## How navigation works

The back stack uses **pop-or-push** logic:

- If the target step's class is **already in the stack**, the stack pops down to it — no duplicate entries.
- If the target step's class is **not in the stack**, it is pushed.

This means `controller.back()` and a declared `controller.toEnterEmail()` exit both navigate back to `EnterEmail` correctly, regardless of how deep the stack is.
