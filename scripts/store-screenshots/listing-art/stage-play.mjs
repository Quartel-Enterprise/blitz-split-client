// Stages Google Play's listing art (store_listings/listing-art/play_*) where supply uploads it from:
// fastlane/metadata/android/<Play locale>/images/<image type>/, each image under the name compose.mjs gave it (the
// stores sort a shelf by file name, so the art's order is kept). Latin America's Spanish goes to each of Play's
// regional codes, as the listing text does (scripts/release/store-listing.mjs).
//
//   node scripts/store-screenshots/listing-art/stage-play.mjs
//   PLAY_DEVICES=play_phone,wear ART_LOCALES=en-US,ja node scripts/store-screenshots/listing-art/stage-play.mjs
//
// PLAY_DEVICES stages those image types alone (play_phone, play_tablet_7, play_tablet_10, wear; every one when unset):
// supply replaces only the image types it finds staged, so the others stay on Google Play as they are.
//
// The Wear OS watch's four screens of each language (scripts/store-screenshots/watch/draw-wear.sh, build/outputs/
// store-screenshots-wear/<locale>/) are staged as wearScreenshots, bare: Google Play takes no frame, text or background
// around a watch's.
//
// The foldable art (play_foldable_*) is not staged: supply has no image type for it, so it goes up by hand in the
// Play Console, from store_listings/listing-art/.
import { copyFile, mkdir, readdir, readFile, rm } from "node:fs/promises";
import { join } from "node:path";
import { PLAY_LOCALES } from "../../release/store-listing.mjs";

const ROOT = join(import.meta.dirname, "../../..");
const ART = join(ROOT, "store_listings/listing-art");
const METADATA = join(ROOT, "fastlane/metadata/android");
const IMAGE_TYPES = { play_phone: "phoneScreenshots", play_tablet_7: "sevenInchScreenshots", play_tablet_10: "tenInchScreenshots" };
const IMAGES_PER_DEVICE = 8;
const WEAR_ART = join(ROOT, "build/outputs/store-screenshots-wear");
const WEAR_IMAGE_TYPE = "wearScreenshots";
const WEAR_IMAGES = 4;

const rows = (await readFile(join(ROOT, "scripts/store-screenshots/locales.tsv"), "utf8")).split("\n").filter((row) => row.trim() && !row.startsWith("#"));
const languageOf = Object.fromEntries(rows.map((row) => row.split("\t")).map(([tag, language]) => [tag, language]));

// The image types to stage: PLAY_DEVICES's, or every one.
const PLAY_DEVICES = [...Object.keys(IMAGE_TYPES), "wear"];
const devices = process.env.PLAY_DEVICES ? [...new Set(process.env.PLAY_DEVICES.split(",").map((device) => device.trim()).filter(Boolean))] : PLAY_DEVICES;
const unknownDevices = devices.filter((device) => !PLAY_DEVICES.includes(device));
if (devices.length === 0 || unknownDevices.length) throw new Error(`PLAY_DEVICES names devices of ${PLAY_DEVICES.join(", ")}: ${process.env.PLAY_DEVICES}`);

// The locales to stage: ART_LOCALES's, or every one the art was drawn in. A language with no art stops the run before anything is replaced.
const wanted = process.env.ART_LOCALES ? process.env.ART_LOCALES.split(",") : Object.keys(languageOf);
const unknown = wanted.filter((tag) => !languageOf[tag]);
if (unknown.length) throw new Error(`locales.tsv has no ${unknown.join(", ")}`);

// Everything is checked before anything is replaced: a language with no art stops the run with the staging as it was.
const plan = [];
for (const tag of wanted) {
  const playLocales = PLAY_LOCALES[languageOf[tag]];
  if (!playLocales) throw new Error(`store-listing.mjs has no Play locale for ${tag}`);
  for (const [device, imageType] of Object.entries(IMAGE_TYPES)) {
    if (!devices.includes(device)) continue;
    const folder = join(ART, device, tag);
    const images = (await readdir(folder).catch(() => [])).filter((name) => name.endsWith(".png")).sort();
    if (images.length !== IMAGES_PER_DEVICE) throw new Error(`${device} ${tag} has ${images.length} images, Google Play's shelf takes ${IMAGES_PER_DEVICE}: run compose.mjs`);
    plan.push({ playLocales, imageType, folder, images });
  }
  // The watch's folder of the language, checked with the rest: a language drawn without it stops the run too.
  if (!devices.includes("wear")) continue;
  const wearFolder = join(WEAR_ART, tag);
  const wearImages = (await readdir(wearFolder).catch(() => [])).filter((name) => name.endsWith(".png")).sort();
  if (wearImages.length !== WEAR_IMAGES) throw new Error(`The Wear OS watch ${tag} has ${wearImages.length} images, Google Play's shelf takes ${WEAR_IMAGES} here: run watch/draw-wear.sh`);
  plan.push({ playLocales, imageType: WEAR_IMAGE_TYPE, folder: wearFolder, images: wearImages });
}
let staged = 0;
for (const playLocale of new Set(plan.flatMap((entry) => entry.playLocales))) {
  await rm(join(METADATA, playLocale, "images"), { recursive: true, force: true });
}
for (const { playLocales, imageType, folder, images } of plan) {
  for (const playLocale of playLocales) {
    const target = join(METADATA, playLocale, "images", imageType);
    await mkdir(target, { recursive: true });
    for (const image of images) {
      await copyFile(join(folder, image), join(target, image));
      staged++;
    }
  }
}
console.log(`Staged ${staged} images into ${METADATA}`);
