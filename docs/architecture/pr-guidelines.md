## Pull Request Guidelines

- PR titles and descriptions must be written in **English**
- Self-assign to my user when creating a PR
- Write the description before the review, in terms of the problem, the expected behavior and the edge cases
  (offline, without an account, another platform): a reviewer, human or AI, only learns the intent of the change
  from it
- Say which platforms the change touches and how each was checked. A change to a shared ViewModel changes Android,
  Apple and the web at once, so it is checked on all three even when only one UI file changed
- Before opening the PR, run `/code-review` on the change
- There is no CI in this repository yet, so run locally what it would:

```bash
./scripts/ktlint.sh
./gradlew build jvmTest :koverVerifyCi assertModuleGraph
npm --prefix webApp run build
```

  and build the `BoraRachar-iOS` and `BoraRachar-macOS` schemes after `./iosApp/generate-project.sh`.
