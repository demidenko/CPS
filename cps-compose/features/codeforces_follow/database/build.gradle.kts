plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.demich.cps.features.codeforces.follow.database"
    compileSdk = 33

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.generateKotlin", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        freeCompilerArgs += "-Xstring-concat=inline"
    }
}

dependencies {
    implementation(project(":features:room_base"))
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:utils"))
    implementation(project(":data:accounts:userinfo"))

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}