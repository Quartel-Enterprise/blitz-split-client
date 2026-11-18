#!/usr/bin/env node
// What tidying the App Store's shelves takes, and whether they were left as staged. A shelf is one locale's
// screenshots for one device size (an appScreenshotSet). `tidy_app_store_screenshots` of fastlane/Fastfile reads
// every shelf of the editable version in full and asks here what to do with it: the lane talks to App Store Connect,
// and what it removes and how it sorts is decided, and tested, here (docs/store-listing-screenshots.md#things-that-bite).
//
//   node scripts/store-screenshots/shelves.mjs plan <shelves.json>
//       Prints, as JSON, each shelf's screenshots to remove and the order to leave the others in.
//   node scripts/store-screenshots/shelves.mjs check <shelves.json>
//       Fails when a shelf isn't as it was staged, naming the locale, the shelf and the file: a screen it lacks, a
//       file that never finished uploading, a copy left, an order that isn't the file names'.
//
// <shelves.json> is what the lane read:
//   { "locales": [], "shelves": [{ "id", "locale", "shelf", "isWhole", "screenshots": [{ "id", "fileName", "state" }] }] }
// with the version's locales, each shelf's screenshots in shelf order and `state` App Store Connect's
// assetDeliveryState (COMPLETE once the image is there). `isWhole` is false when the lane couldn't tell that it
// read every screenshot of the shelf, in its order.
//
// Plain Node, no dependencies.

import { appendFileSync, readFileSync, realpathSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { listingScreens, stagedImage } from "./local-set.mjs";

/** The state of a screenshot whose image App Store Connect has: anything else is still on its way, or never came. */
export const COMPLETE = "COMPLETE";

// File-name order, the shelf order: 02 before 10, as deliver sorts (the Naturally gem).
const byFileName = new Intl.Collator("en", { numeric: true });

/**
 * What to do with one shelf, and what no tidying fixes.
 *
 * A file that went up twice keeps one copy: the first whose image is there. Spaceship creates a screenshot's record
 * and then uploads its image, and a create it sends again after a gateway error leaves a record with no image ahead
 * of the one that gets it, so the first copy by position may be an empty one. A file with no finished copy keeps
 * them all and is a problem, as is a screen of `screens` the shelf lacks: sending the locale again is what fixes
 * those, and removing or reordering would only hide them.
 *
 * Nothing is removed from, and no order is given for, a shelf that wasn't read whole: a copy counted against a
 * partial list may be the only good one, and App Store Connect refuses an order that leaves a screenshot out. Nor
 * for one that holds a screenshot without a file name: two of those are not copies of one file.
 *
 * Answers `remove` (ids), `order` (the ids that stay, in file-name order, or null) and `problems` (one line each).
 */
export function tidyShelf({ isWhole, screenshots }, screens = listingScreens()) {
  if (!isWhole) {
    return { remove: [], order: null, problems: ["could not be read whole, so nothing was removed or sorted: run the tidy-up again"] };
  }
  if (screenshots.length === 0) return { remove: [], order: [], problems: ["holds no screenshot"] };
  if (screenshots.some((screenshot) => !screenshot.fileName)) {
    return { remove: [], order: null, problems: ["holds a screenshot with no file name, so nothing was removed or sorted"] };
  }

  const problems = [];
  const copiesOf = Map.groupBy(screenshots, (screenshot) => screenshot.fileName);
  const remove = new Set();
  for (const [fileName, copies] of copiesOf) {
    const kept = copies.find((copy) => copy.state === COMPLETE);
    if (kept) {
      for (const copy of copies) if (copy !== kept) remove.add(copy.id);
    } else {
      const states = [...new Set(copies.map((copy) => copy.state ?? "no state"))].join(", ");
      problems.push(`${fileName} never finished uploading (${states})`);
    }
  }
  const staying = screenshots.filter((screenshot) => !remove.has(screenshot.id));
  const order = staying.toSorted((one, other) => byFileName.compare(one.fileName, other.fileName)).map((screenshot) => screenshot.id);

  // What the shelf should hold: each screen once, from the shelf's device (the iPhone Duo's has two).
  const fileNames = [...copiesOf.keys()];
  const named = fileNames.map((fileName) => ({ fileName, parts: stagedImage(fileName, screens) }));
  for (const { fileName, parts } of named) {
    if (!parts) problems.push(`${fileName} is not <screen>_<device>.png, with a screen of screens.tsv`);
  }
  const staged = named.filter(({ parts }) => parts);
  const devices = [...new Set(staged.map(({ parts }) => parts.device))].sort();
  for (const screen of screens) {
    const files = staged.filter(({ parts }) => parts.screen === screen).map(({ fileName }) => fileName);
    if (files.length > 1) problems.push(`${screen} is there ${files.length} times (${files.join(", ")}): each screen goes up once`);
    if (files.length === 0 && devices.length > 0) problems.push(`${devices.map((device) => `${screen}_${device}.png`).join(" or ")} is missing`);
  }
  return { remove: [...remove], order, problems };
}

/**
 * What is wrong with a shelf as it stands, after the tidy-up: what `tidyShelf` can't fix, and what it would still
 * do, which a tidy-up that worked has left nothing of.
 */
export function shelfProblems(shelf, screens = listingScreens()) {
  const { remove, order, problems } = tidyShelf(shelf, screens);
  const found = [...problems];
  const copies = Map.groupBy(
    shelf.screenshots.filter((screenshot) => remove.includes(screenshot.id)),
    (screenshot) => screenshot.fileName,
  );
  for (const [fileName, extra] of copies) found.push(`${fileName} is there ${extra.length + 1} times`);
  if (order && remove.length === 0 && order.join() !== shelf.screenshots.map((screenshot) => screenshot.id).join()) {
    found.push(`is not in file-name order: ${shelf.screenshots.map((screenshot) => screenshot.fileName).join(", ")}`);
  }
  return found;
}

/** The lane's instructions: per shelf, by its id, what to remove and the order to leave. */
export function plan({ shelves }, screens = listingScreens()) {
  return {
    shelves: shelves.map((shelf) => {
      const { remove, order } = tidyShelf(shelf, screens);
      return { id: shelf.id, locale: shelf.locale, shelf: shelf.shelf, remove, order };
    }),
  };
}

/**
 * Every shelf's problems, one line each, `<locale> / <shelf>: <what is wrong>`, then the shelves that aren't there:
 * deliver deletes a locale's shelves before it uploads the new ones, so an upload that failed leaves a locale
 * without one, or without any, which no shelf's own problems would say. Every locale of the version holds the
 * shelves the others do; a device the set leaves out is on no locale's, and is not missed.
 */
export function check({ locales = [], shelves }, screens = listingScreens()) {
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
    console.log(read.shelves.length === 0 ? "No locale of the version holds a screenshot: nothing to check." : `${read.shelves.length} shelves of ${locales} locale(s) hold each screen once, in file-name order.`);
    return;
  }
  const lines = problems.map((problem) => `- ${problem}`);
  console.log([`${problems.length} thing(s) wrong on the shelves:`, ...lines].join("\n"));
  // On the run's page, where someone reads it, and not only in the lane's log.
  if (process.env.GITHUB_STEP_SUMMARY) {
    const how = "A shelf that isn't there, lacks a file or holds one that never finished uploading is put right by sending its locale again; a copy left or a shelf out of order, by running the tidy-up again (docs/store-listing-screenshots.md#repairing-shelves).";
    appendFileSync(process.env.GITHUB_STEP_SUMMARY, ["### Shelves that are not as staged", "", ...lines, "", how, ""].join("\n"));
  }
  process.exit(1);
}

// Why: macOS resolves /tmp and /var to /private, so the path a shell passes and the module's own differ unresolved.
if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) main(process.argv.slice(2));
