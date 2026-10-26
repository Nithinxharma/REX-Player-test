// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose.compiler) apply false
  alias(libs.plugins.kotlinx.serialization) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.room) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.android.lint) apply false
  alias(libs.plugins.android.multiplatform.library) apply false
  alias(libs.plugins.buildkonfig) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.compose.multiplatform) apply false
}
