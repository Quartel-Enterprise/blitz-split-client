import type { ChangeEvent, CompositionEvent } from 'react'

type TextElement = HTMLInputElement | HTMLTextAreaElement

/**
 * The typed text with its letter uppercased, or null to leave it as typed: a lone lowercase letter typed into an
 * empty field, after a dead key's accent, or over a selected text. Not the capital lowercased, nor a lowercase letter
 * the field had before (deleting down to it), so someone who wants it lowercase keeps it. The caret stays after it.
 */
export function capitalizedFirstLetter(typed: string, previous: string): string | null {
  // Why: NFC, so an "é" typed as "e" and its accent is one letter, as it is on the Mac
  const letter = typed.normalize('NFC')
  const before = previous.normalize('NFC')
  if (!/^\p{Ll}$/u.test(letter) || before.includes(letter)) return null
  const uppercased = letter.toUpperCase()
  // "ß" uppercases to "SS": two letters where one was typed
  return [...uppercased].length === 1 && before !== uppercased ? uppercased : null
}

// Why: a phone's or a tablet's keyboard capitalizes on its own (autocapitalize); a computer's physical keyboard doesn't
const physicalKeyboard = window.matchMedia('(hover: hover) and (pointer: fine)')

// Why: an accent typed with a dead key ("´" then "a") settles once its composition ends, against the text before it
const textBeforeComposition = new WeakMap<TextElement, string>()

/**
 * The props of a field for a name, a description or a comment, so it starts with a capital letter (beta feedback 3
 * item 27): a phone's keyboard capitalizes each sentence, or each word of a person's `'words'` name ("Maria Silva"),
 * and on a computer the first letter is uppercased as it is typed. Never on an email, a payment key, a code, a link,
 * an amount or a search. Spread it in place of `value` and `onChange`.
 */
export function freeTextProps(value: string, onValueChange: (value: string) => void, capitalization: 'sentences' | 'words' = 'sentences') {
  const settle = (typed: string, previous: string) => {
    onValueChange((physicalKeyboard.matches && capitalizedFirstLetter(typed, previous)) || typed)
  }
  return {
    value,
    autoCapitalize: capitalization,
    onChange: (event: ChangeEvent<TextElement>) => {
      if ((event.nativeEvent as InputEvent).isComposing) onValueChange(event.target.value)
      else settle(event.target.value, value)
    },
    onCompositionStart: (event: CompositionEvent<TextElement>) => {
      textBeforeComposition.set(event.currentTarget, event.currentTarget.value)
    },
    // Why: the composition's own input events already sent its text; a phone's keyboard composes every word
    onCompositionEnd: (event: CompositionEvent<TextElement>) => {
      const previous = textBeforeComposition.get(event.currentTarget) ?? value
      const capitalized = physicalKeyboard.matches && capitalizedFirstLetter(event.currentTarget.value, previous)
      if (capitalized) onValueChange(capitalized)
    },
  }
}
