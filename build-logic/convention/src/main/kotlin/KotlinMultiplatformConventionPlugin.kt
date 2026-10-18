import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.quare.blitzsplit.buildlogic.configureCoverage
import com.quare.blitzsplit.buildlogic.createNamespace
import com.quare.blitzsplit.buildlogic.getAndroidSdkVersions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Every shared module targets the platforms whose UI consumes it: Android, iOS and macOS (the
 * SwiftUI app) and JS (the React app). jvm is there for fast unit tests and coverage only.
 */
class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            val sdkVersions = getAndroidSdkVersions()

            extensions.configure<KotlinMultiplatformExtension> {
                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension>(
                    "androidLibrary",
                ) {
                    namespace = createNamespace()
                    compileSdk = sdkVersions.compileSdk
                    minSdk = sdkVersions.minSdk
                }

                iosArm64()
                iosSimulatorArm64()
                macosArm64()

                js {
                    browser()
                    useEsModules()
                }

                jvm()

                applyDefaultHierarchyTemplate()

                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                    optIn.addAll(
                        "kotlin.time.ExperimentalTime",
                        "kotlin.uuid.ExperimentalUuidApi",
                        "kotlin.js.ExperimentalJsExport",
                        "kotlinx.coroutines.ExperimentalCoroutinesApi",
                    )
                }

                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                }
            }

            configureCoverage()
        }
    }
}
