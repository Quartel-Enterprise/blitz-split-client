import type { JsPaymentPerson, RegisterPaymentPresenter } from 'blitzsplit-shared'
import { Avatar, PersonButton } from '@/components/common'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/** A member's face, blank for someone no longer in the group, as Saldos draws them; also "Quem pagou a quem?"'s. */
export function PersonFace({ person, className = 'size-10 text-sm' }: { person: JsPaymentPerson | null | undefined; className?: string }) {
  if (!person) return <span className={cn('rounded-full bg-muted', className)} />
  return <Avatar initial={person.initials} color={person.color} photoUrl={person.photoUrl} className={className} />
}

/**
 * A face of the dialog that opens the person's profile over it (beta feedback 18), Perfil for this device's user;
 * someone no longer in the group stays a blank face.
 */
export function ProfileFace({
  presenter,
  person,
  className,
}: {
  presenter: RegisterPaymentPresenter
  person: JsPaymentPerson | null | undefined
  className?: string
}) {
  return (
    <PersonButton
      onOpen={person ? () => presenter.openPerson(person.id) : null}
      label={person ? (person.isMe ? t('registerPayment_openMyProfile') : t('registerPayment_openProfile', { name: person.name })) : undefined}
      isRound
      className="flex"
    >
      <PersonFace person={person} className={className} />
    </PersonButton>
  )
}
