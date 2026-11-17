package com.quare.blitzsplit.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

private const val CACHE_PROJECT_LIBRARIES = "blitzsplit.native.cacheProjectLibraries"

/**
 * With `blitzsplit.native.cacheProjectLibraries=true`, the link of a test binary compiles each
 * module of this build to native code once, into Kotlin/Native's cache (`~/.konan`), and every
 * other test binary reuses it. Kotlin/Native only does that for the libraries under the Gradle
 * home (the `-Xauto-cache-from` the Kotlin plugin passes), so each module's test link compiled
 * every module it depends on again.
 *
 * Off unless asked for: an entry is named after its module's contents, so every change leaves one
 * more in `~/.konan` and nothing deletes them, and the flag holds this checkout's path, which keeps
 * two checkouts from sharing a link through Gradle's build cache. The macOS jobs of CI turn it on
 * (`.github/actions/gradle/action.yml`), where the runner is thrown away and the path is one.
 *
 * The flag is the compiler's own, undocumented: a Kotlin version that drops it only warns about an
 * argument it doesn't know, and the links go back to compiling every module (docs/ci.md, "Speed
 * and timeouts", says how that looks).
 */
internal fun Project.cacheProjectLibrariesInNativeTestLinks() {
    val isOn = providers.gradleProperty(CACHE_PROJECT_LIBRARIES).map(String::toBoolean).getOrElse(false)
    if (!isOn) return
    val projectLibraries = "-Xauto-cache-from=${rootDir.absolutePath}"
    extensions.configure<KotlinMultiplatformExtension> {
        targets.withType<KotlinNativeTarget>().configureEach {
            binaries.withType<TestExecutable>().configureEach {
                freeCompilerArgs += projectLibraries
            }
        }
    }
}
