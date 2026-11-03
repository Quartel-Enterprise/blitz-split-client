import type { ReactNode } from 'react'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Drawer, DrawerContent, DrawerDescription, DrawerHeader, DrawerTitle } from '@/components/ui/drawer'
import { LoadingRegion, SkeletonForm, SkeletonLine, useAppearsAfter } from '@/components/ui/skeleton'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * A dialog (a drawer on the phone) whose screen is still reading what it shows: its title when it doesn't depend on
 * what loads, else a shimmering line, and the body's shimmer (a form's fields by default). The whole dialog waits
 * 150 ms, so a fast read opens the real one at once instead of this one first. Closing it is the screen's close.
 */
export function LoadingDialog({
  isDesktop,
  title,
  isWaitingForInternet = false,
  onClose,
  className,
  children,
}: {
  isDesktop: boolean
  title?: string
  isWaitingForInternet?: boolean
  onClose: () => void
  className?: string
  children?: ReactNode
}) {
  const appears = useAppearsAfter()
  if (!appears) return null
  const onOpenChange = (open: boolean) => {
    if (!open) onClose()
  }
  const heading = title ?? t('common_loading')
  const body = (
    <LoadingRegion isDelayed={false} isWaitingForInternet={isWaitingForInternet} className="flex flex-col gap-5">
      {!title && <SkeletonLine className="h-5 w-1/2" />}
      {children ?? <SkeletonForm />}
    </LoadingRegion>
  )
  return isDesktop ? (
    <Dialog open onOpenChange={onOpenChange}>
      <DialogContent className={cn('max-h-[calc(100dvh-2rem)] overflow-y-auto sm:max-w-[520px]', className)}>
        <DialogHeader className="pr-8">
          <DialogTitle className={cn(!title && 'sr-only')}>{heading}</DialogTitle>
          <DialogDescription className="sr-only">{heading}</DialogDescription>
        </DialogHeader>
        {body}
      </DialogContent>
    </Dialog>
  ) : (
    <Drawer open onOpenChange={onOpenChange} repositionInputs={false}>
      <DrawerContent className="data-[vaul-drawer-direction=bottom]:max-h-[92dvh]">
        <DrawerHeader className="group-data-[vaul-drawer-direction=bottom]/drawer-content:text-left">
          <DrawerTitle className={cn(!title && 'sr-only')}>{heading}</DrawerTitle>
          <DrawerDescription className="sr-only">{heading}</DrawerDescription>
        </DrawerHeader>
        <div className="overflow-y-auto px-4 pb-6">{body}</div>
      </DrawerContent>
    </Drawer>
  )
}
