plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ng.name.servix.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ng.name.servix.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "0.6.2"
        buildConfigField("String", "API_BASE_URL", "\"https://api.servix.name.ng/api/v1/\"")
        buildConfigField("String", "WEB_BASE_URL", "\"https://www.servix.name.ng\"")
    }

    // Stable test signing: the CI restores one keystore from repository secrets so every APK
    // carries the same certificate and in-app updates keep working. Without the secrets we fall
    // back to the throwaway debug key (updates over an older install will then need an uninstall).
    val testKeyStorePath = System.getenv("SERVIX_TEST_KEYSTORE")
    val testKeyStorePassword = System.getenv("SERVIX_TEST_KEYSTORE_PASSWORD")
    val hasTestKeyStore = !testKeyStorePath.isNullOrBlank() && !testKeyStorePassword.isNullOrBlank()

    signingConfigs {
        if (hasTestKeyStore) {
            create("testRelease") {
                storeFile = file(testKeyStorePath!!)
                storePassword = testKeyStorePassword
                keyAlias = "servix-test"
                keyPassword = testKeyStorePassword
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "SELF_UPDATE_CHANNEL", "\"github\"")
            if (hasTestKeyStore) {
                signingConfig = signingConfigs.getByName("testRelease")
            }
        }
        release {
            buildConfigField("String", "SELF_UPDATE_CHANNEL", "\"none\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
