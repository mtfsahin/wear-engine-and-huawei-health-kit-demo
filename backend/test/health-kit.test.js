import { test, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import { siteFromLocation, subscribe } from '../src/health-kit.js'

const realFetch = globalThis.fetch
afterEach(() => {
  globalThis.fetch = realFetch
})

const siteCross = (location) =>
  new Response(JSON.stringify({ error: { code: 121001, message: 'request forbidden due to site cross' } }), {
    status: 403,
    headers: location ? { Location: location } : {},
  })

test('siteFromLocation reads the host from a full URL or a bare host', () => {
  assert.equal(
    siteFromLocation('https://health-api.cloud.huawei.eu/healthkit/v2/subscriptions'),
    'health-api.cloud.huawei.eu',
  )
  assert.equal(siteFromLocation('health-api.cloud.huawei.asia'), 'health-api.cloud.huawei.asia')
  assert.equal(siteFromLocation(''), null)
  assert.equal(siteFromLocation(null), null)
})

test('a site cross answer is retried once on the Location host', async () => {
  const urls = []
  globalThis.fetch = async (url) => {
    urls.push(url)
    return urls.length === 1
      ? siteCross('https://health-api.cloud.huawei.eu/healthkit/v2/subscriptions')
      : new Response('[]', { status: 200 })
  }

  assert.deepEqual(await subscribe('token'), [])
  assert.equal(urls.length, 2)
  assert.equal(new URL(urls[1]).host, 'health-api.cloud.huawei.eu')
})

test('without a Location header the site cross error is returned, no other host is tried', async () => {
  const urls = []
  globalThis.fetch = async (url) => {
    urls.push(url)
    return siteCross(null)
  }

  await assert.rejects(subscribe('token'), /403/)
  assert.equal(urls.length, 1)
})
