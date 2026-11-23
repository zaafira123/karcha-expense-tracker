const SHARE_PATHS = new Set(['/share-target', '/import'])

self.addEventListener('fetch', event => {
  const requestUrl = new URL(event.request.url)
  if (event.request.method !== 'POST' || !SHARE_PATHS.has(requestUrl.pathname)) return
  event.respondWith(handleShare(event.request))
})

async function handleShare(request) {
  const form = await request.formData()
  const sharedText = [form.get('title'), form.get('text'), form.get('url')]
    .filter(value => typeof value === 'string' && value.trim())
    .join('\n')
    .trim()
  if (!sharedText) return new Response('No message was shared.', { status: 400, headers: { 'Content-Type': 'text/plain; charset=utf-8' } })

  const existing = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
  let client = existing.find(item => new URL(item.url).origin === self.location.origin) || null
  if (client) {
    await client.navigate('/import')
  } else {
    client = await self.clients.openWindow('/import')
  }

  if (client) await deliverShare(client, sharedText)
  return new Response('Kharcha received the shared message.', { status: 200, headers: { 'Content-Type': 'text/plain; charset=utf-8' } })
}

async function deliverShare(client, text) {
  for (let attempt = 0; attempt < 20; attempt++) {
    const channel = new MessageChannel()
    const acknowledged = new Promise(resolve => {
      const timeout = setTimeout(() => resolve(false), 300)
      channel.port1.onmessage = event => {
        clearTimeout(timeout)
        resolve(event.data?.type === 'KHARCHA_SHARE_RECEIVED')
      }
    })
    client.postMessage({ type: 'KHARCHA_SHARED_SMS', text }, [channel.port2])
    if (await acknowledged) return
    const refreshed = await self.clients.get(client.id)
    if (refreshed) client = refreshed
  }
}
