import { Camera, Check, ImageIcon, Info, Loader2, Pencil, Trash2, UserRound } from 'lucide-react'
import { useRef, type ChangeEvent } from 'react'
import { EditProfilePresenter, type JsEditProfileState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, providerLabel } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { useMediaQuery } from '@/hooks/useMediaQuery'
import { t, type MessageKey } from '@/i18n'
import { avatarColorLabel, avatarStyle } from '@/lib/avatar'
import type { AccountProvider } from '@/lib/platform'
import { cn } from '@/lib/utils'

async function onPhotoFile(presenter: EditProfilePresenter, event: ChangeEvent<HTMLInputElement>) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    presenter.photoPicked(new Int8Array(await file.arrayBuffer()))
  } catch {
    presenter.photoUnreadable()
  }
}

const nameErrors: Record<string, MessageKey> = {
  TOO_SHORT: 'editProfile_nameTooShort',
  TOO_LONG: 'editProfile_nameTooLong',
}

/** "Your profile": name, photo and color edited in place, with Save and Cancel once something changed. */
export function EditProfileCard({ isPending }: { isPending: boolean }) {
  const presenter = usePresenter(() => new EditProfilePresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsEditProfileState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return <EditProfileForm presenter={presenter} state={state} isPending={isPending} />
}

function EditProfileForm({ presenter, state, isPending }: { presenter: EditProfilePresenter; state: JsEditProfileState; isPending: boolean }) {
  const [name, setName] = useDraft(state.name, (value) => presenter.changeName(value))
  const photoInput = useRef<HTMLInputElement>(null)
  // Why: only a touch device has a camera the "capture" attribute can open; a desktop browser shows a file chooser.
  const canTakePhoto = useMediaQuery('(pointer: coarse)')
  const choosePhoto = (useCamera: boolean) => {
    const input = photoInput.current
    if (!input) return
    if (useCamera) input.setAttribute('capture', 'user')
    else input.removeAttribute('capture')
    input.click()
  }
  const nameError = state.nameError ? nameErrors[state.nameError] : null
  return (
    <Card>
      <form
        className="flex flex-col gap-6"
        onSubmit={(event) => {
          event.preventDefault()
          presenter.save()
        }}
      >
        <CardHeader>
          <CardTitle>{t('profile_yourProfile')}</CardTitle>
          {isPending && <CardDescription className="text-destructive">{t('profile_pending')}</CardDescription>}
        </CardHeader>
        <CardContent className="flex flex-col gap-6">
          <div className="flex flex-col items-center gap-2">
            <div className="relative">
              <Avatar initial={state.avatarInitial} color={state.avatarColor} photoUrl={state.photoUrl} className="size-24 text-4xl transition-colors" />
              {state.isPreparingPhoto && (
                <span className="absolute inset-0 flex items-center justify-center rounded-full bg-background/60">
                  <Loader2 className="size-6 animate-spin" />
                </span>
              )}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    type="button"
                    size="icon"
                    variant="secondary"
                    aria-label={t('editProfile_changePhoto')}
                    className="absolute -right-1 -bottom-1 size-9 rounded-xl border-[3px] border-card"
                  >
                    <Pencil />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {/* Why: a desktop browser ignores "capture" and would open the file chooser twice over. */}
                  {canTakePhoto && (
                    <DropdownMenuItem onSelect={() => choosePhoto(true)}>
                      <Camera /> {t('editProfile_takePhoto')}
                    </DropdownMenuItem>
                  )}
                  <DropdownMenuItem onSelect={() => choosePhoto(false)}>
                    <ImageIcon /> {t('editProfile_choosePhoto')}
                  </DropdownMenuItem>
                  {state.accountPhotoProvider && (
                    <DropdownMenuItem onSelect={() => presenter.useAccountPhoto()}>
                      <UserRound /> {t('editProfile_useAccountPhoto', { provider: providerLabel(state.accountPhotoProvider as AccountProvider, true) })}
                    </DropdownMenuItem>
                  )}
                  {state.hasPhoto && (
                    <DropdownMenuItem variant="destructive" onSelect={() => presenter.removePhoto()}>
                      <Trash2 /> {t('editProfile_removePhoto')}
                    </DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
            {state.showsPhotoError && (
              <span role="alert" className="text-xs text-destructive">
                {t('editProfile_photoError')}
              </span>
            )}
            <input ref={photoInput} type="file" accept="image/*" hidden onChange={(event) => onPhotoFile(presenter, event)} />
          </div>
          {state.showsAvatarColors && (
            <div className="flex flex-col gap-2">
              <span id="edit-avatar-color" className="text-sm font-medium">
                {t('editProfile_avatarColor')}
              </span>
              <div role="radiogroup" aria-labelledby="edit-avatar-color" className="flex gap-3">
                {state.avatarColors.map((code) => {
                  const isSelected = code === state.avatarColor
                  return (
                    <button
                      key={code}
                      type="button"
                      role="radio"
                      aria-checked={isSelected}
                      aria-label={avatarColorLabel(code)}
                      style={avatarStyle(code)}
                      onClick={() => presenter.selectAvatarColor(code)}
                      className={cn(
                        'avatar flex size-10 items-center justify-center rounded-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring',
                        isSelected && 'ring-2 ring-primary ring-offset-2 ring-offset-card',
                      )}
                    >
                      {isSelected && <Check className="size-4" />}
                    </button>
                  )
                })}
              </div>
              <span className="text-xs text-muted-foreground">{t('editProfile_avatarColorHint')}</span>
            </div>
          )}
          <div className="flex flex-col gap-2">
            <Label htmlFor="edit-profile-name">{t('editProfile_nameLabel')}</Label>
            <Input
              id="edit-profile-name"
              autoComplete="name"
              value={name}
              aria-invalid={nameError !== null}
              aria-describedby="edit-profile-name-help"
              onChange={(event) => setName(event.target.value)}
            />
            <div className="flex justify-between gap-2 text-xs">
              <span id="edit-profile-name-help" role={nameError ? 'alert' : undefined} className={nameError ? 'text-destructive' : 'text-muted-foreground'}>
                {t(nameError ?? 'editProfile_nameHint', { min: state.nameMinLength, max: state.nameMaxLength })}
              </span>
              <span className="text-muted-foreground tabular-nums">
                {state.nameLength}/{state.nameMaxLength}
              </span>
            </div>
          </div>
          {state.accountProvider && (
            <div role="note" className="flex gap-2.5 rounded-lg bg-muted p-3 text-xs leading-relaxed text-muted-foreground">
              <Info className="mt-0.5 size-4 shrink-0" />
              <span>{t('editProfile_accountNotice', { provider: providerLabel(state.accountProvider as AccountProvider, true) })}</span>
            </div>
          )}
        </CardContent>
        <CardFooter className="flex flex-wrap items-center gap-2">
          <span className="mr-auto text-xs text-muted-foreground">{t('editProfile_footer')}</span>
          {state.hasChanges && (
            <div className="ml-auto flex gap-2">
              <Button type="button" variant="outline" onClick={() => presenter.discard()}>
                {t('common_cancel')}
              </Button>
              <Button type="submit" disabled={!state.canSave}>
                {state.isSaving && <Loader2 className="animate-spin" />}
                {t('editProfile_save')}
              </Button>
            </div>
          )}
        </CardFooter>
      </form>
    </Card>
  )
}
