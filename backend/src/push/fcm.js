import { readFile } from 'node:fs/promises'
import admin from 'firebase-admin'
import { config } from '../config.js'

let app = null

const firebase = async () => {
  if (app) return app
  if (!config.fcm.serviceAccountFile) throw new Error('FCM_SERVICE_ACCOUNT_FILE is missing')

  const credentials = JSON.parse(await readFile(config.fcm.serviceAccountFile, 'utf8'))
  app = admin.initializeApp({ credential: admin.credential.cert(credentials) })
  return app
}

export const sendFcm = async (token, message) => {
  const messaging = admin.messaging(await firebase())

  return messaging.send({
    token,
    notification: { title: message.title, body: message.body },
    data: message.data,
    android: { priority: 'high' },
  })
}
