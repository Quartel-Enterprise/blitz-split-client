// The tests of shelves.mjs, how the tidy-up of the App Store's shelves sorts and what it reports:
// node --test scripts/store-screenshots/shelves.test.mjs
//
// A shelf is written as the lane reads it: its screenshots in shelf order, each with its file name, the state App
// Store Connect gives it and its checksum.

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import { listingScreens } from "./local-set.mjs";
import { check, plan, shelfProblems, tidyShelf } from "./shelves.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(HERE, "../..");
const SCRIPT = join(HERE, "shelves.mjs");
const SCREENS = listingScreens();

let nextId = 0;
/** A screenshot as App Store Connect lists it. An image has one checksum, whatever the ids it is listed under. */
const shot = (fileName, { state = "COMPLETE", checksum = `md5-of-${fileName}` } = {}) => ({ id: `s${++nextId}`, fileName, state, checksum });
/** The same screenshot listed once more, under another id: what an upload can leave on a shelf. */
const again = (screenshot) => ({ ...screenshot, id: `s${++nextId}` });
const fileOf = (screen, device) => `${screen}_${device}.png`;
/** A device's ten screens in the order their uploads landed, which is not the file names'. */
const uploaded = (device, screens = SCREENS) => {
  const shots = screens.map((screen) => shot(fileOf(screen, device)));
  return [...shots.slice(3), ...shots.slice(0, 3)];
};
const shelfOf = (screenshots, { locale = "en-US", shelf = "APP_IPHONE_67", isWhole = true } = {}) => ({ id: `${locale}-${shelf}`, locale, shelf, isWhole, screenshots });
const namesOf = (shelf, ids) => ids.map((id) => shelf.screenshots.find((screenshot) => screenshot.id === id).fileName);
/** The shelf once the lane gave it the plan's order. */
const sorted = (shelf) => {
  const { order } = tidyShelf(shelf);
  return { ...shelf, screenshots: order.map((id) => shelf.screenshots.find((screenshot) => screenshot.id === id)) };
};
const inOrder = (device) => SCREENS.map((screen) => fileOf(screen, device));

describe("how the tidy-up sorts a shelf", () => {
  // Mac de-DE and the iPad 11" en-US of run 44324532909: 11 entries, two of them one screenshot under two ids.
  // An order of the 10 distinct ids was refused, and one of all 11 was taken.
  test("orders every entry of a shelf that lists a screenshot twice, the two side by side", () => {
    const shots = uploaded("mac");
    const listedTwice = shots.find(({ fileName }) => fileName === fileOf("02_split_by_items", "mac"));
    const shelf = shelfOf([...shots, again(listedTwice)], { locale: "de-DE", shelf: "APP_DESKTOP" });
    const { order } = tidyShelf(shelf);
    assert.equal(order.length, 11);
    assert.deepEqual([...order].sort(), shelf.screenshots.map(({ id }) => id).sort());
    const names = namesOf(shelf, order);
    assert.deepEqual(names.slice(0, 3), [fileOf("01_group", "mac"), fileOf("02_split_by_items", "mac"), fileOf("02_split_by_items", "mac")]);
    assert.deepEqual([...new Set(names)], inOrder("mac"));
    // The entry that stood first stays first of the two.
    assert.equal(order[1], listedTwice.id);
  });

  // Deleting one id of such a pair deleted both: that is how the iPhone 6.7" en-US shelf lost 08 in that run.
  test("never names a screenshot to remove", () => {
    const shots = uploaded("iphone_6_7");
    const shelf = shelfOf([...shots, again(shots[4]), shot(fileOf("01_group", "iphone_6_7"), { state: "AWAITING_UPLOAD", checksum: null })]);
    assert.deepEqual(Object.keys(tidyShelf(shelf)).sort(), ["order", "problems"]);
    const [todo] = plan({ shelves: [shelf] }).shelves;
    assert.deepEqual(Object.keys(todo).sort(), ["id", "locale", "order", "shelf"]);
    assert.equal(todo.order.length, shelf.screenshots.length);
  });

  test("gives no order to a shelf that wasn't read whole, and says so", () => {
    const shelf = shelfOf(uploaded("mac"), { shelf: "APP_DESKTOP", isWhole: false });
    const { order, problems } = tidyShelf(shelf);
    assert.equal(order, null);
    assert.deepEqual(problems, ["could not be read whole, so it was not sorted: run the tidy-up again"]);
  });

  test("gives no order to a shelf with a screenshot that has no file name", () => {
    const nameless = [{ id: "a", state: "COMPLETE" }, { id: "b", state: "COMPLETE" }];
    const { order, problems } = tidyShelf(shelfOf([...uploaded("mac"), ...nameless], { shelf: "APP_DESKTOP" }));
    assert.equal(order, null);
    assert.deepEqual(problems, ["holds a screenshot with no file name, so it was not sorted"]);
  });

  test("leaves a shelf that is already as staged as it is", () => {
    const shelf = shelfOf(SCREENS.map((screen) => shot(fileOf(screen, "ipad_11"))), { shelf: "APP_IPAD_PRO_3GEN_11" });
    const { order, problems } = tidyShelf(shelf);
    assert.deepEqual(order, shelf.screenshots.map(({ id }) => id));
    assert.deepEqual(problems, []);
    assert.deepEqual(shelfProblems(shelf), []);
  });

  test("sorts ten uploaded out of order, which is then as staged", () => {
    const shelf = shelfOf(uploaded("iphone_6_7"));
    assert.deepEqual(namesOf(shelf, tidyShelf(shelf).order), inOrder("iphone_6_7"));
    assert.match(shelfProblems(shelf)[0], /^is not in file-name order: 04_balances_iphone_6_7\.png, /);
    assert.deepEqual(shelfProblems(sorted(shelf)), []);
  });

  test("sorts as deliver does, 2 before 10, whatever the digits' count", () => {
    const shelf = shelfOf([shot("10_b_mac.png"), shot("9_a_mac.png"), shot("2_c_mac.png")]);
    assert.deepEqual(namesOf(shelf, tidyShelf(shelf, ["2_c", "9_a", "10_b"]).order), ["2_c_mac.png", "9_a_mac.png", "10_b_mac.png"]);
  });
});

describe("what sorting doesn't fix", () => {
  test("names a file a shelf lists twice, and how: one image under two ids, or two records", () => {
    const shots = uploaded("mac");
    const oneImage = shelfOf([...shots, again(shots[0])], { shelf: "APP_DESKTOP" });
    assert.deepEqual(tidyShelf(oneImage).problems, ["04_balances_mac.png is listed 2 times, under 2 ids with one checksum"]);
    // Sorted, it still lists it twice: the check after the tidy-up fails on it.
    assert.deepEqual(check({ shelves: [sorted(oneImage)] }), ["en-US / APP_DESKTOP: 04_balances_mac.png is listed 2 times, under 2 ids with one checksum"]);

    const twoRecords = shelfOf([...shots, shot(shots[0].fileName, { state: "AWAITING_UPLOAD", checksum: null })], { shelf: "APP_DESKTOP" });
    assert.deepEqual(tidyShelf(twoRecords).problems, ["04_balances_mac.png is listed 2 times, in the states COMPLETE, AWAITING_UPLOAD"]);
  });

  test("names the file a shelf is short of, and still sorts what is there", () => {
    const shelf = shelfOf(uploaded("iphone_6_7", SCREENS.filter((screen) => screen !== "08_payers_and_currencies")));
    const { order, problems } = tidyShelf(shelf);
    assert.equal(order.length, 9);
    assert.deepEqual(problems, ["08_payers_and_currencies_iphone_6_7.png is missing"]);
    assert.deepEqual(check({ shelves: [sorted(shelf)] }), ["en-US / APP_IPHONE_67: 08_payers_and_currencies_iphone_6_7.png is missing"]);
  });

  test("names a file that never finished uploading", () => {
    const stuck = shot(fileOf("03_receipt", "mac"), { state: "FAILED", checksum: null });
    const shelf = shelfOf([...uploaded("mac").filter(({ fileName }) => fileName !== stuck.fileName), stuck], { shelf: "APP_DESKTOP" });
    assert.deepEqual(tidyShelf(shelf).problems, ["03_receipt_mac.png never finished uploading (FAILED)"]);
  });

  // The last locale's images may still be on their way when the tidy-up starts: waiting is enough, not a resend.
  test("tells a file still being processed from one that never finished", () => {
    const processing = shot(fileOf("03_receipt", "mac"), { state: "UPLOAD_COMPLETE", checksum: null });
    const shelf = shelfOf([...uploaded("mac").filter(({ fileName }) => fileName !== processing.fileName), processing], { shelf: "APP_DESKTOP" });
    assert.deepEqual(tidyShelf(shelf).problems, ["03_receipt_mac.png is still being processed (UPLOAD_COMPLETE): run the tidy-up again in a few minutes"]);
  });

  test("names both displays' files for a screen the iPhone Duo's shelf lacks, and a screen it has from both", () => {
    const open = ["01_group", "04_balances", "06_statistics", "07_home", "09_activity"];
    const staged = SCREENS.map((screen) => shot(fileOf(screen, open.includes(screen) ? "iphone_duo_open" : "iphone_duo")));
    const short = shelfOf(staged.filter(({ fileName }) => !fileName.startsWith("05_invite")), { shelf: "APP_IPHONE_DUO" });
    assert.deepEqual(tidyShelf(short).problems, ["05_invite_iphone_duo.png or 05_invite_iphone_duo_open.png is missing"]);
    const twice = shelfOf([...staged, shot(fileOf("01_group", "iphone_duo"))], { shelf: "APP_IPHONE_DUO" });
    assert.deepEqual(tidyShelf(twice).problems, ["01_group is there 2 times (01_group_iphone_duo_open.png, 01_group_iphone_duo.png): each screen goes up once"]);
  });

  test("names a file that is no staged image, and a shelf with nothing on it", () => {
    const shelf = shelfOf([...uploaded("mac"), shot("Screenshot 2026-11-18.png")], { shelf: "APP_DESKTOP" });
    assert.deepEqual(tidyShelf(shelf).problems, ["Screenshot 2026-11-18.png is not <screen>_<device>.png, with a screen of screens.tsv"]);
    assert.deepEqual(tidyShelf(shelfOf([])).problems, ["holds no screenshot"]);
  });
});

describe("the check after the tidy-up", () => {
  test("fails on a file listed twice and on a shelf out of order, naming the locale and the shelf", () => {
    const shots = SCREENS.map((screen) => shot(fileOf(screen, "mac")));
    const listedTwice = shelfOf([...shots.slice(0, 2), again(shots[1]), ...shots.slice(2)], { locale: "de-DE", shelf: "APP_DESKTOP" });
    const unsorted = shelfOf(uploaded("mac"), { shelf: "APP_DESKTOP" });
    const asStaged = shelfOf(SCREENS.map((screen) => shot(fileOf(screen, "mac"))), { locale: "ja", shelf: "APP_DESKTOP" });
    const problems = check({ locales: ["de-DE", "en-US", "ja"], shelves: [listedTwice, unsorted, asStaged] });
    assert.equal(problems.length, 2);
    assert.equal(problems[0], "de-DE / APP_DESKTOP: 02_split_by_items_mac.png is listed 2 times, under 2 ids with one checksum");
    assert.match(problems[1], /^en-US \/ APP_DESKTOP: is not in file-name order: 04_balances_mac\.png, /);
  });

  // deliver deletes a locale's shelves before it uploads the new ones: what an upload that failed leaves is no
  // shelf, which no shelf's own problems say.
  test("fails on a shelf a locale lacks and the others have, and on a locale with none", () => {
    const whole = (locale, shelf, device) => shelfOf(SCREENS.map((screen) => shot(fileOf(screen, device))), { locale, shelf });
    const shelves = [whole("en-US", "APP_IPHONE_67", "iphone_6_7"), whole("en-US", "APP_IPAD_PRO_3GEN_11", "ipad_11"), whole("ja", "APP_IPHONE_67", "iphone_6_7")];
    assert.deepEqual(check({ locales: ["en-US", "ja", "ko"], shelves }), [
      "ja / APP_IPAD_PRO_3GEN_11: is not there, and other locales have it",
      "ko: has no shelf, where other locales have APP_IPAD_PRO_3GEN_11, APP_IPHONE_67",
    ]);
    // A device every locale goes without was left out of the set.
    assert.deepEqual(check({ locales: ["en-US", "ja"], shelves: [shelves[0], shelves[2]] }), []);
  });

  // What is left when every locale's upload failed once deliver had deleted its shelves: not "as staged".
  test("fails on a version with no shelf at all", () => {
    assert.deepEqual(check({ locales: ["en-US", "ja"], shelves: [] }), ["no locale of the version holds a screenshot"]);
  });

  test("plans by the shelf's id, for the lane to find each one", () => {
    const shelf = shelfOf(uploaded("mac"), { locale: "de-DE", shelf: "APP_DESKTOP" });
    const [todo] = plan({ shelves: [shelf] }).shelves;
    assert.deepEqual({ id: todo.id, locale: todo.locale, shelf: todo.shelf }, { id: "de-DE-APP_DESKTOP", locale: "de-DE", shelf: "APP_DESKTOP" });
    assert.equal(todo.order.length, 10);
  });
});

describe("the script, as the lane runs it", () => {
  const run = (command, shelves, env = {}) => {
    const root = mkdtempSync(join(tmpdir(), "shelves-"));
    try {
      const file = join(root, "shelves.json");
      const summary = join(root, "summary.md");
      writeFileSync(file, JSON.stringify({ shelves }));
      writeFileSync(summary, "");
      const { status, stdout } = spawnSync("node", [SCRIPT, command, file], { encoding: "utf8", env: { ...process.env, GITHUB_STEP_SUMMARY: summary, ...env } });
      return { status, stdout, summary: readFileSync(summary, "utf8") };
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  };

  test("prints the plan as JSON", () => {
    const shelf = shelfOf(uploaded("mac"), { shelf: "APP_DESKTOP" });
    const { status, stdout } = run("plan", [shelf]);
    assert.equal(status, 0);
    assert.deepEqual(JSON.parse(stdout).shelves[0].order, tidyShelf(shelf).order);
  });

  test("passes shelves that are as staged, and fails on one that isn't, on the run's page too", () => {
    const asStaged = shelfOf(SCREENS.map((screen) => shot(fileOf(screen, "iphone_6_7"))));
    const passed = run("check", [asStaged]);
    assert.equal(passed.status, 0);
    assert.match(passed.stdout, /^1 shelves of 1 locale\(s\) hold each screen once, in file-name order\.\n$/);
    assert.equal(passed.summary, "");

    const short = shelfOf(asStaged.screenshots.filter(({ fileName }) => !fileName.startsWith("08_")));
    const failed = run("check", [asStaged, short]);
    assert.equal(failed.status, 1);
    assert.match(failed.stdout, /^1 thing\(s\) wrong on the shelves:\n- en-US \/ APP_IPHONE_67: 08_payers_and_currencies_iphone_6_7\.png is missing\n$/);
    assert.match(failed.summary, /^### Shelves that are not as staged\n\n- en-US \/ APP_IPHONE_67: 08_payers_and_currencies_iphone_6_7\.png is missing\n/);
    // Whoever reads the summary is about to repair a shelf: told here what removes a file.
    assert.match(failed.summary, /Don't delete one of a file's two entries by hand/);
  });

  test("says how it is called when it is called wrong", () => {
    assert.equal(run("sort", []).status, 1);
  });

  test("fails the check of a version with no shelf", () => {
    const { status, stdout } = run("check", []);
    assert.equal(status, 1);
    assert.match(stdout, /^1 thing\(s\) wrong on the shelves:\n- no locale of the version holds a screenshot\n$/);
  });

  // The lane reads each shelf in full and asks the script, by these words: a shelf read through the relationship
  // App Store Connect includes with it is 10 screenshots at most, whatever it lists. And it deletes no screenshot:
  // one of a doubled entry's two ids takes the other with it.
  test("is what the lane asks, about shelves it read in full, and the lane deletes nothing", () => {
    const fastfile = readFileSync(join(REPO_ROOT, "fastlane/Fastfile"), "utf8");
    const lane = fastfile.slice(fastfile.indexOf("def tidy_app_store_screenshots"), fastfile.indexOf("# The stable releases"));
    assert.ok(lane.includes('"scripts/store-screenshots/shelves.mjs"'));
    assert.ok(lane.includes('app_store_shelves("plan", ') && lane.includes('app_store_shelves("check", '));
    assert.ok(lane.includes('pages.call("relationships/appScreenshots")') && lane.includes('pages.call("appScreenshots")'));
    for (const key of ["locales:", "shelves:", "id:", "locale:", "shelf:", "isWhole:", "screenshots:", "fileName:", "state:", "checksum:"]) assert.ok(lane.includes(key), key);
    assert.doesNotMatch(lane, /app_screenshots\.group_by|reorder_screenshots|delete_app_screenshot|\.delete!/);
  });
});
