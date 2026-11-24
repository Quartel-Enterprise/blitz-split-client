// Draws the store listing's art (docs/store-listing-screenshots.md): ten images per device and language, from the
// captures of capture.sh, the words of copy.tsv and the sample trip's facts. One long picture is drawn per set and each
// image is its slice of it, so the ticket-shaped band runs on from one image to the next.
//
//   node scripts/store-screenshots/listing-art/compose.mjs
//   ART_DEVICES=iphone_6_7,ipad_13 ART_LOCALES=en-US,pt-BR node scripts/store-screenshots/listing-art/compose.mjs
import { existsSync } from "node:fs";
import { mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { pathToFileURL } from "node:url";
import { launchChrome } from "../../readme-hero/chrome.mjs";
import { describePng, readPixels } from "../creative-assets/png.mjs";
import { escapeHtml, FACES, FACTS, isRightToLeft, ROOT, screenshotOpaque } from "../creative-assets/shared.mjs";

const HERE = import.meta.dirname;
const CAPTURES = join(ROOT, "build/outputs/apple-screenshot-captures");
const PLAY_CAPTURES = join(ROOT, "build/outputs/play-art-captures");
const PAGES = join(ROOT, "build/outputs/listing-art-pages");
const OUTPUT = join(ROOT, "store_listings/listing-art");
const RECEIPT = join(ROOT, "scripts/store-screenshots/receipt.jpg");
const COLORS = ["#0077cc", "#00558f", "#0070c0", "#00558f", "#0070c0", "#173a80", "#1b2f6e", "#173a80", "#0070c0", "#0077cc"];
const SHARE_PEOPLE = { promise: [2, 3], split: [1, 4] };

const FILES = {
  group: "01_group", items: "02_split_by_items", receipt: "03_receipt", balances: "04_balances", invite: "05_invite",
  statistics: "06_statistics", home: "07_home", payers: "08_payers_and_currencies", light: "10_light_theme",
  pay: "11_pay", split: "12_split_modes",
};

// The ten images of a listing, in order: its words, its screen and what is drawn over the screen.
const STORY = [
  { copy: "promise", screen: "group", callout: "group", extra: "tickets" },
  { copy: "pix", screen: "pay", callout: "pay", only: "pt-BR" },
  { copy: "receipt", screen: "receipt", extra: "receipt" },
  { copy: "items", screen: "items", callout: "items" },
  { copy: "balances", screen: "balances", callout: "balances" },
  { copy: "invite", screen: "invite", extra: "pills" },
  { copy: "split", screen: "split", callout: "split", extra: "tickets" },
  { copy: "currencies", screen: "payers", callout: "payers" },
  { copy: "statistics", screen: "statistics", callout: "statistics" },
  { copy: "home", screen: "home", callout: "home", except: "pt-BR" },
  { copy: "offline", screen: "light" },
];

// Google Play holds eight a device, and Android's art draws no light theme nor "Dividir": the story without them, and
// with Pix in Brazil's place of the groups at a glance.
const PLAY_STORY = STORY.filter((entry) => !["split", "offline"].includes(entry.copy));

// The Mac's art draws its window as it is, with its own corners and shadow, in place of a device, and tells the
// phones' story: ten images, as the App Store's upload wants on every shelf.
const MAC_STORY = STORY;
const MAC_CALLOUTS = {
  split: { x: 950, y: 490, w: 880, h: 220, s: 1.6 },
  pay: { x: 1010, y: 590, w: 850, h: 510, s: 1.4 },
  group: { x: 320, y: 320, w: 750, h: 160, s: 1.7 },
  items: { x: 1500, y: 740, w: 650, h: 520, s: 1.3 },
  balances: { x: 310, y: 590, w: 900, h: 330, s: 1.4 },
  statistics: { x: 500, y: 470, w: 1050, h: 560, s: 1.2 },
  home: { x: 940, y: 160, w: 1300, h: 330, s: 1.25 },
  payers: { x: 1440, y: 330, w: 720, h: 400, s: 1.4 },
};

// A callout is the part of a capture drawn larger over it: x, y, width, height and its scale, in the capture's pixels.
// `card` finds its top and height in the capture instead, as a language's longer words move what is above it: the first
// run of rows from `from` on, at least `min` tall, whose pixel at `probe` differs from the screen's own at `edge`.
const PHONE_CALLOUTS = {
  group: { x: 40, y: 1170, w: 1210, h: 360, s: 0.9, card: { from: 900, min: 200, max: 420, probe: 90, edge: 8 } },
  pay: { x: 30, y: 1260, w: 1230, h: 430, s: 0.92, card: { from: 1120, min: 300, max: 520, probe: 60, edge: 8 } },
  items: { x: 50, y: 1250, w: 850, h: 155, s: 1.25, card: { from: 1170, min: 100, max: 175, probe: 90, edge: 8 } },
  balances: { x: 40, y: 1930, w: 1210, h: 620, s: 0.95, card: { from: 1650, min: 300, max: 620, probe: 90, edge: 8 } },
  split: { x: 40, y: 755, w: 1250, h: 125, s: 0.944, card: { from: 640, min: 80, max: 140, probe: 90, edge: 8 } },
  payers: { x: 40, y: 735, w: 900, h: 250, s: 1.1 },
  statistics: { x: 40, y: 1600, w: 1210, h: 420, s: 0.92, card: { from: 1450, min: 280, max: 460, probe: 90, edge: 8 } },
  home: { x: 40, y: 500, w: 990, h: 340, s: 1.05 },
};
const IPAD_CALLOUTS = {
  group: { x: 766, y: 651, w: 576, h: 304, s: 1.3 },
  pay: { x: 826, y: 972, w: 1080, h: 288, s: 1.1 },
  items: { x: 560, y: 1050, w: 790, h: 350, s: 1.2 },
  balances: { x: 1415, y: 440, w: 1134, h: 563, s: 1.02 },
  split: { x: 570, y: 725, w: 800, h: 95, s: 1.3 },
  payers: { x: 555, y: 755, w: 720, h: 185, s: 1.35 },
  statistics: { x: 580, y: 1020, w: 1030, h: 270, s: 1.1 },
  home: { x: 680, y: 300, w: 1000, h: 420, s: 1.05 },
};
const DUO_CALLOUTS = {
  pay: { x: 85, y: 923, w: 1000, h: 429, s: 0.98 },
  items: { x: 60, y: 825, w: 850, h: 150, s: 1.2 },
  split: { x: 60, y: 500, w: 1085, h: 115, s: 0.98 },
  payers: { x: 60, y: 525, w: 760, h: 225, s: 1.25 },
  home: { x: 40, y: 200, w: 1090, h: 420, s: 0.95 },
};
const PLAY_PHONE_CALLOUTS = {
  group: { x: 40, y: 960, w: 1160, h: 260, s: 0.95, card: { from: 850, min: 180, max: 300, probe: 90, edge: 8 } },
  pay: { x: 40, y: 950, w: 1160, h: 400, s: 0.95, card: { from: 900, min: 300, max: 440, probe: 90, edge: 8 } },
  items: { x: 40, y: 930, w: 1100, h: 170, s: 1.05 },
  balances: { x: 40, y: 1560, w: 1160, h: 420, s: 0.95 },
  payers: { x: 40, y: 600, w: 900, h: 300, s: 1.1 },
  statistics: { x: 40, y: 1480, w: 1160, h: 460, s: 0.95, card: { from: 1380, min: 300, max: 480, probe: 90, edge: 8 } },
  home: { x: 40, y: 440, w: 1160, h: 380, s: 0.95 },
};
const PLAY_TABLET_CALLOUTS = {
  group: { x: 280, y: 650, w: 760, h: 180, s: 1.6 },
  items: { x: 110, y: 620, w: 950, h: 250, s: 1.3 },
  balances: { x: 1060, y: 470, w: 1460, h: 470, s: 1.0 },
  payers: { x: 100, y: 470, w: 900, h: 200, s: 1.4 },
  home: { x: 220, y: 220, w: 700, h: 330, s: 1.6 },
};
const DUO_OPEN_CALLOUTS = {
  group: { x: 61, y: 967, w: 1245, h: 290, s: 1.05 },
  balances: { x: 1488, y: 609, w: 1114, h: 640, s: 1.02 },
  statistics: { x: 50, y: 1349, w: 1315, h: 424, s: 1.05 },
};

const PHONE = { title: [90, 190, 134], device: [165, 790, 960, 24, 168], band: 1420, notch: 110, wide: false, callouts: PHONE_CALLOUTS };
const scaled = (kind, ratio) => ({
  ...kind,
  title: kind.title.map((value) => Math.round(value * ratio)),
  device: kind.device.map((value) => Math.round(value * ratio)),
  band: Math.round(kind.band * ratio),
  notch: Math.round(kind.notch * ratio),
  ratio,
});
const IPAD = { title: [130, 100, 158], device: [236, 385, 2260, 26, 72], band: 1180, notch: 130, wide: true, callouts: IPAD_CALLOUTS };
// Google Play's foldable shelf asks for the unfolded screen in 1:1 and in 4:3 and the outer screen in 10:16. The unfolded
// capture is 1.2:1, so one capture feeds both unfolded canvases. No callouts yet.
const FOLDABLE_WIDE = { title: [130, 100, 158], band: 1180, notch: 130, wide: true, callouts: {}, play: true };
const FOLDABLE_OUTER = { title: [90, 190, 134], device: [350, 620, 900, 24, 168], band: 1300, notch: 110, wide: false, callouts: {}, play: true };
const DUO_CLOSED = { width: 1398, height: 2034, slot: "iphoneduo", title: [90, 130, 116], device: [179, 610, 1040, 24, 96], band: 1120, notch: 90, wide: false, callouts: DUO_CALLOUTS };
const DUO_OPEN = { width: 2853, height: 2007, slot: "iphoneduoopen", title: [150, 105, 158], device: [226, 385, 2400, 26, 84], band: 1120, notch: 130, wide: true, callouts: DUO_OPEN_CALLOUTS };

const DEVICES = {
  iphone_6_7: () => ({ ...PHONE, width: 1290, height: 2796, slot: "iphone67" }),
  iphone_6_3: () => ({ ...scaled(PHONE, 1206 / 1290), width: 1206, height: 2622, slot: "iphone63" }),
  ipad_13: () => ({ ...IPAD, width: 2732, height: 2048, slot: "ipad13" }),
  ipad_11: () => ({ ...scaled(IPAD, 2388 / 2732), width: 2388, height: 1668, slot: "ipad13" }),
  play_phone: () => ({ ...scaled(PHONE, 1242 / 1290), width: 1242, height: 2484, slot: "playphone", play: true, callouts: PLAY_PHONE_CALLOUTS }),
  play_tablet_10: () => ({ ...scaled(IPAD, 2560 / 2732), width: 2560, height: 1440, slot: "playtablet10", play: true, callouts: PLAY_TABLET_CALLOUTS }),
  play_tablet_7: () => ({ ...scaled(IPAD, 2560 / 2732), width: 2560, height: 1440, slot: "playtablet7", play: true, callouts: {} }),
  play_foldable_1x1: () => ({ ...FOLDABLE_WIDE, device: [180, 400, 1800, 26, 72], width: 2160, height: 2160, slot: "playfoldable" }),
  play_foldable_4x3: () => ({ ...FOLDABLE_WIDE, device: [416, 385, 2048, 26, 72], width: 2880, height: 2160, slot: "playfoldable" }),
  play_foldable_outer: () => ({ ...FOLDABLE_OUTER, width: 1600, height: 2560, slot: "playfoldablefolded" }),
  mac: () => ({ title: [150, 110, 150], device: [300, 360, 2280, 0, 0], band: 1060, notch: 140, wide: true, width: 2880, height: 1800, slot: "mac", window: true, callouts: MAC_CALLOUTS }),
  iphone_duo: (screen) => (["group", "balances", "statistics"].includes(screen) ? DUO_OPEN : DUO_CLOSED),
};


async function table(path) {
  const rows = (await readFile(path, "utf8")).split("\n").filter(Boolean);
  const header = rows.find((row) => row.startsWith("# tag")).slice(2).split("\t");
  return new Map(
    rows.filter((row) => !row.startsWith("#")).map((row) => {
      const fields = row.split("\t");
      if (fields.length !== header.length) throw new Error(`copy.tsv: ${fields[0]} has ${fields.length} fields`);
      return [fields[0], Object.fromEntries(header.map((name, index) => [name, fields[index]]))];
    }),
  );
}

// The run of rows the card fills, or null: the capture then falls back on the callout's own place. Each capture is
// read once per panel, so its pixels aren't kept: a whole run would hold gigabytes of them.
function findCard(png, card, ratio, mirrored) {
  const { width, height, pixels } = readPixels(png);
  const differs = (y) => {
    const at = (x) => (y * width + (mirrored ? width - 1 - Math.round(x * ratio) : Math.round(x * ratio))) * 4;
    const [probe, edge] = [at(card.probe), at(card.edge)];
    return Math.abs(pixels[probe] - pixels[edge]) + Math.abs(pixels[probe + 1] - pixels[edge + 1]) + Math.abs(pixels[probe + 2] - pixels[edge + 2]) > 12;
  };
  // Short gaps are text or a row's divider crossing the probe's column.
  const gap = Math.round(8 * ratio);
  for (let y = Math.round(card.from * ratio); y < height; y++) {
    if (!differs(y)) continue;
    let end = y;
    for (let next = y; next < height && next - end <= gap; next++) if (differs(next)) end = next;
    if (end - y >= card.min * ratio) return { y, h: Math.min(end - y + 1, Math.round(card.max * ratio)) };
    y = end;
  }
  return null;
}

async function calloutOf(kind, file, name, mirrored) {
  const spec = kind.callouts[name];
  // Why: the Mac window lays a right-to-left language out only partly mirrored, so a mirrored callout misses.
  if (!spec || (kind.window && mirrored)) return "";
  const ratio = kind.ratio ?? 1;
  const png = await readFile(file);
  const captureWidth = describePng(png).width;
  const designWidth = { ipad13: 2732, iphone63: 1290 }[kind.slot] ?? captureWidth;
  const capture = captureWidth / designWidth;
  const found = spec.card ? findCard(png, spec.card, capture, mirrored) : null;
  // Why: a right-to-left screen is the left-to-right one mirrored, so its parts sit on the other side.
  const left0 = mirrored ? captureWidth - (spec.x + spec.w) * capture : spec.x * capture;
  const [x, y, w, h] = [left0, found?.y ?? spec.y * capture, spec.w * capture, found?.h ?? spec.h * capture];
  const [left, top, deviceWidth, padding] = kind.device;
  const fit = (deviceWidth - 2 * padding) / captureWidth;
  const scale = spec.s * (ratio / capture);
  const [drawnWidth, drawnHeight] = [Math.round(w * scale), Math.round(h * scale)];
  const boxLeft = Math.max(40, Math.min(kind.width - drawnWidth - 40, Math.round(left + padding + (x + w / 2) * fit - drawnWidth / 2)));
  const boxTop = Math.round(top + padding + (y + h / 2) * fit - drawnHeight / 2);
  return `<div class="crop" style="left:${boxLeft}px;top:${boxTop}px;width:${drawnWidth}px;height:${drawnHeight}px;background-image:url(${pathToFileURL(file).href});background-size:${Math.round(captureWidth * scale)}px auto;background-position:${-Math.round(x * scale)}px ${-Math.round(y * scale)}px"></div>`;
}

function extraOf(kind, entry, words, people) {
  if (entry.extra === "tickets") {
    const [first, second] = SHARE_PEOPLE[entry.copy];
    const top = kind.height - (kind.wide ? 500 : 496);
    return [[36, -6, first], [kind.width - 326, 6, second]].map(([left, angle, person]) =>
      `<div class="ticket" style="left:${left}px;top:${top}px;transform:rotate(${angle}deg)"><div class="stub"><img src="${pathToFileURL(join(FACES, `person${person}.jpg`)).href}" alt=""><span>${escapeHtml(people[person].name)}</span><b>${escapeHtml(people[person].share)}</b></div></div>`).join("");
  }
  if (entry.extra === "receipt") {
    return `<img class="receipt" src="${pathToFileURL(RECEIPT).href}" alt="" style="left:50px;top:${kind.height - 960}px;width:470px">`;
  }
  if (entry.extra === "pills") {
    return `<div class="pills" style="left:60px;top:${kind.height - 300}px;width:${kind.width - 120}px"><div class="pill">${escapeHtml(words.noAccount)}</div><div class="pill">${escapeHtml(words.worksOffline)}</div></div>`;
  }
  return "";
}

async function panelOf(device, tag, entry, words, people, offset) {
  const kind = DEVICES[device](entry.screen);
  const file = join(kind.play ? PLAY_CAPTURES : CAPTURES, kind.slot, tag, `${FILES[entry.screen]}.png`);
  if (!existsSync(file)) {
    const how = kind.play
      ? "./gradlew :android:store_screenshots:testDebugUnitTest -PplayArt --rerun"
      : "scripts/store-screenshots/listing-art/capture.sh";
    throw new Error(`No capture at ${file}: run ${how}`);
  }
  const [titleLeft, titleTop, titleSize] = kind.title;
  const [left, top, width, padding, radius] = kind.device;
  const lines = words[entry.copy].split("|");
  const title = (kind.wide ? [lines.join(" ")] : lines).map((line) => `<span>${escapeHtml(line)}</span>`).join("");
  const rtl = isRightToLeft(tag);
  const side = rtl ? "right" : "left";
  // Why: the stage is left to right so the panels sit side by side; the words in it are read in the language's own
  // direction, or Arabic's and Hebrew's closing period lands at the start of the line.
  return {
    kind,
    html: `<section class="panel" dir="${rtl ? "rtl" : "ltr"}" style="left:${offset}px;width:${kind.width}px;height:${kind.height}px"><h1 data-width="${kind.width - 2 * titleLeft}" style="${side}:${titleLeft}px;top:${titleTop}px;font-size:${titleSize}px">${title}</h1>${kind.window ? `<img class="window" src="${pathToFileURL(file).href}" alt="" style="left:${left}px;top:${top}px;width:${width}px">` : `<div class="device" style="left:${left}px;top:${top}px;width:${width}px;padding:${padding}px;border-radius:${radius}px"><img src="${pathToFileURL(file).href}" alt="" style="border-radius:${radius - padding}px"></div>`}${await calloutOf(kind, file, entry.callout, side === "right")}${extraOf(kind, entry, words, people)}</section>`,
  };
}

const CSS = `
*{box-sizing:border-box;margin:0}html,body{overflow:hidden;background:#0068b5}
body{position:relative;color:#fff;font-family:-apple-system,BlinkMacSystemFont,"SF Pro Display",sans-serif;-webkit-font-smoothing:antialiased}
#stage{position:absolute;top:0}.band{position:absolute;left:0;top:0}.panel{position:absolute;top:0;overflow:hidden}
h1{position:absolute;font-weight:800;letter-spacing:-.03em;line-height:1.08}h1 span{display:block;width:max-content;white-space:nowrap}
:lang(ar) h1,:lang(he) h1,:lang(hi) h1,:lang(th) h1,:lang(ur) h1,:lang(fa) h1,:lang(ta) h1,:lang(te) h1,:lang(mr) h1,:lang(gu) h1,:lang(pa) h1,:lang(ml) h1{letter-spacing:0;line-height:1.3}
.device{position:absolute;background:linear-gradient(145deg,#55606c,#15191e 35%,#0b0d10 70%,#434d58);box-shadow:0 80px 150px rgba(0,24,60,.55),0 20px 50px rgba(0,24,60,.4)}
.device img{display:block;width:100%}
.window{position:absolute;filter:drop-shadow(0 60px 110px rgba(0,20,50,.55)) drop-shadow(0 0 2px rgba(0,0,0,.6))}
.crop{position:absolute;overflow:hidden;border-radius:44px;background-repeat:no-repeat;background-color:#1c1c1e;box-shadow:0 50px 100px rgba(0,20,50,.6),0 0 0 4px rgba(255,255,255,.22)}
.receipt{position:absolute;border-radius:16px;transform:rotate(-8deg);box-shadow:0 60px 110px rgba(0,20,50,.55)}
.ticket{position:absolute;filter:drop-shadow(0 30px 40px rgba(0,24,60,.45))}
.stub{width:290px;height:430px;padding:40px 12px 0;display:flex;flex-direction:column;align-items:center;background:#fff;color:#06283f;-webkit-mask:radial-gradient(circle 28px at 0 50%,transparent 98%,#000) left/51% 100% no-repeat,radial-gradient(circle 28px at 100% 50%,transparent 98%,#000) right/51% 100% no-repeat}
.stub img{width:160px;height:160px;border-radius:50%;object-fit:cover;margin-bottom:18px}
.stub span{font-size:46px;font-weight:600;margin-bottom:10px;white-space:nowrap}.stub b{font-size:52px;font-weight:800;letter-spacing:-.02em;color:#00639c;white-space:nowrap}
.pills{position:absolute;display:flex;justify-content:center;align-items:center;gap:26px;flex-wrap:wrap}
.pill{padding:30px 58px;border-radius:80px;background:#fff;color:#00507f;font-size:58px;font-weight:800;letter-spacing:-.015em;white-space:nowrap;box-shadow:0 40px 80px rgba(0,20,50,.5)}`;

// Shrinks what is too long for its place, then says what still doesn't fit. The tickets' names, and their amounts,
// shrink together, so the tickets of one image stay one size.
const FIT = `(() => {
  const shrink = (elements, width) => { if (!elements.length) return; let size = parseFloat(getComputedStyle(elements[0]).fontSize);
    while (elements.some((element) => element.scrollWidth > width) && size > 16) { size -= 1; elements.forEach((element) => { element.style.fontSize = size + 'px'; }); } };
  const titles = [...document.querySelectorAll('h1')];
  const tooWide = (title) => [...title.children].some((line) => line.offsetWidth > Number(title.dataset.width));
  titles.forEach((title) => { let size = parseFloat(getComputedStyle(title).fontSize);
    while (tooWide(title) && size > 40) { size -= 1; title.style.fontSize = size + 'px'; } });
  shrink([...document.querySelectorAll('.stub span')], 262);
  shrink([...document.querySelectorAll('.stub b')], 262);
  const pills = [...document.querySelectorAll('.pill')];
  pills.forEach((pill) => shrink([pill], pill.parentElement.offsetWidth));
  return [
    ...titles.filter(tooWide).map((title) => title.textContent),
    ...[...document.querySelectorAll('.stub span, .stub b')].filter((text) => text.scrollWidth > 262).map((text) => text.textContent),
    ...pills.filter((pill) => pill.scrollWidth > pill.parentElement.offsetWidth).map((pill) => pill.textContent),
  ];
})()`;

function stageOf(panels) {
  const total = panels.reduce((sum, panel) => sum + panel.kind.width, 0);
  const height = Math.max(...panels.map((panel) => panel.kind.height));
  const { band, notch } = panels[0].kind;
  let offset = 0;
  const offsets = panels.map((panel) => { const start = offset; offset += panel.kind.width; return start; });
  const arcs = offsets.slice(1).map((x) => `L${x - notch},${band} A${notch},${notch} 0 0 0 ${x + notch},${band}`).join(" ");
  const path = `M0,${band} ${arcs} L${total},${band}`;
  const stops = panels.map((panel, index) => `${COLORS[index]} ${((100 * (offsets[index] + panel.kind.width / 2)) / total).toFixed(2)}%`).join(",");
  const svg = `<svg class="band" width="${total}" height="${height}" viewBox="0 0 ${total} ${height}"><path d="${path} L${total},${height} L0,${height} Z" fill="rgba(255,255,255,.13)"/><path d="${path}" fill="none" stroke="rgba(255,255,255,.55)" stroke-width="7" stroke-dasharray="30 26" stroke-linecap="round"/></svg>`;
  return { total, height, offsets, stops, svg };
}

async function main() {
  const copy = await table(join(HERE, "copy.tsv"));
  if (!existsSync(FACTS)) {
    throw new Error(`No ${FACTS}: run ./gradlew :android:store_screenshots:testDebugUnitTest -PcreativeAssets --rerun`);
  }
  const facts = JSON.parse(await readFile(FACTS, "utf8"));
  // Why: an empty variable means every one, as capture.sh reads ART_LOCALES.
  const tags = process.env.ART_LOCALES ? process.env.ART_LOCALES.split(",") : [...copy.keys()];
  const devices = process.env.ART_DEVICES ? process.env.ART_DEVICES.split(",") : Object.keys(DEVICES);
  const unknownTags = tags.filter((tag) => !copy.has(tag) || !facts[tag]);
  if (unknownTags.length) throw new Error(`copy.tsv or facts.json has no ${unknownTags.join(", ")}`);
  const unknownDevices = devices.filter((device) => !DEVICES[device]);
  if (unknownDevices.length) throw new Error(`No device ${unknownDevices.join(", ")}: one of ${Object.keys(DEVICES).join(", ")}`);
  await rm(PAGES, { recursive: true, force: true });
  await mkdir(PAGES, { recursive: true });
  const chrome = await launchChrome();
  try {
    for (const device of devices) {
      for (const tag of tags) {
        const play = device.startsWith("play_");
        const mac = device === "mac";
        const story = (play ? PLAY_STORY : mac ? MAC_STORY : STORY).filter((entry) => (!entry.only || entry.only === tag) && entry.except !== tag);
        if (story.length !== (play ? 8 : 10)) throw new Error(`${tag} has ${story.length} images`);
        const panels = [];
        let offset = 0;
        for (const entry of story) {
          const panel = await panelOf(device, tag, entry, copy.get(tag), facts[tag].people, offset);
          panels.push(panel);
          offset += panel.kind.width;
        }
        const { total, height, offsets, stops, svg } = stageOf(panels);
        const folder = join(OUTPUT, device, tag);
        await mkdir(folder, { recursive: true });
        for (const [index, panel] of panels.entries()) {
          const page = join(PAGES, `${device}-${tag}-${index}.html`);
          const dir = isRightToLeft(tag) ? "rtl" : "ltr";
          await writeFile(page, `<!doctype html><html lang="${tag}" dir="${dir}"><head><meta charset="utf-8"><style>${CSS}html,body{width:${panel.kind.width}px;height:${panel.kind.height}px}</style></head><body><div id="stage" dir="ltr" style="left:${-offsets[index]}px;width:${total}px;height:${height}px;background:linear-gradient(90deg,${stops})">${svg}${panel.html}</div></body></html>`);
          await chrome.setViewport(panel.kind.width, panel.kind.height, 1);
          await chrome.open(pathToFileURL(page).href);
          await chrome.waitFor(`document.readyState === 'complete' && document.fonts.status === 'loaded' && [...document.images].every((image) => image.complete && image.naturalWidth > 0)`);
          const file = join(folder, `${String(index + 1).padStart(2, "0")}_${story[index].copy}.png`);
          const overflowing = await chrome.evaluate(FIT);
          if (overflowing.length) console.warn(`${file}: still too long for its place: ${overflowing.join(" / ")}`);
          await screenshotOpaque(chrome, file, panel.kind.width, panel.kind.height);
        }
        console.log(`Drew ${device} ${tag}`);
      }
    }
  } finally {
    await chrome.close();
  }
}

await main();
