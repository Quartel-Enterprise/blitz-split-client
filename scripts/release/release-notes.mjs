#!/usr/bin/env node
// The release notes of the app, from the fragments the pull requests leave in release-notes/unreleased/ to what a
// release publishes: the version's files of blitz-split-site (its "What's new" page and the feed the apps read), the
// text the testers and the GitHub release get, and the stores' "What's new". docs/release-notes.md says what a
// fragment takes and how a release reads them.
//
//   node scripts/release/release-notes.mjs check
//       Fails on a fragment of the working tree that a release couldn't read (CI, and before a commit).
//   node scripts/release/release-notes.mjs next-version [--ref <commit>]
//       Prints the version the fragments lead to, X.Y.Z, or nothing while no stable version is tagged.
//   node scripts/release/release-notes.mjs collect <version> [--ref <commit>] [--date YYYY-MM-DD]
//                                 [--site <blitz-split-site checkout>] [--out <folder>] [--releases <file>]
//       Picks the notes of <version> (3.1.0, 3.1.0-beta_2). --site writes them as that repository's files, unless it
//       already has the version, whose notes then are the ones every output gets. --out writes github.md, the
//       testers' text and, for a stable version, the stores' text. --releases is the GitHub releases as JSON (each a
//       tag and a body), which lets a platform's testers read every note since the last beta that sent them a build.
//
// Plain Node, no dependencies: the release workflow and the check run it as it is.

import { execFileSync } from "node:child_process";
import { appendFileSync, existsSync, mkdirSync, readdirSync, readFileSync, realpathSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

/**
 * The languages of the notes, English first: the ones blitz-split-site's release notes have. Portugal's Portuguese,
 * pt-PT, is one of its own there, written in full, not the few words over Brazil's that the apps keep for it.
 */
export const LANGS = ["en", "pt", "pt-PT", "es", "fr", "de", "it", "pl", "nl", "ja", "tr", "id", "ru", "uk", "ko", "hi", "ar", "zh", "he", "th", "vi", "zh-Hant", "sv", "cs", "ro", "bn", "ms", "el", "hr", "hu", "ur", "fil", "sr", "sk", "fa", "ta", "nb", "te", "bg", "mr", "sw", "ca", "da", "fi", "lv", "et"];
/** The platforms a note names, in the order it lists them; "all" stands alone, for every one of them. */
export const PLATFORMS = ["android", "ios", "mac", "web"];
/** What a note is, in the order a version lists its notes. Only "fixed" leaves the version a patch. */
export const KINDS = ["new", "improved", "fixed"];
export const FRAGMENTS_DIR = "release-notes/unreleased";
/** The language of the text the testers get with a build (Firebase App Distribution takes one text). */
export const TESTERS_LANG = "pt";
/**
 * The platforms whose testers get a text with the build. They are also the names a release's GitHub notes list in
 * their "Sent to testers:" line (release-beta.yml's finalize writes it), which says whose build went out.
 */
export const TESTERS = ["android", "ios"];
const SENT_TO_TESTERS = "Sent to testers:";
/** What a store takes as a version's "What's new", in characters. */
export const STORE_LIMITS = { play: 500, appStore: 4000 };

const VERSION = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-beta_([1-9]\d*))?$/;
const FRAGMENT_NAME = /^[a-z0-9][a-z0-9-]*\.json$/;
const PLATFORM_NAMES = { android: "Android", ios: "iPhone and iPad", mac: "Mac", web: "Web" };
/** What a store shows for a version with no note for its platform: a store refuses an empty "What's new". */
const NOTHING_TO_SAY = {
  en: "Bug fixes and performance improvements.",
  pt: "Correções de bugs e melhorias de desempenho.",
  "pt-PT": "Correções de erros e melhorias de desempenho.",
  es: "Correcciones de errores y mejoras de rendimiento.",
  fr: "Corrections de bugs et améliorations des performances.",
  de: "Fehlerbehebungen und Leistungsverbesserungen.",
  it: "Correzioni di bug e miglioramenti delle prestazioni.",
  pl: "Poprawki błędów i ulepszenia wydajności.",
  nl: "Bugfixes en prestatieverbeteringen.",
  ja: "バグの修正とパフォーマンスの改善。",
  tr: "Hata düzeltmeleri ve performans iyileştirmeleri.",
  id: "Perbaikan bug dan peningkatan performa.",
  ru: "Исправления ошибок и улучшения производительности.",
  uk: "Виправлення помилок і покращення продуктивності.",
  ko: "버그를 수정하고 성능을 개선했어요.",
  hi: "बग ठीक किए गए और परफ़ॉर्मेंस बेहतर हुई।",
  ar: "إصلاح الأخطاء وتحسين الأداء.",
  zh: "修复了一些问题，并提升了性能。",
  he: "תיקוני באגים ושיפורי ביצועים.",
  th: "แก้ไขข้อบกพร่องและปรับปรุงประสิทธิภาพ",
  vi: "Sửa lỗi và cải thiện hiệu năng.",
  "zh-Hant": "修正錯誤並提升效能。",
  sv: "Felrättningar och prestandaförbättringar.",
  cs: "Opravy chyb a vylepšení výkonu.",
  ro: "Remedieri de erori și îmbunătățiri de performanță.",
  bn: "বাগ সংশোধন ও পারফরম্যান্সের উন্নতি।",
  ms: "Pembetulan pepijat dan peningkatan prestasi.",
  el: "Διορθώσεις σφαλμάτων και βελτιώσεις απόδοσης.",
  hr: "Ispravci pogrešaka i poboljšanja performansi.",
  hu: "Hibajavítások és teljesítménybeli fejlesztések.",
  ur: "خرابیوں کی درستی اور کارکردگی میں بہتری۔",
  fil: "Mga pag-aayos ng bug at pagpapahusay ng performance.",
  sr: "Ispravke grešaka i poboljšanja performansi.",
  sk: "Opravy chýb a vylepšenia výkonu.",
  fa: "رفع اشکال‌ها و بهبود عملکرد.",
  ta: "பிழைத் திருத்தங்கள் மற்றும் செயல்திறன் மேம்பாடுகள்.",
  nb: "Feilrettinger og ytelsesforbedringer.",
  te: "బగ్ పరిష్కారాలు మరియు పనితీరు మెరుగుదలలు.",
  bg: "Поправки на грешки и подобрения в производителността.",
  mr: "बग दुरुस्त्या आणि कार्यक्षमतेत सुधारणा.",
  sw: "Marekebisho ya hitilafu na maboresho ya utendaji.",
  ca: "Correccions d’errors i millores de rendiment.",
  da: "Fejlrettelser og forbedringer af ydeevnen.",
  fi: "Virheenkorjauksia ja suorituskyvyn parannuksia.",
  lv: "Kļūdu labojumi un veiktspējas uzlabojumi.",
  et: "Veaparandused ja jõudluse täiustused.",
};

export class NotesError extends Error {}
const fail = (file, message) => {
  throw new NotesError(`${file}: ${message}`);
};

// --- A fragment ------------------------------------------------------------------------------------------------------

function only(file, value, keys, what) {
  if (value === null || typeof value !== "object" || Array.isArray(value)) fail(file, `${what} is an object`);
  const other = Object.keys(value).find((key) => !keys.includes(key));
  if (other !== undefined) fail(file, `${what} takes only ${keys.join(", ")}, not ${other}`);
}

/** Whether a note is for every platform: `["all"]` and nothing else, a nested list or a longer one included. */
const isForAll = (platforms) => platforms.length === 1 && platforms[0] === "all";

function checkPlatforms(file, what, platforms) {
  const choices = `["all"], or some of ${PLATFORMS.join(", ")}`;
  if (!Array.isArray(platforms) || platforms.length === 0) fail(file, `${what}'s platforms is a list: ${choices}`);
  if (isForAll(platforms)) return;
  const unknown = platforms.find((platform) => !PLATFORMS.includes(platform));
  if (unknown !== undefined) fail(file, `${what}'s platforms takes ${choices}, not ${JSON.stringify(unknown)}`);
  if (new Set(platforms).size !== platforms.length) fail(file, `${what} names a platform twice`);
  if (platforms.length === PLATFORMS.length) fail(file, `${what} names every platform: write ["all"]`);
  const ordered = PLATFORMS.filter((platform) => platforms.includes(platform));
  if (platforms.join() !== ordered.join()) fail(file, `${what}'s platforms come in the order ${PLATFORMS.join(", ")}`);
}

/** The rules of blitz-split-site's notes: the apps and the page draw a note as one plain line. */
function checkText(file, what, text) {
  if (typeof text !== "string" || text.trim() === "" || text !== text.trim() || /[\n\r\t]/.test(text)) {
    fail(file, `${what} is one line, with no space around it`);
  }
  if (/[<>`]|\*\*|__|\]\(/.test(text)) fail(file, `${what} is plain, with no Markdown and no HTML`);
}

/**
 * A fragment's notes, checked: each a kind, the platforms it is for, its text in every language, and whether only
 * the beta versions tell it.
 */
export function parseFragment(file, source) {
  let fragment;
  try {
    fragment = JSON.parse(source);
  } catch (error) {
    fail(file, `not JSON: ${error.message}`);
  }
  only(file, fragment, ["notes"], "the file");
  if (!Array.isArray(fragment.notes) || fragment.notes.length === 0) fail(file, "notes is a list with at least one note");
  return fragment.notes.map((note, index) => {
    const what = `note ${index + 1}`;
    only(file, note, ["kind", "platforms", "betaOnly", "text"], what);
    if (!KINDS.includes(note.kind)) fail(file, `${what}'s kind is one of ${KINDS.join(", ")}`);
    checkPlatforms(file, what, note.platforms);
    if (note.betaOnly !== undefined && typeof note.betaOnly !== "boolean") fail(file, `${what}'s betaOnly is true or false`);
    only(file, note.text, LANGS, `${what}'s text`);
    for (const lang of LANGS) {
      if (!(lang in note.text)) fail(file, `${what} has no text in ${lang}: every note is written in ${LANGS.join(", ")}`);
      checkText(file, `${what}'s text in ${lang}`, note.text[lang]);
    }
    return { kind: note.kind, platforms: note.platforms, betaOnly: note.betaOnly === true, text: note.text };
  });
}

function checkName(path) {
  const name = path.slice(path.lastIndexOf("/") + 1);
  if (!FRAGMENT_NAME.test(name)) fail(path, "a fragment is <branch-description>.json, in lowercase letters, digits and hyphens");
}

// --- Which notes a version tells -------------------------------------------------------------------------------------

/** A version's name as numbers to order by: a version's betas come before it, in their own order. */
export function versionKey(name) {
  const match = VERSION.exec(name);
  if (!match) throw new NotesError(`${name}: a version is X.Y.Z or X.Y.Z-beta_N`);
  const [, major, minor, patch, beta] = match;
  return [Number(major), Number(minor), Number(patch), beta === undefined ? Infinity : Number(beta)];
}

export function compareVersions(a, b) {
  const [x, y] = [versionKey(a), versionKey(b)];
  const at = x.findIndex((part, index) => part !== y[index]);
  return at < 0 ? 0 : x[at] > y[at] ? 1 : -1;
}

export const isBeta = (version) => version.includes("-beta_");

/**
 * The release a version's notes start after, among the tags: for a beta, the release right before it, since the notes
 * that one told aren't told again; for a stable version, the stable version before it, since it tells everything the
 * betas since then told.
 */
export function releaseBefore(version, tags) {
  const earlier = tags
    .filter((tag) => VERSION.test(tag) && compareVersions(tag, version) < 0)
    .filter((tag) => isBeta(version) || !isBeta(tag))
    .sort(compareVersions);
  return earlier.at(-1) ?? null;
}

/**
 * What a note is known by from one release to the next: its fragment's file and its English text. Not the file
 * alone, since a later pull request may add a note to a file a release already read.
 */
export const noteKey = (path, english) => `${path}\n${english}`;

/**
 * The notes a version tells, from the fragments of the commit it is built from (path and notes each, in the order of
 * their paths): the ones the release before it didn't have (`toldBefore`, their keys), in the order new, improved,
 * fixed. A stable version leaves out the notes only a beta tells.
 */
export function selectNotes(fragments, toldBefore, stable) {
  const notes = fragments
    .flatMap((fragment) => fragment.notes.filter((note) => !toldBefore.has(noteKey(fragment.path, note.text.en))))
    .filter((note) => !(stable && note.betaOnly));
  return KINDS.flatMap((kind) => notes.filter((note) => note.kind === kind));
}

/**
 * Whether a release sent a platform's testers a build, as the "Sent to testers:" line of its GitHub notes says. A
 * release without the line, one from before the releases wrote it, sent nothing as far as the notes go.
 */
export function sentToTesters(body, platform) {
  const line = (body ?? "").split(/\r?\n/).find((each) => each.startsWith(SENT_TO_TESTERS));
  if (line === undefined) return false;
  return line.slice(SENT_TO_TESTERS.length).split(",").some((name) => name.trim() === platform);
}

/** The version after `stable` that these notes, the ones since it, lead to: a minor one unless they only fix. */
export function bump(stable, notes) {
  const [major, minor, patch] = versionKey(stable);
  const adds = notes.some((note) => !note.betaOnly && note.kind !== "fixed");
  return adds ? `${major}.${minor + 1}.0` : `${major}.${minor}.${patch + 1}`;
}

// --- What a release publishes ----------------------------------------------------------------------------------------

const isFor = (note, platform) => isForAll(note.platforms) || note.platforms.includes(platform);

/** A version's file of blitz-split-site in one language (its docs/release-notes.md). */
export function siteFile(notes, lang, date) {
  return { date, notes: notes.map((note) => ({ text: note.text[lang], platforms: note.platforms })) };
}

/** The notes as the GitHub release lists them: in English, each naming its platforms unless it is for all. */
export function githubNotes(notes) {
  if (notes.length === 0) return "";
  const line = (note) => {
    const platforms = isForAll(note.platforms) ? "" : ` (${note.platforms.map((platform) => PLATFORM_NAMES[platform]).join(", ")})`;
    return `- ${note.text.en}${platforms}`;
  };
  return `## What's new\n\n${notes.map(line).join("\n")}\n`;
}

const lines = (notes, platform, lang) => notes.filter((note) => isFor(note, platform)).map((note) => `• ${note.text[lang]}`);

/** The notes of one platform as a list in plain text, one per line; empty with none. */
export const plainNotes = (notes, platform, lang) => lines(notes, platform, lang).join("\n");

/**
 * A store's "What's new" for one platform: its notes while they fit the store's limit, the last ones left out first,
 * and a sentence that says nothing when the version has no note for the platform. A first note longer than the limit
 * is cut, so a version with something to say never gets that sentence.
 */
export function storeNotes(notes, platform, lang, limit) {
  const all = lines(notes, platform, lang);
  if (all.length === 0) return NOTHING_TO_SAY[lang];
  const fitting = [...all];
  while (fitting.length > 0 && fitting.join("\n").length > limit) fitting.pop();
  return fitting.length > 0 ? fitting.join("\n") : `${all[0].slice(0, limit - 1)}…`;
}

// --- Git and files ---------------------------------------------------------------------------------------------------

const git = (...args) => execFileSync("git", args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });

/** The fragments' paths in a commit, in order. */
function fragmentPathsAt(ref) {
  const paths = git("ls-tree", "--name-only", ref, `${FRAGMENTS_DIR}/`).split("\n");
  return paths.filter((path) => path.endsWith(".json")).sort();
}

function fragmentsAt(ref) {
  return fragmentPathsAt(ref).map((path) => {
    checkName(path);
    return { path, notes: parseFragment(path, git("show", `${ref}:${path}`)) };
  });
}

/** The versions released from a commit or from one before it. */
const tagsAt = (ref) => git("tag", "--merged", ref).split("\n").filter((tag) => VERSION.test(tag));

const toldAtRef = new Map();

/**
 * The keys of the notes a release's commit had. Read without checking them: a fragment of an older release may
 * predate a rule of today's (a language added since), and one it couldn't read told nothing. Kept per release, since
 * a run asks for the same one for the version and for each platform's testers.
 */
function toldAt(ref) {
  if (toldAtRef.has(ref)) return toldAtRef.get(ref);
  const keys = new Set();
  for (const path of fragmentPathsAt(ref)) {
    try {
      for (const note of JSON.parse(git("show", `${ref}:${path}`)).notes) keys.add(noteKey(path, note.text.en));
    } catch {
      continue;
    }
  }
  toldAtRef.set(ref, keys);
  return keys;
}

/** The notes `version` tells from a commit's fragments, counted from the release before it among `tags`. */
function notesOf(version, fragments, tags) {
  const before = releaseBefore(version, tags);
  return selectNotes(fragments, before === null ? new Set() : toldAt(before), !isBeta(version));
}

/** The version the fragments lead to, or null while no stable version is tagged: version.properties says it then. */
export function nextVersion(ref) {
  const stable = tagsAt(ref).filter((tag) => !isBeta(tag)).sort(compareVersions).at(-1);
  if (stable === undefined) return null;
  return bump(stable, selectNotes(fragmentsAt(ref), toldAt(stable), true));
}

const siteVersionFile = (site, lang, version) => join(site, "release-notes", lang, `${version}.json`);

/**
 * The languages the site's release notes have: its folders. The site's, not `LANGS`, are the ones written, since the
 * two repositories gain a language in pull requests of their own and either may merge first: a language only the
 * fragments have waits for the site, and one only the site has stops the release, which would leave it without notes.
 */
function siteLangs(site) {
  const dir = join(site, "release-notes");
  if (!existsSync(dir)) fail(dir, "the folder is missing: not a checkout of blitz-split-site");
  const langs = readdirSync(dir).filter((name) => !name.startsWith("."));
  const unknown = langs.find((lang) => !LANGS.includes(lang));
  if (unknown !== undefined) fail(join(dir, unknown), `the site has release notes in ${unknown} and the fragments don't: add it to LANGS and to the fragments`);
  return langs;
}

/**
 * The language a missing one is read as: a region's (pt-PT) as its language's (pt), any other as English, a script's
 * (zh-Hant) too, since its language's (zh) is written in another script.
 */
const fallbackOf = (lang) => {
  const [language, subtag] = lang.split("-");
  return subtag !== undefined && subtag.length !== 4 ? language : "en";
};

/** A version blitz-split-site already has, written by hand or by an earlier run, as notes; null without it. */
function readSiteVersion(site, version) {
  if (!existsSync(siteVersionFile(site, "en", version))) return null;
  const langs = siteLangs(site);
  const files = Object.fromEntries(
    LANGS.map((lang) => {
      const file = siteVersionFile(site, langs.includes(lang) ? lang : fallbackOf(lang), version);
      if (!existsSync(file)) fail(file, "the file is missing: the site has this version in English");
      return [lang, JSON.parse(readFileSync(file, "utf8"))];
    }),
  );
  for (const lang of LANGS) {
    if (files[lang].notes?.length !== files.en.notes?.length) fail(siteVersionFile(site, lang, version), "not as many notes as in English");
  }
  return files.en.notes.map((note, index) => ({
    kind: null,
    platforms: note.platforms,
    betaOnly: false,
    text: Object.fromEntries(LANGS.map((lang) => [lang, files[lang].notes[index].text])),
  }));
}

function write(file, text) {
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, text);
}

function writeSiteVersion(site, version, date, notes) {
  for (const lang of siteLangs(site)) write(siteVersionFile(site, lang, version), `${JSON.stringify(siteFile(notes, lang, date), null, 2)}\n`);
}

function writeOutputs(out, version, notes, testers) {
  write(join(out, "github.md"), githubNotes(notes));
  for (const platform of TESTERS) write(join(out, "testers", `${platform}.txt`), plainNotes(testers[platform], platform, TESTERS_LANG));
  if (isBeta(version)) return;
  for (const lang of LANGS) {
    write(join(out, "stores", "play", `${lang}.txt`), storeNotes(notes, "android", lang, STORE_LIMITS.play));
    write(join(out, "stores", "app-store-ios", `${lang}.txt`), storeNotes(notes, "ios", lang, STORE_LIMITS.appStore));
    write(join(out, "stores", "app-store-mac", `${lang}.txt`), storeNotes(notes, "mac", lang, STORE_LIMITS.appStore));
  }
}

// --- The commands ----------------------------------------------------------------------------------------------------

/** Today where the app is released from, so a beta sent in the evening isn't dated the day after. */
const today = () => new Intl.DateTimeFormat("en-CA", { timeZone: "America/Sao_Paulo" }).format(new Date());

function check() {
  if (!existsSync(FRAGMENTS_DIR)) return 0;
  // Why: a hidden file a system leaves (macOS's .DS_Store) isn't a misnamed fragment.
  const names = readdirSync(FRAGMENTS_DIR).filter((name) => !name.startsWith("."));
  for (const name of names) {
    const path = `${FRAGMENTS_DIR}/${name}`;
    checkName(path);
    parseFragment(path, readFileSync(path, "utf8"));
  }
  return names.length;
}

/**
 * What each platform's testers read: the version's notes, or with the releases at hand every note since the last one
 * that sent them a build, so a beta that left a platform out doesn't keep its notes from that platform's testers. A
 * tag with no release among them sent nothing, which tells a note again rather than never.
 */
function testersNotes(version, notes, fragments, tags, releases) {
  if (releases === undefined) return Object.fromEntries(TESTERS.map((platform) => [platform, notes]));
  const bodies = new Map(JSON.parse(readFileSync(releases, "utf8")).map((release) => [release.tag, release.body]));
  return Object.fromEntries(
    TESTERS.map((platform) => [platform, notesOf(version, fragments, tags.filter((tag) => sentToTesters(bodies.get(tag), platform)))]),
  );
}

function collect(version, { ref = "HEAD", date = today(), site, out, releases }) {
  versionKey(version);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new NotesError(`${date}: a date is YYYY-MM-DD`);
  const inSite = site === undefined ? null : readSiteVersion(site, version);
  const [fragments, tags] = [fragmentsAt(ref), tagsAt(ref)];
  const notes = inSite ?? notesOf(version, fragments, tags);
  const writesSite = site !== undefined && inSite === null && notes.length > 0;
  if (writesSite) writeSiteVersion(site, version, date, notes);
  if (out !== undefined) writeOutputs(out, version, notes, testersNotes(version, notes, fragments, tags, releases));
  if (process.env.GITHUB_OUTPUT) appendFileSync(process.env.GITHUB_OUTPUT, `has_notes=${notes.length > 0}\n`);
  const from = inSite === null ? "the fragments" : "the site, which already has the version";
  console.log(`${version}: ${notes.length} note(s) from ${from}${writesSite ? `, written to ${site}` : ""}`);
}

function options(args, names) {
  const found = {};
  const rest = [];
  for (let index = 0; index < args.length; index += 1) {
    const name = args[index].replace(/^--/, "");
    if (!args[index].startsWith("--")) rest.push(args[index]);
    // Why the value can't start with --: `--ref --out dir` would otherwise read "--out" as the commit.
    else if (!names.includes(name) || args[index + 1] === undefined || args[index + 1].startsWith("--")) throw new NotesError(`${args[index]}: takes one of --${names.join(", --")}, each with a value`);
    else found[name] = args[(index += 1)];
  }
  return [rest, found];
}

function main([command, ...args]) {
  // The paths are the repository's, wherever the script is run from; --site and --out are the caller's.
  const cwd = process.cwd();
  const fromCaller = (path) => (path === undefined ? undefined : resolve(cwd, path));
  process.chdir(join(dirname(fileURLToPath(import.meta.url)), "..", ".."));
  if (command === "check") {
    console.log(`${check()} fragment(s) in ${FRAGMENTS_DIR}/, all readable`);
  } else if (command === "next-version") {
    const [, { ref = "HEAD" }] = options(args, ["ref"]);
    const version = nextVersion(ref);
    if (version !== null) console.log(version);
  } else if (command === "collect") {
    const [[version], found] = options(args, ["ref", "date", "site", "out", "releases"]);
    if (version === undefined) throw new NotesError("collect takes the version: 3.1.0, 3.1.0-beta_2");
    collect(version, { ...found, site: fromCaller(found.site), out: fromCaller(found.out), releases: fromCaller(found.releases) });
  } else {
    throw new NotesError("Usage: release-notes.mjs check | next-version [--ref <commit>] | collect <version> [--ref <commit>] [--date YYYY-MM-DD] [--site <folder>] [--out <folder>] [--releases <file>]");
  }
}

// Only when run, not when the tests import it. The real path, since a folder reached through a link (macOS's /tmp)
// isn't spelled as import.meta.url spells it.
if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    if (!(error instanceof NotesError)) throw error;
    console.error(error.message);
    process.exit(1);
  }
}
