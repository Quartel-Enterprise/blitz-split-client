import { t, type MessageKey } from '@/i18n'

/**
 * The five avatar colors, with the same values on every platform (see the canvas rules): a [background, text] pair
 * per theme, and the `data` tone [light, dark] a mark that stands for the person takes (a bar, a segment of a split).
 */
export const avatarColors: Record<string, { label: MessageKey; light: [string, string]; dark: [string, string]; data: [string, string] }> = {
  blue: { label: 'color_blue', light: ['#C9E6FF', '#001E2F'], dark: ['#004B6F', '#C9E6FF'], data: ['#017EB7', '#69C1FE'] },
  peach: { label: 'color_peach', light: ['#FFDBCE', '#370E00'], dark: ['#71361C', '#FFDBCE'], data: ['#B95D35', '#FFA17B'] },
  green: { label: 'color_green', light: ['#BDEFD3', '#00210F'], dark: ['#00513A', '#BDEFD3'], data: ['#008863', '#5ACDA1'] },
  lilac: { label: 'color_lilac', light: ['#E8DDFF', '#21005D'], dark: ['#4F378B', '#E8DDFF'], data: ['#8069C0', '#C3ACFF'] },
  sand: { label: 'color_sand', light: ['#FFE08B', '#241A00'], dark: ['#574500', '#FFE08B'], data: ['#907400', '#D9B53C'] },
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
    '--avatar-data': color.data[0],
    '--avatar-data-dark': color.data[1],
  } as React.CSSProperties
}

/**
 * The twelve category colors, with the same values on every platform (see the canvas rules). A mark that carries the
 * category (a slice, a legend dot, a bar) takes the `data` tone, not the background: the background reads at under
 * 2:1 on a card, the data tone at 4:1 light and 7:1 dark (WCAG's 3:1 for a meaningful mark).
 */
export const categoryColors: Record<string, { light: [string, string]; dark: [string, string]; data: [string, string] }> = {
  blue: { light: ['#C9E6FF', '#001E2F'], dark: ['#004B6F', '#C9E6FF'], data: ['#017EB7', '#69C1FE'] },
  orange: { light: ['#FFDBCE', '#370E00'], dark: ['#71361C', '#FFDBCE'], data: ['#B95D35', '#FFA17B'] },
  green: { light: ['#BDEFD3', '#00210F'], dark: ['#00513A', '#BDEFD3'], data: ['#008863', '#5ACDA1'] },
  purple: { light: ['#E8DDFF', '#21005D'], dark: ['#4F378B', '#E8DDFF'], data: ['#8069C0', '#C3ACFF'] },
  yellow: { light: ['#FFE08B', '#241A00'], dark: ['#574500', '#FFE08B'], data: ['#907400', '#D9B53C'] },
  red: { light: ['#FFDAD6', '#410002'], dark: ['#93000A', '#FFDAD6'], data: ['#D93D35', '#FF9F94'] },
  teal: { light: ['#A5F0E6', '#00201C'], dark: ['#00504A', '#A5F0E6'], data: ['#00867C', '#3ECDC0'] },
  pink: { light: ['#FFD8EC', '#3E001D'], dark: ['#7B2951', '#FFD8EC'], data: ['#B55882', '#FF9AC4'] },
  indigo: { light: ['#DDE1FF', '#00164E'], dark: ['#2F3A8F', '#DDE1FF'], data: ['#6570C7', '#ABB3FF'] },
  lime: { light: ['#DDF0A0', '#141E00'], dark: ['#3B4D00', '#DDF0A0'], data: ['#678020', '#A8C35C'] },
  brown: { light: ['#F1DFD1', '#2A1709'], dark: ['#5B4130', '#F1DFD1'], data: ['#986E53', '#E0AF90'] },
  gray: { light: ['#E0E3E8', '#191C20'], dark: ['#44474C', '#E0E3E8'], data: ['#74777C', '#B7B9BE'] },
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
    '--avatar-data': color.data[0],
    '--avatar-data-dark': color.data[1],
  } as React.CSSProperties
}
