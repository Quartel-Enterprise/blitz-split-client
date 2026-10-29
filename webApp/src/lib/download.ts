/**
 * Saves text as a file through a temporary link. Why: the object URL is revoked a moment later, since WebKit
 * starts reading it after click() returns, and revoking it at once can leave the download empty.
 */
export function downloadFile(contents: string, fileName: string, type: string) {
  const url = URL.createObjectURL(new Blob([contents], { type }))
  const link = Object.assign(document.createElement('a'), { href: url, download: fileName })
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
