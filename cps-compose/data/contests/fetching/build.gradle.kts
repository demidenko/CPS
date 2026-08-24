plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.demich.cps.contests.fetching"
}

dependencies {
    implementation(project(":data:contests:database"))
    implementation(project(":kotlin-stdlib-boost"))
}