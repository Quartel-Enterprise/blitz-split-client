import com.android.build.api.dsl.LibraryExtension
import com.quare.blitzsplit.buildlogic.createNamespace
import com.quare.blitzsplit.buildlogic.getAndroidSdkVersions
import com.quare.blitzsplit.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * An Android library that draws with Jetpack Compose: the design system and the native UI of
 * each feature (`:android:feature:*`). Kotlin comes built into AGP 9, so only the Compose
 * compiler plugin is applied on top.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            val sdkVersions = getAndroidSdkVersions()

            extensions.configure<LibraryExtension> {
                namespace = createNamespace()
                compileSdk = sdkVersions.compileSdk
                defaultConfig {
                    minSdk = sdkVersions.minSdk
                }
            }

            dependencies {
                val composeBom = platform(libs.findLibrary("androidx-compose-bom").get())
                add("implementation", composeBom)
                add("debugImplementation", composeBom)
                add("implementation", libs.findLibrary("compose-runtime").get())
                add("implementation", libs.findLibrary("compose-foundation").get())
                add("implementation", libs.findLibrary("compose-material3").get())
                add("implementation", libs.findLibrary("compose-ui").get())
                add("implementation", libs.findLibrary("compose-ui-toolingPreview").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtimeCompose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodelCompose").get())
                add("implementation", platform(libs.findLibrary("koin-bom").get()))
                add("implementation", libs.findLibrary("koin-compose-viewmodel").get())
                add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
                if (path != ":android:designsystem") {
                    add("implementation", project(":android:designsystem"))
                }
            }
        }
    }
}
