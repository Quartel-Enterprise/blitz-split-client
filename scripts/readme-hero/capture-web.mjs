// Captures the web app's group screen for the README's opening image (scripts/readme-hero/generate.sh): opens the dev
// server in a headless Chrome with a profile of its own, has the shared code seed the English sample trip into its
// empty Room (webApp/src/bridge/storeScreenshots.ts) and photographs the page, dark and light.
//
//   node scripts/readme-hero/capture-web.mjs <dev server URL> <output folder>
import { readFile } from 'node:fs/promises'
import { join } from 'node:path'
import { launchChrome } from './chrome.mjs'

const [url, output] = process.argv.slice(2)
if (!url || !output) {
  console.error('Usage: capture-web.mjs <dev server URL> <output folder>')
  process.exit(1)
}
const TABLES = join(import.meta.dirname, '../store-screenshots')
// The browser window the image draws, in CSS pixels: wide enough for the web's sidebar and the group's two panes.
const WIDTH = 1280
const HEIGHT = 800
// The group's own card (src/i18n balance_title): Home's sidebar already lists the group by name before it opens.
const GROUP_READY = 'Your balance in this group'
// How long the faces and the group's photo take to show once the group is on screen.
const SETTLE_MS = 1500

/**
 * The photos of scripts/store-screenshots/assets.tsv by the path the seed reads them at, as the Apple app files them
 * (StoreScreenshotsRun.swift): faces/person<n>.jpg, groups/<photo>.jpg and receipt.jpg.
 */
async function photos() {
  const rows = (await readFile(join(TABLES, 'assets.tsv'), 'utf8'))
    .split('\n')
    .filter((line) => line.trim() !== '' && !line.startsWith('#'))
  const entries = await Promise.all(
    rows.map(async (row) => {
      const [name, file] = row.split('\t')
      const path =
        name === 'receipt'
          ? 'receipt.jpg'
          : name.startsWith('group_')
            ? `groups/${name.slice('group_'.length)}.jpg`
            : `faces/${name}.jpg`
      return [path, (await readFile(join(TABLES, file))).toString('base64')]
    }),
  )
  return Object.fromEntries(entries)
}

/*
 * Why: Vite reloads the page when it optimizes a dependency it found late, and the seed run again over the half of the
 * trip the profile's Room already holds would fail or show the trip twice. A second document marks the run spoiled,
 * and the capture starts over in a fresh profile, by when Vite has its dependencies.
 */
const RELOADED = 'The page reloaded while the trip was seeded'
const ATTEMPTS = 3

const allPhotos = await photos()
for (const isDark of [true, false]) {
  for (let attempt = 1; ; attempt++) {
    try {
      await capture(isDark)
      break
    } catch (error) {
      if (attempt >= ATTEMPTS || !error.message.includes(RELOADED)) throw error
      console.log(`${RELOADED}: starting over in a fresh profile`)
    }
  }
}

async function capture(isDark) {
  const chrome = await launchChrome()
  try {
    await chrome.setViewport(WIDTH, HEIGHT, 2, isDark)
    const run = { language: 'en', isDark, screen: 'group', photos: allPhotos }
    // The language goes to the page's storage too, which src/i18n reads before the shared code starts. A failed seed
    // (an unhandled rejection of startShared) stops the wait with its cause instead of timing out.
    await chrome.beforeEachPage(`
      if (sessionStorage.getItem('readmeHeroStarted')) {
        window.__storeScreenshotsError = ${JSON.stringify(RELOADED)}
      } else {
        sessionStorage.setItem('readmeHeroStarted', '1')
        window.__storeScreenshots = ${JSON.stringify(run)}
      }
      localStorage.setItem('language', ${JSON.stringify(run.language)})
      window.addEventListener('unhandledrejection', (event) => {
        window.__storeScreenshotsError ??= String(event.reason?.stack ?? event.reason)
      })
    `)
    await chrome.open(url)
    await chrome.waitFor(
      `(() => {
        if (window.__storeScreenshotsError) throw new Error(window.__storeScreenshotsError)
        return document.body.innerText.includes(${JSON.stringify(GROUP_READY)})
      })()`,
      180_000,
    )
    await chrome.waitFor('[...document.images].every((image) => image.complete)')
    await new Promise((resolve) => setTimeout(resolve, SETTLE_MS))
    const file = join(output, `web-${isDark ? 'dark' : 'light'}.png`)
    await chrome.screenshot(file)
    console.log(`Captured ${file}`)
  } finally {
    await chrome.close()
  }
}
