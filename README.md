> **Note:** To access all shared projects, get information about environment setup, and view other guides, please visit [Explore-In-HMOS-Wearable Index](https://github.com/Explore-In-HMOS-Wearable/hmos-index).

# Wear Engine and Huawei Health Kit demo

An Android app, with a small Node.js backend, that shows the two ways a phone app works with
a Huawei watch:

- **Wear Engine**: the phone app talks to its own app on the watch. It finds the watch,
  checks the watch app with ping, and sends and receives P2P messages.
- **Huawei Health Kit Cloud**: the phone app reads the user's workouts from the Huawei
  cloud, after the user grants access, and gets a push notification when a new workout
  arrives.

```
Wear Engine     phone app  <->  Huawei Health app  <->  watch app        (Bluetooth)
Health Kit      watch  ->  Huawei Health app  ->  Huawei cloud  ->  phone app / backend  (REST)
```

The full walkthrough, from the console setup to production notes, is in
**[docs/DEVELOPMENT_GUIDE.md](docs/DEVELOPMENT_GUIDE.md)**.

## What the app does

| Tab | Feature | Code |
| --- | --- | --- |
| Wear Engine | Ask for the `DEVICE_MANAGER` permission | `managers/AuthManager.kt` |
| Wear Engine | List the paired watches and pick one | `managers/DeviceManager.kt` |
| Wear Engine | Switch between a lite and a smart watch app (top right) | `managers/WatchType.kt` |
| Wear Engine | Ping the watch app, send and receive messages, send a test file | `managers/P2pManager.kt` |
| Health Kit | Authorize with Web OAuth or the login-free SDK | `healthkit/auth/` |
| Health Kit | Read the workouts of the last 7 days to 1 year | `healthkit/api/ActivityRecordsApi.kt` |
| Health Kit | Turn on workout push notifications | `push/WorkoutSubscriptions.kt` |
| Health Kit | Delete the app's data and cancel the authorization | `healthkit/api/DataDeletionApi.kt` |
| Backend | Receive Huawei's subscription callback and send the push | `backend/` |

## Quick start

1. Register your app on HUAWEI Developers and apply for Wear Engine and Health Service Kit.
   See [Console setup](docs/DEVELOPMENT_GUIDE.md#2-console-setup).
2. Copy `local.properties.example` to `local.properties` and fill in your values.
3. Copy `keystore.properties.example` to `keystore.properties` and point it at the keystore
   whose SHA-256 fingerprint you registered. Wear Engine rejects the debug key.
4. For push: add your own `app/agconnect-services.json` (Push Kit) and, for phones with
   Google services, `app/google-services.json` (Firebase).
5. Open the project in Android Studio and run it on a real phone that has the Huawei Health
   app, paired with a Huawei watch.
6. For workout notifications, run the backend: see [backend/README.md](backend/README.md).

No ID, secret or key is in this repository. `local.properties`, `keystore.properties`, the
two JSON files and `backend/.env` are all in `.gitignore`.

## Project layout

```
app/                    Android app (Kotlin, Jetpack Compose)
  managers/             Wear Engine: permission, devices, P2P
  healthkit/            Health Kit Cloud: authorization, tokens, workouts, deletion
  push/                 push token and backend registration
  ui/, viewmodels/      screens and state
backend/                Node.js service for the Health Kit subscription and the push
docs/                   development guide
```

## Requirements

| | Version |
| --- | --- |
| Android Studio | A recent version with AGP 8.9 |
| Android | minSdk 23, compileSdk and targetSdk 35 |
| Wear Engine SDK | `com.huawei.hms:wearengine:5.0.3.305` |
| Health Kit login-free SDK | `com.huawei.hms:healthlite:6.16.0.300` |
| Huawei Health on the phone | 14.0.12.310 or later |
| Node.js (backend) | 20.6 or later |

## Related samples

- [wear-engine-mobile-to-watch](https://github.com/Explore-In-HMOS-Wearable/wear-engine-mobile-to-watch), the Wear Engine part on its own
- [wear-engine-ios-to-watch-file-message](https://github.com/Explore-In-HMOS-Wearable/wear-engine-ios-to-watch-file-message), the same for iOS

## License

See [LICENSE](LICENSE).
