# Deploying

The service is a single Node.js process. It listens on `127.0.0.1:3000` only, and a reverse
proxy in front of it terminates TLS. Huawei calls the callback over HTTPS, so the server
needs a domain name and a certificate from a public certificate authority.

The files in this folder show one way to run it: a systemd unit and Caddy, which gets and
renews a Let's Encrypt certificate on its own.

## Steps

1. Copy the service to the server, for example to `/opt/wear-health-push`:

   ```bash
   scp -r ../src ../package.json ../package-lock.json user@your-server:/opt/wear-health-push/
   ssh user@your-server "cd /opt/wear-health-push && npm ci --omit=dev"
   ```

2. Create `/opt/wear-health-push/.env` from `../.env.example` and fill in your values. Keep
   it readable by the service user only (`chmod 600`). It is never part of the repository.

3. Put `wear-health-push.service` in `/etc/systemd/system/`, then start it:

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl enable --now wear-health-push
   ```

4. Put `Caddyfile` in `/etc/caddy/Caddyfile`, replace `your-domain.example.com` with your
   domain, and reload Caddy.

5. Check it:

   ```bash
   systemctl status wear-health-push
   journalctl -u wear-health-push -f
   curl https://your-domain.example.com/health
   ```

## What Huawei needs

On the Health Service Kit card on HUAWEI Developers, register the subscription capability in
HTTP mode with this callback URL:

```
https://your-domain.example.com/healthkit/notifications
```

The path must end with `/healthkit/notifications`. The console runs a connectivity test and
expects 204, which the service answers on both GET and POST. The card then shows a
subscriber ID and a subscriber secret: they go into `HEALTH_SUBSCRIBER_ID` and
`HEALTH_SUBSCRIBER_SECRET`.

## What the push providers need

| Provider | What to set | Where it comes from |
| --- | --- | --- |
| Push Kit | `HMS_APP_ID`, `HMS_APP_SECRET` | AppGallery Connect, your app, App information |
| Firebase | `FCM_SERVICE_ACCOUNT_FILE`, the path to a service account key | Firebase console, Project settings, Service accounts |

A phone needs one of the two. Huawei phones with HMS Core use Push Kit, phones with Google
services use Firebase, and the app picks whichever returns a token.
