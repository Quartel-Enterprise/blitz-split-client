import { Copy } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { t } from '@/i18n'
import { cn } from '@/lib/utils'

/**
 * The 12 recovery words, numbered so they are written down in order, and their Copy. They read row by row, the order
 * the restore screen's fields take them in.
 */
export function RecoveryWordsBox({ words, onCopy, columns = 3 }: { words: string[]; onCopy: () => void; columns?: 2 | 3 }) {
  return (
    <div className="flex flex-col gap-1 rounded-lg border border-dashed border-muted-foreground/50 bg-muted px-3.5 pt-3 pb-1.5">
      <ol className={cn('grid gap-x-3 gap-y-1.5', columns === 3 ? 'grid-cols-2 sm:grid-cols-3' : 'grid-cols-2')}>
        {words.map((word, index) => (
          <li key={index} className="flex items-baseline gap-1.5">
            <span className="min-w-5 text-right text-sm text-muted-foreground tabular-nums">{index + 1}</span>
            <span className="font-mono font-semibold">{word}</span>
          </li>
        ))}
      </ol>
      <Button variant="ghost" size="sm" className="self-end" onClick={onCopy}>
        <Copy /> {t('recoveryCode_copy')}
      </Button>
    </div>
  )
}

// A numbered list, one word a line, so the downloaded file is read as it is written down.
export function numberedRecoveryWords(code: string): string {
  return code
    .split(' ')
    .map((word, index) => `${index + 1}. ${word}`)
    .join('\n')
}
