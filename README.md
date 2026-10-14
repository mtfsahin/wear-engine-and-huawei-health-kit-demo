> **Note:** To access all shared projects, get information about environment setup, and view other guides, please visit [Explore-In-HMOS-Wearable Index](https://github.com/Explore-In-HMOS-Wearable/hmos-index).

# Wear Engine demo

An Android app that talks to its own app on a Huawei watch with **Huawei Wear Engine**. It
asks for the permission, lists the paired watches, checks the watch app with ping, and
sends and receives messages.

The Huawei Health Kit part that used to live here is now its own project,
`huawei-health-kit-demo`. This project covers Wear Engine only.

## Contents

1. [What you will build](#what-you-will-build)
2. [What you need](#what-you-need)
3. [How it works](#how-it-works)
4. [Step 1. Register your app](#step-1-register-your-app)
5. [Step 2. Apply for Wear Engine](#step-2-apply-for-wear-engine)
6. [Step 3. Get the watch app identity](#step-3-get-the-watch-app-identity)
7. [Step 4. Configure the project](#step-4-configure-the-project)
8. [Step 5. Run the app](#step-5-run-the-app)
9. [Step 6. Read the code](#step-6-read-the-code)
10. [Test checklist](#test-checklist)
11. [Result codes](#result-codes)
12. [Troubleshooting](#troubleshooting)
13. [Official documentation](#official-documentation)

## What you will build

One screen with five steps, in the order a real app runs them:

| Step on the screen | What happens | Wear Engine call |
| --- | --- | --- |
| 1. Permission | The app checks `DEVICE_MANAGER` and asks for it if needed | `checkPermissions`, `requestPermission` |
| 2. Watch | The app lists the watches paired in Huawei Health and picks the connected one | `getBondedDevices` |
| 3. Watch app | You choose the lite or the smart watch app, then ping it | `setPeerPkgName`, `setPeerFingerPrint`, `ping` |
| 4. Send a message | The text goes to the watch app | `send` |
| 5. Messages from the watch | Messages from the watch app show up here | `registerReceiver` |

A log at the bottom shows every call and its result.

## What you need

| | Version |
| --- | --- |
| Android Studio | A recent version that supports AGP 8.9 |
| JDK | 17 or later (Android Studio ships one) |
| Android phone | Android 6.0 (API 23) or later, a real phone |
| Huawei Health app on the phone | 14.0.12.310 or later |
| Huawei watch | Paired with the phone in Huawei Health |
| Watch app | Your own app installed on the watch, lite (JS) or smart (ArkTS) |
| HUAWEI developer account | Identity verified, as an individual or an enterprise |

The emulator does not work: Wear Engine runs through the Huawei Health app and Bluetooth.

## How it works

Your phone app never talks to the watch directly. The Huawei Health app is in the middle.

```
+------------------+   Wear Engine   +-------------------+   Bluetooth   +----------------+
|  This phone app  | <-------------> | Huawei Health app | <-----------> | Your watch app |
+------------------+                 +-------------------+               +----------------+
```

Huawei Health checks three things on every call:

1. Your app has an approved Wear Engine application on HUAWEI Developers.
2. The APK is signed with the certificate whose SHA-256 fingerprint you registered.
3. The user granted the `DEVICE_MANAGER` permission.

## Step 1. Register your app

1. Sign in to [AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html).
2. Create a project, then add an Android app to it.
   - **Package name:** the `applicationId` of your app. In this sample it is
     `com.example.wearenginedemo`. Change it in `app/build.gradle.kts` to your own value.
   - Note the **App ID**. You need it in Step 4.
3. Create a release keystore if you do not have one:

   ```bash
   keytool -genkeypair -v -keystore release.jks -alias release -keyalg RSA -keysize 2048 -validity 10000
   ```

4. Read its SHA-256 fingerprint:

   ```bash
   keytool -list -v -keystore release.jks -alias release
   ```

5. In AppGallery Connect, open **Project settings > General information** and add the
   SHA-256 fingerprint to the app.
6. Open **HUAWEI Developers > Console > App services > HUAWEI ID** and apply for the HUAWEI ID
   service for the same package name and fingerprint.

> Wear Engine rejects an APK signed with the Android debug key. The project signs both debug
> and release builds with your keystore (Step 4).

## Step 2. Apply for Wear Engine

1. Open **HUAWEI Developers > Console > App services > Wear Engine**.
2. Click **Apply for Wear Engine** and select your Android app.
3. Select only **Basic device information (`DEVICE_MANAGER`)**. It covers the watch list,
   ping and messages. Extra permissions are a common reason for rejection.
4. Upload the two description files the form asks for, then submit.

The review usually takes 1 to 2 weeks. Until it is approved, calls fail with result code 8.
After the approval it can take up to 24 hours to reach the phone. Clearing the data of the
Huawei Health app refreshes it at once.

## Step 3. Get the watch app identity

The phone app has to name the watch app it talks to. Ask the watch developer for these two
values:

| Watch app | Package name | Fingerprint |
| --- | --- | --- |
| Lite wearable (JS, WATCH GT, FIT and D series) | The bundle name | The bundle name, `_`, then the Base64 public key of its signing certificate, for example `com.example.watch_BNkXKaBX14TZ...` |
| Smart wearable (ArkTS, WATCH 5, WATCH Ultimate 2 and newer) | The bundle name | The App ID of the watch app in AppGallery Connect |

The watch app names your phone app the same way: your package name and the SHA-256
fingerprint from Step 1. A wrong value on either side is the most common reason why nothing
arrives.

## Step 4. Configure the project

1. Clone the project and open it in Android Studio.

   ```bash
   git clone https://github.com/mtfsahin/wear-engine-and-huawei-health-kit-demo.git
   ```

2. Copy `local.properties.example` to `local.properties` and fill it in:

   | Key | Value |
   | --- | --- |
   | `sdk.dir` | Path to your Android SDK. Android Studio writes this line for you. |
   | `wearengine.appId` | The App ID from Step 1 |
   | `wearengine.lite.peerPackage` | Package name of your lite watch app |
   | `wearengine.lite.peerFingerprint` | Fingerprint of your lite watch app |
   | `wearengine.smart.peerPackage` | Bundle name of your smart watch app |
   | `wearengine.smart.peerFingerprint` | App ID of your smart watch app |

   You can leave the lite or the smart pair empty if you only have one watch app.

3. Copy `keystore.properties.example` to `keystore.properties` and point it at the keystore
   from Step 1:

   ```properties
   storeFile=release.jks
   storePassword=your-store-password
   keyAlias=release
   keyPassword=your-key-password
   ```

   `storeFile` is relative to the project root. Both files are in `.gitignore`, so your
   values never reach the repository.

## Step 5. Run the app

1. Pair the watch with the phone in the Huawei Health app.
2. Install and open your watch app on the watch.
3. Connect the phone with USB and click **Run** in Android Studio, or run:

   ```bash
   ./gradlew installDebug
   ```

4. On the first start, Huawei Health shows the permission screen. Allow it.
5. Choose your watch, choose **Lite watch** or **Smart watch**, and tap **Ping**. You should see
   `202: the watch app is running`.
6. Type a message and tap **Send**. The log shows `Sent: ...` and the watch app gets the text.
7. Send a message from the watch app. It shows up under **5. Messages from the watch**.

## Step 6. Read the code

The whole sample is four Kotlin files:

| File | What it holds |
| --- | --- |
| `MainActivity.kt` | Starts the screen |
| `WearEngineViewModel.kt` | Every Wear Engine call, in the order of the steps |
| `WatchApp.kt` | The lite and smart watch app identities from `local.properties` |
| `ui/WearEngineScreen.kt` | The screen with the five steps |

### 6.1 Add the SDK

The SDK comes from Huawei's Maven repository, listed in `settings.gradle.kts`:

```kotlin
maven { url = uri("https://developer.huawei.com/repo/") }
```

`gradle/libs.versions.toml` pins the version:

```toml
wearengine = "5.0.3.305"
```

Write the version in full. Huawei's repository has no version index, so `5.0.3.+` does not
resolve.

### 6.2 Declare the App ID and Huawei Health

`AndroidManifest.xml` declares the permission, the App ID and the Huawei Health package:

```xml
<uses-permission android:name="com.huawei.wearengine.permission.DEVICE_MANAGER" />

<meta-data
    android:name="com.huawei.hms.client.appid"
    android:value="${huaweiAppId}" />

<queries>
    <package android:name="com.huawei.health" />
</queries>
```

`${huaweiAppId}` is filled from `wearengine.appId` in `local.properties`.

### 6.3 Get the clients

All Wear Engine features start from `HiWear`:

```kotlin
private val authClient = HiWear.getAuthClient(application)
private val deviceClient = HiWear.getDeviceClient(application)
private val p2pClient = HiWear.getP2pClient(application)
```

### 6.4 Check and ask for the permission

With Huawei Health 11.0.3.512 or later, `DEVICE_MANAGER` is often already granted, so the app
checks first and asks only when needed:

```kotlin
authClient.checkPermissions(arrayOf(Permission.DEVICE_MANAGER))
    .addOnSuccessListener { results ->
        if (results.all { it }) onPermissionGranted() else requestPermission()
    }
```

```kotlin
authClient.requestPermission(object : AuthCallback {
    override fun onOk(permissions: Array<out Permission>?) {
        onPermissionGranted()
    }

    override fun onCancel() {
        log("The permission was not granted")
    }
}, Permission.DEVICE_MANAGER)
```

### 6.5 List the watches

`getBondedDevices` returns the watches paired in Huawei Health. Huawei Health keeps only one
watch connected at a time, so the app selects the connected one:

```kotlin
deviceClient.bondedDevices
    .addOnSuccessListener { devices ->
        val watches = devices.orEmpty().filterNotNull()
        watches.firstOrNull { it.isConnected }?.let(::selectWatch)
    }
```

`isConnected` is the state at the time of the call, not a live value. Tap **Refresh** to read
it again.

### 6.6 Name the watch app

Lite and smart watches run different watch apps, so `WatchApp.kt` keeps both identities and
the switch on the screen picks one:

```kotlin
p2pClient.setPeerPkgName(watchApp.packageName)
p2pClient.setPeerFingerPrint(watchApp.fingerprint)
```

### 6.7 Listen to the watch

The app registers a receiver as soon as a watch is selected, so no reply is missed:

```kotlin
private val receiver = Receiver { message ->
    if (message.type == Message.MESSAGE_TYPE_DATA) {
        val text = String(message.data, Charsets.UTF_8)
    }
}

p2pClient.registerReceiver(watch, receiver)
```

The receiver is removed in `onCleared()` with `p2pClient.unregisterReceiver(receiver)`.

### 6.8 Ping the watch app

`ping` checks the watch app and starts it when it is installed but closed:

```kotlin
p2pClient.ping(watch) { result ->
    when (result) {
        200 -> "the watch app is not installed"
        201 -> "the watch app is installed but not running"
        202 -> "the watch app is running"
    }
}
```

### 6.9 Send a message

```kotlin
val message = Message.Builder()
    .setPayload(text.toByteArray(Charsets.UTF_8))
    .build()

p2pClient.send(watch, message, object : SendCallback {
    override fun onSendResult(resultCode: Int) {
        if (resultCode == 207) log("Sent: $text")
    }

    override fun onSendProgress(progress: Long) = Unit
})
```

Message size limits:

| From | To | Maximum |
| --- | --- | --- |
| Android phone | Lite watch | 1 KB |
| Android phone | Smart watch | 4 KB |
| Lite watch | Android phone | 1 KB |
| Smart watch | Android phone | 4 KB |

For bigger data, split it into numbered pieces and join them on the other side.

## Test checklist

| Check | Expected result |
| --- | --- |
| Open the app the first time | Huawei Health shows the permission screen, then the watch list fills |
| Ping with the watch app open | `202` |
| Ping with the watch app closed | `201`, and the watch app opens |
| Ping with no watch app | `200` |
| Send a message | Log shows `Sent: ...`, the watch app shows the text |
| Send from the watch app | The text shows under **5. Messages from the watch** |
| Take the permission back in Huawei Health (Me > Settings > Open device capability) | Calls fail with result code 9 |

## Result codes

| Code | Meaning |
| --- | --- |
| 0 | Success |
| 2 | The Huawei Health app is not installed |
| 3 | The Huawei Health app is installed, but nobody is signed in |
| 4 | No watch is paired in Huawei Health |
| 5 | A parameter is wrong |
| 7 | The user has not accepted the Huawei Health user agreement or privacy statement |
| 8 | The Wear Engine application is not approved yet, or the App ID, package name or fingerprint does not match the console |
| 9 | The user has not granted `DEVICE_MANAGER` |
| 12 | Internal error |
| 14 | The Huawei Health app is too old |
| 16 | The watch is not connected |
| 200 | Ping: the watch app is not installed |
| 201 | Ping: the watch app is installed but not running |
| 202 | Ping: the watch app is running |
| 203 | System error during the P2P call |
| 206 | Send failed: identities do not match, or the watch app has no receiver |
| 207 | Send succeeded |

The full list is on the official [Result codes](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/errocode-0000001054450278) page.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| Result code 8 | Wait for the Wear Engine approval. Check the App ID in `local.properties`, the package name and the SHA-256 fingerprint. Clear the data of the Huawei Health app to refresh the approval. |
| Result code 9 | Tap **Grant permission**. |
| Empty watch list | Pair the watch in Huawei Health. Check that the build is signed with the registered keystore. |
| Ping answers 200 or 201 | Install or open the watch app. Check the package name for the selected watch type. |
| Send answers 206 | Compare the package names and fingerprints on both sides. Check that the watch app registers a receiver. |
| `package name missing in local.properties` in the log | Fill in the lite or smart keys in `local.properties` and build again. |

## Official documentation

- [Getting started](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/dev-process-0000001051058039)
- [Applying for the Wear Engine service](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/applying-wearengine-0000001050777982)
- [Integrating the Wear Engine SDK](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/integrating-phone-sdk-0000001051137958)
- [Requesting user authorization](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/requesting-user-authorization-0000001050819181)
- [App-to-app message communications](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/phone-send-message-0000001051059209)
- [Result codes](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/errocode-0000001054450278)
- [Version change history](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/version-change-history-0000001086350238)

## Related samples

- [wear-engine-mobile-to-watch](https://github.com/Explore-In-HMOS-Wearable/wear-engine-mobile-to-watch), another Wear Engine phone sample
- [wear-engine-ios-to-watch-file-message](https://github.com/Explore-In-HMOS-Wearable/wear-engine-ios-to-watch-file-message), the same for iOS

## License

See [LICENSE](LICENSE).
