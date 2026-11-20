// The tests of play-screenshot-groups.mjs, what the jobs that send Google Play's screenshots decide, and of what
// holds it to the workflow, the action and the lane that use it:
// node --test scripts/release/play-screenshot-groups.test.mjs
//
// The GitHub CLI is a stand-in that writes down what it was asked and answers what the test tells it to.

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { chmodSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { afterEach, beforeEach, describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import { PLAY_LOCALES as LISTING_PLAY_LOCALES } from "./store-listing.mjs";
import { DEFAULT_GROUP_SIZE, GroupsError, QUOTA_DOC, changedLocales, groupLocales, groupSummary, quotaReached, quotaWarning } from "./play-screenshot-groups.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(HERE, "../..");
const SCRIPT = join(HERE, "play-screenshot-groups.mjs");
// Google Play's locales, sorted as a run's draw job lists them, however many the listing has.
const PLAY_LOCALES = Object.values(LISTING_PLAY_LOCALES).flat().sort();
// 27 codes, the number the listing had when the groups were sized: a language added later changes no test here.
const TWENTY_SEVEN = Array.from({ length: 27 }, (_, index) => `l${index + 1}`);
const cli = (args, options = {}) => spawnSync("node", [SCRIPT, ...args], { encoding: "utf8", ...options });

describe("the groups Google Play's screenshots go up in", () => {
  const sizes = (groups) => groups.map((each) => each.locales.split(",").length);

  test("are five for 27 locales, where one job per locale was 27 saves", () => {
    assert.equal(DEFAULT_GROUP_SIZE, 6);
    assert.deepEqual(sizes(groupLocales(TWENTY_SEVEN)), [6, 6, 5, 5, 5]);
  });

  test("hold every locale once, in the order given", () => {
    for (const size of [1, 4, 5, 6, 7, PLAY_LOCALES.length - 1, PLAY_LOCALES.length, 100]) {
      assert.deepEqual(groupLocales(PLAY_LOCALES, size).flatMap((each) => each.locales.split(",")), PLAY_LOCALES, `size ${size}`);
    }
  });

  test("are as few as the size allows, none larger than it, and of sizes one apart at most", () => {
    for (let size = 1; size <= PLAY_LOCALES.length + 3; size += 1) {
      const counts = sizes(groupLocales(PLAY_LOCALES, size));
      assert.equal(counts.length, Math.ceil(PLAY_LOCALES.length / size), `size ${size}`);
      assert.ok(Math.max(...counts) <= size, `size ${size}`);
      assert.ok(Math.max(...counts) - Math.min(...counts) <= 1, `size ${size}`);
    }
  });

  test("are one job per locale with a size of one, and one job with a size that takes them all", () => {
    assert.equal(groupLocales(PLAY_LOCALES, 1).length, PLAY_LOCALES.length);
    assert.deepEqual(groupLocales(["en-US", "pt-BR"], 6), [{ group: 1, of: 1, locales: "en-US,pt-BR", name: "1 of 1: en-US, pt-BR" }]);
  });

  test("say which they are and what they hold, for the job's name and the lane's option", () => {
    assert.deepEqual(groupLocales(["ar", "de-DE", "en-US"], 2), [
      { group: 1, of: 2, locales: "ar,de-DE", name: "1 of 2: ar, de-DE" },
      { group: 2, of: 2, locales: "en-US", name: "2 of 2: en-US" },
    ]);
  });

  test("refuse no locale at all, one listed twice, and a size that is no whole number from 1 up", () => {
    assert.throws(() => groupLocales([]), GroupsError);
    assert.throws(() => groupLocales("en-US"), GroupsError);
    assert.throws(() => groupLocales(["en-US", "en-US"]), /listed twice/);
    for (const size of [0, -1, 2.5, Number.NaN]) assert.throws(() => groupLocales(["en-US"], size), /whole number/);
  });

  test("refuse what is not a store's code, since a locale becomes a word of the lane's command line", () => {
    for (const odd of ["en US", "en-US,pt-BR", "", "en-US validate_only:false", "$(id)", 7]) assert.throws(() => groupLocales([odd]), /Not a store's locale/);
  });

  test("are printed as one line of JSON, as a job's matrix reads it from a step's output", () => {
    const { stdout, status } = cli(["groups", JSON.stringify(PLAY_LOCALES)]);
    assert.equal(status, 0);
    assert.equal(stdout.trim().split("\n").length, 1);
    assert.deepEqual(JSON.parse(stdout), groupLocales(PLAY_LOCALES));
    assert.equal(JSON.parse(cli(["groups", JSON.stringify(TWENTY_SEVEN), "--size=9"]).stdout).length, 3);
  });

  test("stop the step, printing no matrix, when the list or the size can't be read", () => {
    for (const args of [["groups", ""], ["groups", "not json"], ["groups", "[]"], ["groups", '["en-US"]', "--size=0"], ["groups", '["en-US"]', "--size="], ["nothing"]]) {
      const { stdout, stderr, status } = cli(args);
      assert.equal(status, 1, args.join(" "));
      assert.equal(stdout, "");
      assert.notEqual(stderr, "");
    }
  });
});

describe("the locales of a group that are sent", () => {
  const shelf = (staged, held) => ({ staged, held });

  test("leave out a locale whose every staged shelf is what Google Play holds, in the same order", () => {
    assert.deepEqual(changedLocales({ "en-US": { phoneScreenshots: shelf(["a", "b"], ["a", "b"]), tenInchScreenshots: shelf(["c"], ["c"]) } }), []);
  });

  test("take a locale with one image that changed, on any shelf", () => {
    const shelves = {
      "de-DE": { phoneScreenshots: shelf(["a", "b"], ["a", "b"]) },
      "en-US": { phoneScreenshots: shelf(["a", "b"], ["a", "b"]), sevenInchScreenshots: shelf(["c", "d"], ["c", "x"]) },
      "pt-BR": { phoneScreenshots: shelf(["a", "b"], ["a", "b"]) },
    };
    assert.deepEqual(changedLocales(shelves), ["en-US"]);
  });

  test("take a locale whose images are the same in another order, since the shelf shows them in order", () => {
    assert.deepEqual(changedLocales({ "en-US": { phoneScreenshots: shelf(["a", "b"], ["b", "a"]) } }), ["en-US"]);
  });

  test("take a locale Google Play holds more images of, or fewer, or none", () => {
    assert.deepEqual(changedLocales({ more: { phoneScreenshots: shelf(["a"], ["a", "b"]) }, fewer: { phoneScreenshots: shelf(["a", "b"], ["a"]) }, none: { phoneScreenshots: shelf(["a"], []) } }), ["more", "fewer", "none"]);
  });

  test("take a locale with a shelf Google Play couldn't be asked about", () => {
    assert.deepEqual(changedLocales({ "en-US": { phoneScreenshots: shelf(["a"], null) } }), ["en-US"]);
  });

  test("are none for a group Google Play holds whole, which then saves nothing", () => {
    const { stdout, status } = cli(["changed"], { input: JSON.stringify({ ar: { phoneScreenshots: shelf(["a"], ["a"]) }, "de-DE": { phoneScreenshots: shelf(["b"], ["b"]) } }) });
    assert.equal(status, 0);
    assert.deepEqual(JSON.parse(stdout), []);
  });
});

describe("Google Play's daily save quota", () => {
  const groups = groupLocales(["ar", "de-DE", "en-US", "fr-FR", "ja-JP", "pt-BR"], 2);
  let root;
  let env;

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), "play-screenshot-groups-"));
    mkdirSync(join(root, "bin"));
    // Answers what GH_ANSWER holds, or fails when GH_FAILS is set, and writes down what it was asked.
    writeFileSync(join(root, "bin/gh"), `#!/usr/bin/env bash\necho "$*" >> "${join(root, "gh.log")}"\nif [ -n "\${GH_FAILS:-}" ]; then echo "HTTP 403: Resource not accessible by integration" >&2; exit 1; fi\necho "\${GH_ANSWER:-0}"\n`);
    chmodSync(join(root, "bin/gh"), 0o755);
    env = { ...process.env, PATH: `${join(root, "bin")}:${process.env.PATH}`, GH_TOKEN: "token", GITHUB_REPOSITORY: "owner/repo", GITHUB_RUN_ID: "42", QUOTA_MARKER: "play-save-quota-reached-2" };
  });

  afterEach(() => rmSync(root, { recursive: true, force: true }));

  const asked = () => (existsSync(join(root, "gh.log")) ? readFileSync(join(root, "gh.log"), "utf8").trim().split("\n") : []);

  test("stops a group once one before it, in the same attempt of the run, was refused a save", () => {
    assert.equal(quotaReached({ ...env, GH_ANSWER: "1" }), true);
    assert.deepEqual(asked(), ["api repos/owner/repo/actions/runs/42/artifacts?name=play-save-quota-reached-2 --jq .total_count"]);
  });

  test("lets a group go when no group left the marker", () => {
    assert.equal(quotaReached({ ...env, GH_ANSWER: "0" }), false);
    assert.equal(cli(["quota-reached"], { env: { ...env, GH_ANSWER: "0" } }).stdout, "false\n");
    assert.equal(cli(["quota-reached"], { env: { ...env, GH_ANSWER: "1" } }).stdout, "true\n");
  });

  test("lets a group go when GitHub can't say, since Google Play then says it itself", () => {
    const { stdout, stderr, status } = cli(["quota-reached"], { env: { ...env, GH_FAILS: "1" } });
    assert.equal(status, 0);
    assert.equal(stdout, "false\n");
    assert.match(stderr, /this job tries: HTTP 403/);
  });

  test("lets a group go, without asking, when the job names no marker or no run", () => {
    for (const missing of ["QUOTA_MARKER", "GITHUB_REPOSITORY", "GITHUB_RUN_ID"]) assert.equal(quotaReached({ ...env, [missing]: "", GH_ANSWER: "1" }), false);
    assert.deepEqual(asked(), []);
  });

  test("names, on the run's page, the locales not sent, the groups not tried and how to send them later", () => {
    const summary = groupSummary({ groups, group: 2, notSent: ["en-US"], unchanged: ["fr-FR"], quota: true });
    assert.match(summary, /^### Google Play's daily save quota is used up \(group 2 of 3\)\n/);
    assert.match(summary, /Nothing is wrong with the images or the run/);
    assert.match(summary, /\n- \*\*Not sent, group 2 of 3:\*\* en-US\n/);
    assert.match(summary, /\n- \*\*Not tried by this attempt, group 3 of 3:\*\* ja-JP, pt-BR\n/);
    // A job run again alone is an attempt of its own: a group after it may have gone in an earlier one.
    assert.match(summary, /went up in an earlier attempt of this run stays sent/);
    assert.match(summary, /\n- \*\*Already on Google Play as drawn, so not sent again:\*\* fr-FR\n/);
    assert.match(summary, /own re-run button, "send 2 of 3" first and one at a time/);
    assert.ok(summary.includes(QUOTA_DOC));
    // The group before it landed: nothing of it is to send again.
    assert.doesNotMatch(summary, /\bar\b|de-DE|group 1 of 3/);
  });

  test("names no group as not tried when the last one is the one refused", () => {
    assert.doesNotMatch(groupSummary({ groups, group: 3, notSent: ["ja-JP", "pt-BR"], quota: true }), /Not tried/);
  });

  test("is one warning for the group refused, and one for each group after it, with its locales", () => {
    const refused = quotaWarning({ groups, group: 2, notSent: ["en-US"] });
    assert.match(refused, /^Google Play's daily save quota is used up: en-US not sent, and the groups after this one are not tried\./);
    const untried = quotaWarning({ groups, group: 3 });
    assert.match(untried, /used up by a group before this one, so ja-JP, pt-BR were not tried\./);
    for (const warning of [refused, untried]) {
      assert.match(warning, /Run this job again later with its own re-run button/);
      // One line: a workflow command takes its message up to the end of the line.
      assert.doesNotMatch(warning, /\n/);
    }
  });

  test("names the whole group when the lane named no locale of the one refused", () => {
    assert.match(quotaWarning({ groups, group: 2, notSent: [] }), /^Google Play's daily save quota is used up: en-US, fr-FR not sent,/);
    assert.match(groupSummary({ groups, group: 2, notSent: [], quota: true }), /\n- \*\*Not sent, group 2 of 3:\*\* en-US, fr-FR\n/);
    assert.match(cli(["warning", `--groups=${JSON.stringify(groups)}`, "--group=2", "--not-sent="]).stdout, /used up: en-US, fr-FR not sent/);
  });

  test("is told apart from a group that simply went, or had nothing to send", () => {
    assert.equal(groupSummary({ groups, group: 1, sent: ["ar"], unchanged: ["de-DE"] }), "### Google Play screenshots, group 1 of 3\n\n- **Sent:** ar\n- **Already on Google Play as drawn, so not sent again:** de-DE");
    assert.equal(groupSummary({ groups, group: 1, unchanged: ["ar", "de-DE"] }), "### Google Play screenshots, group 1 of 3\n\n- **Sent:** nothing, so no save was spent\n- **Already on Google Play as drawn, so not sent again:** ar, de-DE");
    assert.match(groupSummary({ groups, group: 1, sent: ["ar", "de-DE"], validateOnly: true }), /\n- \*\*Google Play would take, nothing changed:\*\* ar, de-DE$/);
  });

  test("is written from what the lane's step said, an output left empty too", () => {
    const matrix = JSON.stringify(groups);
    const went = cli(["summary", `--groups=${matrix}`, "--group=1", "--sent=ar,de-DE", "--unchanged=", "--not-sent=", "--quota=", "--validate-only=false"]);
    assert.equal(went.status, 0);
    assert.equal(went.stdout, "### Google Play screenshots, group 1 of 3\n\n- **Sent:** ar, de-DE\n");
    const refused = cli(["summary", `--groups=${matrix}`, "--group=1", "--sent=", "--unchanged=", "--not-sent=ar,de-DE", "--quota=reached", "--validate-only=false"]);
    assert.match(refused.stdout, /\n- \*\*Not sent, group 1 of 3:\*\* ar, de-DE\n- \*\*Not tried by this attempt, group 2 of 3:\*\* en-US, fr-FR\n- \*\*Not tried by this attempt, group 3 of 3:\*\* ja-JP, pt-BR\n/);
    assert.equal(cli(["warning", `--groups=${matrix}`, "--group=2"]).stdout, `${quotaWarning({ groups, group: 2 })}\n`);
    assert.equal(cli(["warning", `--groups=${matrix}`, "--group=1", "--not-sent=ar"]).stdout, `${quotaWarning({ groups, group: 1, notSent: ["ar"] })}\n`);
    assert.equal(cli(["summary", "--groups=nothing", "--group=1"]).status, 1);
    assert.equal(cli(["summary", `--groups=${matrix}`, "--group=9"]).status, 1);
  });
});

// The script decides, and three files act on it: the called workflow, the action that runs a Play lane and the lane.
// A name changed in one of them alone would only show in a run that reaches Google Play, on a day its quota ran out.
describe("the jobs that send Google Play's screenshots", () => {
  const read = (path) => readFileSync(join(REPO_ROOT, path), "utf8");
  const upload = read(".github/workflows/play-store-screenshots-upload.yml");
  const release = read(".github/workflows/release-production.yml");
  const alone = read(".github/workflows/play-store-screenshots.yml");
  const action = read(".github/actions/play-store-lane/action.yml");
  const fastfile = read("fastlane/Fastfile");
  /** A job of a workflow, from its key to the next job's. */
  const job = (workflow, name) => workflow.match(new RegExp(`\\n {2}${name}:\\n[\\s\\S]*?(?=\\n {2}[\\w-]+:\\n|$)`))?.[0] ?? assert.fail(`no job ${name}`);
  /** A job's steps, each from its name on. */
  const steps = (source) => source.split(/\n {6}- name: /).slice(1);

  test("are a few locales each, grouped by the script from the size the caller names", () => {
    assert.match(upload, /node scripts\/release\/play-screenshot-groups\.mjs groups "\$LOCALES" --size="\$GROUP_SIZE"/);
    assert.match(upload, /group_size:\n(?:\s+\w+: .*\n)*?\s+default: 6\n/);
    assert.match(job(upload, "send"), /matrix:\n\s+group: \$\{\{ fromJSON\(needs\.groups\.outputs\.groups\) \}\}\n/);
    assert.match(job(upload, "send"), /max-parallel: 1\n/);
    assert.match(job(upload, "send"), /fail-fast: false\n/);
  });

  test("hand a group's locales to the lane, which sends them through one edit", () => {
    assert.match(upload, /upload_screenshots platform:android locales:\$\{\{ matrix\.group\.locales \}\}\n/);
    assert.match(fastfile, /play_locales = options\[:locales\]\.to_s\.split\(","\)/);
    // An empty list must not read as no locale named, which sends everything staged.
    assert.match(fastfile, /UI\.user_error!\("locales: names no locale"\) if play_locales\.empty\?/);
    assert.equal(fastfile.match(/skip_upload_screenshots: false/g)?.length, 1);
  });

  test("ask the script which locales changed before any edit is opened to write", () => {
    assert.match(fastfile, /play-screenshot-groups\.mjs", __dir__\)\n\s+changed, status = Open3\.capture2\("node", script, "changed"/);
    const sending = fastfile.slice(fastfile.indexOf("def upload_play_screenshots"), fastfile.indexOf("def play_locales_to_send"));
    assert.ok(sending.indexOf("play_locales_to_send(") < sending.indexOf("upload_to_play_store("));
    assert.match(sending, /if changed\.empty\?\n[\s\S]*?\n {4}return\n {2}end\n/);
  });

  test("read the refusal Google Play words as its daily save quota, and no other error, as sent later", () => {
    assert.match(fastfile, /PLAY_SAVE_QUOTA_MESSAGE = "Daily save quota exceeded"\.freeze/);
    assert.match(fastfile, /raise unless e\.message\.include\?\(PLAY_SAVE_QUOTA_MESSAGE\)\n\n\s+return play_save_quota_reached\(changed, unchanged, quota_is_no_failure\)/);
  });

  test("end well on the quota only because the job says it reads the lane's outputs", () => {
    assert.match(upload, /validate_only:\$\{\{ inputs\.validate_only \}\} save_quota_is_no_failure:true\n/);
    assert.match(fastfile, /quota_is_no_failure = options\[:save_quota_is_no_failure\]\.to_s == "true"/);
    assert.match(fastfile, /UI\.user_error!\(message\) unless is_no_failure && told\n/);
  });

  test("pass what the lane says through the action to the job, under the names each reads", () => {
    const written = [...fastfile.matchAll(/github_output\(([^)]*)\)/g)].flatMap((match) => [...match[1].matchAll(/(\w+): /g)].map((each) => each[1]));
    assert.deepEqual([...new Set(written)].sort(), ["not_sent", "save_quota", "sent", "unchanged"]);
    const passed = Object.fromEntries([...action.matchAll(/\n {2}([\w-]+):\n\s+description: .*\n\s+value: \$\{\{ steps\.lane\.outputs\.(\w+) \}\}/g)].map((match) => [match[1], match[2]]));
    assert.deepEqual(passed, { sent: "sent", unchanged: "unchanged", "not-sent": "not_sent", "save-quota": "save_quota" });
    assert.match(action, /- name: Run the lane\n\s+id: lane\n/);
    for (const output of Object.keys(passed)) assert.ok(upload.includes(`\${{ steps.send.outputs.${output} }}`), output);
    assert.match(upload, /if \[ "\$SAVE_QUOTA" = reached \]/);
    assert.match(fastfile, /save_quota: "reached"/);
  });

  test("leave the marker a later group looks for, named after the attempt so a job run again another day goes", () => {
    const send = job(upload, "send");
    assert.match(send, /QUOTA_MARKER: play-save-quota-reached-\$\{\{ github\.run_attempt \}\}\n/);
    assert.match(send, /if: \$\{\{ steps\.send\.outputs\.save-quota == 'reached' \}\}\n\s+uses: actions\/upload-artifact@v\d+\n\s+with:\n\s+name: \$\{\{ env\.QUOTA_MARKER \}\}\n/);
    // A group that couldn't see the marker and was refused too leaves it again: without overwrite that upload fails.
    assert.match(send.slice(send.indexOf("name: ${{ env.QUOTA_MARKER }}")), /^(?:.*\n){1,5}\s+overwrite: true\n/);
    assert.match(send, /script=scripts\/release\/play-screenshot-groups\.mjs\n\s+reached="\$\(node "\$script" quota-reached\)"/);
  });

  test("stop before anything reaches Google Play once the quota is used up", () => {
    const all = steps(job(upload, "send"));
    const check = all.findIndex((step) => step.includes("id: quota\n"));
    assert.ok(check >= 0);
    const after = all.slice(check + 1);
    assert.ok(after.length >= 4);
    for (const step of after) {
      const condition = step.match(/\n\s+if: (.+)\n/)?.[1] ?? "";
      assert.ok(condition.includes("steps.quota.outputs.reached != 'true'") || condition.includes("steps.send.outputs.save-quota == 'reached'"), step.split("\n")[0]);
    }
    // And nothing before the check talks to Google Play.
    for (const step of all.slice(0, check)) assert.doesNotMatch(step, /play-store-lane|PLAY_STORE/);
  });

  test("can read the run's artifacts, whichever workflow calls them", () => {
    for (const [caller, name] of [[release, "android-screenshots"], [alone, "send"]]) {
      assert.match(job(caller, name), /permissions:\n\s+contents: read\n(?:\s+#.*\n)*\s+actions: read\n/);
      assert.match(job(caller, name), /uses: \.\/\.github\/workflows\/play-store-screenshots-upload\.yml\n/);
    }
  });

  test("come after a release's bundle, so the bundle is never the save the quota refuses", () => {
    const screenshots = job(release, "android-screenshots");
    assert.match(screenshots, /\n {4}needs: \[plan, android-upload, wear-upload, android-screenshots-render\]\n/);
    // No status function: a failed or skipped upload skips the screenshots.
    assert.doesNotMatch(screenshots.match(/\n {4}if: (.+)\n/)[1], /always\(\)|cancelled\(\)|failure\(\)/);
    assert.match(job(release, "android-upload"), /play_upload aab:/);
    assert.match(job(release, "wear-upload"), /play_upload aab:wear-aab\/\*\.aab form_factor:wear /);
  });
});
