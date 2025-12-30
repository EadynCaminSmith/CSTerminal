pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/horizontalsystems/bitcoin-kit-android/master/maven") }
        maven { url = uri("https://raw.githubusercontent.com/horizontalsystems/hd-wallet-kit-android/master/maven") }
    }
}


rootProject.name = "CS Terminal"
include(":app")
