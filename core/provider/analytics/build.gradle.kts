import org.gradle.api.tasks.PathSensitivity

plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            // Why: screen views come from its back stack (ScreenViewTracker).
            implementation(projects.core.navigation)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        // Why: RouteScreenViewTest checks it covers every kind of Route, which only reflection lists.
        jvmTest.dependencies {
            implementation(kotlin("reflect"))
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
        }
    }
}

// Why: AnalyticsCatalogTest reads the features' sources and the catalog, which aren't this module's inputs.
tasks.withType<Test>().configureEach {
    if (name == "jvmTest") {
        inputs
            .files(
                fileTree(rootProject.layout.projectDirectory.dir("feature")) {
                    include("**/src/*Main/**/*.kt")
                    exclude("**/build/**")
                },
            ).withPropertyName("featureSources")
            .withPathSensitivity(PathSensitivity.RELATIVE)
        inputs
            .file(rootProject.layout.projectDirectory.file("docs/analytics/README.md"))
            .withPropertyName("analyticsCatalog")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
