plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.demich.cps.contests.loading_engine"

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
    implementation(project(":data:contests:database"))
    implementation(project(":data:contests:loading"))
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:utils"))
    implementation(project(":kotlin-stdlib-boost"))

    implementation(libs.kotlinx.coroutines.android)
}