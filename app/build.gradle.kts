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
}
