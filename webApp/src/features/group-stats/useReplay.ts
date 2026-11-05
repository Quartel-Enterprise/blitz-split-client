import { useEffect, useState } from 'react'

/**
 * False for one frame each time `key` changes, then true: a chart draws its start state (no bars, an empty ring) and
 * lets its CSS transitions grow it to the values, so a new grain or period plays in again.
 */
export function useReplay(key: string): boolean {
  const [isPlayed, setPlayed] = useState(false)
  useEffect(() => {
    setPlayed(false)
    let second = 0
    const first = requestAnimationFrame(() => {
      second = requestAnimationFrame(() => setPlayed(true))
    })
    return () => {
      cancelAnimationFrame(first)
      cancelAnimationFrame(second)
    }
  }, [key])
  return isPlayed
}
