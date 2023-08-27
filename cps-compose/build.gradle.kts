// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val gradle_version = "8.1.1"
    id("com.android.application") version gradle_version apply false
    id("com.android.library") version gradle_version apply false
    val kotlin_version = "1.9.0"
    id("org.jetbrains.kotlin.android") version kotlin_version apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlin_version apply false
    id("com.google.devtools.ksp") version "$kotlin_version-+" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}