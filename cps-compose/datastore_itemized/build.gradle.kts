plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = ("com.demich.datastore_itemized")

    defaultConfig {
        minSdk = 22

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
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines.android)
}