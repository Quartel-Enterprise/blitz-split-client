package com.quare.blitzsplit.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// How long a build on CI waits, in all, for a simulator that lists no device. The run of main that
// lost its devices listed none for the seven minutes its remaining suites took to fail.
private val ciWait = 10.minutes
private val runtimeHeading = Regex("""^-- (.+) --$""")
private val deviceId = Regex("""[0-9A-F]{8}-(?:[0-9A-F]{4}-){3}[0-9A-F]{12}""")

/**
 * The device the Kotlin plugin picks for a simulator suite of [family], from the standard output of
 * `simctl list devices available`: the first one of the last runtime of that family listed. It mirrors the
 * plugin's own lookup (XcodeDefaultTestDevicesValueSource, as of Kotlin 2.4), which is internal.
 */
private fun findDefaultDeviceOrNull(
    availableDevices: String,
    family: SimulatorFamily,
): String? {
    var device: String? = null
    var isFirstOfRuntime = false
    availableDevices.lineSequence().forEach { line ->
        val runtime = runtimeHeading.find(line)?.groupValues?.get(1)
        if (runtime != null) {
            isFirstOfRuntime = runtime.substringBefore(' ').equals(family.runtimeName, ignoreCase = true)
        } else if (isFirstOfRuntime) {
            deviceId.find(line)?.let {
                device = it.value
                isFirstOfRuntime = false
            }
        }
    }
    return device
}

private class CommandResult(
    val arguments: List<String>,
    val status: String,
    val output: String,
    val errors: String,
    val hasTimedOut: Boolean,
    val hasFailedToRun: Boolean,
) {
    val hasAnswered: Boolean
        get() = !hasTimedOut && !hasFailedToRun

    fun toLogEntry(): String =
        listOf("$ xcrun ${arguments.joinToString(" ")} ($status)", output.trimEnd(), errors.trimEnd())
            .filter(String::isNotEmpty)
            .joinToString("\n")
}

/**
 * The simulator every module's suite runs on.
 *
 * Its single usage slot is the lock the simulator's test tasks take turns on. Each suite starts its
 * own `simctl`, and two of them at once can crash CoreSimulator while it reports the device set
 * ("Invalid parameter not satisfying: devicePair"), failing a module's suite with exit code 134
 * before any test runs.
 *
 * It also hands each suite its device. The Kotlin plugin asks `simctl` for one per module and fails
 * the suite when the answer has none ("Xcode does not support simulator tests for
 * ios_simulator_arm64"). On a CI runner the devices went missing in the middle of a run, and every
 * suite after that failed without running. So an answer with no device logs the simulator's state,
 * and on CI the suite waits for a device to be listed again.
 */
abstract class IosSimulator : BuildService<IosSimulator.Parameters> {
    interface Parameters : BuildServiceParameters {
        /** How long the build waits, in all, for a simulator that lists no device. */
        val waitSeconds: Property<Long>
    }

    private val logger = Logging.getLogger(IosSimulator::class.java)
    private val pollInterval = 5.seconds
    private val commandTimeout = 1.minutes
    private val availableDevices = listOf("simctl", "list", "devices", "available")

    // What tells a missing runtime from a missing device: every device with the reason it is
    // unavailable, the runtimes CoreSimulator knows, and the disk images they are mounted from.
    private val stateCommands = listOf(
        listOf("simctl", "list", "devices"),
        listOf("simctl", "list", "runtimes"),
        listOf("simctl", "runtime", "list"),
    )

    // The wait allowance and a simctl that stopped answering are the build's, whichever simulator
    // found them; the outage each family logged is its own, so one family's doesn't mute the other's.
    private var waitLeft: Duration? = null
    private val loggedOutages = mutableSetOf<SimulatorFamily>()
    private var hasStoppedAnswering = false

    @Synchronized
    fun fetchDevice(family: SimulatorFamily): String {
        // A simctl that no longer answers would take its timeout from every suite left, and an
        // xcrun that cannot run has nothing new to say.
        val listing = if (hasStoppedAnswering) null else run(availableDevices)
        val device = listing?.let { findDefaultDeviceOrNull(it.output, family) ?: waitForDeviceOrNull(it, family) }
        if (device != null) {
            loggedOutages -= family
            return device
        }
        throw GradleException(
            "The ${family.runtimeName} simulator has no available device to run the tests on: `xcrun simctl list " +
                "devices available` listed none under a ${family.runtimeName} runtime, or gave no answer. Its state " +
                "is logged where a suite first found none.",
        )
    }

    // The time waited comes out of the build's allowance, so a simulator that never comes back
    // holds the build once, not once per module.
    private fun waitForDeviceOrNull(
        firstListing: CommandResult,
        family: SimulatorFamily,
    ): String? {
        if (loggedOutages.add(family)) {
            logger.warn(
                "The ${family.runtimeName} simulator lists no available device. Its state:\n{}",
                buildStateReport(firstListing),
            )
        }
        var listing = firstListing
        val allowance = waitLeft ?: parameters.waitSeconds.get().seconds
        // An xcrun that cannot run stays that way, unlike a simulator that lists no device.
        if (allowance > Duration.ZERO && !firstListing.hasFailedToRun) {
            logger.warn(
                "Waiting up to {} for the ${family.runtimeName} simulator to list a device.",
                toWholeSeconds(allowance),
            )
            val start = TimeSource.Monotonic.markNow()
            try {
                while (start.elapsedNow() < allowance) {
                    Thread.sleep(pollInterval.inWholeMilliseconds)
                    // The last polls get what is left of the allowance, not a whole timeout each.
                    val timeLeft = allowance - start.elapsedNow()
                    val timeout = timeLeft.coerceAtLeast(pollInterval).coerceAtMost(commandTimeout)
                    listing = run(availableDevices, timeout)
                    val device = findDefaultDeviceOrNull(listing.output, family)
                    if (device != null) {
                        logger.warn(
                            "The ${family.runtimeName} simulator listed a device again after {}.",
                            toWholeSeconds(start.elapsedNow()),
                        )
                        return device
                    }
                }
                logger.warn(
                    "The ${family.runtimeName} simulator still lists no available device after {}. Its state now:\n{}",
                    toWholeSeconds(start.elapsedNow()),
                    buildStateReport(listing),
                )
            } finally {
                waitLeft = (allowance - start.elapsedNow()).coerceAtLeast(Duration.ZERO)
            }
        }
        hasStoppedAnswering = !listing.hasAnswered
        return null
    }

    private fun toWholeSeconds(duration: Duration): Duration = duration.inWholeSeconds.seconds

    // The commands that describe the state would hang, or fail to run, like the listing did.
    private fun buildStateReport(listing: CommandResult): String {
        val results = if (listing.hasAnswered) listOf(listing) + stateCommands.map(::run) else listOf(listing)
        return results.joinToString("\n\n") { it.toLogEntry() }
    }

    // A simctl that hangs would otherwise hold the simulator's only slot for the rest of the build,
    // so it gets a timeout. Its output goes through files, not pipes: reading the pipe of one that
    // hangs would never return.
    private fun run(
        arguments: List<String>,
        timeout: Duration = commandTimeout,
    ): CommandResult {
        val files = mutableListOf<File>()
        var process: Process? = null
        try {
            val output = File.createTempFile("simctl", ".out").also(files::add)
            val errors = File.createTempFile("simctl", ".err").also(files::add)
            val started = ProcessBuilder(listOf(XCRUN) + arguments)
                .redirectOutput(output)
                .redirectError(errors)
                .start()
                .also { process = it }
            val hasFinished = started.waitFor(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            if (!hasFinished) started.destroyForcibly().waitFor()
            val waited = toWholeSeconds(timeout)
            val status = if (hasFinished) "exit code ${started.exitValue()}" else "no answer in $waited"
            return CommandResult(arguments, status, output.readText(), errors.readText(), !hasFinished, false)
        } catch (e: IOException) {
            return CommandResult(arguments, "could not run: $e", "", "", false, true)
        } finally {
            // A wait interrupted by a cancelled build would leave a simctl that hangs running.
            process?.takeIf { it.isAlive }?.destroyForcibly()
            files.forEach { it.delete() }
        }
    }

    private companion object {
        const val XCRUN = "/usr/bin/xcrun"
    }
}

/**
 * Runs one module's simulator suite at a time, each iOS or watchOS one on the device [IosSimulator]
 * hands it. A task disabled on this host (the simulator's, off a Mac) never reads its device.
 */
internal fun Project.configureIosSimulatorTests() {
    val simulator = gradle.sharedServices.registerIfAbsent("iosSimulator", IosSimulator::class) {
        maxParallelUsages.set(1)
        // GitHub Actions sets CI to true.
        parameters.waitSeconds.set(
            providers.environmentVariable("CI").map { if (it.toBoolean()) ciWait.inWholeSeconds else 0L }.orElse(0L),
        )
    }
    tasks.withType<KotlinNativeSimulatorTest>().configureEach {
        usesService(simulator)
        // By the task's name, which it has from the start: the Kotlin plugin sets its target name
        // and disables it off a Mac in its own configuration, which may run after this one.
        SimulatorFamily.entries.firstOrNull { name.startsWith(it.taskPrefix) }?.let { family ->
            device.set(simulator.map { it.fetchDevice(family) })
        }
    }
    skipWatchosTestsWithoutRuntime()
}

/*
 * Why: CI runs no watchOS suite, and on a Mac without the watchOS runtime the suites, and the test
 * binaries only they run, would fail or slow `build` and `allTests` on every module. The iOS suites
 * keep failing without a device, as CI needs.
 */
private fun Project.skipWatchosTestsWithoutRuntime() {
    val hasRuntime = providers.of(SimulatorRuntimeValueSource::class.java) {
        parameters.runtimeName.set(SimulatorFamily.WATCHOS.runtimeName)
    }
    val reason = "a watchOS simulator runtime is installed (xcodebuild -downloadPlatform watchOS)"
    tasks.withType<KotlinNativeSimulatorTest>().configureEach {
        if (name.startsWith(SimulatorFamily.WATCHOS.taskPrefix)) onlyIf(reason) { hasRuntime.get() }
    }
    tasks.withType<KotlinNativeLink>().configureEach {
        if (name.endsWith("TestWatchosSimulatorArm64")) onlyIf(reason) { hasRuntime.get() }
    }
}
