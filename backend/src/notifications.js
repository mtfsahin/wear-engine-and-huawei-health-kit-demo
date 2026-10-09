import { readMeta } from './health-kit.js'
import { findDevice } from './store.js'
import { sendPush } from './push/index.js'

const minutesBetween = (start, end) => {
  if (!start || !end) return null
  return Math.max(1, Math.round((Number(end) - Number(start)) / 60000))
}

export const workoutMessage = (notification) => {
  const startTime = readMeta(notification, 'startTime')
  const endTime = readMeta(notification, 'endTime')
  const minutes = minutesBetween(startTime, endTime)

  return {
    title: 'New workout',
    body: minutes ? `A ${minutes} minute workout is ready in Huawei Health.` : 'A new workout is ready in Huawei Health.',
    data: {
      eventType: notification.eventType ?? '',
      activityType: readMeta(notification, 'activityType') ?? '',
      startTime: startTime ?? '',
      endTime: endTime ?? '',
    },
  }
}

export const notifyDevice = async (notification) => {
  const device = await findDevice(notification.openId)
  if (!device) return console.log(`No device registered for ${notification.openId}`)
  if (!device.pushToken) return console.log(`No push token for ${notification.openId}`)

  const id = await sendPush(device, workoutMessage(notification))
  console.log(`Pushed ${notification.eventType} to ${device.provider}: ${id}`)
}
