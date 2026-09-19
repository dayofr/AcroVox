plugins {
    alias(libs.plugins.pulse.android.application)
    alias(libs.plugins.pulse.android.compose)
    alias(libs.plugins.pulse.hilt)
}

android {
    namespace = "com.pulseaudio.podcast"

    defaultConfig {
        applicationId = "com.pulseaudio.podcast"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
}
