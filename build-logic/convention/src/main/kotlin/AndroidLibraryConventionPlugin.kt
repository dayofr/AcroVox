import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        configureUnitTests()
        extensions.configure<LibraryExtension> {
            configureAndroid()
            testOptions.targetSdk = AcroVoxSdk.TARGET
            lint.targetSdk = AcroVoxSdk.TARGET
        }
        dependencies {
            add("testImplementation", libs.library("junit"))
        }
    }
}
