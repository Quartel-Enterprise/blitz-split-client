import { ArrowLeft, Camera, Info, Loader2 } from 'lucide-react'
import { useRef } from 'react'
import { ProfileSetupPresenter, type JsProfileSetupState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar, AvatarColorPicker } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { t, type MessageKey } from '@/i18n'
import { cn } from '@/lib/utils'

export function ProfileSetupScreen() {
  const presenter = usePresenter(() => new ProfileSetupPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsProfileSetupState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return <ProfileSetupForm presenter={presenter} state={state} />
}

const nameErrors: Record<string, MessageKey> = {
  TOO_SHORT: 'profileSetup_nameTooShort',
  TOO_LONG: 'profileSetup_nameTooLong',
}

function ProfileSetupForm({ presenter, state }: { presenter: ProfileSetupPresenter; state: JsProfileSetupState }) {
  const [name, setName] = useDraft(state.name, (value) => presenter.changeName(value))
  const photoInput = useRef<HTMLInputElement>(null)
  const nameError = state.nameError ? nameErrors[state.nameError] : null
  return (
    <main className="flex min-h-dvh items-center justify-center bg-muted/40 p-4 sm:p-8">
      <form
        className="flex w-full max-w-md flex-col gap-6 rounded-xl border bg-card p-6 shadow-sm sm:p-8"
        onSubmit={(event) => {
          event.preventDefault()
          presenter.continueClicked()
        }}
      >
        <Button type="button" variant="ghost" size="sm" className="-ml-2 self-start" onClick={() => presenter.back()}>
          <ArrowLeft /> {t('common_back')}
        </Button>
        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-semibold tracking-tight">{t('profileSetup_title')}</h1>
          <p className="text-sm text-muted-foreground">{t('profileSetup_subtitle')}</p>
        </div>
        <div className="flex flex-col items-center gap-2">
          <div className="relative">
            <Avatar initial={state.avatarInitial} color={state.avatarColor} photoUrl={state.photoUrl} className="size-20 text-3xl transition-colors" />
            {state.isPreparingPhoto && (
              <span className="absolute inset-0 flex items-center justify-center rounded-full bg-background/60">
                <Loader2 className="size-6 animate-spin" />
              </span>
            )}
            <Button
              type="button"
              size="icon"
              variant="secondary"
              aria-label={t('profileSetup_addPhoto')}
              className="absolute -right-2 -bottom-2 size-9 rounded-xl border-[3px] border-card"
              onClick={() => photoInput.current?.click()}
            >
              <Camera className="size-4" />
            </Button>
            <input
              ref={photoInput}
              type="file"
              accept="image/*"
              hidden
              onChange={async (event) => {
                const file = event.target.files?.[0]
                event.target.value = ''
                if (!file) return
                try {
                  presenter.photoPicked(new Int8Array(await file.arrayBuffer()))
                } catch {
                  presenter.photoUnreadable()
                }
              }}
            />
          </div>
          {state.showsPhotoError && (
            <span role="alert" className="text-xs text-destructive">
              {t('profileSetup_photoError')}
            </span>
          )}
        </div>
        <div className="flex flex-col gap-2">
          <span id="avatar-color" className="text-sm font-medium">
            {t('profileSetup_avatarColor')}
          </span>
          <AvatarColorPicker colors={state.avatarColors} selected={state.avatarColor} onSelect={(code) => presenter.selectAvatarColor(code)} labelledBy="avatar-color" />
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="profile-name">{t('profileSetup_nameLabel')}</Label>
          <Input
            id="profile-name"
            autoComplete="name"
            placeholder={t('profileSetup_namePlaceholder')}
            value={name}
            aria-invalid={nameError !== null}
            aria-describedby="profile-name-help"
            onChange={(event) => setName(event.target.value)}
          />
          <span id="profile-name-help" role={nameError ? 'alert' : undefined} className={cn('text-xs', nameError ? 'text-destructive' : 'text-muted-foreground')}>
            {t(nameError ?? 'profileSetup_nameHint', { min: state.nameMinLength, max: state.nameMaxLength })}
          </span>
        </div>
        <div role="note" className="flex gap-2.5 rounded-lg bg-muted p-3 text-xs leading-relaxed text-muted-foreground">
          <Info className="mt-0.5 size-4 shrink-0" />
          <span>{t('profileSetup_savedLocally')}</span>
        </div>
        <Button type="submit" size="lg" disabled={!state.canContinue || state.isSaving}>
          {state.isSaving && <Loader2 className="animate-spin" />}
          {t('profileSetup_continue')}
        </Button>
      </form>
    </main>
  )
}
