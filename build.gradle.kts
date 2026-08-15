// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version libs.versions.agp apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin apply false
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin apply false
    alias(libs.plugins.hilt) apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
}
