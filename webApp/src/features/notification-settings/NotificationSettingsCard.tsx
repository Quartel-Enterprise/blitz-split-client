import { BellOff } from 'lucide-react'
import { useEffect } from 'react'
import {
  NotificationSettingsPresenter,
  type JsNotificationSettingsAction,
  type JsNotificationSettingsState,
} from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { LoadingRegion, Skeleton, SkeletonLine } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'
import { startWebPush } from '@/push/webPush'

const labels: Record<string, { title: MessageKey; detail: MessageKey }> = {
  NEW_EXPENSE: { title: 'notifications_newExpense', detail: 'notifications_newExpenseDetail' },
  PAYMENT_MARKED: { title: 'notifications_paymentMarked', detail: 'notifications_paymentMarkedDetail' },
  SETTLE_REMINDERS: { title: 'notifications_settleReminders', detail: 'notifications_settleRemindersDetail' },
  INVITES: { title: 'notifications_invites', detail: 'notifications_invitesDetail' },
}

/**
 * The NotificationPermission's name for the browser's. A browser with no notifications at all reports NOT_DETERMINED,
 * so the toggles, which wait for a permission to be known, still come.
 */
function readPermission(): string {
  if (!('Notification' in window)) return 'NOT_DETERMINED'
  switch (Notification.permission) {
    case 'granted':
      return 'GRANTED'
    case 'denied':
      return 'DENIED'
    default:
      return 'NOT_DETERMINED'
  }
}

/**
 * "Notifications" inside the profile, as the canvas draws the web: the four toggles, saved with the account.
 * A browser can't open its own settings, so the blocked notice says where to allow them instead of a button.
 */
export function NotificationSettingsCard({ className }: { className?: string }) {
  const presenter = usePresenter(() => new NotificationSettingsPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsNotificationSettingsState) => void) => p.observeState(onChange))
  useActions(
    presenter,
    (p, onAction: (action: JsNotificationSettingsAction) => void) => p.observeActions(onAction),
    (action) => {
      if (action.type !== 'request-permission' || !('Notification' in window)) return
      void Notification.requestPermission().then(() => {
        presenter?.permissionChecked(readPermission())
        void startWebPush()
      })
    },
  )
  // Why: read again whenever the tab comes back, since the person may allow or block it in the site settings.
  useEffect(() => {
    if (!presenter) return
    let lastPermission = readPermission()
    const check = () => {
      const permission = readPermission()
      presenter.permissionChecked(permission)
      // Allowed in the site settings meanwhile: subscribe now rather than on the next load
      if (permission === 'GRANTED' && lastPermission !== 'GRANTED') void startWebPush()
      lastPermission = permission
    }
    check()
    window.addEventListener('focus', check)
    return () => window.removeEventListener('focus', check)
  }, [presenter])
  if (!presenter || !state) return null

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>{t('notifications_title')}</CardTitle>
        <CardDescription>{t('notifications_footer')}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {state.showsPermissionDenied && (
          <div role="status" className="flex items-start gap-3 rounded-lg border bg-muted/50 p-3 text-sm">
            <BellOff className="mt-0.5 size-4 shrink-0 text-destructive" />
            <p>
              <strong className="font-semibold">{t('notifications_deniedTitle')}</strong> {t('notifications_deniedBody')}
            </p>
          </div>
        )}
        {state.showsTogglesLoading ? (
          <TogglesSkeleton count={Math.max(state.items.length, 4)} />
        ) : (
          <div className={cn('flex flex-col divide-y rounded-lg border', !state.areTogglesEnabled && 'opacity-50')}>
            {state.items.map((item) => {
              const label = labels[item.type]
              const id = `notification-${item.type}`
              return (
                <label key={item.type} htmlFor={id} className="flex min-h-14 items-center gap-3 px-3 py-2">
                  <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                    <span className="text-sm font-medium">{t(label.title)}</span>
                    <span className="text-xs text-muted-foreground">{t(label.detail)}</span>
                  </span>
                  <Switch
                    id={id}
                    checked={item.isEnabled}
                    disabled={!state.areTogglesEnabled}
                    onCheckedChange={(checked) => presenter.toggle(item.type, checked)}
                  />
                </label>
              )
            })}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

/** The toggles while the choices or the browser's permission are still read: a title, its detail and a switch each. */
function TogglesSkeleton({ count }: { count: number }) {
  return (
    <LoadingRegion className="flex flex-col divide-y rounded-lg border">
      {Array.from({ length: count }, (_, index) => (
        <span key={index} className="flex min-h-14 items-center gap-3 px-3 py-2">
          <span className="flex flex-1 flex-col gap-2">
            <SkeletonLine className={index % 2 === 0 ? 'w-1/2' : 'w-2/5'} />
            <SkeletonLine className="h-2.5 w-3/4" />
          </span>
          <Skeleton className="h-5 w-9 rounded-full" />
        </span>
      ))}
    </LoadingRegion>
  )
}
