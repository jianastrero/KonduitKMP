package dev.jianastrero.journey.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Piggyback(
    val id: String,
    val on: PiggybackTrigger = PiggybackTrigger.ON_ENTER
)

enum class PiggybackTrigger {
    ON_ENTER,
    ON_EXIT
}
