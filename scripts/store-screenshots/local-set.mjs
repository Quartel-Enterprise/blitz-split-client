#!/usr/bin/env node
// A set of App Store screenshots captured and framed on a Mac, on its way to App Store Connect through GitHub, where
// the key is: the `app-store-screenshots` workflow uploads it (docs/store-listing-screenshots.md#sending-a-set-made-on-a-mac).
//
//   node scripts/store-screenshots/local-set.mjs check [<folder>] [--platform ios|mac] [--allow-missing-devices]
//       Fails on a set App Store Connect would take wrong, saying what is wrong with it: a locale of the listing it
//       lacks, a device with a screen missing, an image whose size isn't its device's, a file that isn't a PNG.
//   node scripts/store-screenshots/local-set.mjs send [<folder>] [--name <name>] [--platform ios|mac]
//                                                      [--allow-missing-devices] [--replace] [--dry-run]
//       Checks the set, packs it and uploads it as the assets of a draft GitHub release, then prints the command that
//       dispatches the workflow. --dry-run checks and packs, and sends nothing.
//   node scripts/store-screenshots/local-set.mjs report --jobs <file> --set <name> …
//       The workflow's last step: which locales landed, from the run's jobs.
//
// <folder> holds the two staged folders, by either pair of names: `screenshots` and `screenshots-mac` (a checkout's
// fastlane/, the default) or `fastlane-screenshots` and `fastlane-screenshots-mac`. --ios and --mac name each one
// instead.
//
// Plain Node, no dependencies. `send` runs `tar` and the GitHub CLI.

import { execFileSync, spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { appendFileSync, closeSync, createReadStream, existsSync, fstatSync, lstatSync, mkdtempSync, openSync, readFileSync, readSync, readdirSync, realpathSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { APP_STORE_LOCALES } from "../release/store-listing.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(HERE, "../..");

export const WORKFLOW = "app-store-screenshots.yml";
/** The tag of the draft release a set travels as. A draft has no tag in the repository until it is published. */
export const TAG_PREFIX = "store-screenshots-";
export const tagOf = (name) => `${TAG_PREFIX}${name}`;
export const SET_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]{0,59}$/;
export const CHECKSUMS = "SHA256SUMS";
/** GitHub takes a release asset under 2 GiB, with no limit on a release's total size. */
export const MAX_ASSET_BYTES = 2 * 1024 ** 3;

/**
 * The devices of each App Store version, by the suffix their images' file names end in (`appleDevices` and
 * `macDevices` of build-logic's StoreScreenshots.kt, which a test holds these to), with the size the frames draw
 * each at (docs/store-listing-screenshots.md#what-the-images-show) and the shelf it goes on. The iPhone Duo's two
 * displays share one shelf of ten: App Store Connect has one device size for both.
 */
export const PLATFORMS = {
  ios: {
    label: "iPhone and iPad",
    folders: ["screenshots", "fastlane-screenshots"],
    archive: "ios.tar",
    devices: {
      iphone_6_3: { shelf: "iphone_6_3", size: [1206, 2622] },
      iphone_6_7: { shelf: "iphone_6_7", size: [1290, 2796] },
      ipad_13: { shelf: "ipad_13", size: [2732, 2048] },
      ipad_11: { shelf: "ipad_11", size: [2388, 1668] },
      iphone_duo: { shelf: "iphone_duo", size: [1398, 2034] },
      iphone_duo_open: { shelf: "iphone_duo", size: [2853, 2007] },
    },
  },
  mac: {
    label: "Mac",
    folders: ["screenshots-mac", "fastlane-screenshots-mac"],
    archive: "mac.tar",
    devices: {
      mac: { shelf: "mac", size: [2880, 1800] },
    },
  },
};
export const SHELF_LABELS = {
  iphone_6_3: 'iPhone 6.3"',
  iphone_6_7: 'iPhone 6.7"',
  ipad_13: 'iPad 13"',
  ipad_11: 'iPad 11"',
  iphone_duo: "iPhone Duo",
  mac: "Mac",
};

export class SetError extends Error {}

/** The locales the App Store listing has: a set has a folder for each, and no other. */
export const listingLocales = () => Object.values(APP_STORE_LOCALES).flat().sort();

/** The screens every shelf shows, by the name their files start with (01_group…), in shelf order. */
export const listingScreens = () =>
  readFileSync(join(HERE, "screens.tsv"), "utf8")
    .split("\n")
    .filter((row) => row && !row.startsWith("#"))
    .map((row) => row.split("\t")[1]);

const PNG_SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);

// The chunk every PNG ends with: its length, its name and its checksum, which never change.
const PNG_END = Buffer.from([0, 0, 0, 0, 0x49, 0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82]);

/**
 * A PNG's width and height, read off its header, and whether the file ends as a PNG does: one cut short by a copy
 * or a framing run that was interrupted has the header and not the end. Null for a file that isn't a PNG.
 */
export function readPng(file) {
  const header = Buffer.alloc(24);
  const end = Buffer.alloc(PNG_END.length);
  const descriptor = openSync(file, "r");
  try {
    const { size } = fstatSync(descriptor);
    if (readSync(descriptor, header, 0, 24, 0) < 24) return null;
    readSync(descriptor, end, 0, end.length, Math.max(0, size - end.length));
  } finally {
    closeSync(descriptor);
  }
  if (!header.subarray(0, 8).equals(PNG_SIGNATURE) || header.toString("latin1", 12, 16) !== "IHDR") return null;
  return { width: header.readUInt32BE(16), height: header.readUInt32BE(20), isWhole: end.equals(PNG_END) };
}

// Problems are gathered by what is wrong, with the locales it is wrong in, so one mistake made in every locale
// (an iPad framed upright) reads as one line and not as 260.
class Problems {
  #byMessage = new Map();

  add(message, locale) {
    if (!this.#byMessage.has(message)) this.#byMessage.set(message, new Set());
    if (locale) this.#byMessage.get(message).add(locale);
  }

  lines(localeCount) {
    return [...this.#byMessage].map(([message, locales]) => {
      if (locales.size === 0) return message;
      const where = locales.size === localeCount && localeCount > 1 ? "every locale" : [...locales].sort().join(", ");
      return `${message} (${where})`;
    });
  }
}

/**
 * Checks one version's staged folder, `<folder>/<locale>/<screen>_<device>.png`. Answers its problems, one line
 * each, and what it holds: the locales, the files to pack, and each shelf's images in a locale.
 *
 * deliver replaces every screenshot of a locale with what it is handed, so a shelf the set leaves out is emptied.
 * A device missing from the whole set is refused unless `allowMissingDevices` says it is left out on purpose; one
 * that some locales have and others lack is always refused.
 */
export function checkPlatform(platform, folder, { allowMissingDevices = false } = {}) {
  const { label, devices } = PLATFORMS[platform];
  const expectedLocales = listingLocales();
  const screens = listingScreens();
  // Longest first, so a screen whose name starts another's is never taken for it.
  const screensByLength = [...screens].sort((one, other) => other.length - one.length);
  const problems = new Problems();
  const shelves = [...new Set(Object.values(devices).map((device) => device.shelf))];
  const report = { platform, folder, locales: [], files: [], bytes: 0, ignored: [], shelves: {}, missingShelves: [] };

  if (!existsSync(folder) || !statSync(folder).isDirectory()) {
    return { ...report, problems: [`No ${label} screenshots: ${folder} is not a folder`] };
  }
  // Hidden files (the Finder's .DS_Store) are left out of the set, not refused: nothing packs or uploads them.
  const visible = (directory) =>
    readdirSync(directory).filter((name) => {
      if (name.startsWith(".")) report.ignored.push(join(directory, name));
      return !name.startsWith(".");
    });

  // shelf -> locale -> screen -> the devices that have an image of it
  const held = Object.fromEntries(shelves.map((shelf) => [shelf, new Map()]));
  for (const locale of visible(folder).sort()) {
    const localeFolder = join(folder, locale);
    if (!lstatSync(localeFolder).isDirectory()) {
      problems.add(`${locale} is not a locale's folder`);
      continue;
    }
    if (!expectedLocales.includes(locale)) {
      problems.add(`${locale} is not a locale of the App Store listing (store-listing.mjs, APP_STORE_LOCALES)`);
      continue;
    }
    report.locales.push(locale);
    for (const name of visible(localeFolder).sort()) {
      const file = join(localeFolder, name);
      const stats = lstatSync(file);
      if (!stats.isFile()) {
        problems.add(`${name} is not a file`, locale);
        continue;
      }
      if (!name.endsWith(".png")) {
        problems.add(`${name} is not a PNG`, locale);
        continue;
      }
      const screen = screensByLength.find((candidate) => name.startsWith(`${candidate}_`));
      const device = screen && name.slice(screen.length + 1, -".png".length);
      if (!screen || !Object.hasOwn(devices, device)) {
        problems.add(`${name} is not <screen>_<device>.png: no screen of screens.tsv with a device of ${Object.keys(devices).join(", ")}`, locale);
        continue;
      }
      const image = readPng(file);
      const [width, height] = devices[device].size;
      if (!image) {
        problems.add(`${name} is named as a PNG and isn't one`, locale);
        continue;
      }
      if (image.width !== width || image.height !== height) {
        problems.add(`${name} is ${image.width}x${image.height} and ${device} takes ${width}x${height}`, locale);
        continue;
      }
      if (!image.isWhole) {
        problems.add(`${name} is cut short: it starts as a PNG and doesn't end as one`, locale);
        continue;
      }
      const ofLocale = held[devices[device].shelf];
      if (!ofLocale.has(locale)) ofLocale.set(locale, new Map());
      if (!ofLocale.get(locale).has(screen)) ofLocale.get(locale).set(screen, []);
      ofLocale.get(locale).get(screen).push(device);
      report.files.push(`${locale}/${name}`);
      report.bytes += stats.size;
    }
  }

  const missingLocales = expectedLocales.filter((locale) => !report.locales.includes(locale));
  if (missingLocales.length === expectedLocales.length) {
    return { ...report, problems: [`No ${label} screenshots: ${folder} has no locale of the App Store listing`, ...problems.lines(0)] };
  }
  for (const locale of missingLocales) problems.add(`The listing has ${locale} and the ${label} set lacks it`);

  for (const shelf of shelves) {
    const ofLocale = held[shelf];
    if (ofLocale.size === 0) {
      report.missingShelves.push(shelf);
      if (!allowMissingDevices) {
        problems.add(
          `No ${SHELF_LABELS[shelf]} image in the set: the upload replaces a locale's screenshots with the set's, which would empty that shelf (--allow-missing-devices when that is meant)`,
        );
      }
      continue;
    }
    for (const locale of report.locales) {
      const ofScreen = ofLocale.get(locale) ?? new Map();
      const missing = screens.filter((screen) => !ofScreen.has(screen));
      if (missing.length === screens.length) {
        problems.add(`No ${SHELF_LABELS[shelf]} image, which other locales have`, locale);
      } else if (missing.length > 0) {
        problems.add(`${SHELF_LABELS[shelf]} has ${screens.length - missing.length} of ${screens.length} screens, without ${missing.join(", ")}`, locale);
      }
      // Only the iPhone Duo's shelf has two devices: ten images each way would put the first five screens up twice.
      for (const [screen, ofDevices] of ofScreen) {
        if (ofDevices.length > 1) problems.add(`${screen} is on the ${SHELF_LABELS[shelf]} shelf ${ofDevices.length} times (${ofDevices.join(", ")}): each screen goes up once`, locale);
      }
    }
    // What a locale gets: how many screens from each of the shelf's devices. A set that passes has every screen on
    // every shelf of every locale, but the iPhone Duo's may come from one display in a locale and from both in
    // another (a display that wasn't captured gives its screens to the other one), which is said and not counted.
    const ofShelf = Object.keys(devices).filter((device) => devices[device].shelf === shelf);
    const splits = [...ofLocale.values()].map((ofScreen) => ofShelf.map((device) => [...ofScreen.values()].filter((ofDevices) => ofDevices.includes(device)).length));
    const isSameEverywhere = splits.every((split) => split.join() === splits[0].join());
    report.shelves[shelf] = isSameEverywhere ? Object.fromEntries(ofShelf.map((device, index) => [device, splits[0][index]])) : { total: Math.max(...splits.map((split) => split.reduce((sum, count) => sum + count, 0))) };
  }
  return { ...report, problems: problems.lines(report.locales.length) };
}

/** What a locale gets on each shelf, in words: `iPhone 6.3" 10, iPhone Duo 10 (5 open, 5 closed)`. */
export function describeShelves(report) {
  return Object.entries(report.shelves)
    .map(([shelf, counts]) => {
      if (counts.total !== undefined) return `${SHELF_LABELS[shelf]} ${counts.total} (open or closed, by locale)`;
      if (shelf !== "iphone_duo") return `${SHELF_LABELS[shelf]} ${counts[shelf] ?? 0}`;
      const open = counts.iphone_duo_open ?? 0;
      const closed = counts.iphone_duo ?? 0;
      return `${SHELF_LABELS[shelf]} ${open + closed} (${open} open, ${closed} closed)`;
    })
    .join(", ");
}

const megabytes = (bytes) => `${(bytes / 1024 ** 2).toFixed(0)} MB`;

/**
 * Where each version's staged folder is: the ones `--ios` and `--mac` name, or the two under `folder`, by either
 * pair of names. A platform the options leave out isn't looked for.
 */
export function resolveFolders({ folder, ios, mac, platforms }) {
  const base = folder ?? join(REPO_ROOT, "fastlane");
  const named = { ios, mac };
  return Object.fromEntries(
    platforms.map((platform) => {
      if (named[platform]) return [platform, resolve(named[platform])];
      const candidates = PLATFORMS[platform].folders.map((name) => join(base, name));
      return [platform, resolve(candidates.find((candidate) => existsSync(candidate)) ?? candidates[0])];
    }),
  );
}

/** Checks every platform asked for. Throws a SetError that lists every problem; answers the reports otherwise. */
export function checkSet(options) {
  const folders = resolveFolders(options);
  const reports = options.platforms.map((platform) => checkPlatform(platform, folders[platform], options));
  const problems = reports.flatMap((report) => report.problems.map((problem) => `${PLATFORMS[report.platform].label}: ${problem}`));
  if (problems.length > 0) {
    throw new SetError(`The set is not sent, ${problems.length} problem(s):\n${problems.map((problem) => `  - ${problem}`).join("\n")}`);
  }
  return reports;
}

function describe(reports) {
  return reports.flatMap((report) => {
    const lines = [
      `${PLATFORMS[report.platform].label}: ${report.locales.length} locale(s), ${report.files.length} image(s), ${megabytes(report.bytes)}, from ${report.folder}`,
      `  each locale: ${describeShelves(report)}`,
    ];
    if (report.missingShelves.length > 0) {
      lines.push(`  left out on purpose, so emptied in every locale: ${report.missingShelves.map((shelf) => SHELF_LABELS[shelf]).join(", ")}`);
    }
    if (report.ignored.length > 0) lines.push(`  ${report.ignored.length} hidden file(s) left out (${report.ignored[0]}…)`);
    return lines;
  });
}

// --- check, for the workflow too ---------------------------------------------------------------------------------------

function check(options) {
  const reports = checkSet(options);
  console.log(describe(reports).join("\n"));
  // The workflow's last job says what each locale took, and a job hands the next one only words.
  if (process.env.GITHUB_OUTPUT) {
    appendFileSync(process.env.GITHUB_OUTPUT, reports.map((report) => `${report.platform}_shelves=${describeShelves(report)}\n`).join(""));
  }
}

// --- send ----------------------------------------------------------------------------------------------------------------

const sha256 = (file) =>
  new Promise((done, fail) => {
    const hash = createHash("sha256");
    createReadStream(file)
      .on("data", (chunk) => hash.update(chunk))
      .on("error", fail)
      .on("end", () => done(hash.digest("hex")));
  });

/**
 * Packs a checked folder into `<out>/<archive>`, only the files the check listed: no hidden file, and nothing of
 * macOS's own beside them (ustar has no room for extended attributes, and COPYFILE_DISABLE keeps tar from adding a
 * `._` file per image). Not compressed, since a PNG already is.
 */
export function pack(report, out) {
  const archive = join(out, PLATFORMS[report.platform].archive);
  const list = join(out, `${report.platform}.files`);
  writeFileSync(list, `${report.files.join("\n")}\n`);
  execFileSync("tar", ["-cf", archive, "--format=ustar", "-C", report.folder, "-T", list], { env: { ...process.env, COPYFILE_DISABLE: "1" }, stdio: ["ignore", "inherit", "inherit"] });
  rmSync(list);
  return archive;
}

const defaultName = (now = new Date()) => now.toISOString().replace(/[-:]/g, "").replace("T", "-").slice(0, 13);

/** The command that dispatches the workflow for a set: only what differs from its defaults is said. */
export function dispatchCommand(name, { platforms, allowMissingDevices }) {
  const fields = [`set=${name}`];
  if (!platforms.includes("ios")) fields.push("ios=false");
  if (!platforms.includes("mac")) fields.push("macos=false");
  if (allowMissingDevices) fields.push("allow_missing_devices=true");
  return `gh workflow run ${WORKFLOW} ${fields.map((field) => `-f ${field}`).join(" ")}`;
}

async function send(options) {
  const name = options.name ?? defaultName();
  if (!SET_NAME.test(name)) throw new SetError(`--name is letters, digits, dots, dashes and underscores, 60 at most: ${name}`);
  // What git takes in a tag on top of SET_NAME's letters: no "..", and no "." or ".lock" at the end.
  if (/\.\.|\.$|\.lock$/.test(name)) throw new SetError(`--name couldn't be a tag: no "..", and no "." or ".lock" at the end: ${name}`);
  const tag = tagOf(name);
  const reports = checkSet(options);
  console.log(describe(reports).join("\n"));

  const gh = (args) => execFileSync("gh", args, { cwd: REPO_ROOT, stdio: ["ignore", "inherit", "inherit"] });
  // The release of the set's tag, or null when there is none. Any other failure stops the run: a release that
  // couldn't be read is not one that isn't there, and the set would go up beside it or be deleted in its place.
  const findRelease = () => {
    const { status, stdout, stderr } = spawnSync("gh", ["release", "view", tag, "--json", "isDraft"], { cwd: REPO_ROOT, encoding: "utf8" });
    if (status === 0) return JSON.parse(stdout);
    if (/release not found/i.test(stderr ?? "")) return null;
    throw new SetError(`Could not ask GitHub whether ${tag} is taken: ${(stderr ?? "").trim() || "gh failed"}`);
  };
  const existing = options.dryRun ? null : findRelease();
  if (existing) {
    // Never a published release: only a draft is a set, and only a set is this script's to delete.
    if (!existing.isDraft) throw new SetError(`${tag} is a published release, not a set: pick another --name`);
    if (!options.replace) throw new SetError(`A set named ${name} is already there (the draft release ${tag}): pass --replace to send this one in its place, or another --name`);
  }
  const out = mkdtempSync(join(tmpdir(), "store-screenshots-set-"));
  try {
    const archives = reports.map((report) => pack(report, out));
    const sums = [];
    for (const archive of archives) {
      const { size } = statSync(archive);
      if (size >= MAX_ASSET_BYTES) throw new SetError(`${archive} is ${megabytes(size)}, and GitHub takes a release asset under 2 GiB`);
      sums.push(`${await sha256(archive)}  ${archive.slice(out.length + 1)}`);
      console.log(`Packed ${archive.slice(out.length + 1)}, ${megabytes(size)}`);
    }
    writeFileSync(join(out, CHECKSUMS), `${sums.join("\n")}\n`);
    if (options.dryRun) {
      console.log(`--dry-run: the set is complete and packs. Nothing was sent; it would travel as the draft release ${tag}.`);
      return;
    }

    const notes = [
      "App Store screenshots captured and framed on a Mac, on their way to App Store Connect (docs/store-listing-screenshots.md#sending-a-set-made-on-a-mac).",
      "A draft on purpose: only people who can push to the repository see it, and it has no tag. The `app-store-screenshots` workflow removes it once every locale landed.",
      "",
      // Without this Mac's paths: where the set was, and its hidden files.
      ...describe(reports)
        .filter((line) => !line.includes("hidden file"))
        .map((line) => line.replace(/, from .*$/, "")),
    ].join("\n");
    // The set it replaces goes only once this one is packed: a pack that failed leaves it where it was.
    if (existing) gh(["release", "delete", tag, "--yes"]);
    console.log(`Uploading the set as the draft release ${tag}…`);
    try {
      gh(["release", "create", tag, "--draft", "--title", `App Store screenshots: ${name}`, "--notes", notes, ...archives, join(out, CHECKSUMS)]);
    } catch (error) {
      // A draft left with half its assets would be taken for a set. Nothing is there when the create itself failed.
      // A clean-up that fails too must not hide the upload's failure, nor say that nothing was left.
      let isClean = true;
      try {
        if (findRelease()?.isDraft) gh(["release", "delete", tag, "--yes"]);
      } catch {
        isClean = false;
      }
      throw new SetError(
        isClean
          ? `The upload failed (gh says why above), and nothing of it was left on GitHub. Exit ${error.status}.`
          : `The upload failed (gh says why above), and the draft release ${tag} may be left half sent: remove it with gh release delete ${tag} --yes. Exit ${error.status}.`,
      );
    }
  } finally {
    rmSync(out, { recursive: true, force: true });
  }

  console.log(
    [
      "",
      `Sent as the draft release ${tag}: only people who can push to the repository see it, and it has no tag.`,
      "",
      "Upload it to App Store Connect, on the editable versions, once no release is running (an administrator, on main):",
      "",
      `  ${dispatchCommand(name, options)}`,
      "",
      "Add -f app_version=X.Y.Z to name the version to write on. The run removes the draft once every locale landed.",
      `To take the set back without uploading it: gh release delete ${tag} --yes`,
    ].join("\n"),
  );
}

// --- report, the workflow's last step ---------------------------------------------------------------------------------

/**
 * Which locales landed, from the run's jobs (the REST API's list, latest attempt of each): a locale landed when its
 * upload job, `<caller> / send <locale>` (app-store-screenshots-upload.yml), succeeded. Answers the summary's
 * Markdown and whether every locale of every platform did.
 */
export function landingReport({ jobs, set, version, platforms }) {
  const lines = [`## App Store screenshots: the set \`${set}\``, "", `Written on ${version ? `version ${version}` : "the editable version"} of each platform. Nothing was submitted for review.`, ""];
  let landedAll = true;
  for (const { label, caller, locales, shelves } of platforms) {
    const conclusionOf = (locale) => jobs.find((job) => job.name === `${caller} / send ${locale}`)?.conclusion ?? "not run";
    const failed = locales.filter((locale) => conclusionOf(locale) !== "success");
    const tidy = jobs.find((job) => job.name === `${caller} / tidy up`)?.conclusion ?? "not run";
    landedAll &&= failed.length === 0;
    lines.push(`### ${label}`, "", `- **${locales.length - failed.length} of ${locales.length} locales landed**, each with: ${shelves}.`);
    if (failed.length > 0) {
      lines.push(`- **Not landed:** ${failed.map((locale) => `${locale} (${conclusionOf(locale)})`).join(", ")}. Run each one's job again with its own re-run button: the set is still there.`);
    }
    if (tidy !== "success") {
      lines.push(`- **The shelves were not tidied up** (${tidy}): duplicates may be left and a shelf out of order. Run the "${caller} / tidy up" job again.`);
    }
    lines.push("");
  }
  return { landedAll, markdown: lines.join("\n") };
}

function report(options) {
  const jobs = JSON.parse(readFileSync(options.jobs, "utf8"));
  const platforms = ["ios", "mac"]
    .filter((platform) => options[`${platform}Locales`])
    .map((platform) => ({
      label: PLATFORMS[platform].label,
      caller: options[`${platform}Job`],
      locales: JSON.parse(options[`${platform}Locales`]),
      shelves: options[`${platform}Shelves`],
    }));
  const { landedAll, markdown } = landingReport({ jobs, set: options.set, version: options.version, platforms });
  console.log(markdown);
  if (process.env.GITHUB_OUTPUT) appendFileSync(process.env.GITHUB_OUTPUT, `landed=${landedAll}\n`);
}

// --- The commands ----------------------------------------------------------------------------------------------------

const USAGE = `Usage:
  local-set.mjs check [<folder>] [--platform ios|mac] [--ios <folder>] [--mac <folder>] [--allow-missing-devices]
  local-set.mjs send  [<folder>] [--name <name>] [--platform ios|mac] [--ios <folder>] [--mac <folder>]
                      [--allow-missing-devices] [--replace] [--dry-run]
  local-set.mjs report --jobs <file> --set <name> [--version <X.Y.Z>]
                      [--ios-job <name> --ios-locales <json> --ios-shelves <text>]
                      [--mac-job <name> --mac-locales <json> --mac-shelves <text>]`;

const FLAGS = { "--allow-missing-devices": "allowMissingDevices", "--replace": "replace", "--dry-run": "dryRun" };
const VALUES = {
  "--platform": "platform",
  "--ios": "ios",
  "--mac": "mac",
  "--name": "name",
  "--jobs": "jobs",
  "--set": "set",
  "--version": "version",
  "--ios-job": "iosJob",
  "--ios-locales": "iosLocales",
  "--ios-shelves": "iosShelves",
  "--mac-job": "macJob",
  "--mac-locales": "macLocales",
  "--mac-shelves": "macShelves",
};

export function parseArguments(args) {
  const options = {};
  for (let index = 0; index < args.length; index++) {
    const argument = args[index];
    if (Object.hasOwn(FLAGS, argument)) {
      options[FLAGS[argument]] = true;
    } else if (Object.hasOwn(VALUES, argument)) {
      if (index + 1 >= args.length) throw new SetError(`${argument} takes a value\n${USAGE}`);
      options[VALUES[argument]] = args[++index];
    } else if (argument.startsWith("--") || options.folder) {
      throw new SetError(`Unknown argument ${argument}\n${USAGE}`);
    } else {
      options.folder = resolve(argument);
    }
  }
  if (options.platform && !Object.hasOwn(PLATFORMS, options.platform)) throw new SetError(`--platform is ios or mac\n${USAGE}`);
  // A folder named for one version alone is a set of that version.
  const named = ["ios", "mac"].filter((platform) => options[platform]);
  options.platforms = options.platform ? [options.platform] : named.length > 0 && !options.folder ? named : ["ios", "mac"];
  return options;
}

async function main([command, ...rest]) {
  const options = parseArguments(rest);
  if (command === "check") check(options);
  else if (command === "send") await send(options);
  else if (command === "report" && options.jobs && options.set) report(options);
  else throw new SetError(USAGE);
}

// Why: macOS resolves /tmp and /var to /private, so the path a shell passes and the module's own differ unresolved.
if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main(process.argv.slice(2)).catch((error) => {
    if (!(error instanceof SetError)) throw error;
    console.error(error.message);
    process.exit(1);
  });
}
