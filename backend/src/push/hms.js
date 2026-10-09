import { config } from '../config.js'

const TOKEN_URL = 'https://oauth-login.cloud.huawei.com/oauth2/v3/token'
const PUSH_URL = (appId) => `https://push-api.cloud.huawei.com/v1/${appId}/messages:send`

let cached = { value: '', expiresAt: 0 }

const accessToken = async () => {
  if (cached.value && Date.now() < cached.expiresAt) return cached.value

  const response = await fetch(TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'client_credentials',
      client_id: config.hms.appId,
      client_secret: config.hms.appSecret,
    }),
  })

  const body = await response.json()
  if (!response.ok) throw new Error(`HMS token answered ${response.status}: ${JSON.stringify(body)}`)

  cached = {
    value: body.access_token,
    expiresAt: Date.now() + (body.expires_in - 60) * 1000,
  }
  return cached.value
}

export const sendHms = async (token, message) => {
  const response = await fetch(PUSH_URL(config.hms.appId), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      Authorization: `Bearer ${await accessToken()}`,
    },
    body: JSON.stringify({
      validate_only: false,
      message: {
        token: [token],
        android: {
          urgency: 'HIGH',
          category: 'WORK',
          data: JSON.stringify(message.data),
          notification: {
            title: message.title,
            body: message.body,
            click_action: { type: 3 },
          },
        },
      },
    }),
  })

  const body = await response.json()
  if (body.code !== '80000000') {
    throw new Error(`HMS push answered ${body.code}: ${body.msg}`)
  }

  return body.requestId
}
