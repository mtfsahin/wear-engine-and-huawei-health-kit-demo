import { readFile, writeFile, mkdir } from 'node:fs/promises'
import { dirname } from 'node:path'
import { config } from './config.js'

let devices = null

const load = async () => {
  if (devices) return devices

  const raw = await readFile(config.storeFile, 'utf8').catch(() => '{}')
  devices = JSON.parse(raw)
  return devices
}

const persist = async () => {
  await mkdir(dirname(config.storeFile), { recursive: true })
  await writeFile(config.storeFile, JSON.stringify(devices, null, 2))
}

export const saveDevice = async (device) => {
  const all = await load()
  all[device.openId] = { ...all[device.openId], ...device, updatedAt: Date.now() }
  await persist()
  return all[device.openId]
}

export const findDevice = async (openId) => (await load())[openId] ?? null

export const listDevices = async () => Object.values(await load())

export const removeDevice = async (openId) => {
  const all = await load()
  if (!all[openId]) return false

  delete all[openId]
  await persist()
  return true
}
