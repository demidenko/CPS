plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.demich.cps.fetchstate"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}