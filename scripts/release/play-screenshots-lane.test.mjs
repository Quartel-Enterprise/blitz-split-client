// The tests of the lane that sends Google Play's screenshots (upload_play_screenshots of fastlane/Fastfile): the
// real lane, run by fastlane, against a stand-in for supply's Google Play client (fake-play-client.rb), so nothing
// leaves the machine:
// node --test scripts/release/play-screenshots-lane.test.mjs
//
// They need the fastlane the lanes are checked with (FASTLANE_VERSION of .github/actions/fastlane/action.yml) on
// the PATH, and are skipped without it, as on the runner of the release-notes check: run them on a Mac that has it
// whenever the lane changes. play-screenshot-groups.test.mjs holds the lane's text to the workflow everywhere.
//
// Each runs a copy of the Fastfile and of the script it asks in a folder made for the test, where the staged images
// are files of a few bytes: the lane only reads their SHA-256.

import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { afterEach, beforeEach, describe, test } from "node:test";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(HERE, "../..");
const [, PINNED] = readFileSync(join(REPO_ROOT, ".github/actions/fastlane/action.yml"), "utf8").match(/FASTLANE_VERSION: '([\d.]+)'/);
const installed = spawnSync("fastlane", ["--version"], { encoding: "utf8", env: { ...process.env, FASTLANE_SKIP_UPDATE_CHECK: "1" } });
const skip = installed.status === 0 && installed.stdout.includes(`fastlane ${PINNED}`) ? false : `needs fastlane ${PINNED} on the PATH`;
const QUOTA = "Google Api Error: Invalid request - Daily save quota exceeded.";
const SHELVES = { phoneScreenshots: ["01.png", "02.png"], tenInchScreenshots: ["01.png"] };

describe("the lane that sends Google Play's screenshots", { skip }, () => {
  let root;
  const staged = (locale, shelf, file) => join(root, "fastlane/metadata/android", locale, "images", shelf, file);

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), "play-screenshots-lane-"));
    cpSync(join(REPO_ROOT, "fastlane/Fastfile"), join(root, "fastlane/Fastfile"), { recursive: true });
    cpSync(join(HERE, "play-screenshot-groups.mjs"), join(root, "scripts/release/play-screenshot-groups.mjs"), { recursive: true });
    for (const locale of ["de-DE", "en-US", "pt-BR"]) {
      for (const [shelf, files] of Object.entries(SHELVES)) {
        mkdirSync(dirname(staged(locale, shelf, files[0])), { recursive: true });
        for (const file of files) writeFileSync(staged(locale, shelf, file), `${locale} ${shelf} ${file}`);
      }
    }
    writeFileSync(join(root, "key.json"), "{}");
  });

  afterEach(() => rmSync(root, { recursive: true, force: true }));

  /** What Google Play holds when it has these locales' staged images as they are. */
  const holding = (...locales) =>
    Object.fromEntries(locales.map((locale) => [locale, Object.fromEntries(Object.entries(SHELVES).map(([shelf, files]) => [shelf, files.map((file) => createHash("sha256").update(readFileSync(staged(locale, shelf, file))).digest("hex"))]))]));

  /** Runs the lane with what Google Play `held`; answers its exit code, what reached Google Play and the step's outputs. */
  const lane = (options, { held = {}, commitError = "", output = true } = {}) => {
    writeFileSync(join(root, "held.json"), JSON.stringify(held));
    for (const file of ["play.log", "output.txt"]) writeFileSync(join(root, file), "");
    const { status, stdout, stderr } = spawnSync("fastlane", ["upload_screenshots", "platform:android", ...options], {
      cwd: root,
      encoding: "utf8",
      env: {
        ...process.env,
        LC_ALL: "en_US.UTF-8",
        LANG: "en_US.UTF-8",
        FASTLANE_SKIP_UPDATE_CHECK: "1",
        FASTLANE_OPT_OUT_USAGE: "1",
        FASTLANE_DISABLE_COLORS: "1",
        RUBYOPT: `-r${join(HERE, "fake-play-client.rb")}`,
        FAKE_PLAY_HELD: join(root, "held.json"),
        FAKE_PLAY_LOG: join(root, "play.log"),
        FAKE_PLAY_COMMIT_ERROR: commitError,
        PLAY_STORE_JSON_KEY_PATH: join(root, "key.json"),
        GITHUB_OUTPUT: output ? join(root, "output.txt") : "",
        VALIDATE_ONLY: "",
      },
    });
    const lines = (file) => readFileSync(join(root, file), "utf8").split("\n").filter(Boolean);
    return { status, said: stdout + stderr, play: lines("play.log"), outputs: Object.fromEntries(lines("output.txt").map((line) => line.split("="))) };
  };
  const uploaded = (play) => [...new Set(play.filter((line) => line.startsWith("upload ")).map((line) => line.split(" ")[1].split("/")[0]))].sort();
  const count = (play, call) => play.filter((line) => line === call).length;

  test("saves nothing for a group Google Play already holds as staged", () => {
    const { status, play, outputs } = lane(["locales:en-US,pt-BR"], { held: holding("en-US", "pt-BR") });
    assert.equal(status, 0);
    // The one edit is the one it read through, deleted.
    assert.deepEqual(play, ["begin_edit", "abort_edit"]);
    assert.deepEqual(outputs, { sent: "", unchanged: "en-US,pt-BR" });
  });

  test("sends only the locales that changed, through one edit and one save", () => {
    const { status, play, outputs } = lane(["locales:de-DE,en-US,pt-BR"], { held: holding("en-US") });
    assert.equal(status, 0);
    assert.deepEqual(uploaded(play), ["de-DE", "pt-BR"]);
    assert.equal(count(play, "commit"), 1);
    assert.equal(play.at(-1), "commit");
    assert.deepEqual(outputs, { sent: "de-DE,pt-BR", unchanged: "en-US" });
  });

  test("sends everything staged through one edit when no locale is named, and one locale alone with locale:", () => {
    const everything = lane([]);
    assert.deepEqual(uploaded(everything.play), ["de-DE", "en-US", "pt-BR"]);
    assert.equal(count(everything.play, "commit"), 1);
    const one = lane(["locale:pt-BR"]);
    assert.deepEqual(uploaded(one.play), ["pt-BR"]);
    assert.deepEqual(one.outputs, { sent: "pt-BR", unchanged: "" });
  });

  test("ends well when Google Play refuses the save for its daily quota, for the job that said it reads the outputs", () => {
    const { status, said, play, outputs } = lane(["locales:de-DE,en-US,pt-BR", "save_quota_is_no_failure:true"], { held: holding("en-US"), commitError: QUOTA });
    assert.equal(status, 0);
    assert.equal(count(play, "commit"), 1);
    assert.deepEqual(outputs, { save_quota: "reached", not_sent: "de-DE,pt-BR", unchanged: "en-US" });
    assert.match(said, /Google Play's daily save quota is used up: de-DE, pt-BR not sent/);
  });

  test("stops on the quota, saying what it is, for a caller that reads no output", () => {
    for (const run of [lane(["locales:pt-BR"], { commitError: QUOTA }), lane(["locales:pt-BR", "save_quota_is_no_failure:true"], { commitError: QUOTA, output: false })]) {
      assert.notEqual(run.status, 0);
      assert.match(run.said, /Google Play's daily save quota is used up: pt-BR not sent/);
    }
  });

  test("still fails on any other refusal, whatever the caller said of the quota", () => {
    const { status, said, outputs } = lane(["locales:pt-BR", "save_quota_is_no_failure:true"], { commitError: "Google Api Error: Invalid request - Image too small." });
    assert.notEqual(status, 0);
    assert.match(said, /Image too small/);
    assert.deepEqual(outputs, {});
  });

  test("asks Google Play without saving with validate_only", () => {
    const { status, play, outputs } = lane(["locales:en-US,pt-BR", "validate_only:true"], { held: holding("en-US") });
    assert.equal(status, 0);
    assert.deepEqual(uploaded(play), ["pt-BR"]);
    assert.equal(count(play, "validate"), 1);
    assert.equal(count(play, "commit"), 0);
    assert.deepEqual(outputs, { sent: "pt-BR", unchanged: "en-US" });
  });

  test("refuses a group that names no locale, or one that isn't staged, before it reaches Google Play", () => {
    for (const options of [["locales:"], ["locales:en-US,fr-FR"], ["locales:en-US", "locale:pt-BR"]]) {
      const { status, play } = lane(options);
      assert.notEqual(status, 0, options.join(" "));
      assert.deepEqual(play, [], options.join(" "));
    }
    assert.ok(existsSync(staged("en-US", "phoneScreenshots", "01.png")));
  });
});
