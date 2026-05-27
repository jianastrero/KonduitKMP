plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "dev.jianastrero"
version = "0.1.0"

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // intentionally empty — annotations have no runtime deps
        }
    }
}
