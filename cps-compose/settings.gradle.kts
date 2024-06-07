pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CPS"
include(":app")
include(":datastore_itemized")
include(":data:platforms:api")
include(":data:platforms:utils")
include(":data:accounts:userinfo")
include(":data:contests:database")
include(":data:contests:loading")
include(":features:room_base")
include(":features:codeforces_lost:database")
include(":features:codeforces_follow:database")
include(":features:contests_loading_engine")
