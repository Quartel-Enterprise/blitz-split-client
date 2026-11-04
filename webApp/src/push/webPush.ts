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
  // Why: "Simplificar dívidas" turned on or off (13.7): who now pays you, whom you now pay, or neither.
  debts_simplified_to_you: 'push_debtsSimplifiedToYou',
  debts_simplified_from_you: 'push_debtsSimplifiedFromYou',
  debts_simplified_other: 'push_debtsSimplifiedOther',
  debts_unsimplified_to_you: 'push_debtsUnsimplifiedToYou',
  debts_unsimplified_from_you: 'push_debtsUnsimplifiedFromYou',
  debts_unsimplified_other: 'push_debtsUnsimplifiedOther',
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
    const applicationServerKey = base64UrlToBytes(vapidPublicKey)
    let subscription = await registration.pushManager.getSubscription()
    // Why: a subscription made with another VAPID key (one replaced since) is refused by its push service
    if (subscription && !isSameKey(subscription.options.applicationServerKey, applicationServerKey)) {
      await subscription.unsubscribe()
      subscription = null
    }
    subscription ??= await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey })
    const keys = subscription.toJSON().keys
    if (keys?.p256dh && keys.auth) setPushDevice(subscription.endpoint, keys.p256dh, keys.auth)
  } catch (error) {
    console.warn('Web Push is unavailable', error)
  }
}

/**
 * A notification tapped while this tab runs the app comes here from the service worker, which waits for the answer:
 * a tab still starting, or waiting for the one that holds the database, doesn't listen yet, so it opens another.
 */
export function listenToNotificationTaps(): void {
  if (!('serviceWorker' in navigator)) return
  navigator.serviceWorker.addEventListener('message', (event: MessageEvent<{ type?: string; link?: string }>) => {
    if (event.data?.type !== 'notification-link' || !event.data.link) return
    openNotificationLink(event.data.link)
    event.ports[0]?.postMessage('opened')
  })
}

/**
 * Why: the service worker can't read the app's dictionaries, so the page leaves it the words of its
 * language, with {1}, {2}… where the server's words go.
 */
async function storeWords(): Promise<void> {
  const words = {
    appName: t('appName'),
    reminderTitle: t('push_reminderTitle'),
    kinds: Object.fromEntries(Object.entries(kinds).map(([kind, key]) => [kind, t(key)])),
  }
  const cache = await caches.open(WORDS_CACHE)
  await cache.put(WORDS_URL, new Response(JSON.stringify(words), { headers: { 'Content-Type': 'application/json' } }))
}

function isSameKey(current: ArrayBuffer | null, expected: Uint8Array): boolean {
  if (!current) return false
  const bytes = new Uint8Array(current)
  return bytes.length === expected.length && bytes.every((byte, index) => byte === expected[index])
}

function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const base64 = (value + '='.repeat((4 - (value.length % 4)) % 4)).replace(/-/g, '+').replace(/_/g, '/')
  return Uint8Array.from(atob(base64), (char) => char.charCodeAt(0))
}
