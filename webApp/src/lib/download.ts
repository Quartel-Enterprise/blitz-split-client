/** Saves text as a file through a temporary link. */
export function downloadFile(contents: string, fileName: string, type: string) {
  downloadBlob(new Blob([contents], { type }), fileName)
}

/**
 * Saves a blob as a file through a temporary link. Why: the object URL is revoked a moment later, since WebKit
 * starts reading it after click() returns, and revoking it at once can leave the download empty.
 */
export function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = Object.assign(document.createElement('a'), { href: url, download: fileName })
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
