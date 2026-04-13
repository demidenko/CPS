import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.kotlin.jvm) apply false

    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.compose.compiler) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

private val javaVersion = JavaVersion.toVersion(21)

fun CommonExtension.baseAndroidConfig() {
    val apiLevel = 36
    compileSdk { version = release(version = apiLevel) }

    defaultConfig.apply {
        minSdk = 26
    }

    compileOptions.apply {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

fun Project.configureKotlin() {
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))

            freeCompilerArgs.add("-Xcontext-parameters")
            freeCompilerArgs.add("-Xcontext-sensitive-resolution")

            freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        }
    }
}

subprojects {
    pluginManager.withPlugin("com.android.application") {
        configure<ApplicationExtension> {
            baseAndroidConfig()
        }
        configureKotlin()
    }
    pluginManager.withPlugin("com.android.library") {
        configure<LibraryExtension> {
            baseAndroidConfig()
        }
        configureKotlin()
    }
}