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
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(project(":feature:home"))
    implementation(project(":feature:inbox"))
    implementation(project(":feature:queue"))
    implementation(project(":feature:discover"))
    implementation(project(":feature:library"))
    implementation(project(":feature:podcast"))

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
