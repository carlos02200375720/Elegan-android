import java.time.Duration

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  // alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
  defaultConfig {
    applicationId = "com.elegan"
    minSdk = 24
    targetSdk = 36
    val resolvedVersionCode = providers.gradleProperty("APP_VERSION_CODE")
      .orElse(providers.environmentVariable("APP_VERSION_CODE"))
      .orNull
      ?.trim()
      ?.toIntOrNull()
      ?.takeIf { it > 0 }
      ?: 1
    val resolvedVersionName = providers.gradleProperty("APP_VERSION_NAME")
      .orElse(providers.environmentVariable("APP_VERSION_NAME"))
      .orNull
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: "1.0"
    versionCode = resolvedVersionCode
    versionName = resolvedVersionName
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "ELEGAN_API_BASE_URL", "\"https://elegan-backend-587849039182.us-east1.run.app/\"")
    val sentryDsn = resolveConfigValue("SENTRY_DSN")
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
    buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
  }
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }
  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  sourceSets {
    getByName("main") {
      java.directories.addAll(listOf("src/main", "src/tienda", "src/data"))
      kotlin.directories.addAll(listOf("src/main", "src/tienda", "src/data"))
    }
  }
}

// secrets {
//   propertiesFileName = ".env"
//   defaultPropertiesFileName = ".env.example"
// }

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
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
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.datasource)
  implementation(libs.media3.database)
  implementation(libs.sentry.android)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.withType<Test> {
  failFast = false
  forkEvery = 1
  maxHeapSize = "1024m"
  timeout.set(Duration.ofMinutes(5))
  testLogging { events("passed", "failed", "skipped") }
}

fun resolveConfigValue(key: String): String {
  val envFile = rootProject.file(".env")
  if (envFile.isFile) {
    val fromFile = envFile.readLines()
      .asSequence()
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
      .mapNotNull { line ->
        val idx = line.indexOf('=')
        val k = line.substring(0, idx).trim()
        val v = line.substring(idx + 1).trim().trim('"', '\'')
        if (k == key) v else null
      }
      .firstOrNull()
    if (!fromFile.isNullOrBlank()) return fromFile
  }
  return System.getenv(key)?.trim().orEmpty()
}
