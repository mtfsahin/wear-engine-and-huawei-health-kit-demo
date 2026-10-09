import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

if (rootProject.file("app/google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun secret(key: String, envKey: String, default: String = ""): String =
    localProperties.getProperty(key) ?: System.getenv(envKey) ?: default

val knownSecretKeys = setOf(
    "healthkit.clientId",
    "healthkit.clientSecret",
    "healthkit.deepLink",
    "healthkit.redirectUri",
    "healthkit.loginFreeAppId",
    "healthkit.apiHost",
    "healthkit.schemeSecret",
    "healthkit.authMethod",
    "healthkit.historyWindow",
    "wearengine.appId",
    "backend.baseUrl",
    "wearengine.lite.peerPackage",
    "wearengine.lite.peerFingerprint",
    "wearengine.smart.peerPackage",
    "wearengine.smart.peerFingerprint",
)
localProperties.keys
    .map { it.toString() }
    .filter { key ->
        key !in knownSecretKeys &&
            (key.startsWith("heal", ignoreCase = true) ||
                key.startsWith("wear", ignoreCase = true))
    }
    .forEach { key ->
        logger.warn("local.properties: unknown key '$key' is ignored, default used instead.")
    }


/**
 * Wear Engine checks the APK signature against the fingerprint registered in AppGallery
 * Connect, so the debug keystore is rejected. Put the registered keystore's details in
 * keystore.properties (git ignores it) and both build types are signed with it.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val releaseSigning = keystoreProperties.getProperty("storeFile")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.exists() }
val healthKitDeepLink = secret(
    "healthkit.deepLink",
    "HEALTH_KIT_DEEP_LINK",
    "wearhealthkit://oauth/callback",
)
val deepLinkUri = URI(healthKitDeepLink)

android {
    namespace = "com.example.wearhealthdemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wearhealthdemo"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "HEALTH_KIT_CLIENT_ID",
            "\"${secret("healthkit.clientId", "HEALTH_KIT_CLIENT_ID")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_CLIENT_SECRET",
            "\"${secret("healthkit.clientSecret", "HEALTH_KIT_CLIENT_SECRET")}\"",
        )
        buildConfigField("String", "HEALTH_KIT_DEEP_LINK", "\"$healthKitDeepLink\"")
        buildConfigField(
            "String",
            "HEALTH_KIT_REDIRECT_URI",
            "\"${secret("healthkit.redirectUri", "HEALTH_KIT_REDIRECT_URI")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_LOGIN_FREE_APP_ID",
            "\"${secret("healthkit.loginFreeAppId", "HEALTH_KIT_LOGIN_FREE_APP_ID")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_API_HOST",
            "\"${secret("healthkit.apiHost", "HEALTH_KIT_API_HOST")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_HISTORY_WINDOW",
            "\"${secret("healthkit.historyWindow", "HEALTH_KIT_HISTORY_WINDOW", "NONE")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_SCHEME_SECRET",
            "\"${secret("healthkit.schemeSecret", "HEALTH_KIT_SCHEME_SECRET")}\"",
        )
        buildConfigField(
            "String",
            "HEALTH_KIT_AUTH_METHOD",
            "\"${secret("healthkit.authMethod", "HEALTH_KIT_AUTH_METHOD", "WEB_OAUTH")}\"",
        )

        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${secret("backend.baseUrl", "BACKEND_BASE_URL")}\"",
        )

        buildConfigField(
            "String",
            "WEAR_ENGINE_APP_ID",
            "\"${secret("wearengine.appId", "WEAR_ENGINE_APP_ID")}\"",
        )

        buildConfigField(
            "String",
            "WEAR_LITE_PEER_PACKAGE",
            "\"${secret("wearengine.lite.peerPackage", "WEAR_LITE_PEER_PACKAGE")}\"",
        )
        buildConfigField(
            "String",
            "WEAR_LITE_PEER_FINGERPRINT",
            "\"${secret("wearengine.lite.peerFingerprint", "WEAR_LITE_PEER_FINGERPRINT")}\"",
        )
        buildConfigField(
            "String",
            "WEAR_SMART_PEER_PACKAGE",
            "\"${secret("wearengine.smart.peerPackage", "WEAR_SMART_PEER_PACKAGE")}\"",
        )
        buildConfigField(
            "String",
            "WEAR_SMART_PEER_FINGERPRINT",
            "\"${secret("wearengine.smart.peerFingerprint", "WEAR_SMART_PEER_FINGERPRINT")}\"",
        )

        manifestPlaceholders["healthKitScheme"] = deepLinkUri.scheme ?: "wearhealthkit"
        manifestPlaceholders["healthKitHost"] = deepLinkUri.host ?: "oauth"
        manifestPlaceholders["wearEngineAppId"] =
            secret("wearengine.appId", "WEAR_ENGINE_APP_ID", "your_wearengine_app_id")
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("wearEngine") {
                storeFile = releaseSigning
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        val wearEngine = signingConfigs.findByName("wearEngine")
        getByName("debug") {
            if (wearEngine != null) signingConfig = wearEngine
        }
        release {
            if (wearEngine != null) signingConfig = wearEngine
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.huawei.hms.wearengine)
    implementation(libs.androidx.browser)
    implementation(libs.huawei.hms.healthlite)
    implementation(libs.androidx.fragment)
    implementation(libs.huawei.hms.push)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
