plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.demich.cps.contests.loading"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:clients"))
    implementation(project(":data:platforms:utils"))
    implementation(project(":data:contests:database"))

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)
}