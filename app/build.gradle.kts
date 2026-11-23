plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.truong.dualdesktop"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.truong.dualdesktop"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2-beta"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}