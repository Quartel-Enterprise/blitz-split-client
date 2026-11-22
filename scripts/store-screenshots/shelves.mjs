#!/usr/bin/env node
// How to sort the App Store's shelves, and whether they were left as staged. A shelf is one locale's screenshots
// for one device size (an appScreenshotSet). `tidy_app_store_screenshots` of fastlane/Fastfile reads every shelf of
// the editable version in full and asks here what to do with it: the lane talks to App Store Connect, and what it
// sorts and what it reports is decided, and tested, here (docs/store-listing-screenshots.md#things-that-bite).
//
//   node scripts/store-screenshots/shelves.mjs plan <shelves.json>
//       Prints, as JSON, the order to leave each shelf in: every screenshot it lists, by file name.
//   node scripts/store-screenshots/shelves.mjs check <shelves.json>
//       Fails when a shelf isn't as it was staged, naming the locale, the shelf and the file: a screen it lacks, a
//       file listed twice, one that never finished uploading, an order that isn't the file names'.
//
// Nothing here removes a screenshot, and the lane removes none. A shelf can list one screenshot twice, under two
// ids with the same file name and checksum, and deleting either id deletes both: that is how the first set's
// tidy-up lost a file (November 2026). Only sending the locale again puts such a shelf right.
//
// <shelves.json> is what the lane read:
//   { "locales": [], "shelves": [{ "id", "locale", "shelf", "isWhole", "screenshots": [{ "id", "fileName", "state", "checksum" }] }] }
// with the version's locales, each shelf's screenshots in shelf order, `state` App Store Connect's
// assetDeliveryState (COMPLETE once the image is there) and `checksum` its sourceFileChecksum. `isWhole` is false
// when the lane couldn't tell that it read every screenshot of the shelf, in its order.
//
// Plain Node, no dependencies.

import { appendFileSync, readFileSync, realpathSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { listingScreens, stagedImage, WATCH_SCREENS, WATCH_SHELF_TYPE } from "./local-set.mjs";

/** The state of a screenshot whose image App Store Connect has: anything else is still on its way, or never came. */
export const COMPLETE = "COMPLETE";
/** The state of one whose image went up and is still being processed: it becomes COMPLETE by itself. */
export const PROCESSING = "UPLOAD_COMPLETE";

// File-name order, the shelf order: 02 before 10, as deliver sorts (the Naturally gem).
const byFileName = new Intl.Collator("en", { numeric: true });

/**
 * The order to leave one shelf in, and what sorting doesn't fix.
 *
 * `order` names every screenshot the shelf lists, by file name: App Store Connect refuses an order that leaves one
 * out, which is why a shelf listing 11 could not be sorted with its 10 distinct files, and takes one that names
 * all 11. A file's entries stay next to each other, as they stood.
 *
 * No order is given for a shelf that wasn't read whole, since an order made from a partial list leaves a screenshot
 * out, nor for one that holds a screenshot without a file name, which can't be placed.
 *
 * `problems`, one line each, are what only sending the locale again fixes: a file listed more than once, a file
 * none of whose entries finished uploading, a screen of `screens` the shelf lacks, a file that is no staged image.
 */
export function tidyShelf({ isWhole, screenshots, shelf }, screens = listingScreens()) {
  if (!isWhole) return { order: null, problems: ["could not be read whole, so it was not sorted: run the tidy-up again"] };
  if (screenshots.length === 0) return { order: [], problems: ["holds no screenshot"] };
  if (screenshots.some((screenshot) => !screenshot.fileName)) {
    return { order: null, problems: ["holds a screenshot with no file name, so it was not sorted"] };
  }

  const problems = [];
  const entriesOf = Map.groupBy(screenshots, (screenshot) => screenshot.fileName);
  for (const [fileName, entries] of entriesOf) {
    const states = [...new Set(entries.map((entry) => entry.state ?? "no state"))];
    if (entries.length > 1) {
      // One screenshot under several ids answers the same checksum for each; two uploads of a file would too, so
      // the words say what was read and not which of the two it is. Either way no entry is safe to delete alone.
      const isOneImage = entries[0].checksum && entries.every((entry) => entry.checksum === entries[0].checksum);
      problems.push(`${fileName} is listed ${entries.length} times, ${isOneImage ? `under ${entries.length} ids with one checksum` : `in the states ${states.join(", ")}`}`);
    } else if (entries[0].state === PROCESSING) {
      problems.push(`${fileName} is still being processed (${PROCESSING}): run the tidy-up again in a few minutes`);
    } else if (entries[0].state !== COMPLETE) {
      problems.push(`${fileName} never finished uploading (${states.join(", ")})`);
    }
  }
  const order = screenshots.toSorted((one, other) => byFileName.compare(one.fileName, other.fileName)).map((screenshot) => screenshot.id);

  // What the shelf should hold: each screen once, from the shelf's device (the iPhone Duo's has two). The Apple
  // Watch's shelf holds the watch's four screens, every other one screens.tsv's ten.
  const shelfScreens = shelf === WATCH_SHELF_TYPE ? WATCH_SCREENS : screens;
  const named = [...entriesOf.keys()].map((fileName) => ({ fileName, parts: stagedImage(fileName, [...screens, ...WATCH_SCREENS]) }));
  for (const { fileName, parts } of named) {
    if (!parts) problems.push(`${fileName} is not <screen>_<device>.png, with a screen of screens.tsv`);
    else if (!shelfScreens.includes(parts.screen)) problems.push(`${fileName} is not a screen of this shelf (${shelfScreens.join(", ")})`);
  }
  const staged = named.filter(({ parts }) => parts && shelfScreens.includes(parts.screen));
  const devices = [...new Set(staged.map(({ parts }) => parts.device))].sort();
  for (const screen of shelfScreens) {
    const files = staged.filter(({ parts }) => parts.screen === screen).map(({ fileName }) => fileName);
    if (files.length > 1) problems.push(`${screen} is there ${files.length} times (${files.join(", ")}): each screen goes up once`);
    if (files.length === 0 && devices.length > 0) problems.push(`${devices.map((device) => `${screen}_${device}.png`).join(" or ")} is missing`);
  }
  return { order, problems };
}

/** What is wrong with a shelf as it stands, after the tidy-up: what sorting doesn't fix, and an order still to give. */
export function shelfProblems(shelf, screens = listingScreens()) {
  const { order, problems } = tidyShelf(shelf, screens);
  if (order && order.join() !== shelf.screenshots.map((screenshot) => screenshot.id).join()) {
    return [...problems, `is not in file-name order: ${shelf.screenshots.map((screenshot) => screenshot.fileName).join(", ")}`];
  }
  return problems;
}

/** The lane's instructions: per shelf, by its id, the order to leave it in, or null to leave it alone. */
export function plan({ shelves }, screens = listingScreens()) {
  return { shelves: shelves.map((shelf) => ({ id: shelf.id, locale: shelf.locale, shelf: shelf.shelf, order: tidyShelf(shelf, screens).order })) };
}

/**
 * Every shelf's problems, one line each, `<locale> / <shelf>: <what is wrong>`, then the shelves that aren't there:
 * deliver deletes a locale's shelves before it uploads the new ones, so an upload that failed leaves a locale
 * without one, or without any, which no shelf's own problems would say. Every locale of the version holds the
 * shelves the others do; a device the set leaves out is on no locale's, and is not missed. A version with no shelf
 * at all is not as staged either.
 */
export function check({ locales = [], shelves }, screens = listingScreens()) {
  // Never what a tidy-up is run on: after an upload it means every locale's failed once its shelves were deleted.
  if (shelves.length === 0) return ["no locale of the version holds a screenshot"];
  const problems = shelves.flatMap((shelf) => shelfProblems(shelf, screens).map((problem) => `${shelf.locale} / ${shelf.shelf}: ${problem}`));
  const kinds = [...new Set(shelves.map((shelf) => shelf.shelf))].sort();
  for (const locale of new Set([...locales, ...shelves.map((shelf) => shelf.locale)])) {
    const held = shelves.filter((shelf) => shelf.locale === locale).map((shelf) => shelf.shelf);
    if (held.length === 0 && kinds.length > 0) problems.push(`${locale}: has no shelf, where other locales have ${kinds.join(", ")}`);
    else for (const kind of kinds) if (!held.includes(kind)) problems.push(`${locale} / ${kind}: is not there, and other locales have it`);
  }
  return problems;
}

const USAGE = `Usage:
  shelves.mjs plan <shelves.json>
  shelves.mjs check <shelves.json>`;

function main([command, file]) {
  if (!file || !["plan", "check"].includes(command)) {
    console.error(USAGE);
    process.exit(1);
  }
  const read = JSON.parse(readFileSync(file, "utf8"));
  if (command === "plan") {
    console.log(JSON.stringify(plan(read)));
    return;
  }
  const problems = check(read);
  const locales = new Set(read.shelves.map((shelf) => shelf.locale)).size;
  if (problems.length === 0) {
    console.log(`${read.shelves.length} shelves of ${locales} locale(s) hold each screen once, in file-name order.`);
    return;
  }
  const lines = problems.map((problem) => `- ${problem}`);
  console.log([`${problems.length} thing(s) wrong on the shelves:`, ...lines].join("\n"));
  // On the run's page, where someone reads it, and not only in the lane's log.
  if (process.env.GITHUB_STEP_SUMMARY) {
    const how = "A shelf that isn't there, lacks a file, lists one twice or holds one that never finished uploading is put right by sending its locale again; a shelf that is only out of order, wasn't read whole or holds a file still being processed, by running the tidy-up again. Don't delete one of a file's two entries by hand: they may be one screenshot under two ids, and both go (docs/store-listing-screenshots.md#repairing-shelves).";
    appendFileSync(process.env.GITHUB_STEP_SUMMARY, ["### Shelves that are not as staged", "", ...lines, "", how, ""].join("\n"));
  }
  process.exit(1);
}

// Why: macOS resolves /tmp and /var to /private, so the path a shell passes and the module's own differ unresolved.
if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) main(process.argv.slice(2));
