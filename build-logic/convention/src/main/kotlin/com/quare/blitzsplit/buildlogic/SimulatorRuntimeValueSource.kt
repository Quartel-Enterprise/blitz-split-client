package com.quare.blitzsplit.buildlogic

import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Whether `simctl list runtimes available` lists a usable runtime of a [SimulatorFamily]: an
 * unavailable one (its disk image deleted, or left by an older Xcode) has no device to run on.
 *
 * Why: a Mac set up for the phone app may have no watchOS runtime, which Xcode installs apart, and
 * its watchOS suites and their test binaries are then skipped rather than failing `build` and
 * `allTests` on every module. A simctl that gives no answer counts as having one, so the suite
 * reports the outage as [IosSimulator] does. Off a Mac it has none, and the tasks are disabled there
 * anyway.
 */
abstract class SimulatorRuntimeValueSource : ValueSource<Boolean, SimulatorRuntimeValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val runtimeName: Property<String>
    }

    override fun obtain(): Boolean {
        if (!File(XCRUN).exists()) return false
        val output = File.createTempFile("simctl", ".out")
        return try {
            val process = ProcessBuilder(XCRUN, "simctl", "list", "runtimes", "available")
                .redirectOutput(output)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                true
            } else {
                val prefix = parameters.runtimeName.get() + " "
                process.exitValue() != 0 ||
                    output.readLines().any { it.trimStart().startsWith(prefix, ignoreCase = true) }
            }
        } catch (_: IOException) {
            true
        } finally {
            output.delete()
        }
    }

    private companion object {
        const val XCRUN = "/usr/bin/xcrun"
        const val TIMEOUT_SECONDS = 60L
    }
}
