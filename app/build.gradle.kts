plugins {
    id("com.android.application")
}

android {
    namespace = "org.donttreadonmysmartphone.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.donttreadonmysmartphone.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-dev"
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
