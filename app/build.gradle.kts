plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.thehouse.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.thehouse.app"
        minSdk = 26
        targetSdk = 37
        // Política de versionamiento: VERSIONING.md → 1.0.0 = 10000
        versionCode = 10000
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://ca-thehouse-u2.grayisland-cb722b5f.eastus2.azurecontainerapps.io/\"")
    }

    // NV-03: las credenciales de firma provienen de variables de entorno (nunca del repositorio).
    val keystoreSigning: SigningCredentials? = loadSigningCredentials()

    signingConfigs {
        create("release") {
            keystoreSigning?.let { creds ->
                storeFile = file(creds.storePath)
                storePassword = creds.storePassword
                keyAlias = creds.keyAlias
                keyPassword = creds.keyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
        }
        release {
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
            keystoreSigning?.let {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// versión según VERSIONING.md: 1.0.0 → 10000
// (definida en defaultConfig: versionCode = 10000, versionName = "1.0.0")

/**
 * Lee las credenciales de firma desde variables de entorno THEHOUSE_*.
 *
 * Nunca escribe valores en logs ni los versiona. Si alguna falta, devuelve null
 * y el build release se produce sin signingConfig (fallo posterior en firmado).
 */
data class SigningCredentials(
    val storePath: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

fun loadSigningCredentials(): SigningCredentials? {
    val storePath = System.getenv("THEHOUSE_KEYSTORE_PATH") ?: return null
    val storePassword = System.getenv("THEHOUSE_KEYSTORE_PASSWORD") ?: return null
    val keyAlias = System.getenv("THEHOUSE_KEY_ALIAS") ?: return null
    val keyPassword = System.getenv("THEHOUSE_KEY_PASSWORD") ?: return null
    if (storePath.isBlank() || storePassword.isBlank() ||
        keyAlias.isBlank() || keyPassword.isBlank()
    ) {
        return null
    }
    return SigningCredentials(storePath, storePassword, keyAlias, keyPassword)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)

    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
