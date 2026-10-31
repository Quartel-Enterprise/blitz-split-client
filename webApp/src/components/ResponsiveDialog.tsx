import type { ReactNode } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { cn } from '@/lib/utils'

/** What the dialog or the drawer draws: an optional top (faces), its title, an optional lead, the body and the buttons. */
export interface PaneParts {
  leading?: ReactNode
  title: string
  description?: string
  body: ReactNode
  footer: ReactNode
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
      <DialogContent className={cn('max-h-[calc(100dvh-2rem)] overflow-y-auto', className)}>
        <DialogHeader className="pr-8">
          {pane.leading}
          <DialogTitle>{pane.title}</DialogTitle>
          <DialogDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DialogDescription>
        </DialogHeader>
        {pane.body}
        <DialogFooter>{pane.footer}</DialogFooter>
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          {pane.leading}
          <DrawerTitle>{pane.title}</DrawerTitle>
          <DrawerDescription className={cn(!pane.description && 'sr-only')}>{pane.description ?? pane.title}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4">{pane.body}</div>
        <DrawerFooter className="flex-col-reverse">{pane.footer}</DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}
