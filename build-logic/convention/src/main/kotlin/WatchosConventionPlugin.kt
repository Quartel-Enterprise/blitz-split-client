import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds the Apple Watch targets to a shared module the watchOS app needs, on top of `blitzsplit.kotlin.multiplatform`:
 * `watchosArm64` (arm64_32, the device slice every Apple Watch from Series 4 on runs) and `watchosSimulatorArm64`.
 * The default hierarchy puts both under `appleMain`, so only iOS-only or macOS-only code needs a `watchosMain` actual.
 *
 * `watchosDeviceArm64` (the 64-bit slice of Series 9 and later) waits: it is a Kotlin/Native tier 3 target, and
 * `lifecycle-viewmodel` and supabase-kt don't publish it yet. `watchosArm32` and `watchosX64` are deprecated in Kotlin.
 */
class WatchosConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KotlinMultiplatformConventionPlugin::class.java)
            extensions.configure<KotlinMultiplatformExtension> {
                watchosArm64()
                watchosSimulatorArm64()
            }
        }
    }
}
