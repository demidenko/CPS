import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val gradle_version = "8.3.1"
    id("com.android.application") version gradle_version apply false
    id("com.android.library") version gradle_version apply false

    val kotlin_version = "1.9.23"
    id("org.jetbrains.kotlin.android") version kotlin_version apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version apply false
    id("com.google.devtools.ksp") version "$kotlin_version-+" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

allprojects {
    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
                        "${project.rootDir.absolutePath}/compose_compiler_config.conf"
            )
        }
    }
}