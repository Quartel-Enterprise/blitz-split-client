#!/usr/bin/env bash
#
# Check who releases. Run by .github/actions/release-admin/action.yml.
#
set -eo pipefail

errors="$RUNNER_TEMP/release-admin-errors.txt"
if ! permission="$(gh api "repos/$GITHUB_REPOSITORY/collaborators/$ACTOR/permission" --jq .permission 2> "$errors")"; then
  cat "$errors"
  if grep -q "HTTP 404" "$errors"; then
    echo "::error::Only an administrator of the repository releases a stable version or sends a listing to a store, and $ACTOR is not a user GitHub knows on it (an app or a bot can't). See docs/release-process.md#who-releases."
  else
    echo "::error::Could not read $ACTOR's permission on the repository, so nothing says they may. Run it again."
  fi
  exit 1
fi
if [ "$permission" != admin ]; then
  echo "::error::Only an administrator of the repository releases a stable version or sends a listing to a store, and $ACTOR has the '$permission' permission. See docs/release-process.md#who-releases."
  exit 1
fi
