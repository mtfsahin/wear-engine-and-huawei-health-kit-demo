import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun local(key: String): String = localProperties.getProperty(key, "")

fun buildConfigString(value: String): String = "\"$value\""

android {
    namespace = "com.example.wearenginedemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wearenginedemo"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "LITE_WATCH_PACKAGE", buildConfigString(local("wearengine.lite.peerPackage")))
        buildConfigField("String", "LITE_WATCH_FINGERPRINT", buildConfigString(local("wearengine.lite.peerFingerprint")))
        buildConfigField("String", "SMART_WATCH_PACKAGE", buildConfigString(local("wearengine.smart.peerPackage")))
        buildConfigField("String", "SMART_WATCH_FINGERPRINT", buildConfigString(local("wearengine.smart.peerFingerprint")))

        manifestPlaceholders["huaweiAppId"] = local("wearengine.appId")
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("registered") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        val registered = signingConfigs.findByName("registered")
        debug {
            if (registered != null) signingConfig = registered
        }
        release {
            if (registered != null) signingConfig = registered
            isMinifyEnabled = false
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material3)
    implementation(libs.huawei.wearengine)
}
