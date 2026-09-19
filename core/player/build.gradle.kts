plugins {
    alias(libs.plugins.acrovox.android.library)
    alias(libs.plugins.acrovox.hilt)
}

android {
    namespace = "com.acrovox.core.player"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:data"))
    api(project(":core:download"))
    implementation(project(":core:network"))
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
