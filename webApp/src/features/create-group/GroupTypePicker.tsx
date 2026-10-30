import { useId } from 'react'
import { Check } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { t, type MessageKey } from '@/i18n'

const typeLabels: Record<string, MessageKey> = {
  trip: 'createGroup_typeTrip',
  home: 'createGroup_typeHome',
  event: 'createGroup_typeEvent',
  other: 'createGroup_typeOther',
}

/** The group's type as one radio button per type, the same in "New group" and "Edit group". */
export function GroupTypePicker({ label, types, selected, onSelect }: { label: string; types: string[]; selected: string; onSelect: (type: string) => void }) {
  const labelId = useId()
  return (
    <div className="flex flex-1 flex-col gap-2">
      <span id={labelId} className="text-sm font-medium">
        {label}
      </span>
      <div role="radiogroup" aria-labelledby={labelId} className="flex flex-wrap gap-1.5">
        {types.map((type) => (
          <Button
            key={type}
            type="button"
            role="radio"
            aria-checked={type === selected}
            size="sm"
            variant={type === selected ? 'secondary' : 'outline'}
            onClick={() => onSelect(type)}
          >
            {type === selected && <Check />}
            {t(typeLabels[type] ?? 'createGroup_typeOther')}
          </Button>
        ))}
      </div>
    </div>
  )
}
