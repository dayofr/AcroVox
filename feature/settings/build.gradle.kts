plugins {
    alias(libs.plugins.acrovox.android.feature)
}

android {
    namespace = "com.acrovox.feature.settings"
}

dependencies {
    implementation(project(":core:sync"))
}
