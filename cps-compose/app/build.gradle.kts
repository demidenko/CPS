plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.demich.cps"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.demich.cps"
        minSdk = 26
        targetSdk = 34

        versionCode = 367
        versionName = "1.9.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":datastore_itemized"))
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:utils"))
    implementation(project(":data:accounts:userinfo"))
    implementation(project(":data:contests:database"))
    implementation(project(":data:contests:loading"))
    implementation(project(":features:codeforces_lost:database"))
    implementation(project(":features:codeforces_follow:database"))
    implementation(project(":features:contests_loading_engine"))

    implementation(libs.kotlin.serialization)
    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.datetime)

    implementation("androidx.core:core-ktx:1.12.0")

    implementation(platform("androidx.compose:compose-bom:2023.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.navigation:navigation-compose:2.6.0")

    val lifecycle_version = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version")

    implementation("androidx.work:work-runtime-ktx:2.8.1")

    val accompanist_version = "0.30.0"
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist_version")
    implementation("com.google.accompanist:accompanist-permissions:$accompanist_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}