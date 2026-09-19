plugins {
    alias(libs.plugins.acrovox.android.library)
    alias(libs.plugins.acrovox.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.acrovox.core.designsystem"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(libs.androidx.compose.material.icons)
    api(libs.coil.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
