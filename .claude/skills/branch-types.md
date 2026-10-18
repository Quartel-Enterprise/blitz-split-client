# Branch types

Shared by `create-pr` and `start-task`: used to pick the `<type>` prefix for a branch, commit, or
PR title, and the label of the pull request. Keep this table in sync in one place instead of copying
it into each skill.

| Type | When to use | PR label |
|---|---|---|
| `fix` | Corrects a bug or unintended behavior | `bug` |
| `feature` | Adds new functionality visible to the user | `feature` |
| `enhancement` | Improves existing functionality (performance, UX, accessibility, a layout for another screen size) | `enhancement` |
| `refactor` | Internal code restructuring with no user-facing change | `refactor` |
| `chore` | Build, CI, tooling, skills or documentation only | *(none)* |

The type describes the change for the person using Bora Rachar, whichever UI it lands in. A bug
fixed only in the SwiftUI app is still a `fix`, and a new screen drawn by Android, SwiftUI and React
is one `feature`.

If the changes or the user's request clearly point to one type, use it without asking. If it's
genuinely ambiguous, ask the user.
