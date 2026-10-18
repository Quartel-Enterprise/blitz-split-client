import type { JsWatch } from 'blitzsplit-shared'
import { useEffect, useState } from 'react'

interface Disposable {
  dispose(): void
}

/**
 * Keeps one shared ViewModel (through its Presenter) for the life of a screen and clears it when the
 * screen goes away, which cancels its viewModelScope like Android does. Created in an effect so
 * StrictMode's mount, unmount and mount never leaves a disposed Presenter behind.
 */
export function usePresenter<P extends Disposable>(create: () => P): P | null {
  const [presenter, setPresenter] = useState<P | null>(null)
  useEffect(() => {
    const created = create()
    setPresenter(created)
    return () => {
      created.dispose()
      setPresenter(null)
    }
    // The presenter belongs to the screen, so it is created once per mount
  }, [])
  return presenter
}

/** Mirrors a Presenter's state flow into React state. */
export function useWatch<P, S>(presenter: P | null, observe: (presenter: P, onChange: (state: S) => void) => JsWatch): S | null {
  const [state, setState] = useState<S | null>(null)
  useEffect(() => {
    if (!presenter) return
    const watch = observe(presenter, setState)
    return () => watch.cancel()
    // The observe function only picks the presenter's flow
  }, [presenter])
  return state
}

/** Runs an effect for each action a Presenter emits (copy, download, open the QR scanner…). */
export function useActions<P, A>(presenter: P | null, observe: (presenter: P, onAction: (action: A) => void) => JsWatch, onAction: (action: A) => void) {
  useEffect(() => {
    if (!presenter) return
    const watch = observe(presenter, onAction)
    return () => watch.cancel()
  }, [presenter])
}
