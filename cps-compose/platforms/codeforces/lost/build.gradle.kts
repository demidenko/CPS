plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.demich.cps.platforms.codeforces.lost"
}

dependencies {
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:utils"))

    implementation(libs.kotlinx.serialization.core)
}