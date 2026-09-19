pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "acrovox"

include(":app")
include(":core:model")
include(":core:database")
include(":core:network")
include(":core:designsystem")
include(":core:player")
include(":core:download")
include(":core:sync")
include(":feature:home")
include(":feature:inbox")
include(":feature:queue")
include(":feature:podcast")
include(":feature:player")
include(":feature:discover")
include(":feature:library")
include(":feature:downloads")
include(":feature:settings")
