// The tests of the release workflows' shell scripts, suggest-version.sh and merge-back.sh:
// node --test scripts/release/release-scripts.test.mjs
//
// Each runs a copy of the scripts inside a repository made for the test, with a remote of its own, since they read
// and write the repository they are in. The GitHub CLI is a stand-in that writes down what it was asked.

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
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
