package com.quare.blitzsplit.buildlogic

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

// The Kover variant CI reports and verifies. It holds only the jvm target, where commonTest and
// jvmTest run, so the merged report runs each module's tests once.
const val COVERAGE_VARIANT = "ci"

private const val TESTED_TARGET = "jvm"

// The :testing modules hold the fakes tests share, which are test code even though they live in
// commonMain.
private val Project.isMeasuredForCoverage: Boolean
    get() = !path.endsWith(":testing")

fun Project.configureCoverage() {
    if (!isMeasuredForCoverage) return
    pluginManager.apply("org.jetbrains.kotlinx.kover")

    extensions.configure<KoverProjectExtension> {
        currentProject {
            createVariant(COVERAGE_VARIANT) {
                add(TESTED_TARGET)
            }
        }
    }
}
