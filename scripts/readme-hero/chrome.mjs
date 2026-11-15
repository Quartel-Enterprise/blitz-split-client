// A headless Chrome driven over the DevTools Protocol, with nothing to install: Node's own WebSocket talks to the
// Chrome already on this Mac. Each browser gets a profile of its own in a temporary folder, so the web app's Room
// (OPFS) and storage start empty and nobody's data is touched.
import { spawn } from 'node:child_process'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const CHROME = process.env.CHROME ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'

export async function launchChrome() {
  const profile = await mkdtemp(join(tmpdir(), 'readme-hero-chrome-'))
  const chrome = spawn(
    CHROME,
    [
      '--headless=new',
      '--remote-debugging-port=0',
      `--user-data-dir=${profile}`,
      '--lang=en-US',
      '--hide-scrollbars',
      '--no-first-run',
      '--no-default-browser-check',
      '--disable-extensions',
      'about:blank',
    ],
    { stdio: 'ignore' },
  )
  const port = await debuggingPortOf(profile)
  const targets = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()
  const page = targets.find((target) => target.type === 'page')
  const socket = new WebSocket(page.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => {
    socket.onopen = resolve
    socket.onerror = reject
  })
  let nextId = 0
  const pending = new Map()
  socket.onmessage = (message) => {
    const { id, result, error } = JSON.parse(message.data)
    if (!pending.has(id)) return
    const { resolve, reject } = pending.get(id)
    pending.delete(id)
    if (error) reject(new Error(error.message))
    else resolve(result)
  }
  const send = (method, params = {}) =>
    new Promise((resolve, reject) => {
      const id = ++nextId
      pending.set(id, { resolve, reject })
      socket.send(JSON.stringify({ id, method, params }))
    })
  await send('Page.enable')
  await send('Runtime.enable')

  return {
    /** Sizes the page in CSS pixels, drawn at `scale` device pixels each. */
    async setViewport(width, height, scale = 2, isDark = false) {
      await send('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: scale, mobile: false })
      await send('Emulation.setEmulatedMedia', {
        features: [{ name: 'prefers-color-scheme', value: isDark ? 'dark' : 'light' }],
      })
    },
    /** Runs `source` in every document before its own scripts. */
    async beforeEachPage(source) {
      await send('Page.addScriptToEvaluateOnNewDocument', { source })
    },
    async open(url) {
      await send('Page.navigate', { url })
    },
    async evaluate(expression) {
      const { result, exceptionDetails } = await send('Runtime.evaluate', {
        expression,
        awaitPromise: true,
        returnByValue: true,
      })
      if (exceptionDetails) throw new Error(exceptionDetails.exception?.description ?? exceptionDetails.text)
      return result.value
    },
    /** Waits until `expression` is truthy in the page. */
    async waitFor(expression, timeoutMs = 60_000) {
      const deadline = Date.now() + timeoutMs
      while (Date.now() < deadline) {
        let isReady = false
        try {
          isReady = await this.evaluate(`Boolean(${expression})`)
        } catch (error) {
          // Why: a page loading or reloading (Vite's dependency reload) has no context to run in for a moment; any
          // other error is the page's or the expression's, which waiting won't fix.
          if (!/context|Cannot find|navigat/i.test(error.message)) throw error
        }
        if (isReady) return
        await new Promise((resolve) => setTimeout(resolve, 250))
      }
      throw new Error(`Timed out waiting for ${expression}`)
    },
    /** Writes the page as a PNG or WebP; a transparent page keeps its transparency. */
    async screenshot(path, { format = 'png', quality } = {}) {
      await send('Emulation.setDefaultBackgroundColorOverride', { color: { r: 0, g: 0, b: 0, a: 0 } })
      const { data } = await send('Page.captureScreenshot', { format, quality, captureBeyondViewport: false })
      await writeFile(path, Buffer.from(data, 'base64'))
    },
    async close() {
      socket.close()
      // Why: a Chrome that already ended (a crash) sends no 'exit' again, which would wait forever.
      if (chrome.exitCode === null && chrome.signalCode === null) {
        const exited = new Promise((resolve) => chrome.once('exit', resolve))
        chrome.kill()
        await exited
      }
      await rm(profile, { recursive: true, force: true })
    },
  }
}

// Why: with port 0 Chrome picks a free one and writes it to its profile once it listens.
async function debuggingPortOf(profile) {
  const deadline = Date.now() + 30_000
  while (Date.now() < deadline) {
    try {
      const [port] = (await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')
      if (port) return Number(port)
    } catch {
      // Not written yet
    }
    await new Promise((resolve) => setTimeout(resolve, 100))
  }
  throw new Error(`Chrome didn't start: is it at ${CHROME}? Set CHROME to its binary otherwise`)
}
