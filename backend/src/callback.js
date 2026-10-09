import express, { Router } from 'express'
import { signatureMatches } from './health-kit.js'
import { notifyDevice } from './notifications.js'

const anyBody = express.text({ type: () => true, limit: '1mb' })

const asJson = (body) => {
  if (typeof body !== 'string' || !body.trim()) return {}

  try {
    return JSON.parse(body)
  } catch {
    return {}
  }
}

export const callbackRouter = Router()

callbackRouter.get('/healthkit/notifications', (_request, response) => response.sendStatus(204))

callbackRouter.post('/healthkit/notifications', anyBody, (request, response) => {
  response.sendStatus(204)

  const notification = asJson(request.body)
  if (!notification.openId) return console.log('Connectivity check answered')

  console.log(`Notification ${notification.eventType} for ${notification.openId}`)
  if (!signatureMatches(notification, request.get('x-notification-signature'))) {
    return console.warn(`Dropped ${notification.eventType}, signature did not match`)
  }

  notifyDevice(notification).catch((error) => console.error('Push failed', error))
})
