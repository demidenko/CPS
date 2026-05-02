plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.demich.cps.platforms.codeforces.lost"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:utils"))

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
}