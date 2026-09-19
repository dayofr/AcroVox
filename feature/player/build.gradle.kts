plugins {
    alias(libs.plugins.acrovox.android.feature)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.acrovox.feature.player"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:player"))

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
