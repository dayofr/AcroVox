plugins {
    alias(libs.plugins.acrovox.android.library)
    alias(libs.plugins.acrovox.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.acrovox.core.sync"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(project(":core:data"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.core.ktx)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver)
}
