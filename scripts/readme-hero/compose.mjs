// Draws the README's opening image from the four captures (scripts/readme-hero/generate.sh): hero.html lays the
// devices out, and a headless Chrome photographs it, dark and light, as WebP with its transparency.
//
//   node scripts/readme-hero/compose.mjs <captures folder> <output folder>
import { pathToFileURL } from 'node:url'
import { join, resolve } from 'node:path'
import { launchChrome } from './chrome.mjs'

const [captures, output] = process.argv.slice(2).map((path) => path && resolve(path))
if (!captures || !output) {
  console.error('Usage: compose.mjs <captures folder> <output folder>')
  process.exit(1)
}
// hero.html's size in CSS pixels, photographed at twice that: sharp on a Retina display at the README's width.
const WIDTH = 1280
const HEIGHT = 680
const SCALE = 2
const QUALITY = 90
const page = pathToFileURL(join(import.meta.dirname, 'hero.html'))
const favicon = pathToFileURL(resolve(import.meta.dirname, '../../webApp/public/icon-192.png'))

const chrome = await launchChrome()
try {
  for (const theme of ['dark', 'light']) {
    await chrome.setViewport(WIDTH, HEIGHT, SCALE, theme === 'dark')
    const query = new URLSearchParams({ theme, captures: pathToFileURL(captures).href, favicon: favicon.href })
    await chrome.open(`${page.href}?${query}`)
    await chrome.waitFor(`document.readyState === 'complete' && [...document.images].every((image) => image.complete && image.naturalWidth > 0)`)
    const file = join(output, `readme-hero-${theme}.webp`)
    await chrome.screenshot(file, { format: 'webp', quality: QUALITY })
    console.log(`Wrote ${file}`)
  }
} finally {
  await chrome.close()
}
