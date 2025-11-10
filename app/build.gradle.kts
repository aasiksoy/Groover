plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")


}

android {
    namespace = "be.kuleuven.gt.myapplication2"
    compileSdk = 35

    defaultConfig {
        applicationId = "be.kuleuven.gt.myapplication2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.spotify.android:auth:1.2.5")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.yuyakaido:cardstackview:2.3.4")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.airbnb.android:lottie:6.4.1")
    implementation ("at.favre.lib:bcrypt:0.10.2")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    implementation ("com.github.bumptech.glide:glide:4.15.1")



}