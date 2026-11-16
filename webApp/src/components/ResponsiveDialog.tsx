import type { ReactNode } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { cn } from '@/lib/utils'

/**
 * What the dialog or the drawer draws: an optional top (faces), its title with an optional icon button at its end, an
 * optional lead, the body and the buttons.
 */
export interface PaneParts {
  leading?: ReactNode
  title: string
  // Why: Rachar conta's "Ler cupom fiscal" at the top, in sight before the keyboard covers the footer (beta feedback).
  titleAction?: ReactNode
  description?: string
  body: ReactNode
  footer: ReactNode
}

const FOCUSABLE = 'input:not([disabled]), textarea:not([disabled]), select:not([disabled]), button:not([disabled]), a[href]'

/**
 * Why: the top may hold buttons (Registrar pagamento's faces open a profile, Rachar conta's "Ler cupom fiscal"), which
 * opening the form must not focus, or a keyboard's Enter would leave it; the first field or button past them takes the
 * focus instead.
 */
function focusPastLeading(event: Event) {
  const content = event.currentTarget as HTMLElement
  const tops = Array.from(content.querySelectorAll('[data-dialog-leading], [data-dialog-title-action]'))
  if (!tops.some((top) => top.querySelector(FOCUSABLE))) return
  // Why: a hidden file input (Rachar conta's receipt) matches too, and focusing it would leave the form unfocused.
  const first = Array.from(content.querySelectorAll<HTMLElement>(FOCUSABLE)).find(
    (element) => !element.hidden && element.tabIndex >= 0 && !tops.some((top) => top.contains(element)),
  )
  if (!first) return
  event.preventDefault()
  first.focus()
}

/**
 * A form over the page: a dialog on desktop and tablet, a drawer on the phone, whose footer puts the main action first,
 * under the thumb. Closing it any way goes to `onOpenChange(false)`, the ViewModel's close.
 */
export function ResponsiveDialog({
  isDesktop,
  pane,
  onOpenChange,
  className,
}: {
  isDesktop: boolean
  pane: PaneParts
  onOpenChange: (open: boolean) => void
  className?: string
}) {
  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className={cn('max-h-[calc(100dvh-2rem)] overflow-y-auto [--pane-gutter:--spacing(6)]', className)} onOpenAutoFocus={focusPastLeading}>
        <DialogHeader className="pr-8">
          <div data-dialog-leading className="contents">
            {pane.leading}
          </div>
          <PaneTitle title={<DialogTitle>{pane.title}</DialogTitle>} action={pane.titleAction} />
          <DialogDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DialogDescription>
        </DialogHeader>
        {pane.body}
        {pane.footer != null && <DialogFooter>{pane.footer}</DialogFooter>}
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]" onOpenAutoFocus={focusPastLeading}>
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <div data-dialog-leading className="contents">
            {pane.leading}
          </div>
          <PaneTitle title={<DrawerTitle>{pane.title}</DrawerTitle>} action={pane.titleAction} />
          <DrawerDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4 [--pane-gutter:--spacing(4)]">{pane.body}</div>
        <DrawerFooter className="flex-col-reverse">{pane.footer}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}

/** The title, with its icon button at the end when the pane has one. */
function PaneTitle({ title, action }: { title: ReactNode; action?: ReactNode }) {
  if (action == null || action === false) return title
  return (
    <div className="flex items-center justify-between gap-2">
      {title}
      <div data-dialog-title-action className="-my-2 flex shrink-0">
        {action}
      </div>
    </div>
  )
}
