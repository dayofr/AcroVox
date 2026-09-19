plugins {
    alias(libs.plugins.acrovox.android.library)
    alias(libs.plugins.acrovox.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.acrovox.core.database"

    testOptions.unitTests.isIncludeAndroidResources = true
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}
