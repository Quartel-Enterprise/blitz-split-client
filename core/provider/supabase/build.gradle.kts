import com.codingfeline.buildkonfig.compiler.FieldSpec

plugins {
    alias(libs.plugins.blitzsplit.kotlin.multiplatform)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.supabase.auth)
            api(libs.supabase.postgrest)
            api(libs.supabase.functions)
            implementation(libs.ktor.client.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        appleMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}

// The publishable key is public by design (it ships in every client) and RLS protects the data.
// A Gradle property or an environment variable of the same name points a build at another
// project, e.g. a local `supabase start`.
private val supabaseDefaults = mapOf(
    "SUPABASE_URL" to "https://cfgnuyijsncgpgywoldf.supabase.co",
    "SUPABASE_PUBLISHABLE_KEY" to "sb_publishable_OEqhD3YPNU5Gd7c45jjDRQ_o2kL0UE5",
    "SUPABASE_GOOGLE_WEB_CLIENT_ID" to "",
)

buildkonfig {
    packageName = "com.quare.blitzsplit.core.provider.supabase.generated"
    objectName = "SupabaseConfig"
    exposeObjectWithName = "SupabaseConfig"
    defaultConfigs {
        supabaseDefaults.forEach { (key, default) ->
            val value = providers.gradleProperty(key).orNull ?: System.getenv(key) ?: default
            buildConfigField(FieldSpec.Type.STRING, key, value)
        }
    }
}
