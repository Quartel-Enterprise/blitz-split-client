import { attachCrashReporter, findKotlinClassName } from 'blitzsplit-shared'

/**
 * Crash reporting on the web (docs/crash-reporting.md): Sentry, since Firebase has no Crashlytics for the browser.
 * It starts only in a production build with the project's DSN (`VITE_SENTRY_DSN`): without it, nothing is reported.
 * A blocked request (an ad blocker) leaves it off the same way. Like Crashlytics, it doesn't follow Profile's
 * "Share usage data": a report carries no usage.
 */
const dsn = import.meta.env.VITE_SENTRY_DSN ?? ''

/**
 * The only integrations that run: the ones that catch an error, follow its causes and drop a repeated one. Sentry's
 * other defaults add what a report must not carry (breadcrumbs, the console, the page's address, a session), and a
 * list of the ones kept leaves out whatever a newer version adds too. BrowserApiErrors stays out as well: it wraps
 * every timer and listener in a function of Sentry's script, whose frame would pass for one of the app's own.
 */
const kept = ['EventFilters', 'FunctionToString', 'GlobalHandlers', 'LinkedErrors', 'Dedupe']

/** How many of the shared code's failures wait for Sentry to load; a sync reports once per streak, so few ever do. */
const maxWaiting = 10

/** How far down a failure's causes the class names are looked up; Sentry follows fewer. */
const maxCauses = 10

export async function startCrashReporting(): Promise<void> {
  if (import.meta.env.DEV || dsn === '') return
  // Why: the shared code counts a failure as reported once it is handed over, so the ones recorded while Sentry
  // still loads wait here instead of being lost
  const waiting: unknown[] = []
  let report = (error: unknown): void => {
    if (waiting.length < maxWaiting) waiting.push(error)
  }
  attachCrashReporter((error) => report(error))
  try {
    // Why: loaded only here, so a build without the DSN downloads no Sentry
    const { init, captureException } = await import('@sentry/browser')
    // The failures the shared code recorded: their message is already only class names, or written to be reported
    const recorded = new WeakSet<object>()
    init({
      dsn,
      release: __APP_VERSION__,
      // Why: the betas and the stable versions report to one project, told apart by this
      environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || 'production',
      // Why: each is on by default; with userInfo off, Sentry doesn't take the request's IP address either
      dataCollection: { userInfo: false, cookies: false, httpHeaders: false, httpBodies: [], urlQueryParams: false, stackFrameVariables: false },
      maxBreadcrumbs: 0,
      integrations: (defaults) => defaults.filter((integration) => kept.includes(integration.name)),
      beforeSend: (event, hint) => redacted(event, hint.originalException, isObject(hint.originalException) && recorded.has(hint.originalException)),
    })
    report = (error) => {
      if (isObject(error)) recorded.add(error)
      captureException(error)
    }
    waiting.splice(0).forEach(report)
  } catch {
    // A page that can't load the reporter works as before
    report = () => {}
    waiting.length = 0
  }
}

/**
 * The same rule as Crashlytics' non-fatals: only a failure's class names and stack leave the browser. An uncaught
 * error's message goes blank, cause by cause, since supabase-kt's hold the request's URL and a failing row's values;
 * so do the page's address (a group's invite code is in it), what led to the error (requests, taps, the console) and
 * whatever a rejected promise carried. An error left with no frame of the app's own scripts isn't sent: it came from
 * an extension or an injected script, or it is a rejection with nothing to read.
 */
function redacted<Reported extends RedactableEvent>(event: Reported, original: unknown, isRecorded: boolean): Reported | null {
  const failures = event.exception?.values ?? []
  nameByKotlinClass(failures, original)
  failures.forEach((failure) => {
    if (!isRecorded) failure.value = ''
    if (failure.stacktrace?.frames) failure.stacktrace.frames = failure.stacktrace.frames.flatMap(ownFrame)
  })
  if (!failures.some((failure) => (failure.stacktrace?.frames?.length ?? 0) > 0)) return null
  delete event.message
  delete event.breadcrumbs
  delete event.request
  delete event.extra
  delete event.user
  return event
}

/**
 * With its message blank, a failure is told apart by its class alone, and a Kotlin class reaches the browser under
 * the short name the bundler gave its constructor. Sentry numbers a failure and its causes from the failure down, so
 * each one takes the name of the Kotlin class at its place in the chain; an error of the browser's own keeps its name.
 */
function nameByKotlinClass(failures: Failure[], original: unknown): void {
  const chain: unknown[] = []
  for (let error = original; isObject(error) && chain.length < maxCauses; error = (error as { cause?: unknown }).cause) chain.push(error)
  failures.forEach((failure) => {
    const source = failure.mechanism?.source
    if (source !== undefined && source !== 'cause') return
    const name = findKotlinClassName(chain[failure.mechanism?.exception_id ?? 0])
    if (name) failure.type = name
  })
}

/**
 * Sentry reads a stack line by line, and an uncaught Kotlin failure's stack holds its message and its causes'
 * ("Caused by: …"), which a URL in them turns into a frame. So only a frame at a line of one of the app's own scripts
 * stays, as the script's address without query, with a function name that can't be a sentence.
 */
function ownFrame(frame: StackFrame): StackFrame[] {
  const script = ownScript(frame.filename)
  if (script === null || typeof frame.lineno !== 'number') return []
  const name = frame.function !== undefined && /^[^\s:]+( [^\s:]+)?$/.test(frame.function) ? frame.function : undefined
  return [{ filename: script, function: name, lineno: frame.lineno, colno: frame.colno, in_app: true }]
}

function ownScript(filename: string | undefined): string | null {
  try {
    const url = new URL(filename ?? '')
    return url.origin === location.origin && /\.m?js$/.test(url.pathname) ? url.origin + url.pathname : null
  } catch {
    return null
  }
}

interface StackFrame {
  filename?: string
  function?: string
  lineno?: number
  colno?: number
  in_app?: boolean
}

interface Failure {
  type?: string
  value?: string
  stacktrace?: { frames?: StackFrame[] }
  mechanism?: { source?: string; exception_id?: number }
}

interface RedactableEvent {
  exception?: { values?: Failure[] }
  message?: unknown
  breadcrumbs?: unknown
  request?: unknown
  extra?: unknown
  user?: unknown
}

function isObject(value: unknown): value is object {
  return typeof value === 'object' && value !== null
}
