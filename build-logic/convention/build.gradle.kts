plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    compileOnly(libs.android.gradleApiPlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "pulse.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "pulse.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "pulse.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "pulse.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("androidFeature") {
            id = "pulse.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
