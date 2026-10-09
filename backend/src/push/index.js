import { sendFcm } from './fcm.js'
import { sendHms } from './hms.js'

export const sendPush = (device, message) =>
  device.provider === 'hms'
    ? sendHms(device.pushToken, message)
    : sendFcm(device.pushToken, message)
