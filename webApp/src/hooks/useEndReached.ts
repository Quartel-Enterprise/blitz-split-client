import { useEffect, useRef } from 'react'

/** Calls `onEnd` when the end of a paged list (Activity, "All expenses") scrolls into view, while there is more to show. */
export function useEndReached(isEnabled: boolean, itemCount: number, onEnd: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  const onEndRef = useRef(onEnd)
  onEndRef.current = onEnd
  useEffect(() => {
    const element = ref.current
    if (!isEnabled || !element) return
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) onEndRef.current()
    })
    observer.observe(element)
    return () => observer.disconnect()
    // Why: a new page shown re-arms it, so a short page still asks for the next one.
  }, [isEnabled, itemCount])
  return ref
}
