plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.naze.motion.core.access"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:action"))
    api(project(":core:engine"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
