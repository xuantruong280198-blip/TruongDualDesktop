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
        targetSdk = 35
        versionCode = 1
        versionName = "0.1-diagnostic"
    }
}
