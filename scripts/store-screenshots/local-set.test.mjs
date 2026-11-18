// The tests of local-set.mjs, the App Store screenshots made on a Mac and sent through GitHub:
// node --test scripts/store-screenshots/local-set.test.mjs
//
// Each builds a set in a folder made for the test, of images that are a PNG's header and its end, since the check
// reads only those. The GitHub CLI is a stand-in that writes down what it was asked.

import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { afterEach, beforeEach, describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import {
  PLATFORMS,
  SET_NAME,
  SHELF_LABELS,
  SetError,
  TAG_PREFIX,
  checkPlatform,
  checkSet,
  describeShelves,
  dispatchCommand,
  landingReport,
  listingLocales,
  listingScreens,
  pack,
  parseArguments,
  readPng,
  resolveFolders,
} from "./local-set.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(HERE, "../..");
const SCRIPT = join(HERE, "local-set.mjs");
const LOCALES = listingLocales();
const SCREENS = listingScreens();
// The iPhone Duo's shelf as it is staged: five screens open, five closed (duoScreensStagedOpen).
const OPEN = ["01_group", "04_balances", "06_statistics", "07_home", "09_activity"];

let root;
beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), "local-set-"));
});
afterEach(() => rmSync(root, { recursive: true, force: true }));

/** A PNG's signature, the start of its header and its last chunk, which is all the check reads. */
const png = ([width, height], { isWhole = true } = {}) => {
  const bytes = Buffer.alloc(33);
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]).copy(bytes);
  bytes.writeUInt32BE(13, 8);
  bytes.write("IHDR", 12, "latin1");
  bytes.writeUInt32BE(width, 16);
  bytes.writeUInt32BE(height, 20);
  return isWhole ? Buffer.concat([bytes, Buffer.from([0, 0, 0, 0, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82])]) : bytes;
};
const write = (folder, locale, name, content) => {
  mkdirSync(join(folder, locale), { recursive: true });
  writeFileSync(join(folder, locale, name), content);
};
const isStaged = (screen, device) => !device.startsWith("iphone_duo") || OPEN.includes(screen) === (device === "iphone_duo_open");
/** A whole set of one platform under `<root>/<name>`, every locale, screen and device. `keep` leaves images out. */
const stage = (platform, { name = PLATFORMS[platform].folders[0], keep = () => true } = {}) => {
  const folder = join(root, name);
  for (const locale of LOCALES) {
    for (const screen of SCREENS) {
      for (const [device, { size }] of Object.entries(PLATFORMS[platform].devices)) {
        if (isStaged(screen, device) && keep({ locale, screen, device })) write(folder, locale, `${screen}_${device}.png`, png(size));
      }
    }
  }
  return folder;
};
const problemsOf = (platform, folder, options) => checkPlatform(platform, folder, options).problems;

describe("the check of a set", () => {
  test("takes a whole set, and says what each locale gets", () => {
    const ios = checkPlatform("ios", stage("ios"));
    assert.deepEqual(ios.problems, []);
    assert.deepEqual(ios.locales, LOCALES);
    assert.equal(ios.files.length, LOCALES.length * 50);
    assert.equal(describeShelves(ios), 'iPhone 6.3" 10, iPhone 6.7" 10, iPad 13" 10, iPad 11" 10, iPhone Duo 10 (5 open, 5 closed)');
    const mac = checkPlatform("mac", stage("mac"));
    assert.deepEqual(mac.problems, []);
    assert.equal(mac.files.length, LOCALES.length * 10);
    assert.equal(describeShelves(mac), "Mac 10");
  });

  test("refuses a set without a locale the listing has, naming it", () => {
    const folder = stage("mac", { keep: ({ locale }) => locale !== "ja" && locale !== "fr-FR" });
    assert.deepEqual(problemsOf("mac", folder), ["The listing has fr-FR and the Mac set lacks it", "The listing has ja and the Mac set lacks it"]);
  });

  test("refuses a folder that is no locale of the listing", () => {
    const folder = stage("mac");
    write(folder, "es-419", "01_group_mac.png", png([2880, 1800]));
    writeFileSync(join(folder, "notes.txt"), "");
    assert.deepEqual(problemsOf("mac", folder), ["es-419 is not a locale of the App Store listing (store-listing.mjs, APP_STORE_LOCALES)", "notes.txt is not a locale's folder"]);
  });

  test("refuses a device with fewer screens than the others in a locale, naming the screens", () => {
    const folder = stage("ios", { keep: ({ locale, screen, device }) => !(locale === "de-DE" && device === "ipad_13" && ["03_receipt", "10_light_theme"].includes(screen)) });
    assert.deepEqual(problemsOf("ios", folder), ['iPad 13" has 8 of 10 screens, without 03_receipt, 10_light_theme (de-DE)']);
  });

  test("refuses an image whose size isn't its device's, once for a mistake made in every locale", () => {
    const folder = stage("ios");
    for (const locale of LOCALES) write(folder, locale, "01_group_ipad_13.png", png([2048, 2732]));
    write(folder, "ja", "02_split_by_items_iphone_6_3.png", png([1290, 2796]));
    assert.deepEqual(problemsOf("ios", folder), [
      "01_group_ipad_13.png is 2048x2732 and ipad_13 takes 2732x2048 (every locale)",
      "02_split_by_items_iphone_6_3.png is 1290x2796 and iphone_6_3 takes 1206x2622 (ja)",
      // An image of the wrong size is not on its shelf.
      'iPhone 6.3" has 9 of 10 screens, without 02_split_by_items (ja)',
      'iPad 13" has 9 of 10 screens, without 01_group (every locale)',
    ]);
  });

  test("refuses a file that is not a PNG, by its name or by its bytes", () => {
    const folder = stage("mac");
    write(folder, "it", "01_group_mac.jpg", png([2880, 1800]));
    write(folder, "it", "02_split_by_items_mac.png", Buffer.from("not an image, whatever its name says"));
    mkdirSync(join(folder, "it", "raw"));
    assert.deepEqual(problemsOf("mac", folder), [
      "01_group_mac.jpg is not a PNG (it)",
      "02_split_by_items_mac.png is named as a PNG and isn't one (it)",
      "raw is not a file (it)",
      "Mac has 9 of 10 screens, without 02_split_by_items (it)",
    ]);
  });

  test("refuses a PNG that was cut short", () => {
    const folder = stage("mac");
    write(folder, "it", "07_home_mac.png", png([2880, 1800], { isWhole: false }));
    assert.deepEqual(problemsOf("mac", folder), ["07_home_mac.png is cut short: it starts as a PNG and doesn't end as one (it)", "Mac has 9 of 10 screens, without 07_home (it)"]);
  });

  test("refuses an image no screen and device name", () => {
    const folder = stage("mac");
    write(folder, "it", "11_settings_mac.png", png([2880, 1800]));
    write(folder, "it", "01_group_iphone_6_3.png", png([1206, 2622]));
    const problems = problemsOf("mac", folder);
    assert.equal(problems.length, 2);
    assert.match(problems[0], /^01_group_iphone_6_3\.png is not <screen>_<device>\.png.* \(it\)$/);
    assert.match(problems[1], /^11_settings_mac\.png is not <screen>_<device>\.png.* \(it\)$/);
  });

  test("leaves hidden files out of the set without refusing it", () => {
    const folder = stage("mac");
    writeFileSync(join(folder, ".DS_Store"), "");
    write(folder, "it", ".DS_Store", "");
    const report = checkPlatform("mac", folder);
    assert.deepEqual(report.problems, []);
    assert.equal(report.ignored.length, 2);
    assert.ok(report.files.every((file) => !file.includes(".DS_Store")));
  });

  // deliver replaces every screenshot of a locale with what it is handed.
  test("refuses a set that would empty a device's shelf by leaving the device out, unless that is meant", () => {
    const folder = stage("ios", { keep: ({ device }) => !device.startsWith("iphone_duo") });
    const problems = problemsOf("ios", folder);
    assert.equal(problems.length, 1);
    assert.match(problems[0], /^No iPhone Duo image in the set: .* would empty that shelf \(--allow-missing-devices when that is meant\)$/);
    const allowed = checkPlatform("ios", folder, { allowMissingDevices: true });
    assert.deepEqual(allowed.problems, []);
    assert.deepEqual(allowed.missingShelves, ["iphone_duo"]);
    assert.equal(describeShelves(allowed), 'iPhone 6.3" 10, iPhone 6.7" 10, iPad 13" 10, iPad 11" 10');
  });

  test("refuses a device some locales have and others lack, meant or not", () => {
    const folder = stage("ios", { keep: ({ locale, device }) => !(locale === "ko" && device === "iphone_6_7") });
    for (const allowMissingDevices of [false, true]) {
      assert.deepEqual(problemsOf("ios", folder, { allowMissingDevices }), ['No iPhone 6.7" image, which other locales have (ko)']);
    }
  });

  test("takes the iPhone Duo's ten screens from one display, and refuses a screen from both", () => {
    const closed = stage("ios", { name: "closed", keep: ({ device }) => device !== "iphone_duo_open" });
    for (const locale of LOCALES) for (const screen of OPEN) write(closed, locale, `${screen}_iphone_duo.png`, png([1398, 2034]));
    const report = checkPlatform("ios", closed);
    assert.deepEqual(report.problems, []);
    assert.match(describeShelves(report), /iPhone Duo 10 \(0 open, 10 closed\)$/);

    // One locale's open capture failed, so its ten are closed, and the others' are split: both are whole shelves.
    const mixed = stage("ios", { name: "mixed", keep: ({ locale, device }) => !(locale === "ja" && device === "iphone_duo_open") });
    for (const screen of OPEN) write(mixed, "ja", `${screen}_iphone_duo.png`, png([1398, 2034]));
    const uneven = checkPlatform("ios", mixed);
    assert.deepEqual(uneven.problems, []);
    assert.match(describeShelves(uneven), /iPhone Duo 10 \(open or closed, by locale\)$/);

    const both = stage("ios", { name: "both" });
    write(both, "en-US", "01_group_iphone_duo.png", png([1398, 2034]));
    assert.deepEqual(problemsOf("ios", both), ["01_group is on the iPhone Duo shelf 2 times (iphone_duo, iphone_duo_open): each screen goes up once (en-US)"]);
  });

  test("refuses a folder that isn't there, or holds no locale", () => {
    assert.deepEqual(problemsOf("mac", join(root, "nowhere")), [`No Mac screenshots: ${join(root, "nowhere")} is not a folder`]);
    mkdirSync(join(root, "empty"));
    assert.deepEqual(problemsOf("mac", join(root, "empty")), [`No Mac screenshots: ${join(root, "empty")} has no locale of the App Store listing`]);
  });

  test("lists every platform's problems in one refusal", () => {
    stage("ios", { keep: ({ locale }) => locale !== "ja" });
    assert.throws(
      () => checkSet({ folder: root, platforms: ["ios", "mac"] }),
      (error) => error instanceof SetError && /2 problem\(s\)/.test(error.message) && /iPhone and iPad: The listing has ja/.test(error.message) && /Mac: No Mac screenshots/.test(error.message),
    );
  });
});

describe("the set's folders and options", () => {
  test("finds the staged folders by a checkout's names or by a kept set's", () => {
    stage("ios", { name: "fastlane-screenshots" });
    stage("mac", { name: "fastlane-screenshots-mac" });
    assert.deepEqual(resolveFolders({ folder: root, platforms: ["ios", "mac"] }), { ios: join(root, "fastlane-screenshots"), mac: join(root, "fastlane-screenshots-mac") });
    assert.deepEqual(resolveFolders({ folder: join(root, "other"), platforms: ["mac"] }), { mac: join(root, "other/screenshots-mac") });
    assert.deepEqual(resolveFolders({ folder: root, mac: join(root, "elsewhere"), platforms: ["mac"] }), { mac: join(root, "elsewhere") });
    assert.equal(resolveFolders({ platforms: ["ios"] }).ios, join(REPO_ROOT, "fastlane/screenshots"));
  });

  test("takes both platforms unless one is named", () => {
    assert.deepEqual(parseArguments([]).platforms, ["ios", "mac"]);
    assert.deepEqual(parseArguments([root, "--dry-run"]).platforms, ["ios", "mac"]);
    assert.deepEqual(parseArguments(["--platform", "mac"]).platforms, ["mac"]);
    assert.deepEqual(parseArguments(["--mac", "fastlane/screenshots-mac"]).platforms, ["mac"]);
    assert.deepEqual(parseArguments(["--ios", "a", "--mac", "b", "--allow-missing-devices"]).platforms, ["ios", "mac"]);
    assert.equal(parseArguments(["--allow-missing-devices"]).allowMissingDevices, true);
    assert.throws(() => parseArguments(["--platform", "android"]), SetError);
    assert.throws(() => parseArguments(["--name"]), SetError);
    assert.throws(() => parseArguments(["--force"]), SetError);
    assert.throws(() => parseArguments(["one", "two"]), SetError);
  });

  // A device the frames gain must get its size here, or its images are refused as named after no device.
  test("knows the devices the frames are staged under", () => {
    const source = readFileSync(join(REPO_ROOT, "build-logic/convention/src/main/kotlin/com/quare/blitzsplit/buildlogic/StoreScreenshots.kt"), "utf8");
    const constants = Object.fromEntries([...source.matchAll(/private const val (\w+) = "([^"]+)"/g)].map(([, name, value]) => [name, value]));
    const stagedAs = (name) => {
      const map = source.match(new RegExp(`private val ${name} = mapOf\\(([\\s\\S]*?)\\)\\n`));
      assert.ok(map, `no ${name} in StoreScreenshots.kt`);
      return [...map[1].matchAll(/to "([^"]+)"/g)].map(([, device]) => device).sort();
    };
    assert.ok(constants.DUO_OPEN, "the constants of StoreScreenshots.kt are read");
    assert.deepEqual(Object.keys(PLATFORMS.ios.devices).sort(), stagedAs("appleDevices"));
    assert.deepEqual(Object.keys(PLATFORMS.mac.devices).sort(), stagedAs("macDevices"));
    for (const { devices } of Object.values(PLATFORMS)) {
      for (const { shelf } of Object.values(devices)) assert.ok(SHELF_LABELS[shelf], `${shelf} has a name`);
    }
  });

  test("reads a PNG's size off its header, and whether it ends as one", () => {
    const file = join(root, "image.png");
    writeFileSync(file, png([2853, 2007]));
    assert.deepEqual(readPng(file), { width: 2853, height: 2007, isWhole: true });
    writeFileSync(file, png([2853, 2007], { isWhole: false }));
    assert.equal(readPng(file).isWhole, false);
    writeFileSync(file, "short");
    assert.equal(readPng(file), null);
  });
});

describe("sending a set", () => {
  // A stand-in for gh: writes down each call, on one line, and answers `release view` with GH_VIEW, or as gh does when there
  // is no such release.
  const send = (args, env = {}) => {
    mkdirSync(join(root, "bin"), { recursive: true });
    writeFileSync(
      join(root, "bin/gh"),
      `#!/usr/bin/env bash\ncall="$*"\necho "\${call//$'\\n'/ }" >> "${join(root, "gh.log")}"\nif [ "$1 $2" = "release view" ]; then [ -n "\${GH_VIEW:-}" ] || { echo "\${GH_VIEW_ERROR:-release not found}" >&2; exit 1; }; echo "$GH_VIEW"; fi\nif [ "$1 $2" = "release create" ] && [ -n "\${GH_CREATE_FAILS:-}" ]; then exit 1; fi\n`,
    );
    chmodSync(join(root, "bin/gh"), 0o755);
    return spawnSync("node", [SCRIPT, "send", root, ...args], { encoding: "utf8", env: { ...process.env, PATH: `${join(root, "bin")}:${process.env.PATH}`, GITHUB_OUTPUT: "", ...env } });
  };
  const ghCalls = () => (existsSync(join(root, "gh.log")) ? readFileSync(join(root, "gh.log"), "utf8").trim().split("\n") : []);

  test("packs only the set's images, under their locale", () => {
    const folder = stage("mac");
    writeFileSync(join(folder, ".DS_Store"), "");
    const archive = pack(checkPlatform("mac", folder), root);
    const packed = execFileSync("tar", ["-tf", archive], { encoding: "utf8" }).trim().split("\n").sort();
    assert.equal(packed.length, LOCALES.length * 10);
    assert.equal(packed[0], `${LOCALES[0]}/01_group_mac.png`);
    assert.ok(packed.every((path) => /^[A-Za-z-]+\/\d\d_\w+_mac\.png$/.test(path)));
  });

  test("checks and packs with --dry-run, and sends nothing", () => {
    stage("ios");
    stage("mac");
    const { status, stdout } = send(["--name", "3.0.0", "--dry-run"]);
    assert.equal(status, 0);
    assert.match(stdout, /Packed ios\.tar/);
    assert.match(stdout, /Packed mac\.tar/);
    assert.match(stdout, /Nothing was sent; it would travel as the draft release store-screenshots-3\.0\.0\./);
    assert.deepEqual(ghCalls(), []);
  });

  test("uploads the archives and their checksums as a draft release, and prints how to dispatch the workflow", () => {
    writeFileSync(join(stage("ios"), ".DS_Store"), "");
    stage("mac");
    const { status, stdout } = send(["--name", "3.0.0"]);
    assert.equal(status, 0);
    const [view, create] = ghCalls();
    assert.equal(view, "release view store-screenshots-3.0.0 --json isDraft");
    assert.match(create, /^release create store-screenshots-3\.0\.0 --draft --title App Store screenshots: 3\.0\.0 --notes /);
    assert.match(create, /\/ios\.tar \S+\/mac\.tar \S+\/SHA256SUMS$/);
    // The release's text says what the set holds, and nothing of the Mac it came from.
    assert.match(create, /iPhone and iPad: 26 locale\(s\), 1300 image\(s\), 0 MB {3}each locale: /);
    assert.ok(!create.split(" --notes ")[1].split(" /")[0].includes(root));
    assert.match(stdout, /\n {2}gh workflow run app-store-screenshots\.yml -f set=3\.0\.0\n/);
  });

  test("sends one platform alone, and says so to the workflow", () => {
    stage("mac");
    const { status, stdout } = send(["--name", "mac-only", "--platform", "mac"]);
    assert.equal(status, 0);
    assert.doesNotMatch(ghCalls()[1], /ios\.tar/);
    assert.match(stdout, /gh workflow run app-store-screenshots\.yml -f set=mac-only -f ios=false\n/);
  });

  test("refuses a set that doesn't pass the check, before anything is packed or sent", () => {
    stage("ios");
    const { status, stderr } = send(["--name", "3.0.0"]);
    assert.equal(status, 1);
    assert.match(stderr, /The set is not sent, 1 problem\(s\):\n {2}- Mac: No Mac screenshots/);
    assert.deepEqual(ghCalls(), []);
  });

  test("refuses a name that is taken, replaces a draft when told to, and never a published release", () => {
    stage("mac");
    const taken = send(["--name", "3.0.0", "--platform", "mac"], { GH_VIEW: '{"isDraft":true}' });
    assert.equal(taken.status, 1);
    assert.match(taken.stderr, /A set named 3\.0\.0 is already there/);
    assert.deepEqual(ghCalls(), ["release view store-screenshots-3.0.0 --json isDraft"]);

    rmSync(join(root, "gh.log"));
    const replaced = send(["--name", "3.0.0", "--platform", "mac", "--replace"], { GH_VIEW: '{"isDraft":true}' });
    assert.equal(replaced.status, 0);
    assert.deepEqual(
      ghCalls().map((call) => call.split(" ").slice(0, 3).join(" ")),
      ["release view store-screenshots-3.0.0", "release delete store-screenshots-3.0.0", "release create store-screenshots-3.0.0"],
    );

    rmSync(join(root, "gh.log"));
    const published = send(["--name", "3.0.0", "--platform", "mac", "--replace"], { GH_VIEW: '{"isDraft":false}' });
    assert.equal(published.status, 1);
    assert.match(published.stderr, /is a published release, not a set/);
    assert.deepEqual(ghCalls(), ["release view store-screenshots-3.0.0 --json isDraft"]);
  });

  test("stops when GitHub can't say whether the name is taken, and sends nothing", () => {
    stage("mac");
    const { status, stderr } = send(["--name", "3.0.0", "--platform", "mac", "--replace"], { GH_VIEW_ERROR: "HTTP 502: Bad Gateway" });
    assert.equal(status, 1);
    assert.match(stderr, /Could not ask GitHub whether store-screenshots-3\.0\.0 is taken: HTTP 502/);
    assert.deepEqual(ghCalls(), ["release view store-screenshots-3.0.0 --json isDraft"]);
  });

  test("leaves nothing behind an upload that failed before a draft was made", () => {
    stage("mac");
    const { status, stderr } = send(["--name", "3.0.0", "--platform", "mac"], { GH_CREATE_FAILS: "1" });
    assert.equal(status, 1);
    assert.match(stderr, /The upload failed/);
    assert.deepEqual(
      ghCalls().map((call) => call.split(" ").slice(0, 2).join(" ")),
      ["release view", "release create", "release view"],
    );
  });

  test("refuses a name that couldn't be a tag", () => {
    stage("mac");
    for (const name of ["with space", "../3.0.0", "-f", ""]) {
      const { status, stderr } = send(["--name", name, "--platform", "mac", "--dry-run"]);
      assert.equal(status, 1, name);
      assert.match(stderr, /--name is letters, digits/);
    }
    for (const name of ["3.0..1", "3.0.", "3.0.lock"]) {
      const { status, stderr } = send(["--name", name, "--platform", "mac", "--dry-run"]);
      assert.equal(status, 1, name);
      assert.match(stderr, /--name couldn't be a tag/);
    }
  });

  test("says in the command only what differs from the workflow's defaults", () => {
    assert.equal(dispatchCommand("a", { platforms: ["ios", "mac"] }), "gh workflow run app-store-screenshots.yml -f set=a");
    assert.equal(dispatchCommand("a", { platforms: ["ios"], allowMissingDevices: true }), "gh workflow run app-store-screenshots.yml -f set=a -f macos=false -f allow_missing_devices=true");
  });
});

describe("the workflow's report", () => {
  const job = (name, conclusion = "success") => ({ name, conclusion });
  const platforms = [
    { label: "iPhone and iPad", caller: "iPhone and iPad", locales: ["en-US", "ja"], shelves: 'iPhone 6.3" 10' },
    { label: "Mac", caller: "Mac", locales: ["en-US", "ja"], shelves: "Mac 10" },
  ];
  const allJobs = [job("Fetch and check the set"), job("iPhone and iPad / send en-US"), job("iPhone and iPad / send ja"), job("iPhone and iPad / tidy up"), job("Mac / send en-US"), job("Mac / send ja"), job("Mac / tidy up")];

  test("says every locale landed, and what each took", () => {
    const { landedAll, markdown } = landingReport({ jobs: allJobs, set: "3.0.0", platforms });
    assert.equal(landedAll, true);
    assert.match(markdown, /Written on the editable version of each platform\. Nothing was submitted for review\./);
    assert.match(markdown, /### iPhone and iPad\n\n- \*\*2 of 2 locales landed\*\*, each with: iPhone 6\.3" 10\./);
    assert.doesNotMatch(markdown, /Not landed|not tidied/);
  });

  test("names a locale whose job failed, was cancelled or never ran, on its own platform alone", () => {
    const jobs = allJobs.filter(({ name }) => name !== "Mac / send ja").map((each) => (each.name === "iPhone and iPad / send ja" ? job(each.name, "failure") : each));
    const { landedAll, markdown } = landingReport({ jobs, set: "3.0.0", version: "3.0.1", platforms });
    assert.equal(landedAll, false);
    assert.match(markdown, /Written on version 3\.0\.1 of each platform/);
    assert.match(markdown, /### iPhone and iPad\n\n- \*\*1 of 2 locales landed\*\*.*\n- \*\*Not landed:\*\* ja \(failure\)\./);
    assert.match(markdown, /### Mac\n\n- \*\*1 of 2 locales landed\*\*.*\n- \*\*Not landed:\*\* ja \(not run\)\./);
  });

  test("keeps a failed tidy-up apart from the locales, which landed", () => {
    const jobs = allJobs.map((each) => (each.name === "Mac / tidy up" ? job(each.name, "failure") : each));
    const { landedAll, markdown } = landingReport({ jobs, set: "3.0.0", platforms });
    assert.equal(landedAll, true);
    assert.match(markdown, /The shelves were not tidied up\*\* \(failure\).*Run the "Mac \/ tidy up" job again\./);
  });

  // The report reads the jobs by the names the two workflows give them, and the workflow builds the release's tag
  // and checks the set's name by itself, in shell: held here to what the script does.
  test("matches what the workflows name and check", () => {
    const dispatch = readFileSync(join(REPO_ROOT, ".github/workflows/app-store-screenshots.yml"), "utf8");
    const upload = readFileSync(join(REPO_ROOT, ".github/workflows/app-store-screenshots-upload.yml"), "utf8");
    assert.ok(upload.includes("    name: send ${{ matrix.locale }}\n"));
    assert.ok(upload.includes("    name: tidy up\n"));
    for (const caller of ["iPhone and iPad", "Mac"]) assert.ok(dispatch.includes(`    name: ${caller}\n`), caller);
    assert.ok(dispatch.includes('--ios-job "iPhone and iPad"') && dispatch.includes('--mac-job "Mac"'));
    assert.ok(dispatch.includes(`SET_TAG: ${TAG_PREFIX}\${{ inputs.set }}\n`));
    assert.ok(dispatch.includes(`=~ ${SET_NAME.source} ]]`));
    for (const { archive } of Object.values(PLATFORMS)) assert.ok(dispatch.includes(`fetch ${archive} `), archive);
  });
});
