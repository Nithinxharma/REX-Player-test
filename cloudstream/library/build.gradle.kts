plugins {
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.lagradost.cloudstream3.library"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm()

    sourceSets {
        all {
            languageSettings {
                optIn("com.lagradost.cloudstream3.InternalAPI")
                optIn("com.lagradost.cloudstream3.Prerelease")
                optIn("kotlin.uuid.ExperimentalUuidApi")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            }
        }

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.kotlinx.datetime)
            implementation(libs.nicehttp)
            implementation(libs.jsoup)
            implementation(libs.ksoup)
            implementation(libs.jackson.module.kotlin)
            implementation(libs.ktor.http)
            implementation(libs.ktor.client.core)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.annotation)
            implementation(libs.rhino)
            implementation(libs.newpipeextractor)
        }

        val jvmCommonMain = sourceSets.findByName("jvmCommonMain")
        if (jvmCommonMain != null) {
            androidMain.get().dependsOn(jvmCommonMain)
        }
    }
}
