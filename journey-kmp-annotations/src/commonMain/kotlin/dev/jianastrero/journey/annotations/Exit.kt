package dev.jianastrero.journey.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Exit(
    val name: String,
    val to: KClass<*>
)
