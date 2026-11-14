import { getCustomColorTones } from 'blitzsplit-shared'
import { t, type MessageKey } from '@/i18n'

/** A color's [background, text] pair per theme, and its `data` tone [light, dark]. */
type ColorTones = { light: [string, string]; dark: [string, string]; data: [string, string] }

/** The color's name for screen readers, or its code when the app doesn't know it. */
export function avatarColorLabel(code: string): string {
  const color = avatarColors[code]
  return color ? t(color.label) : code
}

/** The CSS variables the `avatar`, `avatar-mark`, `avatar-stroke` and `avatar-edge` classes read. */
function colorStyle(color: ColorTones): React.CSSProperties {
  return {
    '--avatar-bg': color.light[0],
    '--avatar-fg': color.light[1],
    '--avatar-bg-dark': color.dark[0],
    '--avatar-fg-dark': color.dark[1],
    '--avatar-data': color.data[0],
    '--avatar-data-dark': color.data[1],
  } as React.CSSProperties
}

/** CSS variables for an avatar color; index.css picks the light or dark pair. */
export function avatarStyle(code: string): React.CSSProperties {
  return colorStyle(avatarColors[code] ?? avatarColors.blue)
}

/**
 * The twelve category colors, with the same values on every platform (see the canvas rules). A mark that carries the
 * category (a slice, a legend dot, a bar) takes the `data` tone, not the background: the background reads at under
 * 2:1 on a card, the data tone at 4:1 light and 7:1 dark (WCAG's 3:1 for a meaningful mark).
 */
export const categoryColors: Record<string, ColorTones> = {
  blue: { light: ['#D7E2FF', '#001B3F'], dark: ['#00458F', '#D7E2FF'], data: ['#2A75DC', '#94B9FF'] },
  orange: { light: ['#FFDCC0', '#2D1600'], dark: ['#6B3B00', '#FFDCC0'], data: ['#AE650C', '#FDA54D'] },
  green: { light: ['#AAF5A4', '#002204'], dark: ['#055315', '#AAF5A4'], data: ['#418642', '#82CA7D'] },
  purple: { light: ['#F0DBFF', '#2C0051'], dark: ['#5B3383', '#F0DBFF'], data: ['#8E65B8', '#D3A6FF'] },
  yellow: { light: ['#F6E468', '#201C00'], dark: ['#504700', '#F6E468'], data: ['#857800', '#CBBA42'] },
  red: { light: ['#FFDADA', '#40000B'], dark: ['#8A182B', '#FFDADA'], data: ['#CC4A57', '#FF9EA2'] },
  teal: { light: ['#A5F0E6', '#00201C'], dark: ['#00504A', '#A5F0E6'], data: ['#00867C', '#3ECDC0'] },
  pink: { light: ['#FFD8EC', '#3E001D'], dark: ['#7B2951', '#FFD8EC'], data: ['#B55882', '#FF9AC4'] },
  indigo: { light: ['#E2DFFF', '#0E0664'], dark: ['#3D3C8F', '#E2DFFF'], data: ['#6E6EC3', '#B2B1FF'] },
  lime: { light: ['#DDF0A0', '#141E00'], dark: ['#3B4D00', '#DDF0A0'], data: ['#678020', '#A8C35C'] },
  brown: { light: ['#F1DFD1', '#2A1709'], dark: ['#5B4130', '#F1DFD1'], data: ['#986E53', '#E0AF90'] },
  gray: { light: ['#E0E3E8', '#191C20'], dark: ['#44474C', '#E0E3E8'], data: ['#74777C', '#B7B9BE'] },
}

/**
 * The five avatar colors share the values of the category color of the same hue on every platform (see the canvas
 * rules), so a face and a slice of one hue never drift apart; the `data` tone is what a mark that stands for the
 * person takes (a bar, a segment of a split).
 */
export const avatarColors: Record<string, ColorTones & { label: MessageKey }> = {
  blue: { label: 'color_blue', ...categoryColors.blue },
  peach: { label: 'color_peach', ...categoryColors.orange },
  green: { label: 'color_green', ...categoryColors.green },
  lilac: { label: 'color_lilac', ...categoryColors.purple },
  sand: { label: 'color_sand', ...categoryColors.yellow },
}

/** The category colors' codes, in the order the picker lists them. */
export const categoryColorCodes = Object.keys(categoryColors)

/** The color's name for screen readers: a named color's, "Custom color #3A7BD5", or its code when the app doesn't know it. */
export function categoryColorLabel(code: string): string {
  if (categoryColors[code]) return t(`categoryColor_${code}` as MessageKey)
  return code.startsWith('#') ? t('customColor_label', { hex: code }) : code
}

/**
 * CSS variables for a category color, the same ones an avatar takes so the `avatar` class paints it. A custom color
 * (a hex code) takes the tones the shared code derives from it, as the Android and Apple apps draw it.
 */
export function categoryStyle(code: string): React.CSSProperties {
  return colorStyle(categoryColors[code] ?? customTones(code) ?? categoryColors.blue)
}

function customTones(code: string): ColorTones | undefined {
  const tones = code.startsWith('#') ? getCustomColorTones(code) : null
  if (!tones) return undefined
  return { light: [tones.lightBg, tones.lightFg], dark: [tones.darkBg, tones.darkFg], data: [tones.lightData, tones.darkData] }
}

/**
 * A group's rounded square: the group photo viewer's (16 px corners on its 200 px side) at every size, so a group reads
 * as the same square in a list row, the group's header and the photo opened big (beta feedback of 2026-11-12).
 */
export const GROUP_SHAPE = 'rounded-[8%]'
