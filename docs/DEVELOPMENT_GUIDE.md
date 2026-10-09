# Development guide

This guide explains how the demo is built and how to run it with your own Huawei developer
account. It follows the order in which you would build the same features into your own app.

1. [Overview](#1-overview)
2. [Console setup](#2-console-setup)
3. [Project configuration](#3-project-configuration)
4. [Wear Engine: phone to watch](#4-wear-engine-phone-to-watch)
5. [Health Kit: authorization](#5-health-kit-authorization)
6. [Health Kit: reading workouts](#6-health-kit-reading-workouts)
7. [Workout push notifications](#7-workout-push-notifications)
8. [Deleting data and disconnecting](#8-deleting-data-and-disconnecting)
9. [Testing](#9-testing)
10. [Before production](#10-before-production)
11. [Troubleshooting](#11-troubleshooting)
12. [Official documentation](#12-official-documentation)

---

## 1. Overview

There are two separate paths between a Huawei watch and your phone app. They use different
kits, and you can build one without the other.

| | Wear Engine | Health Kit Cloud |
| --- | --- | --- |
| What moves | Your own messages, between your phone app and your watch app | The user's workout records |
| Path | Phone app, Huawei Health app, watch, over Bluetooth | Watch, Huawei Health app, Huawei cloud, your app or server, over REST |
| Needs your own watch app | Yes | No |
| When | Live, while both apps run | Within minutes after a workout syncs |
| In this demo | Wear Engine tab | Health Kit tab and `backend/` |

The Huawei Health app on the phone is in the middle of both paths. Your phone app never
connects to the watch on its own.

```
+--------------------+        +---------------------+        +----------------+
|  Your phone app    |  P2P   |  Huawei Health app  |  BT    |  Your watch    |
|  (this demo)       | <----> |  (Wear Engine)      | <----> |  app           |
+--------------------+        +---------------------+        +----------------+
          |                              |
          | REST (OAuth 2.0)             | sync
          v                              v
+--------------------+        +---------------------+
|  Your backend      | <----- |  Huawei Health Kit  |
|  (backend/)        | notify |  Cloud              |
+--------------------+        +---------------------+
```

---

## 2. Console setup

Do this first: some steps are reviewed by Huawei and take days.

### 2.1 Developer account

1. Register a HUAWEI ID on [HUAWEI Developers](https://developer.huawei.com/consumer/en/)
   and complete the identity verification, as an individual or an enterprise.
2. Open **Console** and create a project and an Android app in AppGallery Connect. This
   gives you the **App ID** and the **App secret**.

### 2.2 HUAWEI ID service (Wear Engine needs it)

In **App services > Development > HUAWEI ID**, apply for the HUAWEI ID service:

| Field | Value |
| --- | --- |
| Product type | Mobile app |
| Package name | Your `applicationId`, exactly. It cannot be changed later. |
| SHA-256 certificate fingerprint | The fingerprint of the keystore that signs your builds |

Wear Engine checks the App ID, the package name and the signing certificate on every call.
They must match the console.

### 2.3 Wear Engine

In **App services > Development > Wear Engine**, click **Apply for Wear Engine**, select
your Android app and the **Basic device information (`DEVICE_MANAGER`)** permission. This
one permission covers the watch list, ping and P2P messages. Ask only for what you use:
extra permissions are a common reason for rejection.

Upload the two description files the form asks for and submit. The review usually takes 1
to 2 weeks. Until it is approved, calls fail with result code 8 (scope unauthorized).

### 2.4 Health Service Kit

On the **Health Service Kit** card, apply for the scopes this demo uses:

| Scope | Used for |
| --- | --- |
| `openid` | Who the user is (`openId`), required |
| `https://www.huawei.com/healthkit/activityrecord.read` | Reading workout records |
| `https://www.huawei.com/healthkit/activityrecord.write` | Deleting the records this app wrote (section 8) |
| `https://www.huawei.com/healthkit/activity.read` | The detail data of a workout |

Data is only readable from the moment the user authorizes your app. To read older data,
apply for a history scope (`historydata.open.week`, `.month` or `.year`) and set
`healthkit.historyWindow`.

For **Method 1 (Web OAuth)** add your redirect URI to the app's callback URLs.

For **Method 2 (login-free SDK)**:

- Add Huawei's fixed callback URL to the app's callback URLs, exactly like this:
  `https://h5hosting.dbankcdn.com/cch5/healthkit/oauth-h5/oauth-callback.html`
- Enter your own deep link as the **Scheme URL**, for example
  `wearhealthkit://oauth/callback`. The console then gives you a **Scheme Secret**.
- Apply for the login-free authorization permission for your regions.

In test mode, at most 100 users can use the scopes. Apply for verification on the same card
to remove the limit.

### 2.5 Subscription (workout notifications)

On the Health Service Kit card, register the subscription capability in **HTTP** mode with
your backend's callback URL, which must be HTTPS and end with `/healthkit/notifications`.
Huawei runs a connectivity test and expects 204. You then get a **subscriber ID** and a
**subscriber secret** for the backend. See [backend/deploy/README.md](../backend/deploy/README.md).

### 2.6 Push

- **Push Kit** (Huawei phones): enable Push Kit for the app in AppGallery Connect and
  download `agconnect-services.json` into `app/`.
- **Firebase** (phones with Google services): add the app in the Firebase console and
  download `google-services.json` into `app/`. The Google Services plugin is applied only
  when this file exists, so the project builds without it.

---

## 3. Project configuration

### 3.1 local.properties

Copy `local.properties.example` to `local.properties`. The build reads it into
`BuildConfig`. Environment variables with the names in the third column work too, which is
what a CI job would use.

| Key | Meaning | Environment variable |
| --- | --- | --- |
| `wearengine.appId` | App ID, also written into the manifest as `com.huawei.hms.client.appid` | `WEAR_ENGINE_APP_ID` |
| `wearengine.lite.peerPackage` | Package name of your lite watch app | `WEAR_LITE_PEER_PACKAGE` |
| `wearengine.lite.peerFingerprint` | Fingerprint of your lite watch app | `WEAR_LITE_PEER_FINGERPRINT` |
| `wearengine.smart.peerPackage` | Bundle name of your smart watch app | `WEAR_SMART_PEER_PACKAGE` |
| `wearengine.smart.peerFingerprint` | Fingerprint of your smart watch app | `WEAR_SMART_PEER_FINGERPRINT` |
| `healthkit.clientId` | App ID, used as `client_id` | `HEALTH_KIT_CLIENT_ID` |
| `healthkit.clientSecret` | App secret, Method 1 token exchange (see section 10) | `HEALTH_KIT_CLIENT_SECRET` |
| `healthkit.deepLink` | This app's deep link, the Method 2 Scheme URL | `HEALTH_KIT_DEEP_LINK` |
| `healthkit.redirectUri` | Method 1 redirect URI, exactly as registered | `HEALTH_KIT_REDIRECT_URI` |
| `healthkit.schemeSecret` | Method 2 Scheme Secret | `HEALTH_KIT_SCHEME_SECRET` |
| `healthkit.loginFreeAppId` | Method 2 App ID if it differs from `clientId` | `HEALTH_KIT_LOGIN_FREE_APP_ID` |
| `healthkit.apiHost` | Pin a regional Health Kit host, or leave empty | `HEALTH_KIT_API_HOST` |
| `healthkit.authMethod` | `WEB_OAUTH` or `LOGIN_FREE_SDK` at start | `HEALTH_KIT_AUTH_METHOD` |
| `healthkit.historyWindow` | `NONE`, `WEEK`, `MONTH` or `YEAR` | `HEALTH_KIT_HISTORY_WINDOW` |
| `backend.baseUrl` | Your backend, for push notifications | `BACKEND_BASE_URL` |

The keys are case sensitive. The build prints a warning for any unknown `healthkit.*` or
`wearengine.*` key instead of silently using a default.

### 3.2 Signing

Wear Engine rejects an APK signed with the Android debug key. Copy
`keystore.properties.example` to `keystore.properties` and point it at the keystore whose
SHA-256 fingerprint you registered. Both debug and release builds are then signed with it.

### 3.3 What never goes into the repository

`local.properties`, `keystore.properties`, `*.jks`, `app/agconnect-services.json`,
`app/google-services.json` and `backend/.env` are all in `.gitignore`.

---

## 4. Wear Engine: phone to watch

The Wear Engine tab runs these steps in order. Each one needs the one before it.

| # | Step | Call | File |
| --- | --- | --- | --- |
| 1 | Check and ask for the permission | `checkPermissions`, `requestPermission` | `AuthManager.kt` |
| 2 | List the paired watches | `getBondedDevices` | `DeviceManager.kt` |
| 3 | Name the watch app | `setPeerPkgName`, `setPeerFingerPrint` | `P2pManager.kt` |
| 4 | Register a receiver for the watch's messages | `registerReceiver` | `P2pManager.kt` |
| 5 | Check the watch app | `ping` | `P2pManager.kt` |
| 6 | Send a message or a test file | `send` | `P2pManager.kt` |

### 4.1 SDK and manifest

The SDK comes from Huawei's Maven repository, which `settings.gradle.kts` lists:

```kotlin
// gradle/libs.versions.toml
wearengine = "5.0.3.305"
```

The manifest declares the App ID and lets the app see the Huawei Health app:

```xml
<meta-data
    android:name="com.huawei.hms.client.appid"
    android:value="${wearEngineAppId}" />

<queries>
    <package android:name="com.huawei.health" />
</queries>
```

### 4.2 Permission

The Huawei Health app shows the permission screen. With Huawei Health 11.0.3.512 or later,
`DEVICE_MANAGER` is granted by default, so the app checks first and asks only if needed:

```kotlin
authClient.checkPermissions(arrayOf(Permission.DEVICE_MANAGER))
    .addOnSuccessListener { permissions ->
        val granted = permissions?.all { it == true } ?: false
    }

authClient.requestPermission(object : AuthCallback {
    override fun onOk(permissions: Array<Permission?>?) { /* granted */ }
    override fun onCancel() { /* the user said no */ }
}, Permission.DEVICE_MANAGER)
```

### 4.3 The watch and the watch app

`getBondedDevices` returns the watches paired with Huawei Health. Huawei Health connects
only one at a time; pick the connected one. `isConnected` is the state at the time of the
call, not a live value.

Then name the watch app. Lite watches and smart watches run different watch apps with
different identities, so the demo keeps both pairs and the switch in the top right corner
of the Wear Engine tab picks one (`WatchType.kt`):

```kotlin
fun setPeer(watchType: WatchType) {
    p2pClient.setPeerPkgName(watchType.peerPackage)
    p2pClient.setPeerFingerPrint(watchType.peerFingerprint)
}
```

Ask your watch team for these values:

| Watch app | Package name | Fingerprint |
| --- | --- | --- |
| Smart wearable (HarmonyOS 5.0 or later) | The bundle name | The watch app's App ID in AppGallery Connect |
| Lite wearable | The bundle name | The bundle name, `_`, then the Base64 public key of its signing certificate |

The watch app names your phone app the same way, with your package name and the SHA-256
fingerprint of your signing certificate. A wrong value on either side is the most common
reason why nothing arrives.

### 4.4 Receive, ping and send

The receiver is registered as soon as a watch is selected, so replies are never missed:

```kotlin
p2pClient.registerReceiver(device, Receiver { message ->
    if (message.type == Message.MESSAGE_TYPE_DATA) {
        val text = String(message.data, StandardCharsets.UTF_8)
    }
})
```

`ping` checks the watch app and starts it when it is installed but closed:

| Ping result | Meaning |
| --- | --- |
| 200 | Not installed on the watch |
| 201 | Installed, not running |
| 202 | Running, ready to receive |

A message is up to 1 KB. The send callback reports 207 when it was delivered:

```kotlin
val msg = Message.Builder()
    .setPayload(text.toByteArray(StandardCharsets.UTF_8))
    .build()

p2pClient.send(device, msg, object : SendCallback {
    override fun onSendResult(resultCode: Int) { /* 207 = sent */ }
    override fun onSendProgress(progress: Long) {}
})
```

**Send File** sends a small text file written to the app's cache, to show the file path.
When the phone app closes, it sends `{"type":"app_destroy", ...}` so the watch app knows it
is gone (`WatchMessages.kt`).

---

## 5. Health Kit: authorization

Health Kit uses the OAuth 2.0 authorization code flow. The flow has four steps, and the two
methods differ only in step 1.

| Step | What happens | Code |
| --- | --- | --- |
| 1 | Get an authorization code. The user signs in if needed and allows the app. | `auth/WebOAuthAuthorizer.kt` or `auth/LoginFreeAuthorizer.kt` |
| 2 | Change the code into an access token and a refresh token | `token/HealthKitTokenClient.kt` |
| 3 | Read the workouts with the access token | `api/ActivityRecordsApi.kt` |
| 4 | Refresh the access token after 1 hour | `token/HealthKitTokenClient.kt` |

`HealthKitClient.kt` is the single entry point; the view model only talks to it.

### 5.1 The two methods

| | Method 1: Web OAuth | Method 2: login-free SDK |
| --- | --- | --- |
| SDK in the app | None, plain HTTPS | `com.huawei.hms:healthlite:6.16.0.300` |
| Sign-in page | Always | Not shown again when the user is already signed in |
| Where the user allows the app | Huawei page in a Custom Tab or an in-app WebView | HUAWEI ID screen, Huawei Health app, or a Huawei web page |
| Callback | Your redirect URI (`healthkit.redirectUri`) | Huawei's fixed callback URL, plus your deep link as the Scheme URL |

**The two callbacks are different values.** `healthkit.redirectUri` is what Method 1 sends to
Huawei. `healthkit.deepLink` is this app's own scheme, used for the manifest intent filter
and as the Method 2 Scheme URL. Mixing them up produces `invalid redirect_uri` (error 1101,
sub_error 20022) on Huawei's page.

When the redirect URI is not this app's own scheme (an `https` URL), Method 1 opens the page
in `WebAuthActivity` and catches the redirect there, because a Custom Tab cannot hand an
arbitrary web URL back to the app.

Method 2 has two solutions, and the card lets you switch between them while testing:
solution 1 lets the SDK pick the screen, solution 2 always uses the Huawei Health app and
also asks the user to turn on cloud sync.

### 5.2 The Health Kit switch

Besides allowing your app, the user must turn on HUAWEI Health Kit in the Huawei Health app
(Me > Privacy management > Data sharing and authorization). If it is off, the workout list
stays empty and the response header `x-health-app-privacy` is `2`. The **Open Huawei Health**
button takes the user there.

---

## 6. Health Kit: reading workouts

```
GET https://health-api.cloud.huawei.com/healthkit/v2/activityRecords
    ?startTime=<ms>&endTime=<ms>
Authorization: Bearer <access token>
```

- The **Look back** control reads the last 7 days, 30 days, 90 days or 1 year. The window
  always ends now: Health Kit answers 403 "The query time is out of range" when `endTime`
  is far in the past.
- Results come in pages; `ActivityRecordsApi` follows the cursor.
- **Regional sites.** A token only works on the site its HUAWEI ID belongs to. Another site
  answers 403 with code 121001 ("site cross"). A European account, for example, lives on
  `health-api.cloud.huawei.eu`. `ActivityRecordsApi` tries the known hosts, remembers the
  one that answers (`ApiSiteStore.kt`), and `healthkit.apiHost` pins it if you know it.
- `WorkoutType.kt` turns the `activityType` number into a name.

---

## 7. Workout push notifications

Instead of polling, the backend subscribes each user to workout events. Huawei calls the
backend when a workout arrives, and the backend sends a push.

```
app: push token  ->  backend POST /devices
app: openId + access token  ->  backend POST /subscriptions  ->  Huawei subscription
watch records a workout  ->  Huawei calls POST /healthkit/notifications  ->  backend
backend  ->  Push Kit or Firebase  ->  phone shows "New workout"
```

On the phone (`push/`):

1. `PushTokens.kt` gets a token from Push Kit on Huawei phones or from Firebase on phones
   with Google services.
2. `WorkoutSubscriptions.kt` sends the token and the `openId` to the backend, then asks the
   backend to create the Huawei subscription with the user's access token. This runs right
   after a successful authorization.

On the backend (`backend/`):

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/devices` | Store `{openId, pushToken, provider}` |
| POST | `/subscriptions` | Subscribe the user to Health Kit events |
| GET, POST | `/healthkit/notifications` | The callback Huawei calls |
| POST | `/push-test/:openId` | Send a test push |

Huawei signs every notification with `x-notification-signature`, an HMAC-SHA256 over
`openId_eventType_eventTime` keyed with the Base64-decoded subscriber secret. The backend
answers 204 at once, drops a notification whose signature is wrong, and sends the push
afterwards, so Huawei never waits for the push service. Set `HEALTH_SUBSCRIBER_SECRET`
before going live: while it is empty, the signature check is skipped.

Run it locally:

```bash
cd backend
npm install
cp .env.example .env    # fill in your values
npm start
```

Node.js 20.6 or later is needed for `--env-file`. To deploy it, see
[backend/deploy/README.md](../backend/deploy/README.md).

---

## 8. Deleting data and disconnecting

| Button | What it does | Code |
| --- | --- | --- |
| Disconnect | Deletes the stored tokens on the phone. The Huawei authorization stays. | `HealthKitClient.signOut()` |
| Delete data | Deletes the activity records this app wrote, then cancels the authorization | `api/DataDeletionApi.kt` |

Cancelling the authorization is one call. It cancels all scopes the user granted to the
app; with `deleteData=true` the data the app created is deleted at once, otherwise after 3
days unless the user authorizes again:

```
DELETE https://health-api.cloud.huawei.com/healthkit/v2/consents/<app id>?deleteData=true
Authorization: Bearer <access token>
```

Huawei asks every app to give users a way to cancel the authorization. Users can also do it
themselves in Huawei Health (Me > Privacy management > Data sharing and authorization).
For Wear Engine, users can withdraw access in Huawei Health under Me > Settings > Open device
capability; Wear Engine calls then answer result code 9.

---

## 9. Testing

Use a real phone with the Huawei Health app, signed in, paired with a Huawei watch that runs
your watch app.

| Check | Expected |
| --- | --- |
| Open the Wear Engine tab the first time | Permission screen in Huawei Health, then the watch list |
| Ping | 202 when the watch app is running |
| Send Message | 207, the watch app shows the text |
| Watch sends a message | It appears under Received Messages |
| Health Kit, Connect | Huawei's permission screen, then the workout list |
| Record a workout on the watch | A push notification within minutes, if the backend runs |
| Delete data | The list empties and the authorization is cancelled |

---

## 10. Before production

- **Secrets.** This demo keeps the App secret in the app and exchanges the code there, so it
  stays a single module. A real app sends the authorization code to its backend, and only
  the backend holds the secret and the tokens.
- **Token storage.** Tokens are in `SharedPreferences` here. Keep them on your backend, or
  at least encrypt them on the phone.
- **WebView.** Method 1 falls back to a WebView when the redirect URI is not the app's own
  scheme. Register a custom scheme or an App Link you own and stay on the Custom Tab.
- **Login-free SDK.** `healthlite:6.16.0.300` needs `androidx.fragment` at runtime but does
  not declare it, so `app/build.gradle.kts` adds it. Without it the SDK fails with
  `NoClassDefFoundError: androidx/fragment/app/FragmentActivity`.
- **Maven versions.** Huawei's Maven repository publishes no `maven-metadata.xml`, so write
  versions in full (`6.16.0.300`, not `6.16.+`).
- **Verification.** Apply for Health Kit verification to lift the 100-user test limit, and
  remove the scopes your app does not use before you apply.
- **Callback.** Use HTTPS with a certificate from a public certificate authority, and set the
  subscriber secret so the signature is checked.

---

## 11. Troubleshooting

| What you see | What to do |
| --- | --- |
| Wear Engine result code 8 | The Wear Engine application is not approved yet, or the App ID, package name or SHA-256 does not match the console. The approval can take up to 24 hours to reach the phone; clearing Huawei Health's data refreshes it. |
| Wear Engine result code 9 | The user has not granted `DEVICE_MANAGER`. Ask again. |
| Empty watch list | No watch is paired in Huawei Health, or the build is not signed with the registered keystore. |
| Ping answers 200 or 201 | Install or open the watch app; check the peer package name for the selected watch type. |
| Send result 206 | Package name or fingerprint does not match on one side, or the watch app has no receiver. |
| `invalid redirect_uri` (1101, 20022) | `healthkit.redirectUri` is not exactly the registered value. |
| `redirect_uri not registered` (1101, 20023) in Huawei Health | Add Huawei's fixed callback URL to the app (section 2.4). |
| 403 with code 121001 | Wrong regional site. Leave `healthkit.apiHost` empty or set the right host. |
| 403 "The query time is out of range" | The window starts before the authorization. Use a shorter window or a history scope. |
| Empty workout list | The Health Kit switch in Huawei Health is off (section 5.2). |
| 1203 on token refresh | The refresh token expired or was revoked. Authorize again. |
| No push arrives | Check `backend.baseUrl`, the backend logs, the subscription record and the push credentials. |

---

## 12. Official documentation

Wear Engine

- [Getting started](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/dev-process-0000001051058039)
- [Applying for the Wear Engine service](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/applying-wearengine-0000001050777982)
- [Integrating the Wear Engine SDK](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/integrating-phone-sdk-0000001051137958)
- [Requesting user authorization](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/requesting-user-authorization-0000001050819181)
- [App-to-app message communications](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/phone-send-message-0000001051059209)
- [Result codes](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/errocode-0000001054450278)
- [Version change history](https://developer.huawei.com/consumer/en/doc/connectivity-Guides/version-change-history-0000001086350238)

Health Kit Cloud

- [Authentication](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/auth-example-0000001054581058)
- [Login-free authorization, overview](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/cloud-free-auth-login-overview-0000002054076576)
- [Querying created exercise records](https://developer.huawei.com/consumer/en/doc/HMSCore-References/activityrecords_list-0000001050114862)
- [Subscribing to data](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/subscription-0000001078496860)
- [Canceling authorization](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/canceling-authorization-0000001209549384)
- [Applying for verification](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/verification-0000001211786611)

Push

- [Push Kit, obtaining a push token](https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/android-client-dev-0000001050042041)
- [Push Kit, sending downlink messages](https://developer.huawei.com/consumer/en/doc/HMSCore-References/https-send-api-0000001050986197)

Huawei can change APIs, limits and console names. Check the official pages before you go
live.
