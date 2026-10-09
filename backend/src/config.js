const required = (key) => {
  const value = process.env[key]
  if (!value) throw new Error(`${key} is missing from .env`)
  return value
}

export const config = {
  host: process.env.HOST ?? '127.0.0.1',
  port: Number(process.env.PORT ?? 3000),
  storeFile: process.env.STORE_FILE ?? 'data/devices.json',

  healthKit: {
    apiHost: process.env.HEALTH_API_HOST ?? 'health-api.cloud.huawei.com',
    subscriberId: process.env.HEALTH_SUBSCRIBER_ID ?? '',
    subscriberSecret: process.env.HEALTH_SUBSCRIBER_SECRET ?? '',
    clientId: process.env.HEALTH_CLIENT_ID ?? '',
    eventType: process.env.HEALTH_EVENT_TYPE ?? 'ACTIVITY_RECORD_EVENT$UPDATE',
  },

  fcm: {
    serviceAccountFile: process.env.FCM_SERVICE_ACCOUNT_FILE ?? '',
  },

  hms: {
    appId: process.env.HMS_APP_ID ?? '',
    appSecret: process.env.HMS_APP_SECRET ?? '',
  },
}

export const requireHealthKitConfig = () => ({
  subscriberId: required('HEALTH_SUBSCRIBER_ID'),
  subscriberSecret: required('HEALTH_SUBSCRIBER_SECRET'),
})
