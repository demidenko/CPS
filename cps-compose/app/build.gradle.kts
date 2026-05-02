import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

configure<ApplicationExtension> {
    namespace = "com.demich.cps"

    defaultConfig {
        applicationId = "com.demich.cps"

        versionCode = 494
        versionName = "1.9.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

androidComponents {
    onVariants { variant ->
        val appName = "cps"
        variant.outputs.filterIsInstance<com.android.build.api.variant.impl.VariantOutputImpl>()
            .forEach { output ->
                val name = "$appName-${output.versionCode.get()}-${variant.buildType}.apk"
                output.outputFileName.set(name)
            }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(project.layout.projectDirectory.file("compose_compiler_config.conf"))
}

dependencies {
    implementation(project(":kotlin-stdlib-boost"))
    implementation(project(":datastore_itemized"))
    implementation(project(":platforms"))
    implementation(project(":data:platforms:api"))
    implementation(project(":data:platforms:clients"))
    implementation(project(":data:platforms:utils"))
    implementation(project(":data:accounts:userinfo"))
    implementation(project(":data:contests:database"))
    implementation(project(":data:contests:fetching"))
    implementation(project(":features:codeforces_lost:database"))
    implementation(project(":platforms:codeforces:lost"))
    implementation(project(":features:codeforces_follow:database"))
    implementation(project(":features:contests_loading_engine"))

    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.resaca)

    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
}