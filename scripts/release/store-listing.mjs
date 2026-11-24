#!/usr/bin/env node
// The stores' listing text: the app's name, subtitle, keywords and description on Google Play and the App Store,
// and the Play preview video's address, one file per language in store-listing/. docs/store-listing.md says what a
// file takes and how it reaches the stores.
//
//   node scripts/release/store-listing.mjs check
//       Fails on a listing a store would refuse or cut: a language missing, a field over its limit (CI, and before
//       a commit).
//   node scripts/release/store-listing.mjs stage --out <folder>
//       Writes every listing in the layout fastlane uploads, a folder per store locale: <folder>/play/<locale>/ for
//       supply and <folder>/app-store/<locale>/ for deliver, with the App Store's support, marketing and privacy
//       policy addresses and the version's copyright line. `fastlane upload_listing` runs it.
//   node scripts/release/store-listing.mjs copyright
//       Prints the copyright line of an App Store version, with the current year: the release writes it on the
//       version it submits (`fastlane app_store_submit`).
//
// Plain Node, no dependencies: the check and the lane run it as it is.

import { existsSync, mkdirSync, readFileSync, readdirSync, realpathSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { LANGS } from "./release-notes.mjs";

export const LISTING_DIR = "store-listing";

/**
 * The listings of a release notes' language that the stores list by region too, right after the language's own: Spain's
 * Spanish after Latin America's, and Britain's, Australia's and Canada's English after the American one. People there
 * search and read in other words ("ticket", "móvil", "os pagáis"; "flatmates", "holiday", "mates"), as with Portugal's
 * Portuguese, so each has a listing written in full. Their "What's new" is the language's (the Fastfile's
 * with_notes_for).
 */
const REGIONAL_LISTINGS = { en: ["en", "en-GB", "en-AU", "en-CA"], es: ["es", "es-ES"] };

/** The languages a listing is written in: the release notes' and the regional ones of REGIONAL_LISTINGS. */
export const LISTING_LANGS = LANGS.flatMap((lang) => REGIONAL_LISTINGS[lang] ?? [lang]);

/**
 * A listing's fields, in the order its file has them: the heading that starts each one, the most characters its
 * store takes, and whether it is one line. The description is one text for both stores. An optional field may be
 * left out: the Play video is the language's store preview video on YouTube (docs/store-video.md), and a language
 * without one keeps whatever video its Play listing has.
 */
export const FIELDS = [
  { key: "playTitle", heading: "Play title", limit: 30, isLine: true },
  { key: "playShortDescription", heading: "Play short description", limit: 80, isLine: true },
  { key: "playVideo", heading: "Play video", limit: 100, isLine: true, isOptional: true },
  { key: "appStoreName", heading: "App Store name", limit: 30, isLine: true },
  { key: "appStoreSubtitle", heading: "App Store subtitle", limit: 30, isLine: true },
  { key: "appStoreKeywords", heading: "App Store keywords", limit: 100, isLine: true },
  { key: "appStorePromotionalText", heading: "App Store promotional text", limit: 170, isLine: true },
  { key: "description", heading: "Description", limit: 4000, isLine: false },
];

/**
 * The stores' locales each language's listing goes to, the codes the screenshots are staged under too
 * (listing-art/stage-play.mjs and stage-app-store.py read these two maps), since a store attaches a locale's
 * screenshots to the listing of that same locale.
 */
export const PLAY_LOCALES = {
  en: ["en-US"],
  // Britain's listing also goes to the other English locales Play has, which write as Britain (Pierre's call).
  "en-GB": ["en-GB", "en-IN", "en-SG", "en-ZA"],
  "en-AU": ["en-AU"],
  "en-CA": ["en-CA"],
  pt: ["pt-BR"],
  "pt-PT": ["pt-PT"],
  es: ["es-419", "es-US"],
  "es-ES": ["es-ES"],
  fr: ["fr-FR"],
  de: ["de-DE"],
  it: ["it-IT"],
  pl: ["pl-PL"],
  nl: ["nl-NL"],
  ja: ["ja-JP"],
  tr: ["tr-TR"],
  id: ["id"],
  ru: ["ru-RU"],
  uk: ["uk"],
  ko: ["ko-KR"],
  hi: ["hi-IN"],
  ar: ["ar"],
  zh: ["zh-CN"],
  he: ["iw-IL"],
  th: ["th"],
  vi: ["vi"],
  "zh-Hant": ["zh-TW"],
  sv: ["sv-SE"],
  cs: ["cs-CZ"],
  ro: ["ro"],
  bn: ["bn-BD"],
  ms: ["ms", "ms-MY"],
  el: ["el-GR"],
  hr: ["hr"],
  hu: ["hu-HU"],
  ur: ["ur"],
  fil: ["fil"],
  sr: ["sr"],
  sk: ["sk"],
  fa: ["fa"],
  ta: ["ta-IN"],
  nb: ["no-NO"],
  te: ["te-IN"],
  bg: ["bg"],
  mr: ["mr-IN"],
  sw: ["sw"],
  ca: ["ca"],
  da: ["da-DK"],
  fi: ["fi-FI"],
  lv: ["lv"],
  et: ["et"],
  gu: ["gu"],
  sl: ["sl"],
  lt: ["lt"],
  pa: ["pa"],
  kn: ["kn-IN"],
};
export const APP_STORE_LOCALES = {
  en: ["en-US"],
  "en-GB": ["en-GB"],
  "en-AU": ["en-AU"],
  "en-CA": ["en-CA"],
  pt: ["pt-BR"],
  "pt-PT": ["pt-PT"],
  es: ["es-MX"],
  "es-ES": ["es-ES"],
  fr: ["fr-FR"],
  de: ["de-DE"],
  it: ["it"],
  pl: ["pl"],
  nl: ["nl-NL"],
  ja: ["ja"],
  tr: ["tr"],
  id: ["id"],
  ru: ["ru"],
  uk: ["uk"],
  ko: ["ko"],
  hi: ["hi"],
  ar: ["ar-SA"],
  zh: ["zh-Hans"],
  he: ["he"],
  th: ["th"],
  vi: ["vi"],
  "zh-Hant": ["zh-Hant"],
  sv: ["sv"],
  cs: ["cs"],
  ro: ["ro"],
  bn: ["bn-BD"],
  ms: ["ms"],
  el: ["el"],
  hr: ["hr"],
  hu: ["hu"],
  ur: ["ur-PK"],
  // Apple has no Filipino listing locale: the language has a Play listing and no App Store one.
  fil: [],
  // Apple has no Serbian listing locale either: the language has a Play listing and no App Store one.
  sr: [],
  sk: ["sk"],
  // Apple has no Persian listing locale either: the language has a Play listing and no App Store one.
  fa: [],
  ta: ["ta-IN"],
  nb: ["no"],
  te: ["te-IN"],
  // Apple has no Bulgarian listing locale either: the language has a Play listing and no App Store one.
  bg: [],
  mr: ["mr-IN"],
  // Apple has no Swahili listing locale either: the language has a Play listing and no App Store one.
  sw: [],
  ca: ["ca"],
  da: ["da"],
  fi: ["fi"],
  // Apple has no Latvian listing locale either: the language has a Play listing and no App Store one.
  lv: [],
  // Apple has no Estonian listing locale either: the language has a Play listing and no App Store one.
  et: [],
  gu: ["gu-IN"],
  sl: ["sl-SI"],
  // Apple has no Lithuanian listing locale either: the language has a Play listing and no App Store one.
  lt: [],
  pa: ["pa-IN"],
  kn: ["kn-IN"],
};

/** The app's name in a listing: Bora Rachar in Brazil's Portuguese, Blitz Split in every other language. */
export const brandOf = (lang) => (lang === "pt" ? "Bora Rachar" : "Blitz Split");

const HEADING = /^## (.+)$/;
/** A word of a script written without spaces: kanji, hiragana, katakana or Thai. */
const UNSPACED = /[\p{Script=Han}\p{Script=Hiragana}\p{Script=Katakana}\p{Script=Thai}]/u;
/** What each file fastlane reads is called, by field. */
const PLAY_FILES = {
  playTitle: "title.txt",
  playShortDescription: "short_description.txt",
  playVideo: "video.txt",
  description: "full_description.txt",
};
/** The address Play takes for a preview video: a YouTube video's watch page. */
const YOUTUBE_WATCH = /^https:\/\/www\.youtube\.com\/watch\?v=[\w-]{11}$/;
const APP_STORE_FILES = {
  appStoreName: "name.txt",
  appStoreSubtitle: "subtitle.txt",
  appStoreKeywords: "keywords.txt",
  appStorePromotionalText: "promotional_text.txt",
  description: "description.txt",
};

/**
 * What every App Store locale gets beside its text, the same in every language: the address of "App Support", of
 * the developer's site and of the privacy policy on the app's page. Each locale holds its own, and one the upload
 * created starts with none, though the App Store asks for a support address in each before a review. The review
 * looks for a way to reach the developer at the support address, which the site's Support page has and its landing
 * doesn't. The privacy policy's is the page the apps open, which the site shows in the reader's language.
 */
export const APP_STORE_URL_FILES = {
  "support_url.txt": "https://blitzsplit.com/support",
  "marketing_url.txt": "https://blitzsplit.com",
  "privacy_url.txt": "https://blitzsplit.com/privacy",
};
/** Who owns the app, as the site's footer names them (Pierre's call: the same as Bible Planner's). */
export const COPYRIGHT_OWNER = "Quartel Enterprise • PI Software LTDA";
/**
 * The version's copyright line, one for every locale: the year and the owner, without the © the App Store adds. The
 * year is the current one, never written down (Pierre's call, as Bible Planner's footer does): every upload of the
 * listing and every release writes the line again, so it moves on by itself when the year turns.
 */
export const appStoreCopyright = (year = new Date().getFullYear()) => `${year} ${COPYRIGHT_OWNER}`;

export class ListingError extends Error {}
const fail = (file, message) => {
  throw new ListingError(`${file}: ${message}`);
};

/** A text's length as the stores count it: by character, not by UTF-16 unit, so an emoji or a kanji is one. */
export const lengthOf = (text) => [...text].length;

// --- A listing -------------------------------------------------------------------------------------------------------

/**
 * Reads a language's file: a `# Title` line, then each field under its `## Heading`, in the order of FIELDS. Returns
 * the fields by key, trimmed. Fails on anything a store would refuse or cut.
 */
export function parseListing(file, source, lang) {
  const sections = [];
  for (const line of source.split("\n")) {
    const heading = line.match(HEADING);
    if (heading) sections.push({ heading: heading[1].trim(), lines: [] });
    else if (sections.length > 0) sections.at(-1).lines.push(line);
  }
  const headings = sections.map((section) => section.heading);
  const fields = FIELDS.filter((field) => !field.isOptional || headings.includes(field.heading));
  const expected = fields.map((field) => field.heading);
  if (headings.join("\n") !== expected.join("\n")) {
    const all = FIELDS.map((field) => (field.isOptional ? `${field.heading} (optional)` : field.heading));
    fail(file, `takes the sections ${all.join(", ")}, in that order, and has ${headings.join(", ") || "none"}`);
  }
  const listing = {};
  fields.forEach((field, index) => {
    const text = sections[index].lines.join("\n").trim();
    if (text === "") fail(file, `${field.heading} is empty`);
    if (field.isLine && text.includes("\n")) fail(file, `${field.heading} is one line`);
    const length = lengthOf(text);
    if (length > field.limit) fail(file, `${field.heading} has ${length} characters, and its store takes ${field.limit}`);
    listing[field.key] = text;
  });
  if (listing.playVideo !== undefined && !YOUTUBE_WATCH.test(listing.playVideo)) {
    fail(file, `Play video is a YouTube watch address, https://www.youtube.com/watch?v=<id>, not ${listing.playVideo}`);
  }
  checkBrand(file, listing, lang);
  checkKeywords(file, listing);
  return listing;
}

function checkBrand(file, listing, lang) {
  const brand = brandOf(lang);
  for (const key of ["playTitle", "appStoreName"]) {
    const field = FIELDS.find((each) => each.key === key);
    if (!listing[key].startsWith(brand)) fail(file, `${field.heading} starts with the app's name there, ${brand}`);
  }
}

/**
 * The App Store reads the keywords as a list split by commas and already searches the words of the name and the
 * subtitle, so a space after a comma and a keyword repeated from them each waste characters of the 100.
 */
function checkKeywords(file, listing) {
  const keywords = listing.appStoreKeywords.split(",");
  if (keywords.some((keyword) => keyword === "")) fail(file, "App Store keywords has an empty keyword");
  const padded = keywords.find((keyword) => keyword !== keyword.trim());
  if (padded !== undefined) fail(file, `App Store keywords takes no space around a comma, as around "${padded.trim()}"`);
  const lower = keywords.map((keyword) => keyword.toLowerCase());
  const repeated = lower.find((keyword, index) => lower.indexOf(keyword) !== index);
  if (repeated !== undefined) fail(file, `App Store keywords lists "${repeated}" twice`);
  const shown = `${listing.appStoreName} ${listing.appStoreSubtitle}`.toLowerCase();
  // Why: a combining mark (a Hindi vowel sign or virama, a Thai tone) belongs to the word it sits on.
  const words = shown.split(/[^\p{L}\p{M}\p{N}]+/u);
  // Why: Japanese, Chinese and Thai write no spaces between words, so a keyword in their scripts is looked for anywhere
  // in the text.
  const wasted = lower.find((keyword) => words.includes(keyword) || (UNSPACED.test(keyword) && shown.includes(keyword)));
  if (wasted !== undefined) fail(file, `App Store keywords repeats "${wasted}", which the name or the subtitle already has`);
}

/** Every language's listing, by language. Fails when a language of the listings has no file, or a file no language. */
export function readListings(root = ".") {
  const directory = join(root, LISTING_DIR);
  const files = existsSync(directory) ? readdirSync(directory).filter((name) => name.endsWith(".md")) : [];
  const stray = files.find((name) => !LISTING_LANGS.includes(name.slice(0, -".md".length)));
  if (stray !== undefined) fail(join(LISTING_DIR, stray), `is no language of the listings: ${LISTING_LANGS.join(", ")}`);
  return Object.fromEntries(
    LISTING_LANGS.map((lang) => {
      const file = join(LISTING_DIR, `${lang}.md`);
      if (!files.includes(`${lang}.md`)) fail(file, "is missing: every language of the listings has a file");
      return [lang, parseListing(file, readFileSync(join(root, file), "utf8"), lang)];
    }),
  );
}

// --- What fastlane uploads -------------------------------------------------------------------------------------------

/** The files of both stores, by path under the staging folder: <store>/<locale>/<file>. `year` is the copyright's. */
export function stagedFiles(listings, year) {
  const staged = {};
  // `same` is what every locale of a store gets whatever its language, by file name.
  const stageStore = (store, locales, files, same = {}) => {
    for (const [lang, listing] of Object.entries(listings)) {
      for (const locale of locales[lang]) {
        for (const [key, name] of Object.entries(files)) {
          if (listing[key] !== undefined) staged[join(store, locale, name)] = listing[key];
        }
        for (const [name, text] of Object.entries(same)) staged[join(store, locale, name)] = text;
      }
    }
  };
  stageStore("play", PLAY_LOCALES, PLAY_FILES);
  stageStore("app-store", APP_STORE_LOCALES, APP_STORE_FILES, APP_STORE_URL_FILES);
  // Beside the locales' folders, where deliver reads what a version has once.
  staged[join("app-store", "copyright.txt")] = appStoreCopyright(year);
  return staged;
}

function stage(out) {
  const staged = stagedFiles(readListings());
  // Why: a locale dropped from a language's list would otherwise stay staged from an earlier run, and be uploaded.
  for (const store of ["play", "app-store"]) rmSync(join(out, store), { recursive: true, force: true });
  for (const [path, text] of Object.entries(staged)) {
    const file = join(out, path);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, text);
  }
  console.log(`${Object.keys(staged).length} file(s) staged under ${out}`);
}

// --- The commands ----------------------------------------------------------------------------------------------------

function main([command, ...rest]) {
  if (command === "check" && rest.length === 0) {
    const listings = readListings();
    console.log(`${Object.keys(listings).length} listing(s) in ${LISTING_DIR}/, all within the stores' limits`);
  } else if (command === "stage" && rest.length === 2 && rest[0] === "--out" && !rest[1].startsWith("--")) {
    stage(rest[1]);
  } else if (command === "copyright" && rest.length === 0) {
    console.log(appStoreCopyright());
  } else {
    console.error("Usage: store-listing.mjs check | stage --out <folder> | copyright");
    process.exit(2);
  }
}

// Why: macOS resolves /tmp and /var to /private, so the path a shell passes and the module's own differ unresolved.
if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    if (!(error instanceof ListingError)) throw error;
    console.error(error.message);
    process.exit(1);
  }
}
