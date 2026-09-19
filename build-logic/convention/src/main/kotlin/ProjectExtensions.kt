import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

internal object AcroVoxSdk {
    const val COMPILE = 37
    const val TARGET = 36
    const val MIN = 26
}

/** Réglages communs aux modules application et bibliothèque. */
internal fun CommonExtension.configureAndroid() {
    compileSdk = AcroVoxSdk.COMPILE
    defaultConfig.minSdk = AcroVoxSdk.MIN
    compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    compileOptions.targetCompatibility = JavaVersion.VERSION_17
}
