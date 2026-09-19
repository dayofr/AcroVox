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
            id = "acrovox.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "acrovox.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "acrovox.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = "acrovox.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("androidFeature") {
            id = "acrovox.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
