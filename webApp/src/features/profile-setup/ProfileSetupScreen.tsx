import { ArrowLeft, Check, Info, Loader2 } from 'lucide-react'
import { ProfileSetupPresenter, type JsProfileSetupState } from 'blitzsplit-shared'
import { usePresenter, useWatch } from '@/bridge/usePresenter'
import { Avatar } from '@/components/common'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useDraft } from '@/hooks/useDraft'
import { avatarColors, avatarStyle } from '@/lib/avatar'
import { cn } from '@/lib/utils'

export function ProfileSetupScreen() {
  const presenter = usePresenter(() => new ProfileSetupPresenter())
  const state = useWatch(presenter, (p, onChange: (state: JsProfileSetupState) => void) => p.observeState(onChange))
  if (!presenter || !state) return null
  return <ProfileSetupForm presenter={presenter} state={state} />
}

function ProfileSetupForm({ presenter, state }: { presenter: ProfileSetupPresenter; state: JsProfileSetupState }) {
  const [name, setName] = useDraft(state.name, (value) => presenter.changeName(value))
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
          <ArrowLeft /> Voltar
        </Button>
        <div className="flex flex-col gap-1.5">
          <h1 className="text-2xl font-semibold tracking-tight">Como seus amigos te chamam?</h1>
          <p className="text-sm text-muted-foreground">Seu nome aparece só para quem estiver nos seus grupos.</p>
        </div>
        <Avatar initial={state.avatarInitial} color={state.avatarColor} className="size-20 self-center text-3xl transition-colors" />
        <div className="flex flex-col gap-2">
          <span id="avatar-color" className="text-sm font-medium">
            Cor do avatar
          </span>
          <div role="radiogroup" aria-labelledby="avatar-color" className="flex gap-3">
            {state.avatarColors.map((code) => {
              const isSelected = code === state.avatarColor
              return (
                <button
                  key={code}
                  type="button"
                  role="radio"
                  aria-checked={isSelected}
                  aria-label={avatarColors[code]?.label ?? code}
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
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="profile-name">Seu nome</Label>
          <Input
            id="profile-name"
            autoComplete="name"
            placeholder="Ex.: Pierre"
            value={name}
            aria-invalid={state.showsNameTooShort}
            aria-describedby="profile-name-help"
            onChange={(event) => setName(event.target.value)}
          />
          <span id="profile-name-help" role={state.showsNameTooShort ? 'alert' : undefined} className={cn('text-xs', state.showsNameTooShort ? 'text-destructive' : 'text-muted-foreground')}>
            {state.showsNameTooShort ? 'Digite pelo menos 2 letras.' : 'Você pode mudar depois em Perfil.'}
          </span>
        </div>
        <div role="note" className="flex gap-2.5 rounded-lg bg-muted p-3 text-xs leading-relaxed text-muted-foreground">
          <Info className="mt-0.5 size-4 shrink-0" />
          <span>Fica salvo só neste navegador por enquanto. Se você limpar os dados do site, o perfil some.</span>
        </div>
        <Button type="submit" size="lg" disabled={!state.canContinue || state.isSaving}>
          {state.isSaving && <Loader2 className="animate-spin" />}
          Continuar
        </Button>
      </form>
    </main>
  )
}
