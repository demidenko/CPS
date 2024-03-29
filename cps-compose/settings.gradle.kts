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
    versionCatalogs {
        //https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog-declaration
        create("libs") {
            library("kotlin-serialization", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            library("kotlin-datetime", "org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
            library("kotlin-coroutines", "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

            version("room", "2.6.0-beta01")
            library("room-ktx", "androidx.room", "room-ktx").versionRef("room")
            library("room-compiler", "androidx.room", "room-compiler").versionRef("room")
        }
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
