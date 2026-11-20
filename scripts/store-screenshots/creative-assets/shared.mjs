// What the creative assets (compose.mjs here) and the listing art (../listing-art/compose.mjs) share: where the sample
// trip's faces and facts are, which languages read right to left, and how a page becomes an opaque PNG of its size.
import { readFile, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { describePng, withoutAlpha } from "./png.mjs";

export const ROOT = resolve(import.meta.dirname, "../../..");
export const FACTS = join(ROOT, "build/outputs/app-store-creative-asset-facts/facts.json");
export const FACES = join(ROOT, "scripts/store-screenshots/faces");
export const RIGHT_TO_LEFT = new Set(["ar", "he", "ur"]);

export const isRightToLeft = (tag) => RIGHT_TO_LEFT.has(tag.split("-")[0]);

export const escapeHtml = (text) =>
  text.replace(/[&<>"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[character]);

/** Photographs the open page into `file`, drops the alpha channel App Store Connect refuses, and checks its size. */
export async function screenshotOpaque(chrome, file, width, height) {
  await chrome.screenshot(file);
  const flat = withoutAlpha(await readFile(file));
  const size = describePng(flat);
  if (size.width !== width || size.height !== height) throw new Error(`${file} came out ${size.width}x${size.height}`);
  await writeFile(file, flat);
}
