// Draws the App Store's creative assets (docs/store-listing-screenshots.md) of every listing language, from the iPhone
// captures and the sample trip's facts: generate.sh runs it. A headless Chrome photographs each page, and a phrase or
// an amount too long for its place is shrunk until it fits; one that still leaves Apple's art safe area stops the run.
//
//   node scripts/store-screenshots/creative-assets/compose.mjs                  # every language
//   CREATIVE_ASSETS_LOCALES=en-US,ja node scripts/store-screenshots/creative-assets/compose.mjs
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { launchChrome } from "../../readme-hero/chrome.mjs";
import { escapeHtml, FACES, FACTS, isRightToLeft, ROOT, screenshotOpaque } from "./shared.mjs";

const HERE = import.meta.dirname;
const CAPTURES = join(ROOT, "build/outputs/apple-screenshot-captures/iphone67");
const PAGES = join(ROOT, "build/outputs/app-store-creative-asset-pages");
const OUTPUT = join(ROOT, "store_listings/creative-assets");

const SCREENS = { group: "01_group", items: "02_split_by_items", statistics: "06_statistics", home: "07_home" };

// Each asset's canvas and art safe area, from Apple's creative assets specifications, and the people on its tickets
// by their index in the sample trip, the one their faces are filed under.
const ASSETS = [
  {
    name: "header",
    width: 3840,
    height: 1646,
    safe: { x: 1097, y: 493, width: 1646, height: 661 },
    people: [2, 1, 0, 3, 4],
    titleWidth: 1620,
    ticketTextWidth: 262,
  },
  {
    name: "search-results",
    width: 3840,
    height: 2560,
    safe: { x: 836, y: 765, width: 2168, height: 1030 },
    people: [2, 1, 0],
    titleWidth: 1080,
    ticketTextWidth: 290,
  },
];

async function readCopy() {
  const rows = (await readFile(join(HERE, "copy.tsv"), "utf8")).split("\n").filter((row) => row && !row.startsWith("#"));
  return new Map(
    rows.map((row) => {
      const [tag, header, searchResults] = row.split("\t");
      return [tag, { header, "search-results": searchResults }];
    }),
  );
}

async function listingTags() {
  const rows = (await readFile(join(HERE, "../locales.tsv"), "utf8")).split("\n");
  return rows.filter((row) => row && !row.startsWith("#")).map((row) => row.split("\t")[0]);
}

const ticketOf = ({ name, share }, person) => `
  <div class="ticket"><div class="stub">
    <img src="${pathToFileURL(join(FACES, `person${person}.jpg`)).href}" alt="">
    <span>${escapeHtml(name)}</span><b>${escapeHtml(share)}</b>
  </div></div>`;

function pageOf(template, asset, tag, title, people) {
  const capture = (screen) => pathToFileURL(join(CAPTURES, tag, `${SCREENS[screen]}.png`)).href;
  const slots = {
    lang: tag,
    dir: isRightToLeft(tag) ? "rtl" : "ltr",
    css: pathToFileURL(join(HERE, "shared.css")).href,
    title: title.split("|").map((line) => `<span class="line">${escapeHtml(line)}</span>`).join(""),
    tickets: asset.people.map((person) => ticketOf(people[person], person)).join(""),
    home: capture("home"),
    group: capture("group"),
    items: capture("items"),
    statistics: capture("statistics"),
  };
  return template.replace(/\{\{(\w+)\}\}/g, (_, name) => slots[name]);
}

// Shrinks each group of texts together, so the tickets' amounts stay one size, then says what left the safe area.
const fitAndCheck = (asset) => `(() => {
  const fit = (selector, maxWidth) => {
    const elements = [...document.querySelectorAll(selector)];
    let size = parseFloat(getComputedStyle(elements[0]).fontSize);
    while (elements.some((element) => element.offsetWidth > maxWidth) && size > 12) {
      size -= 1;
      elements.forEach((element) => { element.style.fontSize = size + 'px'; });
    }
  };
  const title = document.querySelector('h1');
  let titleSize = parseFloat(getComputedStyle(title).fontSize);
  const lines = [...title.querySelectorAll('.line')];
  while (lines.some((line) => line.offsetWidth > ${asset.titleWidth}) && titleSize > 12) {
    titleSize -= 1;
    title.style.fontSize = titleSize + 'px';
  }
  fit('.stub b', ${asset.ticketTextWidth});
  fit('.stub span', ${asset.ticketTextWidth});
  const safe = ${JSON.stringify(asset.safe)};
  const inSafe = (box) => box.left >= safe.x - 1 && box.top >= safe.y - 1 &&
    box.right <= safe.x + safe.width + 1 && box.bottom <= safe.y + safe.height + 1;
  const boxes = [...document.querySelectorAll('[data-safe] .line, .phone[data-safe], .ticket')];
  return boxes.filter((element) => !inSafe(element.getBoundingClientRect())).map((element) => element.className);
})()`;

async function main() {
  const tags = await listingTags();
  const copy = await readCopy();
  const missingCopy = tags.filter((tag) => !copy.has(tag));
  if (missingCopy.length) throw new Error(`copy.tsv has no row for ${missingCopy.join(", ")}`);
  if (!existsSync(FACTS)) throw new Error(`No ${FACTS}: run scripts/store-screenshots/creative-assets/generate.sh`);
  const facts = JSON.parse(await readFile(FACTS, "utf8"));
  // Why: an empty variable means every language, as generate.sh hands it to capture-ios.sh.
  const asked = process.env.CREATIVE_ASSETS_LOCALES ? process.env.CREATIVE_ASSETS_LOCALES.split(",") : tags;
  const unknown = asked.filter((tag) => !tags.includes(tag));
  if (unknown.length) throw new Error(`locales.tsv has no ${unknown.join(", ")}`);
  const missingCaptures = asked.flatMap((tag) =>
    Object.values(SCREENS)
      .filter((screen) => !existsSync(join(CAPTURES, tag, `${screen}.png`)))
      .map((screen) => `${tag}/${screen}`),
  );
  if (missingCaptures.length) throw new Error(`No iPhone captures of ${missingCaptures.join(", ")}`);

  const missingFacts = asked.filter((tag) => !facts[tag]);
  if (missingFacts.length) throw new Error(`${FACTS} has no ${missingFacts.join(", ")}: run generate.sh again`);
  const templates = new Map(
    await Promise.all(ASSETS.map(async ({ name }) => [name, await readFile(join(HERE, `${name}.html`), "utf8")])),
  );

  await rm(PAGES, { recursive: true, force: true });
  await mkdir(PAGES, { recursive: true });
  const chrome = await launchChrome();
  try {
    for (const tag of asked) {
      await mkdir(join(OUTPUT, tag), { recursive: true });
      for (const asset of ASSETS) {
        const page = join(PAGES, `${tag}-${asset.name}.html`);
        const template = templates.get(asset.name);
        await writeFile(page, pageOf(template, asset, tag, copy.get(tag)[asset.name], facts[tag].people));
        await chrome.setViewport(asset.width, asset.height, 1);
        await chrome.open(pathToFileURL(page).href);
        await chrome.waitFor(
          `document.readyState === 'complete' && document.fonts.status === 'loaded' && ` +
            `[...document.images].every((image) => image.complete && image.naturalWidth > 0)`,
        );
        const outside = await chrome.evaluate(fitAndCheck(asset));
        if (outside.length) throw new Error(`${tag} ${asset.name}: ${outside.join(", ")} left the art safe area`);
        const file = join(OUTPUT, tag, `${asset.name}.png`);
        await screenshotOpaque(chrome, file, asset.width, asset.height);
      }
      console.log(`Drew ${tag}`);
    }
  } finally {
    await chrome.close();
  }
  console.log(`The images are in ${OUTPUT}`);
}

await main();
