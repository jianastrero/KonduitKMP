plugins {
    kotlin("jvm")
}

group = "dev.jianastrero"
version = "0.1.0"

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":journey-kmp-annotations"))
}
