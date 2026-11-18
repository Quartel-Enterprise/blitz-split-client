// The tests of shelves.mjs, what the tidy-up of the App Store's shelves removes, sorts and reports:
// node --test scripts/store-screenshots/shelves.test.mjs
//
// A shelf is written as the lane reads it: its screenshots in shelf order, each with its file name and the state
// App Store Connect gives it.

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
const shot = (fileName, state = "COMPLETE") => ({ id: `s${++nextId}`, fileName, state });
const fileOf = (screen, device) => `${screen}_${device}.png`;
/** A device's ten screens in the order their uploads landed, which is not the file names'. */
const uploaded = (device, screens = SCREENS) => {
  const shots = screens.map((screen) => shot(fileOf(screen, device)));
  return [...shots.slice(3), ...shots.slice(0, 3)];
};
const shelfOf = (screenshots, { locale = "en-US", shelf = "APP_IPHONE_67", isWhole = true } = {}) => ({ id: `${locale}-${shelf}`, locale, shelf, isWhole, screenshots });
const namesOf = (shelf, ids) => ids.map((id) => shelf.screenshots.find((screenshot) => screenshot.id === id).fileName);
/** The shelf once the lane did what the plan says. */
const tidied = (shelf) => {
  const { order } = tidyShelf(shelf);
  return { ...shelf, screenshots: order.map((id) => shelf.screenshots.find((screenshot) => screenshot.id === id)) };
};
const inOrder = (device) => SCREENS.map((screen) => fileOf(screen, device));

describe("what the tidy-up does with a shelf", () => {
  // Mac de-DE and the iPad 11" en-US of run 44324532909: the include showed the first 10, all distinct.
  test("removes the copy in the 11th place of a shelf of 11 and sorts the 10 left", () => {
    const copy = shot(fileOf("02_split_by_items", "mac"));
    const shelf = shelfOf([...uploaded("mac"), copy], { locale: "de-DE", shelf: "APP_DESKTOP" });
    const { remove, order, problems } = tidyShelf(shelf);
    assert.deepEqual(remove, [copy.id]);
    assert.deepEqual(namesOf(shelf, order), inOrder("mac"));
    assert.deepEqual(problems, []);
    assert.deepEqual(shelfProblems(tidied(shelf)), []);
  });

  // The iPhone 6.7" en-US of that run, which lost 08: the record a retried create left without an image came
  // first, and keeping the first copy by position removed the one that had the image.
  test("keeps the copy whose image is there, wherever it stands, and removes the one that got none", () => {
    const empty = shot(fileOf("08_payers_and_currencies", "iphone_6_7"), "AWAITING_UPLOAD");
    const real = shot(fileOf("08_payers_and_currencies", "iphone_6_7"));
    const others = uploaded("iphone_6_7", SCREENS.filter((screen) => screen !== "08_payers_and_currencies"));
    const shelf = shelfOf([...others.slice(0, 8), empty, real, others[8]]);
    const { remove, order, problems } = tidyShelf(shelf);
    assert.deepEqual(remove, [empty.id]);
    assert.ok(order.includes(real.id));
    assert.deepEqual(namesOf(shelf, order), inOrder("iphone_6_7"));
    assert.deepEqual(problems, []);
  });

  test("keeps the first of several copies that all have their image", () => {
    const [first, second, third] = [1, 2, 3].map(() => shot(fileOf("01_group", "mac")));
    const shelf = shelfOf([first, ...uploaded("mac").filter(({ fileName }) => fileName !== first.fileName), second, third], { shelf: "APP_DESKTOP" });
    assert.deepEqual(tidyShelf(shelf).remove, [second.id, third.id]);
    assert.equal(tidyShelf(shelf).order[0], first.id);
  });

  test("removes nothing from a shelf that wasn't read whole, gives it no order, and says so", () => {
    const shelf = shelfOf([...uploaded("mac"), shot(fileOf("02_split_by_items", "mac"))], { shelf: "APP_DESKTOP", isWhole: false });
    const { remove, order, problems } = tidyShelf(shelf);
    assert.deepEqual(remove, []);
    assert.equal(order, null);
    assert.equal(problems.length, 1);
    assert.match(problems[0], /^could not be read whole, so nothing was removed or sorted/);
  });

  test("leaves a shelf that is already tidy as it is", () => {
    const shelf = shelfOf(SCREENS.map((screen) => shot(fileOf(screen, "ipad_11"))), { shelf: "APP_IPAD_PRO_3GEN_11" });
    const { remove, order, problems } = tidyShelf(shelf);
    assert.deepEqual(remove, []);
    assert.deepEqual(order, shelf.screenshots.map(({ id }) => id));
    assert.deepEqual(problems, []);
    assert.deepEqual(shelfProblems(shelf), []);
  });

  test("sorts as deliver does, 2 before 10, whatever the digits' count", () => {
    const shelf = shelfOf([shot("10_b_mac.png"), shot("9_a_mac.png"), shot("2_c_mac.png")]);
    assert.deepEqual(namesOf(shelf, tidyShelf(shelf, ["2_c", "9_a", "10_b"]).order), ["2_c_mac.png", "9_a_mac.png", "10_b_mac.png"]);
  });
});

describe("what no tidy-up fixes", () => {
  test("names the file a shelf is short of, and still sorts what is there", () => {
    const shelf = shelfOf(uploaded("iphone_6_7", SCREENS.filter((screen) => screen !== "08_payers_and_currencies")));
    const { remove, order, problems } = tidyShelf(shelf);
    assert.deepEqual(remove, []);
    assert.equal(order.length, 9);
    assert.deepEqual(problems, ["08_payers_and_currencies_iphone_6_7.png is missing"]);
    // Sorted, the shelf is still short of it: the check after the tidy-up fails on it.
    assert.deepEqual(check({ shelves: [tidied(shelf)] }), ["en-US / APP_IPHONE_67: 08_payers_and_currencies_iphone_6_7.png is missing"]);
  });

  test("names a file none of whose copies finished uploading, and removes none of them", () => {
    const stuck = [shot(fileOf("03_receipt", "mac"), "AWAITING_UPLOAD"), shot(fileOf("03_receipt", "mac"), "FAILED")];
    const shelf = shelfOf([...uploaded("mac").filter(({ fileName }) => fileName !== stuck[0].fileName), ...stuck], { shelf: "APP_DESKTOP" });
    const { remove, order, problems } = tidyShelf(shelf);
    assert.deepEqual(remove, []);
    assert.equal(order.length, 11);
    assert.deepEqual(problems, ["03_receipt_mac.png never finished uploading (AWAITING_UPLOAD, FAILED)"]);
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
  test("fails on a copy that is still there and on a shelf out of order, naming the locale and the shelf", () => {
    const withCopy = shelfOf([...SCREENS.map((screen) => shot(fileOf(screen, "mac"))), shot(fileOf("02_split_by_items", "mac"))], { locale: "de-DE", shelf: "APP_DESKTOP" });
    const unsorted = shelfOf(uploaded("mac"), { shelf: "APP_DESKTOP" });
    const tidy = shelfOf(SCREENS.map((screen) => shot(fileOf(screen, "mac"))), { locale: "ja", shelf: "APP_DESKTOP" });
    const problems = check({ locales: ["de-DE", "en-US", "ja"], shelves: [withCopy, unsorted, tidy] });
    assert.equal(problems.length, 2);
    assert.equal(problems[0], "de-DE / APP_DESKTOP: 02_split_by_items_mac.png is there 2 times");
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
    // A device every locale goes without was left out of the set, and a version with no screenshot has none to miss.
    assert.deepEqual(check({ locales: ["en-US", "ja"], shelves: [shelves[0], shelves[2]] }), []);
    assert.deepEqual(check({ locales: ["en-US", "ja"], shelves: [] }), []);
  });

  test("removes and sorts nothing on a shelf with a screenshot that has no file name", () => {
    const nameless = [{ id: "a", state: "COMPLETE" }, { id: "b", state: "COMPLETE" }];
    const { remove, order, problems } = tidyShelf(shelfOf([...uploaded("mac"), ...nameless], { shelf: "APP_DESKTOP" }));
    assert.deepEqual(remove, []);
    assert.equal(order, null);
    assert.deepEqual(problems, ["holds a screenshot with no file name, so nothing was removed or sorted"]);
  });

  test("plans by the shelf's id, for the lane to find each one", () => {
    const shelf = shelfOf([...uploaded("mac"), shot(fileOf("02_split_by_items", "mac"))], { locale: "de-DE", shelf: "APP_DESKTOP" });
    const [todo] = plan({ shelves: [shelf] }).shelves;
    assert.deepEqual({ id: todo.id, locale: todo.locale, shelf: todo.shelf }, { id: "de-DE-APP_DESKTOP", locale: "de-DE", shelf: "APP_DESKTOP" });
    assert.equal(todo.remove.length, 1);
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
    const copy = shot(fileOf("02_split_by_items", "mac"));
    const { status, stdout } = run("plan", [shelfOf([...uploaded("mac"), copy], { shelf: "APP_DESKTOP" })]);
    assert.equal(status, 0);
    assert.deepEqual(JSON.parse(stdout).shelves[0].remove, [copy.id]);
  });

  test("passes shelves that are as staged, and fails on one that isn't, on the run's page too", () => {
    const tidy = shelfOf(SCREENS.map((screen) => shot(fileOf(screen, "iphone_6_7"))));
    const passed = run("check", [tidy]);
    assert.equal(passed.status, 0);
    assert.match(passed.stdout, /^1 shelves of 1 locale\(s\) hold each screen once, in file-name order\.\n$/);
    assert.equal(passed.summary, "");

    const short = shelfOf(tidy.screenshots.filter(({ fileName }) => !fileName.startsWith("08_")));
    const failed = run("check", [tidy, short]);
    assert.equal(failed.status, 1);
    assert.match(failed.stdout, /^1 thing\(s\) wrong on the shelves:\n- en-US \/ APP_IPHONE_67: 08_payers_and_currencies_iphone_6_7\.png is missing\n$/);
    assert.match(failed.summary, /^### Shelves that are not as staged\n\n- en-US \/ APP_IPHONE_67: 08_payers_and_currencies_iphone_6_7\.png is missing\n/);
  });

  test("says how it is called when it is called wrong", () => {
    assert.equal(run("sort", []).status, 1);
  });

  // The lane reads each shelf in full and asks the script, by these words: a shelf read through the relationship
  // App Store Connect includes with it is 10 screenshots at most, whatever it holds.
  test("is what the lane asks, about shelves it read in full", () => {
    const fastfile = readFileSync(join(REPO_ROOT, "fastlane/Fastfile"), "utf8");
    const lane = fastfile.slice(fastfile.indexOf("def tidy_app_store_screenshots"), fastfile.indexOf("# The stable releases"));
    assert.ok(lane.includes('"scripts/store-screenshots/shelves.mjs"'));
    assert.ok(lane.includes('app_store_shelves("plan", ') && lane.includes('app_store_shelves("check", '));
    assert.ok(lane.includes('pages.call("relationships/appScreenshots")') && lane.includes('pages.call("appScreenshots")'));
    for (const key of ["locales:", "shelves:", "id:", "locale:", "shelf:", "isWhole:", "screenshots:", "fileName:", "state:"]) assert.ok(lane.includes(key), key);
    assert.doesNotMatch(lane, /app_screenshots\.group_by|reorder_screenshots/);
  });
});
