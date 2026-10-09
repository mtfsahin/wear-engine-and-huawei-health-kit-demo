import { createHmac } from 'node:crypto'
import { config } from './config.js'

const FALLBACK_HOSTS = [
  'health-api.cloud.huawei.com',
  'health-api.cloud.huawei.eu',
  'health-api.cloud.huawei.asia',
  'health-api.cloud.huawei.ru',
  'health-api.cloud.huawei.com.cn',
]

const [type, subType] = config.healthKit.eventType.split('$')

let site = config.healthKit.apiHost

const isSiteCross = (status, body) => status === 403 && body.includes('site cross')

const callSite = async (host, path, accessToken, init) => {
  const response = await fetch(`https://${host}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json; charset=UTF-8',
      Authorization: `Bearer ${accessToken}`,
      'x-client-id': config.healthKit.clientId,
      ...init.headers,
    },
  })

  return { response, body: await response.text() }
}

const tryCallSite = async (host, path, accessToken, init) => {
  try {
    return await callSite(host, path, accessToken, init)
  } catch (error) {
    console.warn(`Health Kit site ${host} did not answer: ${error.cause?.code ?? error.message}`)
    return null
  }
}

const call = async (path, accessToken, init) => {
  const first = await tryCallSite(site, path, accessToken, init)
  if (first && !isSiteCross(first.response.status, first.body)) return first

  for (const host of FALLBACK_HOSTS) {
    if (host === site) continue

    const retry = await tryCallSite(host, path, accessToken, init)
    if (!retry || isSiteCross(retry.response.status, retry.body)) continue

    console.log(`Health Kit site moved from ${site} to ${host}`)
    site = host
    return retry
  }

  if (first) return first
  throw new Error('No Health Kit site answered')
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
