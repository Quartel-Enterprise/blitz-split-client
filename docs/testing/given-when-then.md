# Test structure: Given / When / Then

Every test body is split into three sections, each marked with a `// Given`, `// When`, `// Then` comment and
separated by a blank line. (These comments are structure, which is why tests may have them while production code
keeps only `// Why:` comments.)

- **Given**: build the scenario and its inputs, usually one `prepareScenario(...)` call (see
  [prepare-scenario.md](prepare-scenario.md)), plus the events that only bring the screen to the state under test.
- **When**: the single action under test. One behavior per test.
- **Then**: assertions on the resulting state and the recorded interactions.

Each marker is exactly `// Given`, `// When` or `// Then`, alone on its line, once each and in that order. Every
section holds code: when the system under test is a field of the class, its inputs still go in the **Given** as
named `val`s rather than inline in the **When**.

```kotlin
@Test
fun `GIVEN dashes spaces and lower case WHEN normalizing THEN returns the 12 upper case characters`() {
    // Given
    val typed = "k7qm-4xpa 9tre"

    // When
    val normalized = RecoveryCodeFormat.normalize(typed)

    // Then
    assertEquals("K7QM4XPA9TRE", normalized)
}
```

Steps that only build the state go in the **Given**, even when they are events of the same ViewModel; a test that
would need two **When**s checks two behaviors and is split:

```kotlin
@Test
fun `GIVEN an unknown code WHEN restoring THEN shows the invalid code error`() = runTest(testDispatcher) {
    // Given
    prepareScenario(restoreError = InvalidRecoveryCodeException())
    viewModel.onEvent(RestoreUiEvent.OnCodeChanged("K7QM-4XPA-9TRE"))

    // When
    viewModel.onEvent(RestoreUiEvent.OnRestoreClicked)

    // Then
    assertEquals(RestoreError.INVALID_CODE, viewModel.uiState.value.error)
    assertFalse(viewModel.uiState.value.isRestoring)
}
```

When what is under test is the state a screen opens with, the **When** reads it:

```kotlin
// When
val state = viewModel.uiState.value
```

An expected failure is captured in the **When** and asserted in the **Then**:

```kotlin
// When
val result = repository.restoreWithRecoveryCode("K7QM-4XPA")

// Then
assertIs<InvalidRecoveryCodeException>(result.exceptionOrNull())
assertTrue(remote.exchangedCodes.isEmpty())
```

A class whose whole scenario is built in a `@BeforeTest` (a `prepareScenario` without parameters becomes one) has
nothing left to arrange and may leave out `// Given`. A `@BeforeTest` that only sets the main dispatcher doesn't
count: the tests still arrange their own scenario under `// Given`.

## Naming

Test names are backticked sentences that **mirror the three sections**:
`` `GIVEN <state> WHEN <action> THEN <outcome>` ``. The keywords are uppercase so they read as structure, and they
line up with the `// Given` / `// When` / `// Then` blocks of the body, so the name summarizes the test and the
body expands it.

```
GIVEN the identity belongs to another user WHEN connecting THEN signs in to that user
└──────────────── GIVEN ─────────────────┘ └─ WHEN ──┘ └────────── THEN ───────────┘
```

Write the name in the product's terms (a code, the groups, the old device, Google), not the code's (a `Result`, a
`StateFlow`). The name holds only letters, digits, spaces, `-` and `_`: no apostrophes or commas.

## Enforcement

Two custom ktlint rules (in [ktlint-rules](https://github.com/Quartel-Enterprise/ktlint-rules)) check every test
source set:

- `quare:test-name-given-when-then`: an `@Test` name matches `GIVEN … WHEN … THEN …` and holds only the characters
  above.
- `quare:test-body-sections`: the body has `// Given`, `// When` and `// Then`, once each and in that order
  (`// Given` optional in a class with a `@BeforeTest`).

Whether a name really describes its body, or a test checks a single behavior, is for the review.
