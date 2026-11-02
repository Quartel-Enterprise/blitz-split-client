import { openNotificationLink, setPushDevice } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'

/**
 * Atividade's push notifications on the web (Web Push). The VAPID public key comes from the build
 * (`VITE_VAPID_PUBLIC_KEY`): without it the browser never subscribes and the app runs as before. The
 * subscription goes to the shared code once the person allowed notifications, which the toggles of
 * Perfil › Notificações ask for.
 */
const vapidPublicKey: string = import.meta.env.VITE_VAPID_PUBLIC_KEY ?? ''

const WORDS_CACHE = 'blitzsplit-push'
const WORDS_URL = '/push-words.json'

/** Each kind the server sends, worded by the service worker from these. */
const kinds: Record<string, MessageKey> = {
  bill_created: 'push_billCreated',
  bill_edited: 'push_billEdited',
  bill_deleted: 'push_billDeleted',
  comment_added: 'push_commentAdded',
  payment_paid_you: 'push_paymentPaidYou',
  payment_to_you: 'push_paymentToYou',
  payment_received_yours: 'push_paymentReceivedYours',
  payment_from_you: 'push_paymentFromYou',
  payment_undone: 'push_paymentUndone',
  charge_sent: 'push_chargeSent',
  settle_reminder: 'push_settleReminder',
  settle_reminders: 'push_settleReminders',
  added_to_group: 'push_addedToGroup',
  member_joined: 'push_memberJoined',
}

function isSupported(): boolean {
  return vapidPublicKey !== '' && 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window
}

/**
 * Subscribes once notifications are allowed, on every start (a browser may renew the subscription
 * while the app is closed) and right after the person allows them. Failures only mean no notifications.
 */
export async function startWebPush(): Promise<void> {
  if (!isSupported() || Notification.permission !== 'granted') return
  try {
    const registration = await navigator.serviceWorker.register('/push-sw.js')
    await storeWords()
    const subscription =
      (await registration.pushManager.getSubscription()) ??
      (await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: base64UrlToBytes(vapidPublicKey),
      }))
    const keys = subscription.toJSON().keys
    if (keys?.p256dh && keys.auth) setPushDevice(subscription.endpoint, keys.p256dh, keys.auth)
  } catch (error) {
    console.warn('Web Push is unavailable', error)
  }
}

/** A notification tapped while this tab is open comes here from the service worker. */
export function listenToNotificationTaps(): void {
  if (!('serviceWorker' in navigator)) return
  navigator.serviceWorker.addEventListener('message', (event: MessageEvent<{ type?: string; link?: string }>) => {
    if (event.data?.type === 'notification-link' && event.data.link) openNotificationLink(event.data.link)
  })
}

/**
 * Why: the service worker can't read the app's dictionaries, so the page leaves it the words of its
 * language, with {1}, {2}… where the server's words go.
 */
async function storeWords(): Promise<void> {
  const words = {
    reminderTitle: t('push_reminderTitle'),
    kinds: Object.fromEntries(Object.entries(kinds).map(([kind, key]) => [kind, t(key)])),
  }
  const cache = await caches.open(WORDS_CACHE)
  await cache.put(WORDS_URL, new Response(JSON.stringify(words), { headers: { 'Content-Type': 'application/json' } }))
}

function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const base64 = (value + '='.repeat((4 - (value.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0))
}
