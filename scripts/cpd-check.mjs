// Compares the clones of a jscpd JSON report with the baseline. Run by scripts/cpd.sh:
//   node scripts/cpd-check.mjs <report> <baseline> [--update-baseline [--allow-new]] [--base <baseline of main>]
//
// The baseline holds, per pair of files, the lines of code their clones share. Lines, not clones: an edit inside a
// long clone can split it into two shorter ones, which is less copied code, not more. A pair over its lines fails,
// and so does one under them, until --update-baseline lowers them, so a removed clone can't come back unnoticed.

import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { relative } from "node:path";
import { fileURLToPath } from "node:url";

// Why: shorter than this once imports and comments are gone, a clone is a shared header, not copied code.
const MIN_LINES = 5;

const USAGE = "Usage: ./scripts/cpd.sh [--update-baseline [--allow-new]]";

const ROOT = fileURLToPath(new URL("..", import.meta.url));

const [reportPath, baselinePath, ...flags] = process.argv.slice(2);
const options = { update: false, allowNew: false, base: null };
for (let index = 0; index < flags.length; index++) {
    const flag = flags[index];
    if (flag === "--update-baseline") options.update = true;
    else if (flag === "--allow-new") options.allowNew = true;
    else if (flag === "--base" && index + 1 < flags.length) options.base = flags[++index];
    else fail(`Unknown argument: ${flag}\n${USAGE}`);
}
if (options.allowNew && !options.update) fail(`--allow-new only goes with --update-baseline\n${USAGE}`);
if (!existsSync(reportPath)) fail(`jscpd wrote no report at ${reportPath}: see its output above`);

const report = JSON.parse(readFileSync(reportPath, "utf8"));
const baseline = readBaseline(baselinePath);

const TS_IMPORT = /^[ \t]*(import|export)[ \t]+(type[ \t]+)?(\*([ \t]+as[ \t]+\w+)?|\{[^}]*\}|\w+([ \t]*,[ \t]*\{[^}]*\})?)[ \t]+from[ \t]+["'][^"'\n]+["'];?/gm;
const TS_SIDE_EFFECT_IMPORT = /^[ \t]*import[ \t]+["'][^"'\n]+["'];?/gm;
// Why: jscpd cuts fragments at any token, so one may start inside the braces of a multi-line import.
const TS_IMPORT_TAIL = /^([ \t]*(type[ \t]+)?\w+([ \t]+as[ \t]+\w+)?,?[ \t]*\n)*[ \t]*\}[ \t]*from[ \t]+["'][^"'\n]+["'];?/;
const KOTLIN_HEADER = /^\s*((package|import)\s|@file:)/;
const SWIFT_IMPORT = /^\s*(@\w+\s+)*import\s/;
const COMMENT = /^\s*(\/\/|\/\*|\*)/;

function codeLines(clone) {
    let fragment = clone.fragment;
    if (clone.format === "typescript" || clone.format === "tsx") {
        fragment = fragment.replace(TS_IMPORT_TAIL, "").replace(TS_IMPORT, "").replace(TS_SIDE_EFFECT_IMPORT, "");
    }
    const header = clone.format === "swift" ? SWIFT_IMPORT : KOTLIN_HEADER;
    return fragment
        .split("\n")
        .filter((line) => line.trim() !== "" && !COMMENT.test(line) && !header.test(line)).length;
}

const byCodePoint = (a, b) => (a < b ? -1 : a > b ? 1 : 0);
const side = (file) => ({ path: relative(ROOT, file.name), start: file.start, end: file.end });

const clones = report.duplicates
    .map((clone) => ({ clone, lines: codeLines(clone) }))
    .filter(({ lines }) => lines >= MIN_LINES)
    .map(({ clone, lines }) => {
        const [first, second] = [side(clone.firstFile), side(clone.secondFile)].sort((a, b) => byCodePoint(a.path, b.path));
        return { key: `${first.path} <-> ${second.path}`, first, second, lines };
    });

const current = {};
for (const clone of clones) current[clone.key] = (current[clone.key] ?? 0) + clone.lines;

const grown = Object.keys(current).filter((key) => current[key] > (baseline[key] ?? 0));
const shrunk = Object.keys(baseline).filter((key) => (current[key] ?? 0) < baseline[key]);

if (options.update) {
    if (grown.length > 0 && !options.allowNew) {
        reportGrown();
        fail("The baseline only takes copied code in with --allow-new, when the copy is on purpose.");
    }
    const next = options.allowNew ? current : Object.fromEntries(Object.keys(current).map((key) => [key, Math.min(current[key], baseline[key] ?? 0)]).filter(([, lines]) => lines > 0));
    writeBaseline(next);
    console.log(`✅ ${relative(ROOT, baselinePath)}: ${Object.keys(next).length} pairs of files with copied code`);
    process.exit(0);
}

if (options.base) warnAboutGrowth(readBaseline(options.base));

reportGrown();
for (const key of shrunk) {
    console.log(`❌ Less copied code than the baseline lists (${current[key] ?? 0} of ${baseline[key]} lines): ${key}`);
}
if (grown.length > 0) {
    console.log("Extract the copied code into one place the callers share. Only when the copy is on purpose,");
    console.log("run ./scripts/cpd.sh --update-baseline --allow-new and say why in the pull request.");
}
if (shrunk.length > 0) {
    console.log("Run ./scripts/cpd.sh --update-baseline to take the removed copied code out of the baseline.");
}
if (grown.length > 0 || shrunk.length > 0) process.exit(1);

console.log(`✅ No new copied code (${Object.keys(current).length} pairs of files in the baseline)`);

function reportGrown() {
    for (const key of grown) {
        console.log(`❌ New copied code: ${current[key]} lines where the baseline lists ${baseline[key] ?? 0}`);
        for (const { first, second } of clones.filter((clone) => clone.key === key)) {
            console.log(`   ${first.path}:${first.start}-${first.end}`);
            console.log(`   ${second.path}:${second.start}-${second.end}\n`);
        }
    }
}

// Why: --allow-new is the author's call, so CI doesn't fail on it, but a reviewer must see each pair it took in.
function warnAboutGrowth(base) {
    for (const key of Object.keys(baseline).filter((key) => baseline[key] > (base[key] ?? 0))) {
        console.log(`::warning file=.jscpd-baseline.json::The baseline takes in copied code (${base[key] ?? 0} → ${baseline[key]} lines): ${key}`);
    }
}

function readBaseline(path) {
    return existsSync(path) ? JSON.parse(readFileSync(path, "utf8")) : {};
}

function writeBaseline(pairs) {
    const sorted = Object.fromEntries(Object.keys(pairs).sort(byCodePoint).map((key) => [key, pairs[key]]));
    writeFileSync(baselinePath, `${JSON.stringify(sorted, null, 2)}\n`);
}

function fail(message) {
    console.log(`❌ ${message}`);
    process.exit(1);
}
