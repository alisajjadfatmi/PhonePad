plugins {
    id("com.android.application")
}

android {
    namespace = "com.alisajjadfatmi.phonepad"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alisajjadfatmi.phonepad"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = "0.2.1-alpha"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
