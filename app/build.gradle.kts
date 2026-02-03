plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.practicum.playlistmaker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.practicum.playlistmaker"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "SERVER_BASE_URL", "\"https://itunes.apple.com/\"")
            signingConfig = signingConfigs.getByName("debug")
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("String", "SERVER_BASE_URL", "\"https://itunes.apple.com/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.camera.core)
    dependencies {
        kapt(libs.moxy.compiler)
        kapt(libs.room.compiler)

        implementation(libs.kotlinx.collections.immutable)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.okhttp)
        implementation(libs.koin.androidx.compose)
        implementation(libs.navigation.compose)
        implementation(libs.ui.compose)
        implementation(libs.material.compose)
        implementation(libs.activity.compose)
        implementation(platform(libs.firebase.bom))
        implementation(libs.peko)
        implementation(libs.room.runtime)
        implementation(libs.room.ktx)
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.kotlinx.coroutines.android)
        implementation(libs.navigation.fragment)
        implementation(libs.navigation.ui)
        implementation(libs.androidx.fragment.ktx)
        implementation(libs.moxy)
        implementation(libs.moxy.android)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.viewpager2)
        implementation(libs.androidx.viewbinding)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.material)
        implementation(libs.retrofit)
        implementation(libs.retrofit.gson)
        implementation(libs.gson)
        implementation(libs.glide)
        implementation(libs.koin.android)

        debugImplementation(libs.androidx.ui.tooling)

        annotationProcessor(libs.glide.compiler)

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
    }
}
