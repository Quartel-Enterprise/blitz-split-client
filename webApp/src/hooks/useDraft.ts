import { useState } from 'react'

/**
 * A text field's value while the person types. The shared ViewModel answers asynchronously, so a
 * field bound straight to its state would move the caret to the end on every keystroke. The draft
 * follows the ViewModel only when it changes the value by itself (a name filled in from Google).
 */
export function useDraft(value: string, onChange: (value: string) => void): [string, (value: string) => void] {
  const [draft, setDraft] = useState(value)
  const [lastValue, setLastValue] = useState(value)
  if (value !== lastValue) {
    setLastValue(value)
    if (value !== draft) setDraft(value)
  }
  return [
    draft,
    (next: string) => {
      setDraft(next)
      onChange(next)
    },
  ]
}
