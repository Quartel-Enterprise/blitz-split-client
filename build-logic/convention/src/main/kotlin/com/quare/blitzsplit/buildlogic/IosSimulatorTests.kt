package com.quare.blitzsplit.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

/** Holds no state: its single usage slot is the lock the simulator's test tasks take turns on. */
abstract class IosSimulatorLock : BuildService<BuildServiceParameters.None>

/**
 * Runs one module's simulator suite at a time. Each suite starts its own `simctl`, and two of them
 * at once can crash CoreSimulator while it reports the device set ("Invalid parameter not
 * satisfying: devicePair"), failing a module's suite with exit code 134 before any test runs.
 */
internal fun Project.runIosSimulatorTestsOneAtATime() {
    val lock = gradle.sharedServices.registerIfAbsent("iosSimulatorLock", IosSimulatorLock::class) {
        maxParallelUsages.set(1)
    }
    tasks.withType<KotlinNativeSimulatorTest>().configureEach {
        usesService(lock)
    }
}
