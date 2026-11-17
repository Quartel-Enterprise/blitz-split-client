package com.quare.blitzsplit.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

// The store listing screenshots (docs/store-listing-screenshots.md). The generators of :android:store_screenshots are
// its unit tests, which write store-screenshots' Fastlane-shaped output into build/outputs/store-screenshots/<locale>/.
// These tasks copy it where people look at it (store_listings/, git-ignored) and where the fastlane lanes upload from.
// Play and Apple each fan Latin America's Spanish set out to their own regional codes; Spain has a set of its own.
// The devices map each generator output folder to store_listings/' and to the one the store's tool reads; the Mac's
// go apart, as the Mac app is a version of its own on App Store Connect (platform osx).
//
// The iPhone Duo is collected whole, closed and open, and staged one screen at a time. App Store Connect has one
// iPhone Duo device size for its two displays, outer (closed) and inner (open), and takes ten screenshots per device
// size; deliver files both under APP_IPHONE_DUO and skips the ones past ten. So each screen goes up once: open for the
// ones duoScreensStagedOpen names, by their file name (StoreScreen.fileName), closed for the others. Open are the
// screens that use the inner display's width (the group and Home in two columns, Statistics, Activity's feed beside
// the item picked); closed, the bill's sheets, which open are a sheet over the screen behind, Invite, and the light
// theme, so the group shows both ways. A display that wasn't captured gives its screens to the other, and the shelf
// still holds ten. To change what goes up open, edit that list alone: a test of scripts/release/store-listing.test.mjs
// holds its names to the screens of scripts/store-screenshots/screens.tsv, since a name no screen has would send that
// screen up closed with nobody told.
private const val GROUP = "store-screenshots"
private const val GENERATE_TASK = "testDebugUnitTest"
private const val DUO_CLOSED = "iphoneduo"
private const val DUO_OPEN = "iphoneduoopen"
private val listingLocales = listOf(
    "en-US",
    "pt-BR",
    "pt-PT",
    "es",
    "es-ES",
    "fr",
    "de",
    "it",
    "pl",
    "nl",
    "tr",
    "id",
    "ru",
    "uk",
    "ko",
    "hi",
    "ar",
    "zh",
    "he",
    "ja",
)
private val playLocales = mapOf(
    "en-US" to listOf("en-US"),
    "pt-BR" to listOf("pt-BR"),
    "pt-PT" to listOf("pt-PT"),
    "es" to listOf("es-419", "es-US"),
    "es-ES" to listOf("es-ES"),
    "fr" to listOf("fr-FR"),
    "de" to listOf("de-DE"),
    "it" to listOf("it-IT"),
    "pl" to listOf("pl-PL"),
    "nl" to listOf("nl-NL"),
    "tr" to listOf("tr-TR"),
    "id" to listOf("id"),
    "ru" to listOf("ru-RU"),
    "uk" to listOf("uk"),
    "ko" to listOf("ko-KR"),
    "hi" to listOf("hi-IN"),
    "ar" to listOf("ar"),
    "zh" to listOf("zh-CN"),
    "he" to listOf("iw-IL"),
    "ja" to listOf("ja-JP"),
)
private val appStoreLocales = mapOf(
    "en-US" to listOf("en-US"),
    "pt-BR" to listOf("pt-BR"),
    "pt-PT" to listOf("pt-PT"),
    "es" to listOf("es-MX"),
    "es-ES" to listOf("es-ES"),
    "fr" to listOf("fr-FR"),
    "de" to listOf("de-DE"),
    "it" to listOf("it"),
    "pl" to listOf("pl"),
    "nl" to listOf("nl-NL"),
    "tr" to listOf("tr"),
    "id" to listOf("id"),
    "ru" to listOf("ru"),
    "uk" to listOf("uk"),
    "ko" to listOf("ko"),
    "hi" to listOf("hi"),
    "ar" to listOf("ar-SA"),
    "zh" to listOf("zh-Hans"),
    "he" to listOf("he"),
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
    DUO_CLOSED to "iphone_duo",
    DUO_OPEN to "iphone_duo_open",
)
private val duoScreensStagedOpen = setOf("01_group", "04_balances", "06_statistics", "07_home", "09_activity")
private val macDevices = mapOf("mac" to "mac")
private val appleFrameClasses = listOf("*.IPhone*", "*.IPad*", "*.Mac*")

private fun copyGenerated(
    generated: File,
    locales: Map<String, List<String>>,
    devices: Map<String, String>,
    destinationOf: (storeLocale: String, device: String) -> File,
    nameOf: (png: File, device: String) -> String,
    copies: (png: File, sourcePath: String) -> Boolean = { _, _ -> true },
): Int {
    var copied = 0
    locales.forEach { (locale, storeLocales) ->
        devices.forEach { (sourcePath, device) ->
            val pngs = generated
                .resolve("$locale/$sourcePath")
                .listFiles { file -> file.extension == "png" && copies(file, sourcePath) }
                .orEmpty()
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

// Whether a generated image goes on its App Store shelf. Every image does but the iPhone Duo's, which shares its shelf
// between two displays: a screen goes up from the display duoScreensStagedOpen gives it, or from the other one when
// that display's image of it isn't there.
private fun isStagedForAppStore(
    png: File,
    sourcePath: String,
): Boolean {
    val otherDisplay = when (sourcePath) {
        DUO_CLOSED -> DUO_OPEN
        DUO_OPEN -> DUO_CLOSED
        else -> return true
    }
    val isItsDisplay = (png.nameWithoutExtension in duoScreensStagedOpen) == (sourcePath == DUO_OPEN)
    return isItsDisplay || !png.parentFile
        .resolveSibling(otherDisplay)
        .resolve(png.name)
        .isFile
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
    val groupPhotosDir = rootProject.file("scripts/store-screenshots/bank/groups")
    // Decided from the tasks the build was asked for, as in Bible Planner: only a build that produces the App Store
    // shots runs the iPhone and iPad classes, which frame the iOS captures (scripts/store-screenshots/capture-ios.sh).
    // Any other build, the Play listing's included, leaves them out and needs no Mac.
    val generatesAppStoreScreenshots = gradle.startParameter.taskNames.any { requested ->
        val name = requested.substringAfterLast(':')
        name.endsWith("AppStoreScreenshots") || name == "collectStoreScreenshots" || name == "stageStoreScreenshots"
    }

    // `./gradlew build` and `test` would draw every image twice (debug and release): the generators run only when a
    // build asks for the screenshots, or for this module's debug tests by name.
    val modulePath = path
    val generatesScreenshots = gradle.startParameter.taskNames.any { requested ->
        requested.endsWith("StoreScreenshots") || requested == "$modulePath:$GENERATE_TASK"
    }
    val generatesReadmeHero = providers.gradleProperty("readmeHero").isPresent
    val readmeHeroDir = rootProject.layout.buildDirectory
        .dir("outputs/readme-hero-android")
        .get()
        .asFile
    tasks.withType<Test>().configureEach {
        val isGenerator = name == GENERATE_TASK
        onlyIf("the build asked for the store screenshots") { isGenerator && generatesScreenshots }
        systemProperty("storeScreenshots.outputRoot", generatedDir.absolutePath)
        systemProperty("appleScreenshots.capturesDir", capturesDir.absolutePath)
        systemProperty("storeScreenshots.facesDir", facesDir.absolutePath)
        systemProperty("storeScreenshots.groupPhotosDir", groupPhotosDir.absolutePath)
        systemProperty("roborazzi.test.record", "true")
        inputs.dir(facesDir)
        inputs.dir(groupPhotosDir)
        outputs.dir(generatedDir)
        maxHeapSize = "4g"
        // The README's Android phone (scripts/readme-hero) is drawn alone, and only when asked for.
        if (generatesReadmeHero) {
            filter.includeTestsMatching("*.ReadmeHeroScreens")
            systemProperty("readmeHero.outputDir", readmeHeroDir.absolutePath)
            outputs.dir(readmeHeroDir)
            return@configureEach
        }
        filter.excludeTestsMatching("*.ReadmeHeroScreens")
        if (generatesAppStoreScreenshots) {
            inputs.dir(capturesDir)
            doFirst {
                val captured = listOf("iphone63", "iphone67", "ipad13").all { slot ->
                    listingLocales.all { capturesDir.resolve("$slot/$it").isDirectory }
                }
                check(captured) {
                    "No iOS captures of every language in $capturesDir: " +
                        "run scripts/store-screenshots/capture-ios.sh first"
                }
            }
        } else {
            appleFrameClasses.forEach(filter::excludeTestsMatching)
        }
        // Wiped first so a renamed or dropped screen can't leave a stale image to collect.
        doFirst { generatedDir.deleteRecursively() }
    }

    listOf(
        "android" to playDevices,
        "ios" to appleDevices + macDevices,
    ).forEach { (platform, devices) ->
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
                copies = ::isStagedForAppStore,
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
