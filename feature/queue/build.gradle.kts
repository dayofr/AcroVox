plugins {
    alias(libs.plugins.acrovox.android.feature)
}

android {
    namespace = "com.acrovox.feature.queue"
}

dependencies {
    implementation(project(":core:player"))
    implementation(libs.reorderable)
}
