/*
 * Atividade's push notifications on the web. The server sends { kind, title, args, link, tag }, never a sentence:
 * the words come from the app's dictionaries, which the page leaves in the cache (src/push/webPush.ts). A tap hands
 * the link to an open tab of the app, or opens one on it.
 */
const WORDS_CACHE = 'blitzsplit-push'
const WORDS_URL = '/push-words.json'

async function readWords() {
  try {
    const response = await caches.match(WORDS_URL, { cacheName: WORDS_CACHE })
    return response ? await response.json() : null
  } catch {
    return null
  }
}

function fill(template, args) {
  return template.replace(/\{(\d+)\}/g, (match, index) => args[Number(index) - 1] ?? match)
}

self.addEventListener('push', (event) => {
  event.waitUntil(
    (async () => {
      const message = event.data ? event.data.json() : null
      if (!message) return
      const words = await readWords()
      const template = words?.kinds?.[message.kind]
      // A kind this version doesn't know, or no words yet: the group's name at least, as a push must show something
      const body = template ? fill(template, message.args ?? []) : ''
      const title = message.title || words?.reminderTitle || 'Blitz Split'
      await self.registration.showNotification(title, {
        body,
        tag: message.tag,
        icon: '/icon-192.png',
        data: { link: message.link },
      })
    })(),
  )
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const link = event.notification.data?.link
  if (!link) return
  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true })
      const open = windows.find((client) => new URL(client.url).origin === self.location.origin)
      if (open) {
        await open.focus()
        open.postMessage({ type: 'notification-link', link })
        return
      }
      // The app's own address, wherever it is hosted (a preview, a local server)
      const target = new URL(link)
      await self.clients.openWindow(new URL(target.pathname + target.search, self.location.origin).href)
    })(),
  )
})
