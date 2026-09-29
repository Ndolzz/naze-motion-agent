pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "naze-motion-agent"

include(":core:domain")
include(":core:state")
include(":core:action")
include(":core:engine")
include(":core:adapter")
include(":core:ai")
include(":core:agent")
include(":core:access")
include(":app")
