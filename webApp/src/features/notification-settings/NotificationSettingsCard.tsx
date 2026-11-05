import { Bell, BellOff, Smartphone } from 'lucide-react'
import { useEffect } from 'react'
import {
  NotificationSettingsPresenter,
  type JsNotificationSettingsAction,
  type JsNotificationSettingsState,
} from 'blitzsplit-shared'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Skeleton } from '@/components/ui/skeleton'
import { Switch } from '@/components/ui/switch'
import { t, type MessageKey } from '@/i18n'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { cn } from '@/lib/utils'
import { startWebPush } from '@/push/webPush'

const labels: Record<string, { title: MessageKey; detail: MessageKey }> = {
  BILLS: { title: 'notifications_bills', detail: 'notifications_billsDetail' },
  PAYMENTS: { title: 'notifications_payments', detail: 'notifications_paymentsDetail' },
  COMMENTS: { title: 'notifications_comments', detail: 'notifications_commentsDetail' },
  PEOPLE_AND_GROUPS: { title: 'notifications_peopleAndGroups', detail: 'notifications_peopleAndGroupsDetail' },
  PAYMENT_REQUESTS: { title: 'notifications_paymentRequests', detail: 'notifications_paymentRequestsDetail' },
  BALANCE_REMINDER: { title: 'notifications_balanceReminder', detail: 'notifications_balanceReminderDetail' },
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
 * The presenter and its state, with the browser's permission read on open and whenever the tab comes back. Asking for
 * it (a push turned on while never asked) also subscribes this browser once allowed.
 */
function useNotificationSettings() {
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
  return { presenter, state }
}

/**
 * "Notifications" inside the profile, as the canvas draws the web: a row per kind of Activity (and the weekly
 * reminder), each with a Push and an In app switch, saved with the account.
 */
export function NotificationSettingsCard({ className }: { className?: string }) {
  const { presenter, state } = useNotificationSettings()
  if (!presenter || !state) return null
  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>{t('notifications_title')}</CardTitle>
        <CardDescription>{t('notifications_footer')}</CardDescription>
      </CardHeader>
      <CardContent>
        <NotificationSettingsBody presenter={presenter} state={state} />
      </CardContent>
    </Card>
  )
}

/**
 * The same settings from Activity's "Notification settings": a dialog over Activity from the tablet up, a drawer on
 * the phone. Closing it any way goes back to Activity.
 */
export function NotificationSettingsDialog() {
  const { presenter, state } = useNotificationSettings()
  const isDesktop = useMediaQuery('(min-width: 768px)')
  // Why: drawn once the presenter exists, so a close always has somewhere to go back from.
  if (!presenter) return null
  const onOpenChange = (open: boolean) => {
    if (!open) presenter.back()
  }
  const body = state ? <NotificationSettingsBody presenter={presenter} state={state} /> : null
  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[600px]">
        <DialogHeader className="pr-8 text-left">
          <DialogTitle>{t('notifications_title')}</DialogTitle>
          <DialogDescription>{t('notifications_footer')}</DialogDescription>
        </DialogHeader>
        {body}
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{t('notifications_title')}</DrawerTitle>
          <DrawerDescription>{t('notifications_footer')}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{body}</div>
        <DrawerFooter>
          <Button variant="outline" className="h-11" onClick={() => presenter.back()}>
            {t('common_close')}
          </Button>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

// Why: the column headers, in the order of each item's toggles (the shared channels, Push then In app).
const channels = [
  { name: 'PUSH', label: 'notifications_push', icon: Smartphone },
  { name: 'IN_APP', label: 'notifications_inApp', icon: Bell },
] as const

/**
 * The blocked notice, which only stops the pushes, then the rows with their two switches under "Push" and "In app".
 * A browser can't open its own settings, so the notice says where to allow them instead of a button.
 */
function NotificationSettingsBody({ presenter, state }: { presenter: NotificationSettingsPresenter; state: JsNotificationSettingsState }) {
  return (
    <div className="flex flex-col gap-3">
      {state.showsPermissionDenied && (
        <div role="status" className="flex items-start gap-3 rounded-lg border bg-muted/50 p-3 text-sm">
          <BellOff className="mt-0.5 size-4 shrink-0 text-destructive" />
          <p>
            <strong className="font-semibold">{t('notifications_deniedTitle')}</strong> {t('notifications_deniedBody')}
          </p>
        </div>
      )}
      <div aria-busy={state.showsTogglesLoading} className="flex flex-col divide-y rounded-lg border">
        <div className="flex items-end gap-3 px-3 pt-3 pb-2">
          <span className="flex-1 text-xs font-medium text-muted-foreground">{t('notifications_section')}</span>
          {channels.map(({ name, label, icon: Icon }) => (
            <span key={name} className="flex w-12 flex-col items-center gap-1 text-xs font-medium text-muted-foreground">
              <Icon aria-hidden className="size-4" />
              {t(label)}
            </span>
          ))}
        </div>
        {state.items.map((item) => {
          const label = labels[item.type]
          if (!label) return null
          const title = t(label.title)
          return (
            <div key={item.type} className="flex min-h-16 items-center gap-3 px-3 py-2.5">
              <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                <span className="text-sm font-medium">{title}</span>
                <span className="text-xs text-muted-foreground">{t(label.detail)}</span>
              </span>
              {channels.map(({ name, label: channelLabel }, index) => {
                const toggle = item.toggles[index]
                return (
                  <span key={name} className="flex w-12 justify-center">
                    {!toggle ? null : state.showsTogglesLoading ? (
                      <Skeleton aria-hidden className="h-5 w-9 rounded-full" />
                    ) : (
                      <Switch
                        aria-label={t('notifications_toggleLabel', { title, channel: t(channelLabel) })}
                        checked={toggle.isOn}
                        disabled={!toggle.isEnabled}
                        className={cn(!toggle.isEnabled && 'opacity-50')}
                        onCheckedChange={(checked) => presenter.toggle(item.type, toggle.channel, checked)}
                      />
                    )}
                  </span>
                )
              })}
            </div>
          )
        })}
      </div>
      <p className="px-1 text-xs text-muted-foreground">{t('notifications_legend')}</p>
    </div>
  )
}
