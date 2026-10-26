plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

android {
    namespace = "com.demich.cps.features.codeforces.follow.database"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    room3 {
        schemaDirectory("$projectDir/schemas")
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
    implementation(project(":features:room_base"))
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:utils"))
    implementation(project(":data:accounts:userinfo"))
    implementation(project(":platforms:codeforces:follow:storage"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
}