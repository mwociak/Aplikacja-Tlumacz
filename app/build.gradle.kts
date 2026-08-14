import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.hilt.android)
}

android {
  namespace = "com.mcodeproject"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.mcodeproject"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "1.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // --- KONFIGURACJA PODPISU RELEASE ---
  signingConfigs {
    create("release") {
      val keystoreFile = file("${rootDir}/my-upload-key.jks")
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("KEYSTORE_PASSWORD") 
          ?: findProperty("KEYSTORE_PASSWORD") as? String 
          ?: ""
        keyAlias = System.getenv("KEY_ALIAS") 
          ?: findProperty("KEY_ALIAS") as? String 
          ?: ""
        keyPassword = System.getenv("KEY_PASSWORD") 
          ?: findProperty("KEY_PASSWORD") as? String 
          ?: ""
      }
    }
  }

  buildTypes {
    release {
      // OPTIMALIZACJA APK
      isMinifyEnabled = true           // włącza R8 (obfuscacja)
      isShrinkResources = true         // usuwa nieużywane zasoby

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )

      if (file("${rootDir}/my-upload-key.jks").exists()) {
        signingConfig = signingConfigs.getByName("release")
      }
    }

    debug {
      // domyślny debug.keystore z C:\Users\mwoci\.android\debug.keystore
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions { unitTests { isIncludeAndroidResources = true } }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
  }
}


secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.com.mcodeproject"
}


dependencies {
  implementation(platform(libs.androidx.compose.bom))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)

  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation("androidx.datastore:datastore-preferences:1.1.1")

  implementation(libs.converter.moshi)
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.play.services.ads)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.billing.ktx)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)

  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)

  "ksp"(libs.moshi.kotlin.codegen)
  "ksp"(libs.hilt.compiler)
}
tasks.withType<Test> {
  enabled = false
}