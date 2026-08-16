import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// --- Podpisywanie wersji release: sekrety NIE pochodzą z repo ---
// Kolejność źródeł: zmienne środowiskowe (sekrety CI / Freebuff API Keys),
// właściwości Gradle (-P...), lokalny plik keystore.properties (gitignored).
// Jeśli brakuje pliku klucza (my-upload-key.jks) lub danych podpisywania,
// build release zostanie wygenerowany bez podpisu (nie spowoduje błędu).
val keystoreProperties = Properties().apply {
  val propsFile = rootProject.file("keystore.properties")
  if (propsFile.exists()) propsFile.inputStream().use { load(it) }
}

val keystoreFile = rootProject.file("my-upload-key.jks")

val keystorePassword = (
  System.getenv("KEYSTORE_PASSWORD")
    ?: findProperty("KEYSTORE_PASSWORD") as? String
    ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
  ).orEmpty()
val keystoreAlias = (
  System.getenv("KEY_ALIAS")
    ?: findProperty("KEY_ALIAS") as? String
    ?: keystoreProperties.getProperty("KEY_ALIAS")
  ).orEmpty()
val keystoreKeyPassword = (
  System.getenv("KEY_PASSWORD")
    ?: findProperty("KEY_PASSWORD") as? String
    ?: keystoreProperties.getProperty("KEY_PASSWORD")
  ).orEmpty()

val hasKeystore = keystoreFile.exists() &&
  keystorePassword.isNotBlank() &&
  keystoreAlias.isNotBlank() &&
  keystoreKeyPassword.isNotBlank()

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
      if (hasKeystore) {
        storeFile = keystoreFile
        storePassword = keystorePassword
        keyAlias = keystoreAlias
        keyPassword = keystoreKeyPassword
      }
    }
  }

  buildTypes {
    release {
      // OPTYMALIZACJA APK
      isMinifyEnabled = true           // włącza R8 (obfuscacja)
      isShrinkResources = true         // usuwa nieużywane zasoby

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )

      if (hasKeystore) {
        signingConfig = signingConfigs.getByName("release")
      }
    }

    debug {
      // domyślny debug.keystore (generowany automatycznie przez AGP)
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
