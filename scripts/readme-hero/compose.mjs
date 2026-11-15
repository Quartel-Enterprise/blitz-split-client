// Draws the README's opening image from the four captures (scripts/readme-hero/generate.sh): hero.html lays the
// devices out, and a headless Chrome photographs it, dark and light, as WebP with its transparency.
//
//   node scripts/readme-hero/compose.mjs <captures folder> <output folder>
import { readFile, writeFile } from 'node:fs/promises'
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
const template = await readFile(join(import.meta.dirname, 'hero.html'), 'utf8')
const favicon = pathToFileURL(resolve(import.meta.dirname, '../../webApp/public/icon-192.png'))

const chrome = await launchChrome()
try {
  for (const theme of ['dark', 'light']) {
    await chrome.setViewport(WIDTH, HEIGHT, SCALE, theme === 'dark')
    // Why: the page gets its images written in, not read from its address, so it runs no script at all.
    const slots = { theme, captures: pathToFileURL(captures).href, favicon: favicon.href }
    const page = join(captures, `hero-${theme}.html`)
    await writeFile(page, template.replace(/\{\{(\w+)\}\}/g, (_, name) => slots[name]))
    await chrome.open(pathToFileURL(page).href)
    await chrome.waitFor(`document.readyState === 'complete' && [...document.images].every((image) => image.complete && image.naturalWidth > 0)`)
    const file = join(output, `readme-hero-${theme}.webp`)
    await chrome.screenshot(file, { format: 'webp', quality: QUALITY })
    console.log(`Wrote ${file}`)
  }
} finally {
  await chrome.close()
}
