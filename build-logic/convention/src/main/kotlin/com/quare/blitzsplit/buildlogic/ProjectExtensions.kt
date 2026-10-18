package com.quare.blitzsplit.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * `:core:provider:room` becomes `com.quare.blitzsplit.core.provider.room`, so no module repeats
 * its own path as a namespace. Underscores are dropped (`:feature:profile_setup` becomes
 * `...feature.profilesetup`) so the namespace, and with it the generated `R`, matches the package.
 */
fun Project.createNamespace(): String =
    "com.quare.blitzsplit" + path.replace(':', '.').replace("_", "").replace("-", "")

fun Project.getAndroidSdkVersions(): AndroidSdkVersions {
    fun readVersion(name: String): Int = libs
        .findVersion(name)
        .get()
        .requiredVersion
        .toInt()
    return AndroidSdkVersions(
        compileSdk = readVersion("android-compileSdk"),
        minSdk = readVersion("android-minSdk"),
        targetSdk = readVersion("android-targetSdk"),
    )
}
