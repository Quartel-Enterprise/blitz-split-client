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

/** The twelve category colors, with the same values on every platform (see the canvas rules). */
export const categoryColors: Record<string, { light: [string, string]; dark: [string, string] }> = {
  blue: { light: ['#C9E6FF', '#001E2F'], dark: ['#004B6F', '#C9E6FF'] },
  orange: { light: ['#FFDBCE', '#370E00'], dark: ['#71361C', '#FFDBCE'] },
  green: { light: ['#BDEFD3', '#00210F'], dark: ['#00513A', '#BDEFD3'] },
  purple: { light: ['#E8DDFF', '#21005D'], dark: ['#4F378B', '#E8DDFF'] },
  yellow: { light: ['#FFE08B', '#241A00'], dark: ['#574500', '#FFE08B'] },
  red: { light: ['#FFDAD6', '#410002'], dark: ['#93000A', '#FFDAD6'] },
  teal: { light: ['#A5F0E6', '#00201C'], dark: ['#00504A', '#A5F0E6'] },
  pink: { light: ['#FFD8EC', '#3E001D'], dark: ['#7B2951', '#FFD8EC'] },
  indigo: { light: ['#DDE1FF', '#00164E'], dark: ['#2F3A8F', '#DDE1FF'] },
  lime: { light: ['#DDF0A0', '#141E00'], dark: ['#3B4D00', '#DDF0A0'] },
  brown: { light: ['#F1DFD1', '#2A1709'], dark: ['#5B4130', '#F1DFD1'] },
  gray: { light: ['#E0E3E8', '#191C20'], dark: ['#44474C', '#E0E3E8'] },
}

/** The category colors' codes, in the order the picker lists them. */
export const categoryColorCodes = Object.keys(categoryColors)

/** The color's name for screen readers, or its code when the app doesn't know it. */
export function categoryColorLabel(code: string): string {
  return categoryColors[code] ? t(`categoryColor_${code}` as MessageKey) : code
}

/** CSS variables for a category color, the same ones an avatar takes so the `avatar` class paints it. */
export function categoryStyle(code: string): React.CSSProperties {
  const color = categoryColors[code] ?? categoryColors.blue
  return {
    '--avatar-bg': color.light[0],
    '--avatar-fg': color.light[1],
    '--avatar-bg-dark': color.dark[0],
    '--avatar-fg-dark': color.dark[1],
  } as React.CSSProperties
}
