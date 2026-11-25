import { createHmac } from 'node:crypto'
import { config } from './config.js'

const [type, subType] = config.healthKit.eventType.split('$')

const site = config.healthKit.apiHost

const isSiteCross = (status, body) =>
  status === 403 && (body.includes('121001') || body.includes('site cross'))

// The Location header carries the user's site, e.g. https://health-api.cloud.huawei.eu/...
export const siteFromLocation = (location) => {
  if (!location) return null
  try {
    return new URL(location.includes('://') ? location : `https://${location}`).host || null
  } catch {
    return null
  }
}

const callSite = async (host, path, accessToken, init) => {
  const response = await fetch(`https://${host}${path}`, {
    ...init,
    redirect: 'manual',
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      Authorization: `Bearer ${accessToken}`,
      'x-client-id': config.healthKit.clientId,
      ...init.headers,
    },
  })

  return { response, body: await response.text() }
}

// A token only works on its user's site, so the site is decided per call, not cached
const call = async (path, accessToken, init) => {
  const first = await callSite(site, path, accessToken, init)
  if (!isSiteCross(first.response.status, first.body)) return first

  const redirect = siteFromLocation(first.response.headers.get('location'))
  if (!redirect || redirect === site) return first

  console.log(`HealthKit siteCross | from: ${site} | to: ${redirect}`)
  return callSite(redirect, path, accessToken, init)
}

export const subscribe = async (accessToken) => {
  const { response, body } = await call('/healthkit/v2/subscriptions', accessToken, {
    method: 'POST',
    body: JSON.stringify({
      subscriberId: config.healthKit.subscriberId,
      eventTypes: [{ type, subType, eventType: config.healthKit.eventType }],
    }),
  })

  if (!response.ok) throw new Error(`Health Kit answered ${response.status}: ${body}`)

  return JSON.parse(body || '[]')
}

export const unsubscribe = async (accessToken, subscriptionId) => {
  const { response, body } = await call(
    `/healthkit/v2/subscriptions/${subscriptionId}`,
    accessToken,
    { method: 'DELETE' },
  )

  if (!response.ok) throw new Error(`Health Kit answered ${response.status}: ${body}`)
}

export const signatureOf = ({ openId, eventType, eventTime }) =>
  createHmac('sha256', Buffer.from(config.healthKit.subscriberSecret, 'base64'))
    .update(`${openId}_${eventType}_${eventTime}`)
    .digest('base64')

export const signatureMatches = (notification, signature) => {
  if (!config.healthKit.subscriberSecret) return true
  return signature === signatureOf(notification)
}

export const readMeta = (notification, key) =>
  notification.metaData?.find((entry) => entry.metaKey === key)?.metaValue ?? null
