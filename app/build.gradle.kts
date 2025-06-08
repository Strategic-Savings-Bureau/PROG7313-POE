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
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    /*
     * Code Attribution
     *
     * Purpose:
     *   - Integrate Room for local database support with Kotlin coroutine extensions for asynchronous operations.
     *   - Add Supabase SDK for backend integration including authentication, storage, and data handling.
     *   - Use Glide for efficient image loading and caching.
     *   - Implement SwipeRefreshLayout for pull-to-refresh UI patterns.
     *   - Integrate MPAndroidChart for interactive charting and data visualization.
     *   - Add biometric authentication support for enhanced security.
     *   - Use Guava and AndroidX concurrent-futures for additional utilities and concurrency support.
     *
     * Authors/Technologies Used:
     *   - Room Database & Android Jetpack Components: Android Developers
     *   - Supabase SDK: Supabase Open Source Community
     *   - Glide Image Loading: Bumptech
     *   - SwipeRefreshLayout: Android Developers
     *   - MPAndroidChart: PhilJay (Open Source)
     *   - Biometric API: AndroidX Team
     *   - Guava Libraries: Google
     *
     * Date Accessed: 6 June 2025
     *
     * References:
     *   - Room Database: https://developer.android.com/jetpack/androidx/releases/room
     *   - Supabase Documentation: https://supabase.com/docs
     *   - Glide: https://github.com/bumptech/glide
     *   - SwipeRefreshLayout: https://developer.android.com/reference/androidx/swiperefreshlayout/widget/SwipeRefreshLayout
     *   - MPAndroidChart: https://github.com/PhilJay/MPAndroidChart
     *   - Android Biometric API: https://developer.android.com/training/sign-in/biometric-auth
     *   - Guava: https://github.com/google/guava
     */

    // Room dependencies
    val room_version = "2.6.1"
    implementation ("com.google.android.material:material:<version>")
    implementation("androidx.room:room-runtime:$room_version") // Core Room library for database operations
    ksp("androidx.room:room-compiler:$room_version") // Generates Room-related code using KSP (preferred for Kotlin)
    annotationProcessor("androidx.room:room-compiler:$room_version") // Alternative for Java projects (not needed with KSP)
    implementation("androidx.room:room-ktx:$room_version") // Kotlin extensions for Room (adds coroutines support)

    // Supabase dependencies
    val kotlin_version = "3.1.4"
    val ktor_version = "3.1.2"

    implementation(platform("io.github.jan-tennert.supabase:bom:$kotlin_version"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-android:$ktor_version")
    implementation("io.github.jan-tennert.supabase:storage-kt:$ktor_version")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // SwipeRefreshLayout for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // MPAndroidChart for charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // AndroidX concurrent futures Kotlin extensions
    implementation("androidx.concurrent:concurrent-futures-ktx:1.1.0")

    // Google Guava libraries
    implementation("com.google.guava:guava:33.2.1-android")

    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}