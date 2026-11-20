import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.target.Family

plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.blitzsplit.kotlin.multiplatform.watchos)
    alias(libs.plugins.buildkonfig)
}

// The modules SwiftUI and React talk to directly: their ViewModels, UiState, UiEvent and UiAction
// types are exported to the Apple framework (Swift sees them by name). Listed by path and turned into
// dependencies by the map at the end: at the top of a build script, project(":core:model") is the Project itself.
private val exportedToApple = listOf(
    ":core:model",
    ":core:presentation",
    ":core:navigation",
    ":core:expenses",
    ":core:groups",
    ":core:activity",
    ":core:contacts",
    ":core:legal",
    ":core:release_notes",
    ":core:support",
    ":core:provider:analytics",
    ":feature:welcome",
    ":feature:profile_setup",
    ":feature:restore",
    ":feature:recovery_code",
    ":feature:groups",
    ":feature:profile",
    ":feature:edit_profile",
    ":feature:payment_key",
    ":feature:paying_method",
    ":feature:delete_account",
    ":feature:contact_support",
    ":feature:notification_settings",
    ":feature:create_group",
    ":feature:invite",
    ":feature:members",
    ":feature:join",
    ":feature:group_detail",
    ":feature:bill_detail",
    ":feature:split_bill",
    ":feature:edit_group",
    ":feature:register_payment",
    ":feature:global_balances",
    ":feature:group_stats",
    ":feature:category_map",
    ":feature:activity",
    ":feature:contacts",
    ":feature:in_app_update",
    ":feature:legal",
    ":feature:release_notes",
    ":feature:app_download",
).map { dependencies.project(it) }

kotlin {
    // Why: the watchOS app's own `Shared`, kept out of the iOS and macOS ones so their API stays as it is. Xcode
    // builds it through embedAndSignAppleFrameworkForXcode like the others; assembleSharedDebugXCFramework (or
    // assembleSharedReleaseXCFramework) packs both watchOS slices.
    val watchosFramework = XCFramework("Shared")
    listOf(iosArm64(), iosSimulatorArm64(), macosArm64(), watchosArm64(), watchosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
            exportedToApple.forEach { export(it) }
            export(libs.quare.loadable)
            if (target.konanTarget.family == Family.WATCHOS) watchosFramework.add(this)
        }
    }

    js {
        outputModuleName = "blitzsplit-shared"
        binaries.library()
        generateTypeScriptDefinitions()
    }

    sourceSets {
        commonMain.dependencies {
            exportedToApple.forEach { api(it) }
            api(libs.quare.loadable)
            implementation(project(":core:utils"))
            implementation(project(":core:account"))
            implementation(project(":core:profile"))
            implementation(project(":core:groups"))
            implementation(project(":core:contacts"))
            implementation(project(":core:sync"))
            implementation(project(":core:settings"))
            implementation(project(":core:in_app_update"))
            implementation(project(":core:provider:room"))
            implementation(project(":core:provider:supabase"))
            implementation(project(":core:provider:image"))
            implementation(project(":core:provider:crashlytics"))
            implementation(libs.quare.connectivity)
            implementation(libs.kermit)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            // The store screenshots' seed (docs/store-listing-screenshots.md), which the Apple apps and the web run:
            // it writes the sample trip's feed and links, and points Supabase at a server that isn't there.
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.functions)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime)
        }
        // The store screenshots' Room of its own, a file apart from the developer's (StoreScreenshotsDatabase).
        appleMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(libs.koin.test)
        }
    }
}

/*
 * Why: each anonymous account a development task's installs sign in carries the task's branch (`TaskTag`), so
 * finish-task deletes the task's test accounts and the server expires a forgotten one after a day. A build that could
 * reach real people never carries it: CI (every pull request, beta and release), a release build (an Android release
 * task, a release framework, Xcode's Release configuration) and main. The Mac app has none either, since a task's
 * build shares its sandbox container, and so its account, with the App Store app on the same Mac. It lives in the
 * composition root, so a new branch rebuilds this module only, not the cached ones below it.
 */
private val isReleaseBuild: Boolean = providers.environmentVariable("CI").isPresent ||
    providers.environmentVariable("CONFIGURATION").orNull == "Release" ||
    gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

private val taskBranch: String = if (isReleaseBuild) {
    ""
} else {
    runCatching {
        providers
            .exec {
                commandLine("git", "branch", "--show-current")
                isIgnoreExitValue = true
            }.standardOutput.asText
            .get()
            .trim()
    }.getOrDefault("").takeUnless { it == "main" }.orEmpty()
}

buildkonfig {
    packageName = "com.quare.blitzsplit.shared.generated"
    objectName = "SharedConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "TASK_BRANCH", taskBranch)
    }
    targetConfigs {
        create("macosArm64") {
            buildConfigField(FieldSpec.Type.STRING, "TASK_BRANCH", "")
        }
    }
}
