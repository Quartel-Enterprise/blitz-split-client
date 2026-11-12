import { useLayoutEffect, type RefObject } from 'react'

/**
 * Folds a sticky header with the scroll: sets `--p` on `header`, from 0 while its place in the page (`mark`, an empty
 * element right before it) is at the top of the scroller or under it, to 1 once the page scrolled past it by the
 * header's open height less its folded one (its `--open` and `--folded`, index.css). Read on each frame a scroll or a
 * resize asks for, from a passive listener, so the fold follows the finger and never runs on a timer. The scroller
 * is the window's, or a layer's around the page (a group over a bill).
 */
export function useCollapsingHeader(mark: RefObject<HTMLElement | null>, header: RefObject<HTMLElement | null>) {
  useLayoutEffect(() => {
    const element = header.current
    const place = mark.current
    if (!element || !place) return
    const scroller = scrollerOf(element)
    let range = 0
    const measure = () => {
      const style = getComputedStyle(element)
      range = parseFloat(style.getPropertyValue('--open')) - parseFloat(style.getPropertyValue('--folded'))
    }
    let frame = 0
    const update = () => {
      frame = 0
      const top = scroller ? scroller.getBoundingClientRect().top : 0
      const travel = top - place.getBoundingClientRect().top
      const fold = range > 0 ? Math.min(Math.max(travel / range, 0), 1) : 0
      element.style.setProperty('--p', fold.toFixed(4))
    }
    const schedule = () => {
      if (!frame) frame = requestAnimationFrame(update)
    }
    const onResize = () => {
      measure()
      schedule()
    }
    measure()
    update()
    const target: HTMLElement | Window = scroller ?? window
    target.addEventListener('scroll', schedule, { passive: true })
    window.addEventListener('resize', onResize)
    return () => {
      cancelAnimationFrame(frame)
      target.removeEventListener('scroll', schedule)
      window.removeEventListener('resize', onResize)
    }
  }, [mark, header])
}

/** The nearest ancestor that scrolls, or null for the window's. */
function scrollerOf(element: HTMLElement): HTMLElement | null {
  for (let parent = element.parentElement; parent && parent !== document.body; parent = parent.parentElement) {
    const overflow = getComputedStyle(parent).overflowY
    if (overflow === 'auto' || overflow === 'scroll') return parent
  }
  return null
}
