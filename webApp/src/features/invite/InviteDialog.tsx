import { AlertCircle, Check, Copy, Loader2, Share2, WifiOff } from 'lucide-react'
import { InvitePresenter, type JsInviteAction, type JsInviteState, type JsRoute } from 'blitzsplit-shared'
import { toast } from 'sonner'
import { useActions, usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { locale, t } from '@/i18n'
import { copyLink, shareOrCopy } from '@/lib/share'
import { cn } from '@/lib/utils'

const linkId = 'invite-link'

// Why: the blank border a scanner needs around the code, in modules.
const qrQuietZone = 2

/**
 * "Convidar", over the home right after a group is created, or over the group: a dialog on desktop and tablet, a
 * drawer on the phone. The link and the QR code sit in two tabs; closing it any way is the ViewModel's close.
 */
export function InviteDialog({ route }: { route: JsRoute }) {
  const presenter = usePresenter(() => new InvitePresenter(route))
  const state = useWatch(presenter, (p, onChange: (state: JsInviteState) => void) => p.observeState(onChange))
  const isDesktop = useMediaQuery('(min-width: 768px)')
  useActions(presenter, (p, onAction: (action: JsInviteAction) => void) => p.observeActions(onAction), (action) => {
    if (action.type === 'copy' && action.link) {
      copyLink(action.link)
    } else if (action.type === 'share' && action.link) {
      shareOrCopy(t('invite_shareMessage', { group: action.groupName ?? '', link: action.link }), action.link)
    } else if (action.type === 'change-link-failed') {
      toast(t('invite_changeLinkFailed'))
    }
  })
  if (!presenter || !state?.isLoaded) return null

  const onOpenChange = (open: boolean) => {
    if (!open) presenter.close()
  }
  const title = t('invite_title', { group: state.groupName })
  const description = <WhoPicks names={state.notJoinedNames} />
  const body = <InviteBody presenter={presenter} state={state} />
  const close = (
    <Button variant="ghost" onClick={() => presenter.close()} className={cn(!isDesktop && 'h-11')}>
      {t('invite_close')}
    </Button>
  )
  // Why: "Ir para o grupo" while the link is pending, "Ver quem já entrou" once it exists; both open "Membros".
  const next = (
    <Button
      variant="outline"
      onClick={() => (state.isLinkPending ? presenter.goToGroup() : presenter.seeWhoJoined())}
      className={cn(!isDesktop && 'h-11')}
    >
      {state.isLinkPending ? t('invite_goToGroup') : t('invite_seeWhoJoined')}
    </Button>
  )
  const share = (
    <Button disabled={state.isLinkPending} onClick={() => presenter.share()} className={cn(!isDesktop && 'h-11')}>
      <Share2 />
      {t('invite_share')}
    </Button>
  )

  const changeLinkDialog = state.isConfirmingLinkChange && (
    <ConfirmDialog
      title={t('invite_changeLinkTitle')}
      description={t('invite_changeLinkMessage')}
      confirmLabel={t('invite_changeLink')}
      onConfirm={() => presenter.confirmChangeLink()}
      onDismiss={() => presenter.dismissChangeLink()}
    />
  )

  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[480px]">
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {body}
        <DialogFooter>
          {close}
          {next}
          {share}
        </DialogFooter>
      </DialogContent>
      {changeLinkDialog}
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle>{title}</DrawerTitle>
          <DrawerDescription>{description}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{body}</div>
        <DrawerFooter>
          {share}
          {next}
          {close}
        </DrawerFooter>
      </DrawerContent>
      {changeLinkDialog}
    </Drawer>
  )
}

function InviteBody({ presenter, state }: { presenter: InvitePresenter; state: JsInviteState }) {
  return (
    <div className="flex flex-col gap-5">
      {state.isLinkPending ? (
        <PendingLink isOnline={state.isOnline} isFailed={state.isLinkFailed} onRetry={() => presenter.retry()} />
      ) : (
        <Tabs defaultValue="link">
          <TabsList className="w-full">
            <TabsTrigger value="link">{t('invite_tabLink')}</TabsTrigger>
            <TabsTrigger value="qr">{t('invite_tabQr')}</TabsTrigger>
          </TabsList>
          <TabsContent value="link" className="flex flex-col gap-2">
            <Label htmlFor={linkId}>{t('invite_linkLabel')}</Label>
            <div className="flex gap-2">
              <Input
                id={linkId}
                readOnly
                value={state.displayLink ?? ''}
                onFocus={(event) => event.currentTarget.select()}
                className="font-mono"
              />
              <Button variant="secondary" onClick={() => presenter.copy()} aria-live="polite" className="shrink-0">
                {state.isLinkCopied ? <Check /> : <Copy />}
                {state.isLinkCopied ? t('invite_copied') : t('invite_copy')}
              </Button>
            </div>
          </TabsContent>
          <TabsContent value="qr" className="flex justify-center">
            <QrCode size={state.qrSize} modules={state.qrModules} />
          </TabsContent>
        </Tabs>
      )}
      {/* Why: "Trocar link", for the group's creator only, from the group and online. */}
      {state.showsChangeLink && (
        <p className="-mt-3 text-sm text-muted-foreground">
          {t('invite_changeLinkPrompt')}{' '}
          <Button
            variant="link"
            disabled={state.isChangingLink}
            onClick={() => presenter.changeLink()}
            className="h-auto p-0 align-baseline"
          >
            {t('invite_changeLink')}
          </Button>
        </p>
      )}
      <ul className="flex flex-col divide-y rounded-lg border" aria-label={t('invite_membersLabel')}>
        {state.members.map((member) => (
          <li key={member.id} className="flex items-center gap-3 px-3 py-2.5">
            <Avatar initial={member.initials} color={member.color} className="size-8 text-xs" />
            <span className="min-w-0 flex-1 truncate text-sm">
              {member.name}
              {member.isMe && <span className="text-muted-foreground"> · {t('invite_me')}</span>}
            </span>
            <Badge variant={member.hasJoined ? 'secondary' : 'outline'}>{member.hasJoined ? t('invite_joined') : t('invite_notJoined')}</Badge>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Black on white in both themes, as scanners read it best, with the quiet zone around the modules. */
export function QrCode({ size, modules, className = 'size-56' }: { size: number; modules: boolean[]; className?: string }) {
  const cells = size + qrQuietZone * 2
  let path = ''
  modules.forEach((isDark, index) => {
    if (isDark) path += `M${(index % size) + qrQuietZone} ${Math.floor(index / size) + qrQuietZone}h1v1h-1z`
  })
  return (
    <svg
      role="img"
      aria-label={t('invite_qrDescription')}
      viewBox={`0 0 ${cells} ${cells}`}
      shapeRendering="crispEdges"
      className={cn('rounded-xl bg-white', className)}
    >
      <path d={path} fill="#000" />
    </svg>
  )
}

function PendingLink({ isOnline, isFailed, onRetry }: { isOnline: boolean; isFailed: boolean; onRetry: () => void }) {
  if (isFailed) {
    return (
      <Alert role="status" variant="destructive">
        <AlertCircle />
        <AlertDescription className="text-foreground">
          <p>
            <strong className="font-semibold">{t('invite_failedTitle')}</strong> {t('invite_failedBody')}
          </p>
          <Button variant="outline" size="sm" onClick={onRetry} className="mt-2">
            {t('invite_retry')}
          </Button>
        </AlertDescription>
      </Alert>
    )
  }
  if (!isOnline) {
    return (
      <Alert role="status">
        <WifiOff />
        <AlertDescription className="text-foreground">
          <p>
            <strong className="font-semibold">{t('invite_offlineTitle')}</strong> {t('invite_offlineBody')}
          </p>
        </AlertDescription>
      </Alert>
    )
  }
  return (
    <p role="status" className="flex items-center gap-2 rounded-lg border border-dashed p-4 text-sm text-muted-foreground">
      <Loader2 className="size-4 animate-spin" />
      {t('invite_pendingOnline')}
    </p>
  )
}

/** "Quem abrir o link escolhe quem é na lista: Ana, Bruno ou Carla", with the names in bold. */
function WhoPicks({ names }: { names: string[] }) {
  if (names.length === 0) return <>{t('invite_anyoneJoins')}</>
  const joined = new Intl.ListFormat(locale, { type: 'disjunction' }).format(names)
  const [before, after] = t('invite_whoPicks', { names: '\u0000' }).split('\u0000')
  return (
    <>
      {before}
      <strong className="font-semibold text-foreground">{joined}</strong>
      {after}
    </>
  )
}
