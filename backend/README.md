# Wear Health Push

Receives Health Kit subscription notifications from Huawei and pushes them to the phone,
through Firebase on phones with Google services and through Push Kit on Huawei phones.

## Running

```bash
npm install
cp .env.example .env
npm start
```

Node 20.6 or newer, because `--env-file` reads `.env`.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/health` | Liveness, answers `{"ok":true}` |
| POST | `/devices` | The app registers `{openId, pushToken, provider}`, provider is `fcm` or `hms` |
| DELETE | `/devices/:openId` | Drops a registration |
| POST | `/subscriptions` | The app sends `{openId, accessToken}`, the server subscribes that user to Health Kit events |
| POST | `/push-test/:openId` | Sends a test push to a registered device |
| GET, POST | `/healthkit/notifications` | The callback Huawei calls, always answers 204 |

Huawei checks the callback with a connectivity test and expects 204, which is why the GET
answers as well.

## Notification handling

Huawei signs every notification with `x-notification-signature`, an HMAC-SHA256 of
`openId_eventType_eventTime` keyed with the Base64 decoded subscriber secret. A notification
with a wrong signature is dropped without a push. The signature check is skipped while
`HEALTH_SUBSCRIBER_SECRET` is empty, so the callback can be wired up before the secret is in
hand.

The callback answers 204 first and sends the push afterwards, so Huawei is never kept waiting
on Firebase or Push Kit. It answers 204 to every shape of request, including the empty body
and the malformed body the connectivity check may send, because Huawei treats anything else
as a failed callback. It parses the body itself instead of going through the JSON middleware
for that reason.

## Configuration

| Variable | Where it comes from |
| --- | --- |
| `HEALTH_SUBSCRIBER_ID` | Health Kit service card on HUAWEI Developers, after the callback URL is registered |
| `HEALTH_SUBSCRIBER_SECRET` | The same card, Base64, used for the signature |
| `HEALTH_CLIENT_ID` | The Health Kit App ID, sent as `x-client-id` |
| `HEALTH_EVENT_TYPE` | `ACTIVITY_RECORD_EVENT$UPDATE` fires when a workout is added or changed |
| `FCM_SERVICE_ACCOUNT_FILE` | Firebase console, Project settings, Service accounts, generate a private key |
| `HMS_APP_ID`, `HMS_APP_SECRET` | AppGallery Connect, the app that owns Push Kit |

The device registry is a JSON file at `STORE_FILE`, one entry per `openId`. There is no
database because one file is enough for a demo and keeps the deployment to a single process.
