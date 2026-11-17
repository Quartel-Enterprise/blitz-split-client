# Store listing text

What Google Play and the App Store show about the app, besides the screenshots
([store-listing-screenshots.md](store-listing-screenshots.md)) and a version's "What's new"
([release-notes.md](release-notes.md)): its name, its descriptions and, on the App Store, its subtitle, keywords and
promotional text. It is versioned here, one file per language, so a change is reviewed like code and every language
changes together.

```bash
node scripts/release/store-listing.mjs check            # every language is there and within the stores' limits
fastlane upload_listing                                 # send it to both stores (needs Pierre's OK)
fastlane upload_listing platform:android                # one store: android, ios or mac
VALIDATE_ONLY=true fastlane upload_listing              # ask Play whether it would take it, change nothing
```

**Nothing goes to a store without Pierre's OK.** The Google Play half of the lane has run, from a Mac. The App Store
half had never run when this was written: its first run is a test of it too. It uploads text only: no build, no
screenshots, no submission for review. `VALIDATE_ONLY=true` is Google Play's check alone: the App Store has none, so
that run leaves the iPhone's and the Mac's listings out.

The stores are uploaded one after the other, Play, then the iPhone, then the Mac, and one that fails leaves the ones
before it changed: run the lane again for that platform alone once what stopped it is fixed (an App Store platform
with no editable version, for one).

**On Google Play, a change sent through the API takes every change waiting in Publishing overview to review with
it**, the declarations of App content included (Data safety, the content rating, the sign-in details). Play also
refuses any change sent through the API while a declaration contradicts the release that is live, for one the
Advertising ID's "No" over a bundle that still has the `AD_ID` permission. So while something in Publishing overview
must wait for a release, leave Play out of the upload, or take the changes back out of review afterwards (Publishing
overview › Remove changes, which keeps them as not yet sent).

From GitHub, where the stores' keys are, the [`store-listing`](../.github/workflows/store-listing.yml) workflow runs
the lane, by hand, for an administrator of the repository, on `main`:

```bash
gh workflow run store-listing.yml -f android=false          # the App Store alone
gh workflow run store-listing.yml -f validate_only=true     # ask Google Play, change nothing
```

Not while a release is running: both write through the same Play account and on the same App Store version, so the
workflow's first job refuses the run then. Its `version_code` input is the lane's option for a staged rollout, and
`app_version` names the App Store version to write on (`APP_VERSION`), created when it is missing.

On Play the upload reads the production track's release on its way. While the track holds more than one, as in a
staged rollout, say which with `fastlane upload_listing platform:android version_code:<its versionCode>`.

## A language's file

`store-listing/<language>.md`, for each language of the release notes (`LANGS` of `release-notes.mjs`) and for
Spain's Spanish, which has a listing of its own (`LISTING_LANGS` of the script): `en`, `pt` (Brazil), `pt-PT`, `es`
(Latin America), `es-ES`, `fr`, `de`, `it`, `pl`, `nl`, `ja`, `tr`, `id`, `ru`, `uk`. A `# Title` line, then these sections, in
this order:

| Section | Store | Limit | What it is |
| --- | --- | --- | --- |
| `## Play title` | Google Play | 30 | The app's name on Play. Starts with the app's name |
| `## Play short description` | Google Play | 80 | The line under the name, and the first thing searched |
| `## App Store name` | App Store | 30 | The app's name. Starts with the app's name |
| `## App Store subtitle` | App Store | 30 | The line under the name |
| `## App Store keywords` | App Store | 100 | Search terms split by commas, never shown |
| `## App Store promotional text` | App Store | 170 | The lines over the description, which change without a new version |
| `## Description` | Both | 4000 | Play's full description and the App Store's description, the same text |

The limits are in characters, as the stores count them. Every field but the description is one line.

## Writing one

- **The app's name** is Bora Rachar in Brazil's Portuguese and Blitz Split in every other language, Portugal's
  included. The check refuses a name that doesn't start with it.
- **Only what the app does.** A feature the listing promises and the app lacks is a rejection and a bad review:
  check a claim against `docs/product/` before writing it. The app moves no money, and the description says so.
- **No other store or platform by name** in the description, since it goes to both stores: "phone, tablet, computer
  and the web", not the name of a phone. The one exception is "Sign in with Google or Apple", the names of the two
  buttons the app shows on every platform. No competitor's name anywhere, keywords included: both stores remove
  listings for it.
- **No number that changes by itself**, such as how many languages the app is in: nothing would update it in twenty-six
  files.
- **Free, with no ads** is true today and written in every listing ([general.md](product/general.md)): when the app
  gains something paid, the listings change in the same pull request.
- **Search.** Play searches the title, the short description and the full description, so the words people type
  ("split bills", "expenses", "who owes whom") belong in all three, in natural sentences. The App Store searches the
  name, the subtitle and the keywords, and not the description: a keyword already in the name or the subtitle is
  wasted, and so is a space after a comma, both of which the check refuses. Prefer single words, which the App Store
  combines by itself.
- **Each language is written for its reader**, with the words people there search for (a "racha" in Brazil, a "talão"
  in Portugal, a "coloc" in France), not translated word for word. The two regional ones are written in full, not as
  a few words over the other: Portugal's Portuguese ("tu", "telemóvel", "partilhar") and Spain's Spanish ("os pagáis",
  "móvil", "ordenador", "ticket"), beside a Spanish that reads well from Mexico to Argentina ("se pagan", "celular",
  "computadora", "recibo").
- **Plain text.** The stores show no Markdown: paragraphs, lines in capitals as headings, and `•` for a list.

## How it reaches the stores

`store-listing.mjs stage --out <folder>` writes every listing in the layout fastlane uploads, a folder per store
locale: `play/<locale>/` with `title.txt`, `short_description.txt` and `full_description.txt` for supply, and
`app-store/<locale>/` with `name.txt`, `subtitle.txt`, `keywords.txt`, `promotional_text.txt` and `description.txt`
for deliver. `fastlane upload_listing` stages into a temporary folder and uploads it.

A language goes to one or more locales of each store (`PLAY_LOCALES` and `APP_STORE_LOCALES` of the script): Latin
America's Spanish is `es-419` and `es-US` on Play and `es-MX` on the App Store, and Spain's is `es-ES` on both. They
are the codes the screenshots
use (`StoreScreenshots.kt` of `build-logic`), and the two lists stay in step, which a test of
`store-listing.test.mjs` checks: a store attaches a locale's
screenshots, and a version's "What's new", to the listing of that same locale. A locale with a listing and no
screenshots of its own shows the default language's on Play and the primary language's on the App Store. Both tools add a locale the listing
doesn't have yet, so uploading the text is how a new language's listing is created.

On the App Store the name and the subtitle belong to the app, and the rest to a version: deliver writes on the
editable version of each platform, the iPhone's and the Mac's, which get the same text. `APP_VERSION` names another
version.

## Adding a language

Write `store-listing/<language>.md`, add the language to `LANGS` (the release notes need it too:
[release-notes.md](release-notes.md)) and its locales to `PLAY_LOCALES` and `APP_STORE_LOCALES`. A regional
listing of a language the release notes already have, as Spain's Spanish, goes in `LISTING_LANGS` alone, and takes
its locale from its language's list. The check then
passes, and the next `upload_listing` creates the listing in that locale. The `add-language` skill lists the rest.
