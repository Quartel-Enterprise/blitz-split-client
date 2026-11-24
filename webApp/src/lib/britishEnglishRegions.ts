/**
 * The English regions that write as Britain, in lower case: the children of CLDR's en-001 (the Commonwealth) and en-150
 * (Europe), the world's "001" and Europe's "150" themselves included, as AppLanguage's BritishEnglishRegions and the
 * Apple app's own lookup have them. Every other region (the US, Canada, Japan, Brazil, the Philippines…) reads the
 * American source (Pierre's call).
 */
export const britishEnglishRegions: ReadonlySet<string> = new Set([
  '001', '150', 'ad', 'ag', 'ai', 'al', 'at', 'au', 'ba', 'bb', 'bd', 'be', 'bg', 'bm', 'bn', 'bs', 'bw', 'by', 'bz',
  'cc', 'ch', 'ck', 'cx', 'cy', 'cz', 'de', 'dg', 'dk', 'dm', 'ee', 'es', 'fi', 'fj', 'fk', 'fo', 'fr', 'gb', 'gd',
  'ge', 'gg', 'gh', 'gi', 'gl', 'gm', 'gr', 'gy', 'hk', 'hr', 'hu', 'ie', 'im', 'in', 'io', 'is', 'it', 'je', 'jm',
  'ke', 'ki', 'kn', 'ky', 'lc', 'li', 'lk', 'ls', 'lt', 'lu', 'lv', 'mc', 'md', 'me', 'mk', 'mo', 'ms', 'mt', 'mu',
  'mv', 'mw', 'my', 'na', 'nf', 'ng', 'nl', 'no', 'nr', 'nu', 'nz', 'pg', 'pk', 'pl', 'pn', 'pt', 'ro', 'rs', 'ru',
  'sb', 'sc', 'sd', 'se', 'sg', 'sh', 'si', 'sk', 'sl', 'ss', 'sz', 'tc', 'to', 'tt', 'tv', 'tz', 'ua', 'ug', 'va',
  'vc', 'vg', 'vu', 'ws', 'za', 'zm', 'zw',
])
