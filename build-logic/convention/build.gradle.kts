plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kover.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatform") {
            id = "blitzsplit.kotlin.multiplatform"
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("watchos") {
            id = "blitzsplit.kotlin.multiplatform.watchos"
            implementationClass = "WatchosConventionPlugin"
        }
        register("androidCompose") {
            id = "blitzsplit.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
