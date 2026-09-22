plugins {
    alias(libs.plugins.acrovox.android.application)
    alias(libs.plugins.acrovox.android.compose)
    alias(libs.plugins.acrovox.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

/** Version et signature fournies par l'intégration continue ; valeurs locales sinon. */
private val releaseVersion: String = System.getenv("ACROVOX_VERSION")?.removePrefix("v") ?: "0.1.0"
private val releaseVersionCode: Int = System.getenv("ACROVOX_VERSION_CODE")?.toIntOrNull() ?: 1
private val releaseKeystore: String? = System.getenv("ACROVOX_KEYSTORE")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.acrovox.app"

    defaultConfig {
        applicationId = "com.acrovox.app"
        versionCode = releaseVersionCode
        versionName = releaseVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    signingConfigs {
        releaseKeystore?.let { path ->
            create("release") {
                storeFile = file(path)
                storePassword = System.getenv("ACROVOX_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ACROVOX_KEY_ALIAS")
                keyPassword = System.getenv("ACROVOX_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
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
    implementation(libs.androidx.lifecycle.runtime.compose)
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
    baselineProfile(project(":baselineprofile"))
}
