// The tests of store-listing.mjs: node --test scripts/release/store-listing.test.mjs
//
// The rules a listing follows are checked on plain text; the repository's own listings are read as they are, so a
// file over a store's limit fails here as it does in the check.

import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { LANGS } from "./release-notes.mjs";
import {
  APP_STORE_LOCALES,
  FIELDS,
  LISTING_LANGS,
  ListingError,
  PLAY_LOCALES,
  brandOf,
  lengthOf,
  parseListing,
  readListings,
  stagedFiles,
} from "./store-listing.mjs";

const FIELD_TEXT = {
  playTitle: "Blitz Split: Bills & Expenses",
  playShortDescription: "Split bills with friends.",
  appStoreName: "Blitz Split: Split Bills",
  appStoreSubtitle: "Group expenses, who owes whom",
  appStoreKeywords: "roommates,trip,receipt",
  appStorePromotionalText: "Split any bill in seconds.",
  description: "The first paragraph.\n\n• A bullet\n• Another",
};

/** A listing's file, with `changes` over the valid fields; a field set to undefined is left out. */
function fileWith(changes = {}) {
  const text = { ...FIELD_TEXT, ...changes };
  const sections = FIELDS.filter((field) => text[field.key] !== undefined).map((field) => `## ${field.heading}\n\n${text[field.key]}\n`);
  return `# English\n\n${sections.join("\n")}`;
}

const parse = (changes, lang = "en") => parseListing("store-listing/en.md", fileWith(changes), lang);
const refuses = (changes, message, lang) =>
  assert.throws(
    () => parse(changes, lang),
    (error) => error instanceof ListingError && message.test(error.message),
  );

test("a listing's fields come back trimmed, the description with its paragraphs", () => {
  assert.deepEqual(parse(), FIELD_TEXT);
});

test("a listing has every section, in order", () => {
  refuses({ appStoreSubtitle: undefined }, /takes the sections Play title, .* in that order/);
  assert.throws(() => parseListing("f.md", "# English\n", "en"), ListingError);
});

test("an empty field is refused", () => {
  refuses({ playShortDescription: "   " }, /Play short description is empty/);
});

test("a field over its store's limit is refused, counted by character", () => {
  refuses({ playTitle: `Blitz Split: ${"a".repeat(18)}` }, /Play title has 31 characters, and its store takes 30/);
  assert.equal(lengthOf("割り勘👍"), 4);
  // Thirty characters outside the Basic Multilingual Plane are sixty UTF-16 units, and still thirty for a store
  const subtitle = parse({ appStoreSubtitle: "𠮷".repeat(30) }).appStoreSubtitle;
  assert.equal(subtitle.length, 60);
  assert.equal(lengthOf(subtitle), 30);
});

test("only the description takes more than one line", () => {
  refuses({ appStorePromotionalText: "One line\nand another" }, /App Store promotional text is one line/);
});

test("the name starts with the app's name in that language", () => {
  assert.equal(brandOf("pt"), "Bora Rachar");
  assert.equal(brandOf("pt-PT"), "Blitz Split");
  refuses({ playTitle: "Split Bills with Blitz Split" }, /Play title starts with the app's name there, Blitz Split/);
  refuses({}, /Play title starts with the app's name there, Bora Rachar/, "pt");
});

test("the keywords waste no character", () => {
  refuses({ appStoreKeywords: "trip, receipt" }, /no space around a comma, as around "receipt"/);
  refuses({ appStoreKeywords: "trip,,receipt" }, /an empty keyword/);
  refuses({ appStoreKeywords: "trip,receipt,Trip" }, /lists "trip" twice/);
  refuses({ appStoreKeywords: "trip,expenses" }, /repeats "expenses", which the name or the subtitle already has/);
  assert.equal(parse({ appStoreKeywords: "split bill,trip" }).appStoreKeywords, "split bill,trip");
  // Japanese has no spaces to split the name by: a keyword is looked for anywhere in it
  const japanese = { appStoreName: "Blitz Split：割り勘・立て替え精算", appStoreSubtitle: "グループの支払い" };
  refuses({ ...japanese, appStoreKeywords: "旅行,精算" }, /repeats "精算", which the name or the subtitle already has/);
  assert.equal(parse({ ...japanese, appStoreKeywords: "旅行,清算" }).appStoreKeywords, "旅行,清算");
});

test("every language of the listings has a file the stores take", () => {
  const listings = readListings();
  assert.deepEqual(Object.keys(listings), LISTING_LANGS);
});

test("the listings are written in the release notes' languages and Spain's Spanish, after Latin America's", () => {
  assert.deepEqual(
    LISTING_LANGS.filter((lang) => lang !== "es-ES"),
    LANGS,
  );
  assert.equal(LISTING_LANGS[LISTING_LANGS.indexOf("es") + 1], "es-ES");
});

test("every language goes to a locale of each store, none twice", () => {
  for (const locales of [PLAY_LOCALES, APP_STORE_LOCALES]) {
    assert.deepEqual(Object.keys(locales), LISTING_LANGS);
    const all = Object.values(locales).flat();
    assert.equal(new Set(all).size, all.length);
  }
});

test("staging writes each store's files under each of a language's locales", () => {
  const staged = stagedFiles({ es: { ...FIELD_TEXT, description: "Texto" }, en: FIELD_TEXT });
  assert.equal(staged["play/es-419/full_description.txt"], "Texto");
  assert.equal(staged["play/es-US/title.txt"], FIELD_TEXT.playTitle);
  assert.equal(staged["app-store/es-MX/description.txt"], "Texto");
  assert.equal(staged["app-store/en-US/keywords.txt"], FIELD_TEXT.appStoreKeywords);
  assert.equal(staged["app-store/en-US/promotional_text.txt"], FIELD_TEXT.appStorePromotionalText);
  assert.equal(staged["play/en-US/keywords.txt"], undefined);
  assert.equal(staged["play/es-ES/title.txt"], undefined);
  // Play: 3 files for Latin America's 2 locales and 1 English; App Store: 5 files for 1 locale of each
  assert.equal(Object.keys(staged).length, 3 * 3 + 5 * 2);
});

// The screenshots are staged from maps of their own, in Kotlin (build-logic's StoreScreenshots.kt). A store attaches a
// locale's screenshots to the listing of that same locale, so the two must send each language to the same codes.
const SCREENSHOTS_KT = "build-logic/convention/src/main/kotlin/com/quare/blitzsplit/buildlogic/StoreScreenshots.kt";
const storeLocalesOfScreenshots = (name) => {
  const source = readFileSync(SCREENSHOTS_KT, "utf8");
  const map = source.match(new RegExp(`private val ${name} = mapOf\\(([\\s\\S]*?)\\n\\)`));
  assert.ok(map, `no ${name} in StoreScreenshots.kt`);
  return [...map[1].matchAll(/to listOf\(([^)]*)\)/g)].map(([, codes]) =>
    [...codes.matchAll(/"([^"]+)"/g)].map(([, code]) => code),
  );
};
const sorted = (groups) => groups.map((codes) => [...codes].sort().join(",")).sort();

test("the screenshots go to the store locales the listing text goes to", () => {
  assert.deepEqual(sorted(storeLocalesOfScreenshots("playLocales")), sorted(Object.values(PLAY_LOCALES)));
  assert.deepEqual(sorted(storeLocalesOfScreenshots("appStoreLocales")), sorted(Object.values(APP_STORE_LOCALES)));
});

// The iPhone Duo's shelf takes each screen once, open for the ones StoreScreenshots.kt names by file name. A name no
// screen has (a screen renamed, a typo) would send that screen up closed with nothing failing. Staging matches the
// names the frames are written under, StoreScreen's, which are the ones the captures are written under, screens.tsv's.
const STORE_SCREEN_KT =
  "android/store_screenshots/src/test/kotlin/com/quare/blitzsplit/android/storescreenshots/StoreScreen.kt";
const SCREENS_TSV = "scripts/store-screenshots/screens.tsv";

test("the screens the iPhone Duo's shelf takes open are screens the captures and the frames write", () => {
  const list = readFileSync(SCREENSHOTS_KT, "utf8").match(/private val duoScreensStagedOpen = setOf\(([^)]*)\)/);
  assert.ok(list, "no duoScreensStagedOpen in StoreScreenshots.kt");
  const open = [...list[1].matchAll(/"([^"]+)"/g)].map(([, name]) => name);
  const captured = readFileSync(SCREENS_TSV, "utf8")
    .split("\n")
    .filter((row) => row && !row.startsWith("#"))
    .map((row) => row.split("\t")[1]);
  const framed = [...readFileSync(STORE_SCREEN_KT, "utf8").matchAll(/fileName = "([^"]+)"/g)].map(([, name]) => name);
  assert.deepEqual(framed, captured);
  assert.ok(open.length > 0 && open.length < framed.length, "the shelf shows the Duo both closed and open");
  assert.deepEqual(open.filter((name) => !framed.includes(name)), []);
});
