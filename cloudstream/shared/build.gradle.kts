plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    android {
        // Must be unique
        namespace = "com.lagradost.cloudstream4"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources {
            enable = true
        }
    }

    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        all {
            languageSettings {
                optIn("com.lagradost.cloudstream3.InternalAPI")
                optIn("com.lagradost.cloudstream3.Prerelease")
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.coil)
            implementation(libs.kotlinx.immutable.collections)
            implementation(project(":cloudstream:library"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation("androidx.preference:preference-ktx:1.2.1")
            implementation(libs.ktor.client.android)
        }

        jvmMain.dependencies {
            implementation(libs.ktor.client.java)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.lagradost.cloudstream4.generated.resources"
    generateResClass = auto
}