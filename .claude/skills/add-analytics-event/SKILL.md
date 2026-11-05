---
name: add-analytics-event
description: "Instrument Analytics for a new or changed user action: decide each UiEvent case's EventAnalytics, extend TrackedViewModel for a new feature, call trackEvent for the Manual ones, and add each event's row to docs/analytics/README.md. Use whenever a feature adds or changes a UiEvent, or a new feature adds its ViewModel."
---

# Add analytics event

Follows [docs/analytics/README.md](../../../docs/analytics/README.md) ("Tracking a user action" and "What is never
sent"): read it first and apply it as written. This skill is the order of the work, not the rules.

## Workflow

### 1. Wire a new feature

- The feature's `XUiEvent` (and any sealed sub-hierarchy of it) implements `com.quare.blitzsplit.core.presentation.UiEvent`,
  whose `analytics` the compiler asks for on every case.
- Its ViewModel extends `TrackedViewModel<XUiEvent>(trackEvent)`, takes `trackEvent: TrackEvent` in its constructor,
  and writes its exhaustive `when` in `override fun handleEvent(event: XUiEvent)`; the UIs call the inherited `onEvent`.
  A ViewModel that hands a part of its events to a helper (`BillDetailComments.onEvent`) hands them from
  `handleEvent`.
- A ViewModel that refuses whole kinds of events for a while (a lock, a loading gate) overrides `isAccepted(event)`,
  and `onEvent` then neither tracks nor handles those events (`SplitBillViewModel` while it reads a receipt).
- The ViewModel's tests build it with a recording `TrackEvent` (`{ name, params -> events += name to params }`).

### 2. Decide each case

For each case of the `UiEvent`, in the order the file declares them:

- A tap, a pick or a confirmation is a user action: `EventAnalytics.Automatic` when its name and params come from the
  event alone, `EventAnalytics.Manual` when they need the result of handling it. Never `NotTracked` for a tap, even one
  that only opens a screen or a menu.
- `NotTracked` only for what is not a user action at all, as the README lists.
- Params: what kind of thing happened (an enum, a Boolean, a count), never a name, an amount nor an id.

### 3. Send the Manual ones

In `handleEvent`'s branch, once the result is known, call `trackEvent("the_name", mapOf(...))` with the very literal
`EventAnalytics.Manual(...)` declares.

### 4. Catalog

Add one row per new event to the table under `## Events` of `docs/analytics/README.md`, in the feature's place:
`` | `the_name` | `param` (values) | when it is sent | `:feature:x` | ``.

### 5. Verify

```bash
./scripts/ktlint.sh --format
./scripts/heavy-check.sh ./gradlew :feature:<name>:jvmTest :core:provider:analytics:jvmTest
```

`AnalyticsCatalogTest` fails on a name GA4 won't take, an event without its row, a row without its event, or a
`Manual` event no `trackEvent` call sends. The ViewModel's tests cover the events a case sends, Automatic and Manual
alike, as the testing docs ask for any new behavior.
