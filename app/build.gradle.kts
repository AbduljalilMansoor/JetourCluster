plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.jetourcluster"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.jetourcluster"
        minSdk = 30
        // 33 on purpose (same as DisplayMirror): avoids Android 14's strict foreground-service-type checks
        targetSdk = 33
        versionCode = 1
        versionName = "0.1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
