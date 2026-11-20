package com.quare.blitzsplit.buildlogic

/** The simulators whose suites the build hands a device: the runtime family `simctl` lists them under. */
enum class SimulatorFamily(
    val runtimeName: String,
    val taskPrefix: String,
) {
    IOS("iOS", "ios"),
    WATCHOS("watchOS", "watchos"),
}
