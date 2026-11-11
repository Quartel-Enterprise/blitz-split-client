import { useCallback, useEffect, useRef, useState, type RefObject } from 'react'

// Why: how far a scroll goes one way before the bars follow it, so a finger's jitter never flickers them.
const DIRECTION_THRESHOLD_PX = 8

// Why: the frame's phone layout, under Tailwind's md, where the header and the bottom navigation show.
const PHONE_QUERY = '(max-width: 767.98px)'

/**
 * Whether the phone's header and bottom navigation have left: a scroll down hides them, a scroll up brings them back,
 * as Android's bars do (docs/product/home-and-shell.md). They come back near the top, while the page is too short to
 * scroll, from the tablet up, and on every new page (pageKey): hidden only for the page they left on. The scroll may be
 * the window's or a layer's around the frame (a page over a bill), so any scroller holding `root` counts. The second
 * value brings them back, for a bar that takes the keyboard's focus.
 */
export function usePhoneBarsHidden(root: RefObject<HTMLElement | null>, pageKey: string): [boolean, () => void] {
  // Why: the page the bars left on, so another page shows them without an effect resetting the state.
  const [hiddenOn, setHiddenOn] = useState<string | null>(null)
  const pageKeyRef = useRef(pageKey)
  // Why: what the scroll listener reads, set with the state, so a new page or a focused bar never leaves it stale.
  const hiddenOnRef = useRef<string | null>(null)
  useEffect(() => {
    pageKeyRef.current = pageKey
    // Why: forgets the page they left on once another shows (the frame stays), so going back to it brings them out.
    if (hiddenOnRef.current !== null && hiddenOnRef.current !== pageKey) {
      hiddenOnRef.current = null
      setHiddenOn(null)
    }
  }, [pageKey])
  const setHidden = useCallback((hidden: boolean) => {
    hiddenOnRef.current = hidden ? pageKeyRef.current : null
    setHiddenOn(hiddenOnRef.current)
  }, [])

  useEffect(() => {
    const phone = window.matchMedia(PHONE_QUERY)
    // Why: per scroller, the furthest point reached the way the bars last went, which the threshold counts from.
    const anchors = new WeakMap<Element, number>()
    const isHidden = () => hiddenOnRef.current !== null && hiddenOnRef.current === pageKeyRef.current
    // Why: the scroller the bars left with, the window's or a layer's, which the resize check reads.
    let hiddenBy: Element | null = null
    const show = () => setHidden(false)
    const hide = (scroller: Element) => {
      hiddenBy = scroller
      setHidden(true)
    }
    const scrollerOf = (target: EventTarget | null): Element | null => {
      const element = target === document ? document.scrollingElement : target
      const frame = root.current
      if (!(element instanceof Element) || !frame) return null
      return element === document.scrollingElement || element.contains(frame) ? element : null
    }
    const onScroll = (event: Event) => {
      const scroller = scrollerOf(event.target)
      if (!scroller) return
      const range = scroller.scrollHeight - scroller.clientHeight
      // Why: Safari's bounce scrolls past both ends, which is no direction.
      const top = Math.min(Math.max(scroller.scrollTop, 0), Math.max(range, 0))
      const anchor = anchors.get(scroller) ?? top
      if (!phone.matches || range <= 0 || top <= DIRECTION_THRESHOLD_PX) {
        anchors.set(scroller, top)
        if (isHidden()) show()
      } else if (isHidden()) {
        anchors.set(scroller, Math.max(anchor, top))
        if (top < anchor - DIRECTION_THRESHOLD_PX) {
          anchors.set(scroller, top)
          show()
        }
      } else {
        anchors.set(scroller, Math.min(anchor, top))
        if (top > anchor + DIRECTION_THRESHOLD_PX) {
          anchors.set(scroller, top)
          hide(scroller)
        }
      }
    }
    // Why: a page that shrank under the bars (a filter, a list emptied) or a window grown past the phone shows them.
    const onResize = () => {
      const scroller = hiddenBy?.isConnected ? hiddenBy : document.scrollingElement
      if (isHidden() && (!phone.matches || !scroller || scroller.scrollHeight <= scroller.clientHeight)) show()
    }
    const observer = new ResizeObserver(onResize)
    if (root.current) observer.observe(root.current)
    document.addEventListener('scroll', onScroll, { capture: true, passive: true })
    phone.addEventListener('change', onResize)
    return () => {
      observer.disconnect()
      document.removeEventListener('scroll', onScroll, { capture: true })
      phone.removeEventListener('change', onResize)
    }
  }, [root, setHidden])

  return [hiddenOn !== null && hiddenOn === pageKey, () => setHidden(false)]
}
