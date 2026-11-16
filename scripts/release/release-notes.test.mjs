// The tests of release-notes.mjs: node --test scripts/release/release-notes.test.mjs
//
// The rules a fragment follows and which notes a version tells are checked on plain values; the commands are run on
// a repository made for the test, with its own commits and tags, since they read the fragments from git.

import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { after, before, describe, test } from "node:test";
import { fileURLToPath } from "node:url";
import {
  LANGS,
  NotesError,
  STORE_LIMITS,
  bump,
  compareVersions,
  githubNotes,
  noteKey,
  parseFragment,
  plainNotes,
  releaseBefore,
  selectNotes,
  sentToTesters,
  siteFile,
  storeNotes,
} from "./release-notes.mjs";

const SCRIPT = join(dirname(fileURLToPath(import.meta.url)), "release-notes.mjs");

const textOf = (words) => Object.fromEntries(LANGS.map((lang) => [lang, `${words} (${lang})`]));
const note = (kind, words, more = {}) => ({ kind, platforms: ["all"], betaOnly: false, text: textOf(words), ...more });
const fragmentOf = (...notes) => JSON.stringify({ notes: notes.map(({ betaOnly, ...rest }) => (betaOnly ? { ...rest, betaOnly } : rest)) });
const refuses = (source, message) => assert.throws(() => parseFragment("a.json", source), (error) => error instanceof NotesError && message.test(error.message));

describe("a fragment", () => {
  test("is read with its kind, platforms and text", () => {
    const [read] = parseFragment("a.json", fragmentOf(note("new", "Export a group", { platforms: ["android", "ios"] })));
    assert.deepEqual(read, note("new", "Export a group", { platforms: ["android", "ios"] }));
  });

  test("tells a note only the betas tell", () => {
    const [read] = parseFragment("a.json", fragmentOf(note("fixed", "The new export opens", { betaOnly: true })));
    assert.equal(read.betaOnly, true);
  });

  test("is refused when it isn't JSON", () => refuses("{", /not JSON/));
  test("is refused without a note", () => refuses(JSON.stringify({ notes: [] }), /at least one note/));
  test("is refused with a key it doesn't take", () => refuses(JSON.stringify({ notes: [], date: "2026-11-14" }), /not date/));
  test("is refused with an unknown kind", () => refuses(fragmentOf(note("patch", "x")), /kind is one of new, improved, fixed/));

  test("is refused without one of the languages", () => {
    const { ja, ...text } = textOf("Export a group");
    refuses(fragmentOf(note("new", "x", { text })), /no text in ja/);
  });

  test("is refused with a language the notes don't have", () => {
    refuses(fragmentOf(note("new", "x", { text: { ...textOf("x"), vi: "x" } })), /not vi/);
  });

  test("is refused with text the apps can't draw as it is", () => {
    refuses(fragmentOf(note("new", "x", { text: { ...textOf("x"), en: "Now **faster**" } })), /no Markdown/);
    refuses(fragmentOf(note("new", "x", { text: { ...textOf("x"), en: "Two\nlines" } })), /one line/);
    refuses(fragmentOf(note("new", "x", { text: { ...textOf("x"), en: " padded" } })), /one line/);
  });

  test("is refused with platforms the site would refuse", () => {
    const withPlatforms = (platforms) => fragmentOf(note("new", "x", { platforms }));
    refuses(withPlatforms([]), /platforms is a list/);
    refuses(withPlatforms(["windows"]), /not "windows"/);
    refuses(withPlatforms(["all", "web"]), /not "all"/);
    refuses(withPlatforms([["all"]]), /not \["all"\]/);
    refuses(withPlatforms(["ios", "ios"]), /twice/);
    refuses(withPlatforms(["android", "ios", "mac", "web"]), /write \["all"\]/);
    refuses(withPlatforms(["web", "android"]), /in the order/);
  });

  test("is refused when betaOnly isn't true or false", () => refuses(JSON.stringify({ notes: [{ ...note("new", "x"), betaOnly: "yes" }] }), /betaOnly is true or false/));
});

describe("the versions", () => {
  test("order a version's betas before it, by their number", () => {
    const sorted = ["3.1.0", "3.0.0-beta_10", "3.0.0", "3.0.0-beta_2", "3.1.0-beta_1"].sort(compareVersions);
    assert.deepEqual(sorted, ["3.0.0-beta_2", "3.0.0-beta_10", "3.0.0", "3.1.0-beta_1", "3.1.0"]);
  });

  test("a beta's notes start after the release right before it", () => {
    const tags = ["3.0.0-beta_9", "3.0.0-beta_10", "3.0.0", "3.1.0-beta_1", "v-not-a-version"];
    assert.equal(releaseBefore("3.1.0-beta_2", tags), "3.1.0-beta_1");
    assert.equal(releaseBefore("3.1.0-beta_1", tags), "3.0.0");
    assert.equal(releaseBefore("3.0.0-beta_10", tags), "3.0.0-beta_9");
  });

  test("a beta sent again doesn't start after itself, nor after a later one", () => {
    assert.equal(releaseBefore("3.0.0-beta_10", ["3.0.0-beta_9", "3.0.0-beta_10", "3.0.0-beta_11"]), "3.0.0-beta_9");
  });

  test("a stable version's notes start after the stable version before it", () => {
    assert.equal(releaseBefore("3.1.0", ["3.0.0-beta_10", "3.0.0", "3.1.0-beta_1", "3.1.0-beta_2"]), "3.0.0");
    assert.equal(releaseBefore("3.0.0", ["3.0.0-beta_9", "3.0.0-beta_10"]), null);
  });

  test("the first release starts after nothing", () => assert.equal(releaseBefore("3.0.0-beta_1", []), null));

  test("a release sent a platform's testers a build when its notes list the platform", () => {
    const body = ["Build number: 84", "Sent to testers: ios", "", "| Android | Firebase App Distribution, group `beta` | skipped |"].join("\r\n");
    assert.equal(sentToTesters(body, "android"), false);
    assert.equal(sentToTesters(body, "ios"), true);
    assert.equal(sentToTesters("Sent to testers: android, ios", "android"), true);
    assert.equal(sentToTesters("Sent to testers: none", "ios"), false);
  });

  test("a release from before that line, or with no notes, sent nothing as far as the notes go", () => {
    assert.equal(sentToTesters("| Android | Firebase App Distribution | success |", "android"), false);
    assert.equal(sentToTesters(undefined, "android"), false);
  });

  test("only fixes lead to a patch", () => assert.equal(bump("3.0.0", [note("fixed", "a"), note("fixed", "b")]), "3.0.1"));
  test("no note at all leads to a patch", () => assert.equal(bump("3.0.4", []), "3.0.5"));
  test("something new leads to a minor", () => assert.equal(bump("3.0.4", [note("fixed", "a"), note("new", "b")]), "3.1.0"));
  test("an improvement leads to a minor", () => assert.equal(bump("3.2.1", [note("improved", "a")]), "3.3.0"));
  test("a note only the betas tell doesn't count", () => assert.equal(bump("3.0.0", [note("new", "a", { betaOnly: true })]), "3.0.1"));
});

describe("the notes a version tells", () => {
  const fragments = [
    { path: "release-notes/unreleased/a.json", notes: [note("fixed", "Totals add up"), note("new", "Export a group")] },
    { path: "release-notes/unreleased/b.json", notes: [note("improved", "Faster search"), note("fixed", "The export opens", { betaOnly: true })] },
    { path: "release-notes/unreleased/c.json", notes: [note("new", "Dark icon")] },
  ];
  const words = (notes) => notes.map((each) => each.text.en.replace(" (en)", ""));

  const keysOf = (...some) => new Set(some.flatMap((fragment) => fragment.notes.map((each) => noteKey(fragment.path, each.text.en))));

  test("are the ones the release before it didn't have, new first and fixes last", () => {
    assert.deepEqual(words(selectNotes(fragments, keysOf(fragments[0]), false)), ["Dark icon", "Faster search", "The export opens"]);
  });

  test("include a note a later pull request added to a file the release before it already had", () => {
    const before = keysOf({ path: fragments[0].path, notes: [fragments[0].notes[0]] });
    assert.deepEqual(words(selectNotes([fragments[0]], before, false)), ["Export a group"]);
  });

  test("are all of them, in the fragments' order within a kind, after no release", () => {
    assert.deepEqual(words(selectNotes(fragments, new Set(), false)), ["Export a group", "Dark icon", "Faster search", "Totals add up", "The export opens"]);
  });

  test("leave out, in a stable version, the ones only the betas tell", () => {
    assert.deepEqual(words(selectNotes(fragments, new Set(), true)), ["Export a group", "Dark icon", "Faster search", "Totals add up"]);
  });

  test("are none when every fragment was told", () => {
    assert.deepEqual(selectNotes(fragments, keysOf(...fragments), false), []);
  });
});

describe("what a release publishes", () => {
  const notes = [note("new", "Export a group"), note("improved", "The sidebar keeps its width", { platforms: ["mac", "web"] }), note("fixed", "The widget updates", { platforms: ["android"] })];

  test("the site's file has the text of its language and the platforms, nothing else", () => {
    assert.deepEqual(siteFile(notes, "pt", "2026-11-20"), {
      date: "2026-11-20",
      notes: [
        { text: "Export a group (pt)", platforms: ["all"] },
        { text: "The sidebar keeps its width (pt)", platforms: ["mac", "web"] },
        { text: "The widget updates (pt)", platforms: ["android"] },
      ],
    });
  });

  test("the GitHub release lists them in English, naming the platforms of the ones that aren't for all", () => {
    assert.equal(githubNotes(notes), "## What's new\n\n- Export a group (en)\n- The sidebar keeps its width (en) (Mac, Web)\n- The widget updates (en) (Android)\n");
    assert.equal(githubNotes([]), "");
  });

  test("a platform gets the notes for all and its own", () => {
    assert.equal(plainNotes(notes, "android", "pt"), "• Export a group (pt)\n• The widget updates (pt)");
    assert.equal(plainNotes(notes, "ios", "pt"), "• Export a group (pt)");
    assert.equal(plainNotes([notes[2]], "ios", "pt"), "");
  });

  test("a store gets a sentence that says nothing when the version has no note for it", () => {
    assert.equal(storeNotes([notes[2]], "ios", "en", STORE_LIMITS.appStore), "Bug fixes and performance improvements.");
    assert.equal(storeNotes([], "android", "pt", STORE_LIMITS.play), "Correções de bugs e melhorias de desempenho.");
  });

  test("a store gets a note longer than its limit cut, never the sentence that says nothing", () => {
    const told = storeNotes([note("new", "x".repeat(600))], "android", "en", STORE_LIMITS.play);
    assert.equal(told.length, STORE_LIMITS.play);
    assert.ok(told.startsWith("• xxx") && told.endsWith("…"));
  });

  test("a store gets only the first notes that fit its limit, each whole", () => {
    const long = Array.from({ length: 10 }, (_, index) => note("new", `${index} ${"x".repeat(96)}`));
    const told = storeNotes(long, "android", "en", STORE_LIMITS.play);
    assert.ok(told.length <= STORE_LIMITS.play);
    assert.deepEqual(told.split("\n").map((line) => line.slice(2, 3)), ["0", "1", "2", "3"]);
  });
});

describe("the commands", () => {
  let repo;
  let site;
  const git = (...args) => execFileSync("git", args, { cwd: repo, encoding: "utf8" });
  // The script reads the repository it is in, so the test runs a copy of it inside its own.
  const run = (...args) => execFileSync("node", [join(repo, "scripts/release/release-notes.mjs"), ...args], { cwd: repo, encoding: "utf8", env: { ...process.env, GITHUB_OUTPUT: "" } });
  const commit = (message, files) => {
    for (const [path, content] of Object.entries(files)) {
      mkdirSync(dirname(join(repo, path)), { recursive: true });
      writeFileSync(join(repo, path), content);
    }
    git("add", "-A");
    git("commit", "-q", "-m", message);
  };
  const fragment = (name) => `release-notes/unreleased/${name}.json`;
  const siteNotes = (version, lang = "en") => JSON.parse(readFileSync(join(site, "release-notes", lang, `${version}.json`), "utf8"));
  // A version written by hand in the site: its notes in each language, as `notesOf` gives them.
  const writeInSite = (version, notesOf) => {
    for (const lang of LANGS) {
      mkdirSync(join(site, "release-notes", lang), { recursive: true });
      writeFileSync(join(site, "release-notes", lang, `${version}.json`), JSON.stringify({ date: "2026-12-01", notes: notesOf(lang) }));
    }
  };

  // A checkout of the site as it is once it has every language of the fragments; `langs` for one that hasn't.
  const siteWith = (langs) => {
    const dir = mkdtempSync(join(tmpdir(), "release-notes-site-"));
    for (const lang of langs) mkdirSync(join(dir, "release-notes", lang), { recursive: true });
    return dir;
  };

  before(() => {
    repo = mkdtempSync(join(tmpdir(), "release-notes-repo-"));
    site = siteWith(LANGS);
    git("init", "-q", "-b", "main");
    git("config", "user.name", "Test");
    git("config", "user.email", "test@example.com");
    git("config", "commit.gpgsign", "false");
    git("config", "tag.gpgsign", "false");
    commit("the script", { "scripts/release/release-notes.mjs": readFileSync(SCRIPT, "utf8") });
    commit("totals", { [fragment("totals")]: fragmentOf(note("fixed", "Totals add up")) });
    git("tag", "3.0.0-beta_1");
    commit("export", { [fragment("export")]: fragmentOf(note("new", "Export a group")) });
    commit("export fix", { [fragment("export-opens")]: fragmentOf(note("fixed", "The export opens", { betaOnly: true })) });
    git("tag", "3.0.0-beta_2");
    git("tag", "3.0.0");
    // What a stable version's merge-back does: the fragments it told leave main.
    git("rm", "-q", "-r", "release-notes/unreleased");
    git("commit", "-q", "-m", "merge back 3.0.0");
    commit("widget", { [fragment("widget")]: fragmentOf(note("fixed", "The widget updates", { platforms: ["android"] })) });
    git("tag", "3.0.1-beta_1");
    // A later pull request adds its note to the file 3.0.1-beta_1 already read.
    commit("icon", { [fragment("widget")]: fragmentOf(note("fixed", "The widget updates", { platforms: ["android"] }), note("new", "Dark icon")) });
  });

  after(() => {
    rmSync(repo, { recursive: true, force: true });
    rmSync(site, { recursive: true, force: true });
  });

  test("check passes on readable fragments and names the one that isn't", () => {
    assert.match(run("check"), /1 fragment\(s\)/);
    writeFileSync(join(repo, fragment("Broken")), "{}");
    assert.throws(() => run("check"), /Broken\.json: a fragment is <branch-description>\.json/);
    rmSync(join(repo, fragment("Broken")));
  });

  test("a beta tells only what the beta before it didn't", () => {
    run("collect", "3.0.0-beta_2", "--ref", "3.0.0-beta_2", "--date", "2026-11-20", "--site", site);
    assert.deepEqual(siteNotes("3.0.0-beta_2"), {
      date: "2026-11-20",
      notes: [
        { text: "Export a group (en)", platforms: ["all"] },
        { text: "The export opens (en)", platforms: ["all"] },
      ],
    });
    assert.equal(siteNotes("3.0.0-beta_2", "ja").notes[0].text, "Export a group (ja)");
  });

  test("a stable version tells everything since the one before it, but what only the betas tell", () => {
    const out = join(site, "out-stable");
    run("collect", "3.0.0", "--ref", "3.0.0", "--date", "2026-11-25", "--site", site, "--out", out);
    assert.deepEqual(siteNotes("3.0.0").notes.map((each) => each.text), ["Export a group (en)", "Totals add up (en)"]);
    assert.equal(readFileSync(join(out, "stores/play/pt.txt"), "utf8"), "• Export a group (pt)\n• Totals add up (pt)");
    assert.equal(readFileSync(join(out, "github.md"), "utf8"), "## What's new\n\n- Export a group (en)\n- Totals add up (en)\n");
  });

  test("a version the site already has keeps its notes, and every output tells those", () => {
    const out = join(site, "out-written");
    writeInSite("3.0.1", (lang) => [{ text: `By hand (${lang})`, platforms: ["all"] }]);
    assert.match(run("collect", "3.0.1", "--site", site, "--out", out), /from the site/);
    assert.equal(siteNotes("3.0.1").date, "2026-12-01");
    assert.equal(readFileSync(join(out, "stores/app-store-ios/fr.txt"), "utf8"), "• By hand (fr)");
  });

  test("a site that doesn't have a language yet gets the others, and nothing in that one", () => {
    const older = siteWith(LANGS.filter((lang) => lang !== "pt-PT"));
    run("collect", "3.0.1-beta_2", "--date", "2026-12-03", "--site", older);
    assert.equal(JSON.parse(readFileSync(join(older, "release-notes/pt/3.0.1-beta_2.json"), "utf8")).notes[0].text, "Dark icon (pt)");
    assert.equal(existsSync(join(older, "release-notes/pt-PT")), false);
    rmSync(older, { recursive: true, force: true });
  });

  test("a site with a language the fragments don't have stops the release, naming it", () => {
    const newer = siteWith([...LANGS, "vi"]);
    assert.throws(() => run("collect", "3.0.1-beta_2", "--site", newer), /release-notes\/vi: the site has release notes in vi and the fragments don't/);
    rmSync(newer, { recursive: true, force: true });
  });

  test("a version written by hand in a site without Portugal's Portuguese tells Portugal Brazil's", () => {
    const older = siteWith(LANGS.filter((lang) => lang !== "pt-PT"));
    const out = join(older, "out");
    for (const lang of LANGS.filter((each) => each !== "pt-PT")) {
      writeFileSync(join(older, "release-notes", lang, "3.0.3.json"), JSON.stringify({ date: "2026-12-04", notes: [{ text: `By hand (${lang})`, platforms: ["all"] }] }));
    }
    run("collect", "3.0.3", "--site", older, "--out", out);
    assert.equal(readFileSync(join(out, "stores/play/pt-PT.txt"), "utf8"), "• By hand (pt)");
    rmSync(older, { recursive: true, force: true });
  });

  test("a folder that isn't the site's checkout is refused", () => {
    const elsewhere = mkdtempSync(join(tmpdir(), "release-notes-elsewhere-"));
    assert.throws(() => run("collect", "3.0.1-beta_2", "--site", elsewhere), /not a checkout of blitz-split-site/);
    rmSync(elsewhere, { recursive: true, force: true });
  });

  test("an option with no value of its own is refused, not read from the next one", () => {
    assert.throws(() => run("collect", "3.0.1-beta_2", "--ref", "--out", site), /--ref: takes one of/);
  });

  test("a version the site has with fewer notes in one language is refused by name", () => {
    writeInSite("3.0.2", (lang) => (lang === "ja" ? [] : [{ text: "By hand", platforms: ["all"] }]));
    assert.throws(() => run("collect", "3.0.2", "--site", site), /ja\/3\.0\.2\.json: not as many notes as in English/);
  });

  test("a note added to a fragment an earlier beta already told is told by the next one, alone", () => {
    const out = join(site, "out-added");
    run("collect", "3.0.1-beta_2", "--out", out);
    assert.equal(readFileSync(join(out, "github.md"), "utf8"), "## What's new\n\n- Dark icon (en)\n");
  });

  test("a version with no note writes nothing to the site, and its builds go out without a text", () => {
    const out = join(site, "out-empty");
    run("collect", "3.0.1-beta_2", "--ref", "3.0.0", "--site", site, "--out", out);
    // 3.0.0's commit has only what 3.0.0-beta_2 told, the release before 3.0.1-beta_2 there.
    assert.equal(existsSync(join(site, "release-notes", "en", "3.0.1-beta_2.json")), false);
    assert.equal(readFileSync(join(out, "testers/android.txt"), "utf8"), "");
    assert.equal(existsSync(join(out, "stores")), false);
  });

  test("the testers of a platform get its notes in Portuguese", () => {
    const out = join(site, "out-testers");
    run("collect", "3.0.1-beta_1", "--ref", "3.0.1-beta_1", "--out", out);
    assert.equal(readFileSync(join(out, "testers/android.txt"), "utf8"), "• The widget updates (pt)");
    assert.equal(readFileSync(join(out, "testers/ios.txt"), "utf8"), "");
  });

  test("the testers of a platform a beta left out get its notes with their next build", () => {
    const out = join(site, "out-left-out");
    const releases = join(site, "releases.json");
    writeFileSync(releases, JSON.stringify([{ tag: "3.0.0", body: "Build number: 90\nSent to testers: android, ios" }, { tag: "3.0.1-beta_1", body: "Build number: 91\nSent to testers: ios" }]));
    run("collect", "3.0.1-beta_2", "--out", out, "--releases", releases);
    assert.equal(readFileSync(join(out, "testers/android.txt"), "utf8"), "• Dark icon (pt)\n• The widget updates (pt)");
    assert.equal(readFileSync(join(out, "testers/ios.txt"), "utf8"), "• Dark icon (pt)");
    assert.equal(readFileSync(join(out, "github.md"), "utf8"), "## What's new\n\n- Dark icon (en)\n");
  });

  test("the next version is a patch after only fixes, and a minor once something is new", () => {
    assert.equal(run("next-version", "--ref", "3.0.1-beta_1").trim(), "3.0.1");
    assert.equal(run("next-version").trim(), "3.1.0");
  });

  test("the next version is left to version.properties before the first stable version", () => {
    // 3.0.0-beta_1's commit: 3.0.0 is tagged on the same commit as 3.0.0-beta_2, the beta that became it.
    assert.equal(run("next-version", "--ref", "3.0.0-beta_1").trim(), "");
  });
});
