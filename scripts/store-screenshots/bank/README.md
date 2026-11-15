# The image bank

Photos for the store listings and anything else that shows the app with sample data: profile photos in `faces/` and
group photos in `groups/`. Every one is **AI-generated**: the people don't exist and the places were never
photographed, so no image needs a release from anyone.

- 384x384 JPEG, the size of `../faces/person<n>.jpg`, which is more than an avatar or a group's tile is drawn at.
- A profile photo is a full square, never masked into a circle: the app does the cropping.
- A group photo has its subject in the middle and no readable text, so it still reads as a small rounded tile.
- The full-size originals (1024x1024 PNG) are the frames of the Figma file
  [Blitz Split – store listing image bank](https://www.figma.com/design/kFOXEypMV1iuTMNfwPtKl0), where they were made
  with Figma's image generation.

## Which ones the screenshots use

The store screenshots draw the five people of `../faces/` and three group photos of this bank, named in
`SampleTrips` (`TRIP_PHOTO` and each side group's `photo`): `groups/trip-lisbon-tram.jpg`, `groups/home-kitchen.jpg`
and `groups/event-soccer.jpg`. The App Store's UI test passes those same three files to the app
(`iosApp/UITests/StoreScreenshotsUITests.swift`): change the names in both places. The rest of the bank is there to
pick from, see [store-listing-screenshots.md](../../../docs/store-listing-screenshots.md).

## Names

`faces/<man|woman>-<what tells them apart>.jpg` and `groups/<trip|home|event|other>-<subject>.jpg`, the prefix being
the group's type in the app. Lowercase, hyphens, no numbers that only order them.

## Adding one

Generate a square image at 1024x1024, keep the original in the Figma file, and save a 384 px JPEG here:

```bash
sips -s format jpeg -s formatOptions 80 -Z 384 original.png --out scripts/store-screenshots/bank/groups/trip-kyoto-temple.jpg
```

What the prompts that made these had in common:

- **Faces:** "Photorealistic head-and-shoulders portrait of a fictional person who does not exist", who they are and
  what they wear, where they are, "looks like a real profile photo taken by a friend with a phone, not a studio shot",
  and "full-bleed square photo: the background fills the whole frame edge to edge, with no circular mask, no border,
  no white corners", without which some come back already cut into a circle.
- **Groups:** "Photorealistic photo of" the subject, "shot like a friend's phone photo, natural colors", "square
  composition with the subject centered, readable as a small rounded thumbnail", and "no people's faces, no text, no
  logos, no watermark".
