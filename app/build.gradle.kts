plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)

    // Add Supabase to App
    val kotlin_version = "2.1.20"
    kotlin("plugin.serialization") version "$kotlin_version"

    // import kotlin symbol processor (KSP)
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.ssba.strategic_savings_budget_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssba.strategic_savings_budget_app"
        minSdk = 31
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    /*
        * Code Attribution
        * Purpose:
        *   - Adding Room database support to the app for local storage and database operations
        *   - Adding Supabase for backend integration with features like storage, authentication, and data handling
        *   - Using Picasso for image loading and caching
        *   - Implementing SwipeRefreshLayout for adding pull-to-refresh functionality to views
        * Author: Various Open Source Communities
        * Sources:
        *   - Room Database: https://developer.android.com/jetpack/androidx/releases/room
        *   - Supabase: https://supabase.com/docs
        *   - Picasso: https://square.github.io/picasso/
        *   - SwipeRefreshLayout: https://developer.android.com/reference/androidx/swiperefreshlayout/widget/SwipeRefreshLayout
    */

    // Add Room to App
    val room_version = "2.6.1"
    implementation ("com.google.android.material:material:<version>")
    implementation("androidx.room:room-runtime:$room_version") // Core Room library for database operations
    ksp("androidx.room:room-compiler:$room_version") // Generates Room-related code using KSP (preferred for Kotlin)
    annotationProcessor("androidx.room:room-compiler:$room_version") // Alternative for Java projects (not needed with KSP)
    implementation("androidx.room:room-ktx:$room_version") // Kotlin extensions for Room (adds coroutines support)

    // Add Supabase to App
    val kotlin_version = "3.1.4"
    val ktor_version = "3.1.2"

    implementation(platform("io.github.jan-tennert.supabase:bom:$kotlin_version"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:$ktor_version")
    implementation("io.github.jan-tennert.supabase:storage-kt:$ktor_version")

    // Add Glide to App (image loading library)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Add SwipeRefreshLayout to App
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}