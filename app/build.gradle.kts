plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.appmobilemanagementtimes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.appmobilemanagementtimes"
        minSdk = 28
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    aaptOptions {
            noCompress( "mp3", "ogg")
        }

}


dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Sử dụng Firebase BoM để đồng bộ phiên bản
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Phiên bản mới nhất tính đến 4/2025
    implementation("com.google.firebase:firebase-auth") // Đã có trong BoM
    implementation("com.google.firebase:firebase-firestore") // Đã có trong BoM

    implementation("com.google.code.gson:gson:2.11.0") // Cập nhật phiên bản mới nhất
}