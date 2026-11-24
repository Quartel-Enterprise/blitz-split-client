#!/usr/bin/env node
// What the jobs that send Google Play's screenshots decide (play-store-screenshots-upload.yml and the
// upload_screenshots lane of fastlane/Fastfile), kept here so that it has tests: play-screenshot-groups.test.mjs.
//
// Google Play counts every save of an app, the commit of a Play edit, against a daily quota nobody documents
// (docs/store-listing-screenshots.md#google-plays-daily-save-quota). So the locales go up a few per edit instead
// of one each, a locale whose images Google Play already holds is left out of the edit, and a save Google Play
// refuses for the quota stops the groups after it, which are sent later.
//
//   node scripts/release/play-screenshot-groups.mjs groups '["ar","de-DE",…]' [--size 6]
//       The jobs' matrix, as one line of JSON: [{"group":1,"of":5,"locales":"ar,cs-CZ","name":"1 of 5: ar, cs-CZ"},…]
//   node scripts/release/play-screenshot-groups.mjs changed < shelves.json
//       The locales to send, as a JSON array: the ones with a shelf Google Play doesn't hold as it is staged.
//       shelves.json is {"<locale>":{"<shelf>":{"staged":["<sha256>",…],"held":["<sha256>",…]}}}
//   node scripts/release/play-screenshot-groups.mjs quota-reached
//       Prints true when a group of this attempt of the run was refused for the quota (it left the artifact
//       QUOTA_MARKER), false otherwise, also when GitHub can't say. Needs GH_TOKEN, GITHUB_REPOSITORY, GITHUB_RUN_ID.
//   node scripts/release/play-screenshot-groups.mjs summary --groups '<the matrix>' --group 3 [--sent a,b]
//       [--unchanged c] [--not-sent d,e] [--quota reached] [--validate-only true]
//       What a group's job says on the run's page, as Markdown.
//   node scripts/release/play-screenshot-groups.mjs warning --groups '<the matrix>' --group 3 [--not-sent d,e]
//       The one line of the warning a group's job leaves when the quota kept its locales from going: with
//       --not-sent, the group Google Play refused; without, a group after it, which wasn't tried.

import { execFileSync } from "node:child_process";
import { readFileSync, realpathSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

/** How many locales go through one Play edit unless the caller says: 56 locales are ten saves. */
export const DEFAULT_GROUP_SIZE = 6;
/** Where the docs explain the quota and what to do on a day it runs out. */
export const QUOTA_DOC = "docs/store-listing-screenshots.md#google-plays-daily-save-quota";

export class GroupsError extends Error {}

// A locale ends up as a word of fastlane's command line (locales:ar,de-DE), so nothing but a store's code gets there.
const LOCALE = /^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$/;

/**
 * Splits the locales into as few groups as `size` allows, of sizes that differ by one at most, in the order given:
 * 27 locales by 6 are five groups of 6, 6, 5, 5 and 5, not four of 6 and a last one of 3.
 */
export function groupLocales(locales, size = DEFAULT_GROUP_SIZE) {
  if (!Array.isArray(locales) || locales.length === 0) throw new GroupsError("No locale to group: the list is empty.");
  const odd = locales.find((locale) => typeof locale !== "string" || !LOCALE.test(locale));
  if (odd !== undefined) throw new GroupsError(`Not a store's locale: ${JSON.stringify(odd)}.`);
  const twice = locales.find((locale, index) => locales.indexOf(locale) !== index);
  if (twice !== undefined) throw new GroupsError(`${twice} is listed twice.`);
  if (!Number.isInteger(size) || size < 1) throw new GroupsError(`The size of a group is a whole number from 1 up, not ${size}.`);

  const count = Math.ceil(locales.length / size);
  const smaller = Math.floor(locales.length / count);
  const larger = locales.length % count;
  const groups = [];
  let next = 0;
  for (let index = 0; index < count; index += 1) {
    const members = locales.slice(next, next + smaller + (index < larger ? 1 : 0));
    next += members.length;
    groups.push({ group: index + 1, of: count, locales: members.join(","), name: `${index + 1} of ${count}: ${members.join(", ")}` });
  }
  return groups;
}

/**
 * The locales with something to send: a shelf whose staged images are not, one for one and in order, the ones
 * Google Play holds. It is the comparison supply's sync_image_upload makes image by image, made before any edit is
 * opened to write: a locale it leaves out would upload nothing and still cost the save of its edit. A shelf that
 * isn't staged is not in `shelves`, and supply leaves it alone too.
 */
export function changedLocales(shelves) {
  return Object.entries(shelves)
    .filter(([, byShelf]) => Object.values(byShelf).some(({ staged, held }) => !Array.isArray(held) || staged.length !== held.length || staged.some((sha, index) => sha !== held[index])))
    .map(([locale]) => locale);
}

const listOf = (text) => (text ?? "").split(",").map((each) => each.trim()).filter(Boolean);
const spaced = (locales) => locales.join(", ");

/** The group a job is, and the ones that come after it in the run. */
function placeOf(groups, number) {
  const at = groups.findIndex((each) => each.group === number);
  if (at < 0) throw new GroupsError(`There is no group ${number} among the ${groups.length}.`);
  return { own: groups[at], after: groups.slice(at + 1) };
}

/** What a group's job writes on the run's page: what went, what Google Play already had, what the quota held back. */
export function groupSummary({ groups, group, sent = [], unchanged = [], notSent = [], quota = false, validateOnly = false }) {
  const { own, after } = placeOf(groups, group);
  const had = unchanged.length > 0 ? [`- **Already on Google Play as drawn, so not sent again:** ${spaced(unchanged)}`] : [];
  if (quota) {
    return [
      `### Google Play's daily save quota is used up (group ${own.group} of ${own.of})`,
      "",
      `Google Play refused to save this group's screenshots ("Daily save quota exceeded"). Nothing is wrong with the images or the run: the app had too many saves today, and Google says neither how many it allows nor when it counts from zero again. The groups after this one are not tried by this attempt of the run, since each would be refused the same way.`,
      "",
      `- **Not sent, group ${own.group} of ${own.of}:** ${spaced(notSent.length > 0 ? notSent : listOf(own.locales))}`,
      ...after.map((each) => `- **Not tried by this attempt, group ${each.group} of ${each.of}:** ${spaced(listOf(each.locales))}`),
      ...had,
      "",
      `A group listed here that went up in an earlier attempt of this run stays sent: its own job says so, and running it again costs no save.`,
      "",
      `**To send them later**, once Google Play takes saves again (nobody has measured when: try the next day): run each of these jobs again with its own re-run button, "send ${own.group} of ${own.of}" first and one at a time. A job sends its group alone, with the images this run drew, for as long as GitHub keeps the run's artifact, and a locale Google Play already holds costs no save. See ${QUOTA_DOC}.`,
    ].join("\n");
  }
  const went = validateOnly ? "Google Play would take, nothing changed" : "Sent";
  const nothing = validateOnly ? "nothing to ask Google Play about" : "nothing, so no save was spent";
  return [`### Google Play screenshots, group ${own.group} of ${own.of}`, "", `- **${went}:** ${sent.length > 0 ? spaced(sent) : nothing}`, ...had].join("\n");
}

/**
 * The warning's one line: for the group Google Play refused (`notSent` given, the whole group when it names no
 * locale), or for one after it, which wasn't tried (`notSent` left out).
 */
export function quotaWarning({ groups, group, notSent }) {
  const { own } = placeOf(groups, group);
  const later = `Run this job again later with its own re-run button (${QUOTA_DOC}).`;
  if (notSent === undefined) return `Google Play's daily save quota was used up by a group before this one, so ${spaced(listOf(own.locales))} were not tried. ${later}`;
  const refused = notSent.length > 0 ? notSent : listOf(own.locales);
  return `Google Play's daily save quota is used up: ${spaced(refused)} not sent, and the groups after this one are not tried. Nothing is wrong with the images. ${later}`;
}

/**
 * Whether a group of this attempt of the run left the quota's marker. A job that can't tell goes on and asks Google
 * Play itself: the worst that costs is a save Google Play refuses, which ends the same way.
 */
export function quotaReached(env = process.env) {
  const marker = env.QUOTA_MARKER;
  if (!marker || !env.GITHUB_REPOSITORY || !env.GITHUB_RUN_ID) return false;
  try {
    const count = execFileSync("gh", ["api", `repos/${env.GITHUB_REPOSITORY}/actions/runs/${env.GITHUB_RUN_ID}/artifacts?name=${marker}`, "--jq", ".total_count"], { encoding: "utf8", env, stdio: ["ignore", "pipe", "pipe"] });
    return Number.parseInt(count, 10) > 0;
  } catch (error) {
    console.error(`Could not ask GitHub whether the quota was used up, so this job tries: ${String(error.stderr || error.message).trim()}`);
    return false;
  }
}

function parseGroups(text) {
  let groups;
  try {
    groups = JSON.parse(text ?? "");
  } catch {
    throw new GroupsError("--groups is the matrix `groups` printed, as JSON.");
  }
  if (!Array.isArray(groups) || groups.some((each) => !Number.isInteger(each?.group) || typeof each?.locales !== "string")) {
    throw new GroupsError("--groups is the matrix `groups` printed, as JSON.");
  }
  return groups;
}

function main(argv) {
  const [command, ...rest] = argv;
  if (command === "groups") {
    const { values, positionals } = parseArgs({ args: rest, options: { size: { type: "string" } }, allowPositionals: true });
    let locales;
    try {
      locales = JSON.parse(positionals[0] ?? "");
    } catch {
      throw new GroupsError('The locales are a JSON array of the stores\' codes: \'["ar","de-DE"]\'.');
    }
    const size = values.size === undefined ? DEFAULT_GROUP_SIZE : Number(values.size);
    console.log(JSON.stringify(groupLocales(locales, size)));
  } else if (command === "changed") {
    console.log(JSON.stringify(changedLocales(JSON.parse(readFileSync(0, "utf8")))));
  } else if (command === "quota-reached") {
    console.log(String(quotaReached()));
  } else if (command === "summary" || command === "warning") {
    const { values } = parseArgs({
      args: rest,
      options: { groups: { type: "string" }, group: { type: "string" }, sent: { type: "string" }, unchanged: { type: "string" }, "not-sent": { type: "string" }, quota: { type: "string" }, "validate-only": { type: "string" } },
    });
    const place = { groups: parseGroups(values.groups), group: Number(values.group) };
    if (command === "warning") {
      console.log(quotaWarning({ ...place, notSent: values["not-sent"] === undefined ? undefined : listOf(values["not-sent"]) }));
    } else {
      console.log(groupSummary({ ...place, sent: listOf(values.sent), unchanged: listOf(values.unchanged), notSent: listOf(values["not-sent"]), quota: values.quota === "reached", validateOnly: values["validate-only"] === "true" }));
    }
  } else {
    throw new GroupsError("Usage: play-screenshot-groups.mjs groups|changed|quota-reached|summary|warning (see the top of the script).");
  }
}

if (process.argv[1] && realpathSync(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try {
    main(process.argv.slice(2));
  } catch (error) {
    if (!(error instanceof GroupsError)) throw error;
    console.error(error.message);
    process.exit(1);
  }
}
