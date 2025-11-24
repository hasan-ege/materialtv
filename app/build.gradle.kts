plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

android {
    namespace = "com.example.materialtv"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file(rootProject.file(project.properties["MYAPP_RELEASE_STORE_FILE"] as String))
            storePassword = project.properties["MYAPP_RELEASE_STORE_PASSWORD"] as String
            keyAlias = project.properties["MYAPP_RELEASE_KEY_ALIAS"] as String
            keyPassword = project.properties["MYAPP_RELEASE_KEY_PASSWORD"] as String
        }
    }

    defaultConfig {
        applicationId = "com.example.materialtv"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // debug ayarları
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material:1.6.7")
    implementation("com.google.android.material:material:1.11.0") // Re-adding this dependency
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    implementation("androidx.compose.foundation:foundation")
    implementation("com.airbnb.android:lottie-compose:4.0.0")

    val media3_version = "1.3.1"

    // Media3 dependencies - Core
    implementation("androidx.media3:media3-exoplayer:$media3_version")
    implementation("androidx.media3:media3-ui:$media3_version")
    
    // Media3 format support - HLS, DASH, RTSP, SmoothStreaming
    implementation("androidx.media3:media3-exoplayer-hls:$media3_version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3_version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3_version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3_version")

    // Correct FFmpeg decoder dependency from Jellyfin
    implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.3.1+2")

    // WorkManager for background downloads
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // Room for persistence of downloads, playlists, watch history
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Material3 You theming (already using Material3, but add You library)
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // Retrofit & Kotlinx Serialization
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.android.gms:play-services-base:18.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
