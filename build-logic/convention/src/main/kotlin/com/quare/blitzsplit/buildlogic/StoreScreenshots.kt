package com.quare.blitzsplit.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import java.io.File

// The store listing's art (docs/store-listing-screenshots.md). Nothing is framed here any more: the generators of
// :android:store_screenshots are its unit tests, which draw the bare Compose screens of the sample trip under
// Robolectric (Google Play's art, -PplayArt), what the App Store's tickets write (-PcreativeAssets) and the README's
// Android phone (-PreadmeHero). scripts/store-screenshots/listing-art lays the screens out. Each runs alone, only when
// asked for, and `./gradlew build` and `test` draw none of them.
private const val GENERATE_TASK = "testDebugUnitTest"

fun Project.configureStoreScreenshots() {
    val facesDir = rootProject.file("scripts/store-screenshots/faces")
    val groupPhotosDir = rootProject.file("scripts/store-screenshots/bank/groups")

    fun findOutputDir(path: String): File = rootProject.layout.buildDirectory
        .dir(path)
        .get()
        .asFile

    val generatesReadmeHero = providers.gradleProperty("readmeHero").isPresent
    val generatesCreativeAssetFacts = providers.gradleProperty("creativeAssets").isPresent
    val generatesPlayArt = providers.gradleProperty("playArt").isPresent
    // Each draws a different test class alone, so asking for two would quietly draw only the first.
    check(listOf(generatesReadmeHero, generatesCreativeAssetFacts, generatesPlayArt).count { it } <= 1) {
        "-PreadmeHero, -PcreativeAssets and -PplayArt draw different tests: run them one at a time"
    }
    val generators = mapOf(
        "ReadmeHeroScreens" to ("readmeHero.outputDir" to findOutputDir("outputs/readme-hero-android")),
        "CreativeAssetFacts" to ("creativeAssets.outputDir" to findOutputDir("outputs/app-store-creative-asset-facts")),
        "PlayArtCaptures" to ("playArt.outputDir" to findOutputDir("outputs/play-art-captures")),
    )
    // `./gradlew build -PplayArt` or `test -PreadmeHero` must not draw inside an ordinary build: the module's debug tests
    // have to be asked for by name, as the scripts and the lane do.
    val modulePath = path
    val asksForTests = gradle.startParameter.taskNames.any { it == "$modulePath:$GENERATE_TASK" }
    val asked = when {
        generatesReadmeHero -> "ReadmeHeroScreens"
        generatesCreativeAssetFacts -> "CreativeAssetFacts"
        generatesPlayArt -> "PlayArtCaptures"
        else -> null
    }
    tasks.withType<Test>().configureEach {
        val isGenerator = name == GENERATE_TASK
        onlyIf("the build asked for one of the store art's generators") { isGenerator && asked != null && asksForTests }
        systemProperty("storeScreenshots.facesDir", facesDir.absolutePath)
        systemProperty("storeScreenshots.groupPhotosDir", groupPhotosDir.absolutePath)
        inputs.dir(facesDir)
        inputs.dir(groupPhotosDir)
        maxHeapSize = "4g"
        generators.forEach { (testClass, output) ->
            if (testClass == asked) {
                filter.includeTestsMatching("*.$testClass")
                systemProperty(output.first, output.second.absolutePath)
                outputs.dir(output.second)
            } else {
                filter.excludeTestsMatching("*.$testClass")
            }
        }
    }
}
