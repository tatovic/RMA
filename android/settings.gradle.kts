pluginManagement {
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
        // MPAndroidChart se distribuira jedino preko JitPack-a, vidi tech.md sekcija 2.2
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "HomeInventory"
include(":app")
