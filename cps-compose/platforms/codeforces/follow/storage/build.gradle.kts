plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.demich.cps.platforms.codeforces.follow.storage"
}

dependencies {
    implementation(project(":data:accounts:userinfo"))

    implementation(libs.kotlinx.coroutines.android)
}