import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.tasks.asJavaVersion
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

private val javaVersion = JavaLanguageVersion.of(21).asJavaVersion()

fun BaseExtension.baseAndroidConfig() {
    val apiLevel = 36
    compileSdkVersion(apiLevel = apiLevel)

    defaultConfig.apply {
        minSdk = 26
        targetSdk = apiLevel
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

            optIn.add("kotlin.time.ExperimentalTime")

            freeCompilerArgs.add("-Xcontext-parameters")
            freeCompilerArgs.add("-Xcontext-sensitive-resolution")
        }
    }
}

subprojects {
    pluginManager.withPlugin("com.android.application") {
        configure<AppExtension> {
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