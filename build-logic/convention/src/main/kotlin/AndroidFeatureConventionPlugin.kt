import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Module d'écran : bibliothèque Android + Compose + Hilt + ViewModel. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("pulse.android.library")
        pluginManager.apply("pulse.android.compose")
        pluginManager.apply("pulse.hilt")
        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:model"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("hilt-navigation-compose"))
        }
    }
}
