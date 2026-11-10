package com.quare.blitzsplit.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

// The store listing screenshots (docs/store-listing-screenshots.md). The generators of :android:store_screenshots are
// its unit tests, which write store-screenshots' Fastlane-shaped output into build/outputs/store-screenshots/<locale>/.
// These tasks copy it where people look at it (store_listings/, git-ignored) and where the fastlane lanes upload from.
// Play and Apple each fan the one Spanish set out to their own regional codes. The devices map each generator output
// folder to store_listings/' and to the one the store's tool reads; the Mac's go apart, as the Mac app is a version of
// its own on App Store Connect (platform osx).
private const val GROUP = "store-screenshots"
private const val GENERATE_TASK = "testDebugUnitTest"
private val listingLocales = listOf("en-US", "pt-BR", "es", "fr", "de", "it", "pl", "nl", "tr", "id", "ja")
private val playLocales = mapOf(
    "en-US" to listOf("en-US"),
    "pt-BR" to listOf("pt-BR"),
    "es" to listOf("es-419", "es-ES", "es-US"),
    "fr" to listOf("fr-FR"),
    "de" to listOf("de-DE"),
    "it" to listOf("it-IT"),
    "pl" to listOf("pl-PL"),
    "nl" to listOf("nl-NL"),
    "tr" to listOf("tr-TR"),
    "id" to listOf("id"),
    "ja" to listOf("ja-JP"),
)
private val appStoreLocales = mapOf(
    "en-US" to listOf("en-US"),
    "pt-BR" to listOf("pt-BR"),
    "es" to listOf("es-MX", "es-ES"),
    "fr" to listOf("fr-FR"),
    "de" to listOf("de-DE"),
    "it" to listOf("it"),
    "pl" to listOf("pl"),
    "nl" to listOf("nl-NL"),
    "tr" to listOf("tr"),
    "id" to listOf("id"),
    "ja" to listOf("ja"),
)
private val playDevices = mapOf(
    "images/phoneScreenshots" to "phone",
    "images/sevenInchScreenshots" to "tablet_7",
    "images/tenInchScreenshots" to "tablet_10",
)
private val playImageFolders = playDevices.keys.associateWith { it.substringAfter("images/") }
private val appleDevices = mapOf(
    "iphone63" to "iphone_6_3",
    "iphone67" to "iphone_6_7",
    "ipad13" to "ipad_13",
    "ipad11" to "ipad_11",
    "iphoneduo" to "iphone_duo",
)
private val macDevices = mapOf("mac" to "mac")
private val appleFrameClasses = listOf("*.IPhone*", "*.IPad*")

private fun copyGenerated(
    generated: File,
    locales: Map<String, List<String>>,
    devices: Map<String, String>,
    destinationOf: (storeLocale: String, device: String) -> File,
    nameOf: (png: File, device: String) -> String,
): Int {
    var copied = 0
    locales.forEach { (locale, storeLocales) ->
        devices.forEach { (sourcePath, device) ->
            val pngs = generated.resolve("$locale/$sourcePath").listFiles { file -> file.extension == "png" }.orEmpty()
            storeLocales.forEach { storeLocale ->
                val destination = destinationOf(storeLocale, device)
                destination.mkdirs()
                pngs.sorted().forEach { png ->
                    png.copyTo(destination.resolve(nameOf(png, device)), overwrite = true)
                    copied++
                }
            }
        }
    }
    return copied
}

fun Project.configureStoreScreenshots() {
    val generatedDir = rootProject.layout.buildDirectory
        .dir("outputs/store-screenshots")
        .get()
        .asFile
    val capturesDir = rootProject.layout.buildDirectory
        .dir("outputs/apple-screenshot-captures")
        .get()
        .asFile
    val facesDir = rootProject.file("scripts/store-screenshots/faces")
    // Decided from the tasks the build was asked for, as in Bible Planner: only a build that produces the App Store
    // shots runs the iPhone and iPad classes, which frame the iOS captures (scripts/store-screenshots/capture-ios.sh).
    // Any other build, the Play listing's included, leaves them out and needs no Mac.
    val generatesAppStoreScreenshots = gradle.startParameter.taskNames.any { requested ->
        val name = requested.substringAfterLast(':')
        name.endsWith("AppStoreScreenshots") || name == "collectStoreScreenshots" || name == "stageStoreScreenshots"
    }

    tasks.withType<Test>().configureEach {
        systemProperty("storeScreenshots.outputRoot", generatedDir.absolutePath)
        systemProperty("appleScreenshots.capturesDir", capturesDir.absolutePath)
        systemProperty("storeScreenshots.facesDir", facesDir.absolutePath)
        systemProperty("roborazzi.test.record", "true")
        inputs.dir(facesDir)
        outputs.dir(generatedDir)
        maxHeapSize = "4g"
        if (generatesAppStoreScreenshots) {
            inputs.dir(capturesDir)
            doFirst {
                check(listOf("iphone63", "iphone67", "ipad13").all { capturesDir.resolve(it).isDirectory }) {
                    "No iOS captures in $capturesDir: run scripts/store-screenshots/capture-ios.sh first"
                }
            }
        } else {
            appleFrameClasses.forEach(filter::excludeTestsMatching)
        }
        // Wiped first so a renamed or dropped screen can't leave a stale image to collect.
        doFirst { generatedDir.deleteRecursively() }
    }

    listOf("android" to playDevices, "ios" to appleDevices + macDevices).forEach { (platform, devices) ->
        val storeName = if (platform == "android") "PlayStore" else "AppStore"
        val listings = rootProject.file("store_listings/$platform")
        tasks.register("collect${storeName}Screenshots") {
            group = GROUP
            description = "Generates the $platform store screenshots into store_listings/$platform/<locale>/<device>/."
            dependsOn(GENERATE_TASK)
            doLast {
                listings.deleteRecursively()
                val copied = copyGenerated(
                    generated = generatedDir,
                    locales = listingLocales.associateWith { listOf(it) },
                    devices = devices,
                    destinationOf = { locale, device -> listings.resolve("$locale/$device") },
                    nameOf = { png, _ -> png.name },
                )
                logger.lifecycle("collected $copied screenshot(s) into store_listings/$platform/")
            }
        }
    }
    tasks.register("collectStoreScreenshots") {
        group = GROUP
        description = "Generates every store screenshot into store_listings/. Needs the iOS captures."
        dependsOn("collectPlayStoreScreenshots", "collectAppStoreScreenshots")
    }

    val playMetadata = rootProject.file("fastlane/metadata/android")
    tasks.register("stagePlayStoreScreenshots") {
        group = GROUP
        description =
            "Generates the Google Play screenshots into fastlane/metadata/android/, where supply uploads from."
        dependsOn(GENERATE_TASK)
        doLast {
            playLocales.values.flatten().forEach { playMetadata.resolve("$it/images").deleteRecursively() }
            val staged = copyGenerated(
                generated = generatedDir,
                locales = playLocales,
                devices = playImageFolders,
                destinationOf = { locale, folder -> playMetadata.resolve("$locale/images/$folder") },
                nameOf = { png, _ -> png.name },
            )
            logger.lifecycle("stagePlayStoreScreenshots: staged $staged screenshot(s) for fastlane")
        }
    }

    // deliver reads fastlane/screenshots/<locale>/*.png flat and tells the device by the image's size, so the suffix
    // only keeps one screen's iPhone and iPad shots apart; the prefix keeps the shelf order. The Mac's go apart.
    val appleScreenshots = rootProject.file("fastlane/screenshots")
    val macScreenshots = rootProject.file("fastlane/screenshots-mac")
    tasks.register("stageAppStoreScreenshots") {
        group = GROUP
        description = "Frames the iOS and Mac captures into fastlane/screenshots(-mac)/, where deliver uploads from."
        dependsOn(GENERATE_TASK)
        doLast {
            appleScreenshots.deleteRecursively()
            macScreenshots.deleteRecursively()
            val nameOf = { png: File, device: String -> "${png.nameWithoutExtension}_$device.png" }
            val staged = copyGenerated(
                generated = generatedDir,
                locales = appStoreLocales,
                devices = appleDevices,
                destinationOf = { locale, _ -> appleScreenshots.resolve(locale) },
                nameOf = nameOf,
            ) + copyGenerated(
                generated = generatedDir,
                locales = appStoreLocales,
                devices = macDevices,
                destinationOf = { locale, _ -> macScreenshots.resolve(locale) },
                nameOf = nameOf,
            )
            logger.lifecycle("stageAppStoreScreenshots: staged $staged screenshot(s) for fastlane")
        }
    }
    tasks.register("stageStoreScreenshots") {
        group = GROUP
        description = "Stages both stores' screenshots for fastlane. Needs the iOS captures."
        dependsOn("stagePlayStoreScreenshots", "stageAppStoreScreenshots")
    }
}
