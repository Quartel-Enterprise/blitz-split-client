// The tests of heavy-check.sh, the queue every checkout's heavy builds share:
// node --test scripts/heavy-check.test.mjs
//
// Each test has a queue of its own (HEAVY_CHECK_LOCK in a temporary folder). A command writes its name to a log when
// it runs and waits for a file before it ends, so the log is the order the queue ran them in. Each check says which
// checkout it is of (HEAVY_CHECK_CHECKOUT), "here" unless a test gives it another.

import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { existsSync, mkdtempSync, readdirSync, readFileSync, rmSync, utimesSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { afterEach, beforeEach, test } from "node:test";
import { setTimeout as sleep } from "node:timers/promises";
import { fileURLToPath } from "node:url";

const SCRIPT = join(dirname(fileURLToPath(import.meta.url)), "heavy-check.sh");

let folder;
let lock;
let log;
beforeEach(() => {
  folder = mkdtempSync(join(tmpdir(), "heavy-check-"));
  lock = join(folder, "queue.lock");
  log = join(folder, "log");
  writeFileSync(log, "");
});
afterEach(() => rmSync(folder, { recursive: true, force: true }));

const gate = (name) => join(folder, `gate-${name}`);
const open = (name) => writeFileSync(gate(name), "");
/** The shell that writes its name to the log, then waits for its gate to open. */
const step = (name) => `echo ${name} >> "${log}"; while [ ! -e "${gate(name)}" ]; do sleep 0.05; done`;
const ran = () => readFileSync(log, "utf8").split("\n").filter(Boolean);
const waiters = () => (existsSync(`${lock}.queue`) ? readdirSync(`${lock}.queue`).length : 0);
const ELSEWHERE = { env: { HEAVY_CHECK_CHECKOUT: "elsewhere" } };

/** Runs heavy-check.sh, and resolves with how it ended. */
const heavy = (args, { env = {} } = {}) => {
  const child = spawn(SCRIPT, args, { env: { ...process.env, HEAVY_CHECK_LOCK: lock, HEAVY_CHECK_HELD: "", HEAVY_CHECK_HOLDER: "", HEAVY_CHECK_CHECKOUT: "here", ...env }, stdio: ["ignore", "pipe", "pipe"] });
  let output = "";
  child.stdout.on("data", (data) => (output += data));
  child.stderr.on("data", (data) => (output += data));
  const ended = new Promise((resolve) => child.on("close", (code, signal) => resolve({ code, signal, output })));
  return { child, ended };
};
const shell = (script, options) => heavy(["sh", "-c", script], options);

const until = async (condition, what) => {
  for (let tries = 0; tries < 400; tries++) {
    if (condition()) return;
    await sleep(25);
  }
  assert.fail(`timed out waiting for ${what}`);
};

test("exits with the command's code", async () => {
  assert.equal((await shell("exit 7").ended).code, 7);
  assert.equal((await shell("exit 0").ended).code, 0);
});

test("a heavy check run by the command runs straight away", async () => {
  const { code, output } = await shell(`"${SCRIPT}" echo nested`).ended;
  assert.equal(code, 0);
  assert.match(output, /nested/);
});

test("runs the waiters in the order they arrived", async () => {
  const first = shell(step("first"));
  await until(() => ran().length === 1, "the first command");
  const second = shell(step("second"));
  await until(() => waiters() === 1, "the second to wait");
  const third = shell(step("third"));
  await until(() => waiters() === 2, "the third to wait");
  open("third");
  open("second");
  open("first");
  await Promise.all([first.ended, second.ended, third.ended]);
  assert.deepEqual(ran(), ["first", "second", "third"]);
});

test("a script that takes the queue step after step lets a waiter in between", async () => {
  const lane = spawn("sh", ["-c", `"${SCRIPT}" sh -c '${step("lane1")}'; "${SCRIPT}" sh -c '${step("lane2")}'`], { env: { ...process.env, HEAVY_CHECK_LOCK: lock, HEAVY_CHECK_HELD: "", HEAVY_CHECK_CHECKOUT: "here" }, stdio: "ignore" });
  const laneEnded = new Promise((resolve) => lane.on("close", resolve));
  await until(() => ran().length === 1, "the lane's first step");
  const check = shell(step("check"));
  await until(() => waiters() === 1, "the check to wait");
  open("lane1");
  open("check");
  open("lane2");
  await Promise.all([laneEnded, check.ended]);
  assert.deepEqual(ran(), ["lane1", "check", "lane2"]);
});

test("--yield hands the queue to a waiter and comes back once it is the command's again", async () => {
  const long = shell(`${step("before")}; "${SCRIPT}" --yield; echo after >> "${log}"`);
  await until(() => ran().length === 1, "the long command");
  const check = shell(step("check"), ELSEWHERE);
  await until(() => waiters() === 1, "the check to wait");
  open("before");
  await until(() => ran().includes("check"), "the check to run");
  // The long command stays where it yielded for as long as the check runs.
  await sleep(300);
  assert.deepEqual(ran(), ["before", "check"]);
  open("check");
  const [{ code, output }] = await Promise.all([long.ended, check.ended]);
  assert.equal(code, 0);
  assert.match(output, /letting the heavy checks that wait go first/);
  assert.deepEqual(ran(), ["before", "check", "after"]);
});

test("--yield keeps the queue from a check of the command's own checkout", async () => {
  // What waits could delete what the command is in the middle of: it gets the queue when the command ends.
  const long = shell(`${step("before")}; "${SCRIPT}" --yield; ${step("after")}`);
  await until(() => ran().length === 1, "the long command");
  const check = shell(step("check"));
  await until(() => waiters() === 1, "the check to wait");
  open("before");
  await until(() => ran().includes("after"), "the long command to go on");
  assert.deepEqual(ran(), ["before", "after"]);
  open("after");
  open("check");
  const [{ output }] = await Promise.all([long.ended, check.ended]);
  assert.doesNotMatch(output, /letting/);
  assert.deepEqual(ran(), ["before", "after", "check"]);
});

test("--yield lets the checks ahead of one of its own checkout go, and comes back before that one", async () => {
  const long = shell(`${step("before")}; "${SCRIPT}" --yield; ${step("after")}`);
  await until(() => ran().length === 1, "the long command");
  const other = shell(step("other"), ELSEWHERE);
  await until(() => waiters() === 1, "the other checkout's check to wait");
  const own = shell(step("own"));
  await until(() => waiters() === 2, "the own checkout's check to wait");
  const behind = shell(step("behind"), ELSEWHERE);
  await until(() => waiters() === 3, "the check behind it to wait");
  open("before");
  open("other");
  await until(() => ran().includes("after"), "the long command to go on");
  assert.deepEqual(ran(), ["before", "other", "after"]);
  open("after");
  open("own");
  open("behind");
  await Promise.all([long.ended, other.ended, own.ended, behind.ended]);
  assert.deepEqual(ran(), ["before", "other", "after", "own", "behind"]);
});

test("--yield returns at once when nobody waits, and does nothing outside the queue", async () => {
  const held = await shell(`"${SCRIPT}" --yield; echo went on`).ended;
  assert.equal(held.code, 0);
  assert.match(held.output, /went on/);
  const outside = await heavy(["--yield"]).ended;
  assert.equal(outside.code, 0);
  assert.equal(outside.output, "");
});

test("--yield asks nothing of a process that doesn't hold the queue", async () => {
  // What a command left running keeps: the variable of a holder that is gone, whose number is another process by now.
  const bystander = spawn("sleep", ["30"], { stdio: "ignore" });
  const exited = new Promise((resolve) => bystander.on("close", (code, signal) => resolve(signal)));
  const holder = shell(step("holder"));
  await until(() => ran().length === 1, "the holder");
  const check = shell(step("check"));
  await until(() => waiters() === 1, "the check to wait");
  const { code, output } = await heavy(["--yield"], { env: { HEAVY_CHECK_HELD: "1", HEAVY_CHECK_HOLDER: String(bystander.pid) } }).ended;
  assert.equal(code, 0);
  assert.equal(output, "");
  await sleep(200);
  assert.equal(bystander.exitCode, null);
  assert.equal(bystander.signalCode, null);
  bystander.kill("SIGKILL");
  assert.equal(await exited, "SIGKILL");
  open("holder");
  open("check");
  await Promise.all([holder.ended, check.ended]);
  assert.deepEqual(ran(), ["holder", "check"]);
});

test("the lock file names nobody once a check is over", async () => {
  await shell("exit 0").ended;
  assert.equal(readFileSync(lock, "utf8"), "");
});

test("a command that ends while its turn is given away ends the script with its code", async () => {
  // The command asks to yield in the background and ends at once: the check then holds the queue for as long as it runs.
  const long = shell(`${step("before")}; "${SCRIPT}" --yield & exit 3`);
  await until(() => ran().length === 1, "the long command");
  const check = shell(step("check"), ELSEWHERE);
  await until(() => waiters() === 1, "the check to wait");
  open("before");
  assert.equal((await long.ended).code, 3);
  open("check");
  assert.equal((await check.ended).code, 0);
});

test("a waiter that is gone doesn't hold the line", async () => {
  // An older ticket of a process that no longer exists, and one that stopped being touched long ago.
  const queue = `${lock}.queue`;
  await shell("true").ended;
  writeFileSync(join(queue, "00000000000000001-999999999"), "");
  const abandoned = join(queue, `00000000000000002-${process.pid}`);
  writeFileSync(abandoned, "");
  utimesSync(abandoned, new Date(Date.now() - 3_600_000), new Date(Date.now() - 3_600_000));
  const { code } = await shell("exit 0").ended;
  assert.equal(code, 0);
  assert.equal(waiters(), 0);
});

test("waiters that were all stopped keep their places", async () => {
  // A Mac that slept: every waiter wakes with a ticket nobody touched for long, and the one that wakes first is not
  // the one that arrived first.
  const stale = { env: { HEAVY_CHECK_STALE: "4" } };
  const holder = shell(step("holder"), stale);
  await until(() => ran().length === 1, "the holder");
  const first = shell(step("first"), stale);
  await until(() => waiters() === 1, "the first to wait");
  const second = shell(step("second"), stale);
  await until(() => waiters() === 2, "the second to wait");
  first.child.kill("SIGSTOP");
  second.child.kill("SIGSTOP");
  open("holder");
  await holder.ended;
  await sleep(5000);
  second.child.kill("SIGCONT");
  await sleep(300);
  first.child.kill("SIGCONT");
  open("first");
  open("second");
  await Promise.all([first.ended, second.ended]);
  assert.deepEqual(ran(), ["holder", "first", "second"]);
});

test("the place in line doesn't come from the clock", async () => {
  // A ticket numbered far ahead of the clock, as one taken before the clock was set back would be.
  const holder = shell(step("holder"));
  await until(() => ran().length === 1, "the holder");
  const first = shell(step("first"));
  await until(() => waiters() === 1, "the first to wait");
  const [ticket] = readdirSync(`${lock}.queue`);
  assert.match(ticket, /^0+2-\d+$/);
  open("holder");
  open("first");
  await Promise.all([holder.ended, first.ended]);
});

test("a SIGTERM goes on to the command", async () => {
  const running = shell(step("running"));
  await until(() => ran().length === 1, "the command");
  running.child.kill("SIGTERM");
  assert.equal((await running.ended).code, 143);
});
