import { t, type MessageKey } from '@/i18n'

/** The five avatar colors, with the same values on every platform (see the canvas rules). */
export const avatarColors: Record<string, { label: MessageKey; light: [string, string]; dark: [string, string] }> = {
  blue: { label: 'color_blue', light: ['#C9E6FF', '#001E2F'], dark: ['#004B6F', '#C9E6FF'] },
  peach: { label: 'color_peach', light: ['#FFDBCE', '#370E00'], dark: ['#71361C', '#FFDBCE'] },
  green: { label: 'color_green', light: ['#BDEFD3', '#00210F'], dark: ['#00513A', '#BDEFD3'] },
  lilac: { label: 'color_lilac', light: ['#E8DDFF', '#21005D'], dark: ['#4F378B', '#E8DDFF'] },
  sand: { label: 'color_sand', light: ['#FFE08B', '#241A00'], dark: ['#574500', '#FFE08B'] },
}

/** The color's name for screen readers, or its code when the app doesn't know it. */
export function avatarColorLabel(code: string): string {
  const color = avatarColors[code]
  return color ? t(color.label) : code
}

/** CSS variables for an avatar color; index.css picks the light or dark pair. */
export function avatarStyle(code: string): React.CSSProperties {
  const color = avatarColors[code] ?? avatarColors.blue
  return {
    '--avatar-bg': color.light[0],
    '--avatar-fg': color.light[1],
    '--avatar-bg-dark': color.dark[0],
    '--avatar-fg-dark': color.dark[1],
  } as React.CSSProperties
}
