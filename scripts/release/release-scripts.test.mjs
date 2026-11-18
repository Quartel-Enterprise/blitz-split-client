// The tests of the release workflows' shell scripts, suggest-version.sh, merge-back.sh,
// staged-screenshot-locales.sh, app-store-capture-slots.sh, app-store-screenshots-plan.sh and release-production.sh:
// node --test scripts/release/release-scripts.test.mjs
//
// Each runs a copy of the scripts inside a repository made for the test, with a remote of its own, since they read
// and write the repository they are in. The GitHub CLI is a stand-in that writes down what it was asked.

import assert from "node:assert/strict";
import { execFileSync, spawnSync } from "node:child_process";
import { chmodSync, cpSync, existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { afterEach, beforeEach, describe, test } from "node:test";
import { fileURLToPath } from "node:url";

const HERE = dirname(fileURLToPath(import.meta.url));
const SCRIPTS = ["suggest-version.sh", "merge-back.sh", "bump-version.sh", "release-notes.mjs"];
const BRANCH = "release/merge-back";

let root;
let repo;
const git = (...args) => execFileSync("git", args, { cwd: repo, encoding: "utf8" }).trim();
const fragment = (name) => `release-notes/unreleased/${name}.json`;
const commit = (message, files) => {
  for (const [path, content] of Object.entries(files)) {
    mkdirSync(dirname(join(repo, path)), { recursive: true });
    writeFileSync(join(repo, path), content);
  }
  git("add", "-A");
  git("commit", "-q", "-m", message);
};
const versionFile = (name, code) => ({ "version.properties": `# The app's version\nversionName=${name}\nversionCode=${code}\n` });
/** What a file holds on a branch of the remote, or null when the remote doesn't have the branch or the file. */
const onRemote = (branch, path) => {
  try {
    return execFileSync("git", ["--git-dir", join(root, "origin.git"), "show", `${branch}:${path}`], { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  } catch {
    return null;
  }
};
const ghCalls = () => (existsSync(join(root, "gh.log")) ? readFileSync(join(root, "gh.log"), "utf8").trim().split("\n") : []);
const run = (script, args, env = {}) =>
  execFileSync(join(repo, "scripts/release", script), args, {
    cwd: repo,
    encoding: "utf8",
    env: { ...process.env, PATH: `${join(root, "bin")}:${process.env.PATH}`, GH_TOKEN: "", GITHUB_TOKEN: "", GITHUB_OUTPUT: "", GITHUB_STEP_SUMMARY: "", ...env },
  });
const mergeBack = (args, env = {}) => run("merge-back.sh", args, { GH_TOKEN: "token", BRANCH, MERGE_RETRY_SECONDS: "0", ...env });

beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), "release-scripts-"));
  repo = join(root, "repo");
  mkdirSync(join(root, "bin"));
  // Answers `gh pr list` with the pull request GH_OPEN_PR names, and takes everything else.
  writeFileSync(join(root, "bin/gh"), `#!/usr/bin/env bash\necho "$*" >> "${join(root, "gh.log")}"\nif [ "$1 $2" = "pr list" ]; then echo "\${GH_OPEN_PR:-}"; fi\n`);
  chmodSync(join(root, "bin/gh"), 0o755);
  execFileSync("git", ["init", "-q", "--bare", "-b", "main", join(root, "origin.git")]);
  execFileSync("git", ["init", "-q", "-b", "main", repo]);
  git("config", "user.name", "Test");
  git("config", "user.email", "test@example.com");
  git("config", "commit.gpgsign", "false");
  git("config", "tag.gpgsign", "false");
  git("remote", "add", "origin", join(root, "origin.git"));
  for (const script of SCRIPTS) cpSync(join(HERE, script), join(repo, "scripts/release", script), { recursive: true });
  commit("the scripts", versionFile("3.0.0", 83));
});

afterEach(() => rmSync(root, { recursive: true, force: true }));

describe("the version of a release", () => {
  const outputsOf = (args) => {
    const output = join(root, "output");
    writeFileSync(output, "");
    run("suggest-version.sh", args, { GITHUB_OUTPUT: output });
    return Object.fromEntries(readFileSync(output, "utf8").trim().split("\n").map((line) => line.split("=")));
  };

  test("a beta is one past the version's last beta, with the next versionCode", () => {
    git("tag", "3.0.0-beta_1");
    git("tag", "3.0.0-beta_2");
    assert.deepEqual(outputsOf([]), { version: "3.0.0", display_version: "3.0.0-beta_3", version_code: "84", branch: BRANCH });
  });

  test("a stable version is the version as it is, whatever betas it had", () => {
    git("tag", "3.0.0-beta_1");
    assert.deepEqual(outputsOf(["--stable"]), { version: "3.0.0", display_version: "3.0.0", version_code: "84", branch: BRANCH });
  });

  test("a stable version after one is the one the release notes lead to", () => {
    git("tag", "3.0.0");
    assert.equal(outputsOf(["--stable"]).display_version, "3.0.1");
  });

  test("a fragment the release notes can't read stops the release", () => {
    git("tag", "3.0.0");
    commit("export", { [fragment("export")]: JSON.stringify({ notes: [{ kind: "fixed", platforms: ["all"], text: { en: "Fixed" } }] }) });
    assert.throws(() => outputsOf(["--stable"]), /no text in pt/);
  });

  test("a stable version typed again says in the plan that it is already tagged", () => {
    const summary = join(root, "summary");
    const planOf = (args) => {
      writeFileSync(summary, "");
      run("suggest-version.sh", args, { GITHUB_STEP_SUMMARY: summary });
      return readFileSync(summary, "utf8");
    };
    assert.doesNotMatch(planOf(["--stable", "3.0.0"]), /already tagged/);
    git("tag", "3.0.0");
    assert.match(planOf(["--stable", "3.0.0"]), /`3\.0\.0` is already tagged/);
  });

  test("the versionCode is one past the highest build number a release recorded, a stable one's too", () => {
    const releases = join(root, "releases.json");
    writeFileSync(releases, JSON.stringify([{ tag: "3.0.0-beta_1", body: "Build number: 84\r\nSent to testers: android", prerelease: true }, { tag: "3.0.0", body: "Build number: 90\n\n## What's new", prerelease: false }, { tag: "2.4.0", body: null, prerelease: false }]));
    const outputs = (args) => {
      const output = join(root, "output");
      writeFileSync(output, "");
      run("suggest-version.sh", args, { GITHUB_OUTPUT: output, RELEASES_FILE: releases });
      return readFileSync(output, "utf8");
    };
    assert.match(outputs([]), /version_code=91\n/);
    assert.match(outputs(["--stable"]), /version_code=91\n/);
  });

  test("the versionCode is past the one a merge-back that never reached main still holds on its branch", () => {
    git("push", "-q", "origin", "main");
    git("checkout", "-q", "-b", BRANCH);
    commit("a merge-back left open", versionFile("3.0.0", 95));
    git("push", "-q", "origin", BRANCH);
    git("checkout", "-q", "main");
    git("branch", "-q", "-D", BRANCH);
    assert.equal(outputsOf(["--stable"]).version_code, "96");
    // Merged since: its branch is gone from the remote, and the copy fetched before no longer counts.
    git("push", "-q", "origin", "--delete", BRANCH);
    assert.equal(outputsOf(["--stable"]).version_code, "84");
  });

  test("a version that isn't X.Y.Z is refused", () => {
    assert.throws(() => outputsOf(["--stable", "3.0"]), /must be in X\.Y\.Z format/);
    assert.throws(() => outputsOf(["3.0.0-beta_1"]), /must be in X\.Y\.Z format/);
  });
});

describe("the merge-back", () => {
  beforeEach(() => git("push", "-q", "origin", "main"));

  test("of a beta writes the versionCode it consumed and leaves the fragments", () => {
    commit("export", { [fragment("export")]: "{}" });
    git("push", "-q", "origin", "main");
    mergeBack(["3.0.0", "84", "3.0.0-beta_3"], { ACTOR: "someone" });
    assert.match(onRemote(BRANCH, "version.properties"), /versionName=3\.0\.0\nversionCode=84\n/);
    assert.equal(onRemote(BRANCH, fragment("export")), "{}");
    const calls = ghCalls();
    assert.ok(calls.some((call) => call.startsWith(`pr create --base main --head ${BRANCH} --title chore: merge back 3.0.0-beta_3 into main`)));
    assert.ok(calls.includes(`pr edit ${BRANCH} --add-assignee someone`));
    assert.equal(calls.at(-1), `pr merge ${BRANCH} --squash --delete-branch`);
  });

  test("raises the versionName to the one the release used, and never lowers it", () => {
    mergeBack(["3.1.0", "84", "3.1.0-beta_1"]);
    assert.match(onRemote(BRANCH, "version.properties"), /versionName=3\.1\.0\nversionCode=84\n/);
    commit("a major version, by hand", versionFile("4.0.0", 84));
    mergeBack(["3.1.0", "85", "3.1.0-beta_2"]);
    assert.match(onRemote(BRANCH, "version.properties"), /versionName=4\.0\.0\nversionCode=85\n/);
  });

  test("leaves main alone when it already holds the versionCode, or a higher one", () => {
    commit("bumped meanwhile", versionFile("3.0.0", 90));
    assert.match(mergeBack(["3.0.0", "84", "3.0.0-beta_3"]), /nothing to merge back/);
    assert.equal(onRemote(BRANCH, "version.properties"), null);
    assert.deepEqual(ghCalls(), []);
  });

  test("renames the pull request an earlier release left open instead of opening another", () => {
    mergeBack(["3.0.0", "84", "3.0.0-beta_3"], { GH_OPEN_PR: "12" });
    const calls = ghCalls();
    assert.ok(calls.some((call) => call.startsWith(`pr edit ${BRANCH} --title chore: merge back 3.0.0-beta_3 into main`)));
    assert.ok(!calls.some((call) => call.startsWith("pr create")));
  });

  test("of a stable version removes the fragments its commit had, not the ones merged or changed since", () => {
    commit("export", { [fragment("export")]: '{"notes":1}', [fragment("totals")]: '{"notes":1}' });
    git("tag", "3.1.0");
    commit("since", { [fragment("widget")]: '{"notes":1}', [fragment("totals")]: '{"notes":2}' });
    git("push", "-q", "origin", "main", "3.1.0");
    const output = mergeBack(["3.1.0", "84", "3.1.0", "3.1.0"]);
    assert.match(output, /totals\.json changed since 3\.1\.0/);
    assert.match(onRemote(BRANCH, "version.properties"), /versionName=3\.1\.0\nversionCode=84\n/);
    assert.equal(onRemote(BRANCH, fragment("export")), null);
    assert.equal(onRemote(BRANCH, fragment("totals")), '{"notes":2}');
    assert.equal(onRemote(BRANCH, fragment("widget")), '{"notes":1}');
    assert.ok(ghCalls().some((call) => call.includes("Removes the 1 release notes' fragment(s) it told")));
  });

  test("of a release that consumed no versionCode leaves main's, and still writes the version and removes the fragments", () => {
    commit("export", { [fragment("export")]: '{"notes":1}' });
    git("tag", "3.1.0");
    git("push", "-q", "origin", "main", "3.1.0");
    mergeBack(["3.1.0", "0", "3.1.0", "3.1.0"]);
    assert.match(onRemote(BRANCH, "version.properties"), /versionName=3\.1\.0\nversionCode=83\n/);
    assert.equal(onRemote(BRANCH, fragment("export")), null);
    assert.ok(ghCalls().some((call) => call.includes("It consumed no versionCode.") && !call.includes("versionCode 83")));
  });

  test("of a stable version run again, with main already holding it all, changes nothing", () => {
    git("tag", "3.0.0");
    commit("merged back", versionFile("3.0.0", 84));
    git("push", "-q", "origin", "main", "3.0.0");
    // A file a step left in the checkout is not something to merge back.
    writeFileSync(join(repo, "left-behind.txt"), "");
    assert.match(mergeBack(["3.0.0", "84", "3.0.0", "3.0.0"]), /nothing to merge back/);
    assert.deepEqual(ghCalls(), []);
  });
});

describe("the locales the screenshots' upload jobs are made of", () => {
  const SCRIPT = join(HERE, "staged-screenshot-locales.sh");
  const staged = (folder) => execFileSync(SCRIPT, [join(root, folder)], { encoding: "utf8" });
  const stage = (...paths) => {
    for (const path of paths) {
      mkdirSync(dirname(join(root, path)), { recursive: true });
      writeFileSync(join(root, path), "");
    }
  };

  test("are the folders that hold an image, in Google Play's layout and in the App Store's", () => {
    stage("play/pt-BR/images/phoneScreenshots/01.png", "play/pt-BR/images/sevenInchScreenshots/01.png", "play/en-US/images/phoneScreenshots/01.png");
    stage("app-store/zh-Hant/01_iphone67.png", "app-store/ko/01_iphone67.png", "app-store/ko/02_iphone67.png");
    assert.equal(staged("play"), '["en-US","pt-BR"]\n');
    assert.equal(staged("app-store"), '["ko","zh-Hant"]\n');
  });

  test("leave out a locale with no image, and a file that is in no locale's folder", () => {
    stage("play/de-DE/title.txt", "play/en-US/images/phoneScreenshots/01.png", "play/stray.png");
    assert.equal(staged("play"), '["en-US"]\n');
  });

  test("are no answer at all, which skips the jobs, when nothing is staged or the folder is missing", () => {
    stage("play/de-DE/title.txt");
    assert.equal(staged("play"), "");
    assert.equal(staged("missing"), "");
  });
});

describe("the slots a release captures the App Store's screenshots on", () => {
  const SCRIPT = join(HERE, "app-store-capture-slots.sh");
  const IPHONE_AND_IPAD = ["iphone63", "iphone67", "ipad13"];
  const DUO = ["iphoneduo", "iphoneduoopen"];
  const slotsOutput = (env) => execFileSync(SCRIPT, [], { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"], env: { PATH: process.env.PATH, ...env } });
  /** The slots of a run: both Apple platforms and neither marker, unless said otherwise. */
  const slots = ({ ios = true, macos = true, duo = false, mac = false } = {}) => {
    const output = slotsOutput({ IOS: `${ios}`, MACOS: `${macos}`, SCREENSHOTS_IPHONE_DUO: `${duo}`, SCREENSHOTS_MAC: `${mac}` });
    return output === "" ? [] : JSON.parse(output);
  };

  test("are the iPhone's two and the iPad's, which the frames need, with both markers off", () => {
    assert.deepEqual(slots(), IPHONE_AND_IPAD);
    assert.deepEqual(slots({ macos: false }), IPHONE_AND_IPAD);
  });

  test("gain the iPhone Duo's two, closed and open, with its marker", () => {
    assert.deepEqual(slots({ duo: true }), [...IPHONE_AND_IPAD, ...DUO]);
  });

  test("gain the Mac's with its marker, and all three devices with both", () => {
    assert.deepEqual(slots({ mac: true }), [...IPHONE_AND_IPAD, "mac"]);
    assert.deepEqual(slots({ duo: true, mac: true }), [...IPHONE_AND_IPAD, ...DUO, "mac"]);
  });

  test("leave out a device whose platform the run doesn't release, since nothing would upload its images", () => {
    assert.deepEqual(slots({ macos: false, duo: true, mac: true }), [...IPHONE_AND_IPAD, ...DUO]);
    assert.deepEqual(slots({ ios: false, duo: true, mac: true }), [...IPHONE_AND_IPAD, "mac"]);
  });

  test("keep the iPhone's and the iPad's in a run of the Mac alone, whose frames need them too", () => {
    assert.deepEqual(slots({ ios: false, mac: true }), [...IPHONE_AND_IPAD, "mac"]);
  });

  test("are no answer at all, which skips the job, when the run could upload none of them", () => {
    assert.equal(slotsOutput({ IOS: "false", MACOS: "true", SCREENSHOTS_IPHONE_DUO: "false", SCREENSHOTS_MAC: "false" }), "");
    assert.equal(slotsOutput({ IOS: "false", MACOS: "true", SCREENSHOTS_IPHONE_DUO: "true", SCREENSHOTS_MAC: "false" }), "");
    assert.equal(slotsOutput({ IOS: "false", MACOS: "false", SCREENSHOTS_IPHONE_DUO: "true", SCREENSHOTS_MAC: "true" }), "");
  });

  test("are one line of JSON, as a job's matrix reads it from a step's output", () => {
    assert.equal(slotsOutput({ IOS: "true", MACOS: "true", SCREENSHOTS_IPHONE_DUO: "false", SCREENSHOTS_MAC: "false" }), '["iphone63","iphone67","ipad13"]\n');
  });

  test("are refused when an input is neither true nor false, or missing, rather than read as off", () => {
    assert.throws(() => slotsOutput({ IOS: "true", MACOS: "true", SCREENSHOTS_IPHONE_DUO: "yes", SCREENSHOTS_MAC: "false" }), /SCREENSHOTS_IPHONE_DUO is 'yes', neither true nor false/);
    assert.throws(() => slotsOutput({ IOS: "true", MACOS: "true", SCREENSHOTS_IPHONE_DUO: "true" }), /SCREENSHOTS_MAC is '', neither true nor false/);
    assert.throws(() => slotsOutput({ MACOS: "true", SCREENSHOTS_IPHONE_DUO: "true", SCREENSHOTS_MAC: "true" }), /IOS is '', neither true nor false/);
  });
});

describe("what the plan says of a release's screenshots", () => {
  const SCRIPT = join(HERE, "app-store-screenshots-plan.sh");
  /** The summary's row and the warnings of a run: every platform, submitting, screenshots on, unless said otherwise. */
  const plan = ({ screenshots = true, submit = true, ios = true, macos = true, duo = false, mac = false } = {}) => {
    const env = { PATH: process.env.PATH, SCREENSHOTS: `${screenshots}`, SUBMIT: `${submit}`, IOS: `${ios}`, MACOS: `${macos}`, SCREENSHOTS_IPHONE_DUO: `${duo}`, SCREENSHOTS_MAC: `${mac}` };
    const { status, stdout, stderr } = spawnSync(SCRIPT, [], { encoding: "utf8", env });
    assert.equal(status, 0, stderr);
    return { row: stdout.trim(), warnings: stderr.trim().split("\n").filter(Boolean) };
  };
  const SLOTS = "`iphone63`, `iphone67`, `ipad13`";

  test("is that they stay as they are, without a word more, when the release doesn't ask for them", () => {
    assert.deepEqual(plan({ screenshots: false }), { row: "left as they are", warnings: [] });
  });

  test("names the slots the App Store's are captured on, and the marker of each device left out", () => {
    assert.deepEqual(plan(), {
      row: `made again and uploaded with a release. The App Store's are captured on ${SLOTS}, without the iPhone Duo (\`screenshots_iphone_duo\`), without the Mac (\`screenshots_mac\`)`,
      warnings: [],
    });
    assert.equal(plan({ duo: true }).row, `made again and uploaded with a release. The App Store's are captured on ${SLOTS}, \`iphoneduo\`, \`iphoneduoopen\`, without the Mac (\`screenshots_mac\`)`);
    assert.equal(plan({ duo: true, mac: true }).row, `made again and uploaded with a release. The App Store's are captured on ${SLOTS}, \`iphoneduo\`, \`iphoneduoopen\`, \`mac\``);
  });

  test("doesn't call left out a device whose platform the run doesn't release", () => {
    assert.equal(plan({ macos: false }).row, `made again and uploaded with a release. The App Store's are captured on ${SLOTS}, without the iPhone Duo (\`screenshots_iphone_duo\`)`);
    assert.equal(plan({ ios: false, mac: true }).row, `made again and uploaded with a release. The App Store's are captured on ${SLOTS}, \`mac\``);
  });

  test("says none of the App Store's is made in a run that doesn't submit, or of the Mac alone without its marker", () => {
    assert.equal(plan({ submit: false }).row, "made again and uploaded with a release. None of the App Store's is: they only go up in a run that submits for review");
    assert.equal(plan({ ios: false }).row, "made again and uploaded with a release. None of the App Store's is: the Mac's capture is behind `screenshots_mac`");
    assert.equal(plan({ ios: false, macos: false }).row, "made again and uploaded with a release");
  });

  test("warns about each marker that is on and adds no capture", () => {
    const off = plan({ screenshots: false, duo: true, mac: true });
    assert.equal(off.row, "left as they are: `screenshots` is off, and the devices' markers only count with it");
    assert.equal(off.warnings.length, 2);
    assert.match(off.warnings[0], /^::warning title=The iPhone Duo is not captured::screenshots_iphone_duo is on/);
    assert.match(off.warnings[1], /^::warning title=The Mac is not captured::screenshots_mac is on/);
    assert.equal(plan({ submit: false, duo: true, mac: true }).warnings.length, 2);
    assert.deepEqual(plan({ ios: false, duo: true, mac: true }).warnings.map((line) => line.split("::")[1]), ["warning title=The iPhone Duo is not captured"]);
    assert.deepEqual(plan({ macos: false, duo: true, mac: true }).warnings.map((line) => line.split("::")[1]), ["warning title=The Mac is not captured"]);
    assert.deepEqual(plan({ duo: true, mac: true }).warnings, []);
  });

  test("is refused when an input is neither true nor false", () => {
    const env = { PATH: process.env.PATH, SCREENSHOTS: "yes", SUBMIT: "true", IOS: "true", MACOS: "true", SCREENSHOTS_IPHONE_DUO: "false", SCREENSHOTS_MAC: "false" };
    assert.match(spawnSync(SCRIPT, [], { encoding: "utf8", env }).stderr, /SCREENSHOTS is 'yes', neither true nor false/);
    assert.match(spawnSync(SCRIPT, [], { encoding: "utf8", env: { ...env, SCREENSHOTS: "true", IOS: "" } }).stderr, /IOS is '', neither true nor false/);
    assert.notEqual(spawnSync(SCRIPT, [], { encoding: "utf8", env }).status, 0);
  });
});

describe("the stable release's script", () => {
  // A copy next to a stand-in for dispatch-release.sh, which prints what it would dispatch instead of calling GitHub.
  const release = (...inputs) => {
    const scripts = join(root, "release-production");
    mkdirSync(scripts, { recursive: true });
    cpSync(join(HERE, "release-production.sh"), join(scripts, "release-production.sh"));
    writeFileSync(join(scripts, "dispatch-release.sh"), '#!/usr/bin/env bash\necho "dispatched: $*"\n');
    chmodSync(join(scripts, "dispatch-release.sh"), 0o755);
    return execFileSync(join(scripts, "release-production.sh"), inputs, { encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] });
  };

  test("hands every input to the workflow, the devices' markers too", () => {
    const output = release("screenshots=true", "screenshots_iphone_duo=true", "screenshots_mac=false");
    assert.match(output, /dispatched: release-production\.yml -f screenshots=true -f screenshots_iphone_duo=true -f screenshots_mac=false\n/);
  });

  test("says the screenshots stay as they are unless the release asks for them", () => {
    assert.match(release(), /The listings' screenshots stay as they are/);
    assert.match(release("track=internal"), /The listings' screenshots stay as they are/);
    assert.doesNotMatch(release("screenshots=false"), /stay as they are|No screenshot is made|leave the/);
  });

  test("says each device the App Store's screenshots leave out, when its marker wasn't passed", () => {
    const both = release("screenshots=true");
    assert.match(both, /leave the iPhone Duo out: screenshots_iphone_duo=true adds it/);
    assert.match(both, /leave the Mac out: screenshots_mac=true adds it/);
    const duoNamed = release("screenshots=true", "screenshots_iphone_duo=true");
    assert.doesNotMatch(duoNamed, /leave the iPhone Duo out/);
    assert.match(duoNamed, /leave the Mac out/);
    const macNamed = release("screenshots=true", "screenshots_mac=false");
    assert.match(macNamed, /leave the iPhone Duo out/);
    assert.doesNotMatch(macNamed, /leave the Mac out/);
    assert.doesNotMatch(release("screenshots=true", "screenshots_iphone_duo=false", "screenshots_mac=true"), /leave the/);
  });

  test("names no device a run couldn't capture: one that doesn't submit, or without the device's platform", () => {
    assert.doesNotMatch(release("screenshots=true", "submit_for_review=false"), /leave the/);
    assert.doesNotMatch(release("screenshots=true", "ios=false", "macos=false"), /leave the/);
    const macAlone = release("screenshots=true", "ios=false");
    assert.doesNotMatch(macAlone, /leave the iPhone Duo out/);
    assert.match(macAlone, /leave the Mac out/);
    const noMac = release("screenshots=true", "macos=false");
    assert.match(noMac, /leave the iPhone Duo out/);
    assert.doesNotMatch(noMac, /leave the Mac out/);
  });

  test("says a device's marker makes no screenshot without screenshots=true", () => {
    assert.match(release("screenshots_mac=true"), /No screenshot is made/);
    assert.match(release("screenshots=false", "screenshots_iphone_duo=true"), /No screenshot is made/);
    assert.doesNotMatch(release("screenshots_mac=false"), /No screenshot is made/);
    assert.doesNotMatch(release("screenshots=true", "screenshots_mac=true"), /No screenshot is made/);
  });

  test("reads an input passed twice by its last value, as the workflow does", () => {
    assert.match(release("screenshots=true", "screenshots=false", "screenshots_mac=true"), /No screenshot is made/);
    assert.doesNotMatch(release("screenshots=true", "screenshots_mac=true", "screenshots_mac=false"), /No screenshot is made|leave the Mac out/);
    assert.doesNotMatch(release("screenshots_mac=true", "screenshots_mac=false"), /No screenshot is made/);
    assert.match(release("screenshots=false", "screenshots=true"), /leave the iPhone Duo out/);
  });

  test("refuses what isn't input=value", () => {
    assert.throws(() => release("screenshots"), /'screenshots' is not an input/);
  });
});
