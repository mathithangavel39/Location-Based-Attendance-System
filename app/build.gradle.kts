plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.college.locationattendance"
    compileSdk = 33 // 👈 Itha 33 ku maathiyachu

    defaultConfig {
        applicationId = "com.college.locationattendance"
        minSdk = 24
        targetSdk = 33 // 👈 Ithayum 33 ku maathiyachu
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // API 33-ku support aagura correct versions
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    // Google ML Kit Face Detection AI
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    dependencies {
        // ... previous libraries like Glide, Firebase Realtime DB, Auth Candidate, ...

        // 🚀 CANDIDATE ADD: Firebase Storage CANDIDATE ADD
        implementation("com.google.firebase:firebase-storage-ktx")
        implementation("com.google.firebase:firebase-auth-ktx") // Candidate auth is usually required
    }

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Chart & Biometric
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
}