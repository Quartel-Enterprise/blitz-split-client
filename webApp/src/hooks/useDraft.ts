import { useState } from 'react'

/**
 * A text field's value while the person types. The shared ViewModel answers asynchronously, so a
 * field bound straight to its state would move the caret to the end on every keystroke. The draft
 * follows the ViewModel only when it changes the value by itself (a name filled in from Google).
 *
 * An `onChange` that returns a string sets the draft to it instead of what was typed: a field the
 * ViewModel masks ("123.456.789-09") reads the masked text back in the same keystroke.
 */
export function useDraft(value: string, onChange: (value: string) => string | void): [string, (value: string) => void] {
  const [draft, setDraft] = useState(value)
  const [lastValue, setLastValue] = useState(value)
  if (value !== lastValue) {
    setLastValue(value)
    if (value !== draft) setDraft(value)
  }
  return [
    draft,
    (next: string) => {
      const shown = onChange(next)
      setDraft(typeof shown === 'string' ? shown : next)
    },
  ]
}
