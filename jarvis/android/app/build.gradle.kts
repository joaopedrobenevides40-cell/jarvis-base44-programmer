plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jarvis.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jarvis.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Chaves de API ficam em BuildConfig (não no código)
            buildConfigField("String", "JARVIS_API_KEY", "\"${System.getenv("JARVIS_API_KEY") ?: ""}\"")
            buildConfigField("String", "JARVIS_BASE_URL", "\"${System.getenv("JARVIS_BASE_URL") ?: "https://jarvis-backend.fly.dev"}\"")
        }
        debug {
            buildConfigField("String", "JARVIS_API_KEY", "\"debug_key\"")
            buildConfigField("String", "JARVIS_BASE_URL", "\"http://10.0.2.2:8000\"") // emulador → localhost
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Room (banco local para memória)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // OkHttp (comunicação com backend)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Gson (JSON)
    implementation("com.google.code.gson:gson:2.10.1")

    // SpeechRecognizer (voz → texto)
    implementation("androidx.speech:speech:1.0.0-alpha03")

    // Accompanist (permissões Compose)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // DataStore (preferências)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
