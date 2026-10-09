import { Router } from 'express'
import { subscribe } from './health-kit.js'
import { workoutMessage } from './notifications.js'
import { findDevice, listDevices, removeDevice, saveDevice } from './store.js'
import { sendPush } from './push/index.js'

const route = (handler) => (request, response, next) => handler(request, response).catch(next)

export const router = Router()

router.get('/health', (_request, response) => response.json({ ok: true }))

router.get('/devices', route(async (_request, response) => response.json(await listDevices())))

router.post('/devices', route(async (request, response) => {
  const { openId, pushToken, provider } = request.body
  if (!openId || !pushToken) return response.status(400).json({ error: 'openId and pushToken are required' })

  const device = await saveDevice({ openId, pushToken, provider: provider ?? 'fcm' })
  return response.json(device)
}))

router.delete('/devices/:openId', route(async (request, response) => {
  const removed = await removeDevice(request.params.openId)
  return response.sendStatus(removed ? 204 : 404)
}))

router.post('/subscriptions', route(async (request, response) => {
  const { openId, accessToken } = request.body
  if (!openId || !accessToken) return response.status(400).json({ error: 'openId and accessToken are required' })

  const subscriptions = await subscribe(accessToken)
  await saveDevice({ openId, subscriptionId: subscriptions[0]?.subscriptionId ?? null })
  return response.json(subscriptions)
}))

router.post('/push-test/:openId', route(async (request, response) => {
  const device = await findDevice(request.params.openId)
  if (!device) return response.sendStatus(404)

  const id = await sendPush(device, workoutMessage({ eventType: 'TEST', metaData: [] }))
  return response.json({ id })
}))

