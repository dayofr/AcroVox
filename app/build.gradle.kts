plugins {
    alias(libs.plugins.acrovox.android.application)
    alias(libs.plugins.acrovox.android.compose)
    alias(libs.plugins.acrovox.hilt)
}

android {
    namespace = "com.acrovox.app"

    defaultConfig {
        applicationId = "com.acrovox.app"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(project(":core:data"))
    implementation(project(":core:player"))
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(project(":feature:home"))
    implementation(project(":feature:inbox"))
    implementation(project(":feature:queue"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:library"))
    implementation(project(":feature:podcast"))
    implementation(project(":feature:player"))
    implementation(project(":feature:downloads"))
    implementation(project(":core:download"))
    implementation(project(":core:sync"))
    implementation(project(":feature:settings"))

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlinx.coroutines.guava)
    androidTestImplementation(libs.androidx.media3.session)
}
