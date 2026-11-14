plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.utils)
            api(libs.androidx.room.runtime)
            // Why: with the web target in commonMain, sqlite's blocking prepare and execSQL are only on the other
            // targets; the migration specs use sqlite-async's suspend ones, which Room's own dependency doesn't
            // put on this module's shared metadata compile.
            implementation(libs.androidx.sqlite.async)
            implementation(libs.kotlinx.coroutines.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }

        androidMain.dependencies {
            // Why: the framework driver uses the OS SQLite, so the app ships no native library.
            implementation(libs.androidx.sqlite.framework)
        }

        appleMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }

        jvmMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }

        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }

        jsMain.dependencies {
            implementation(libs.androidx.sqlite.web)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspMacosArm64", libs.androidx.room.compiler)
    add("kspJs", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
}
