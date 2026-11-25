#!/usr/bin/env bash
#
# Check the environment. Run by .github/workflows/release-production.yml.
#
set -eo pipefail

# What gh says on stderr is kept apart from its answer, which is read as JSON and as a name below.
errors="$RUNNER_TEMP/environment-errors.txt"
if ! environment="$(gh api "repos/$GITHUB_REPOSITORY/environments/$RELEASE_ENVIRONMENT" 2> "$errors")"; then
  if grep -q "HTTP 404" "$errors"; then
    echo "::error::The $RELEASE_ENVIRONMENT environment doesn't exist: create it, with a deployment branch policy that only allows main. See docs/release-process.md#who-releases."
    exit 1
  fi
  cat "$errors"
  echo "::error::Could not read the $RELEASE_ENVIRONMENT environment, so nothing says which branches may deploy to it. Dispatch the release again."
  exit 1
fi
if [ "$(jq -r '.deployment_branch_policy.custom_branch_policies // false' <<< "$environment")" != true ]; then
  echo "::error::The $RELEASE_ENVIRONMENT environment doesn't limit its deployments to branches it names: any branch, or any protected one, could deploy. Set its deployment branches to 'Selected branches and tags', with main alone. See docs/release-process.md#who-releases."
  exit 1
fi
if ! allowed="$(gh api "repos/$GITHUB_REPOSITORY/environments/$RELEASE_ENVIRONMENT/deployment-branch-policies" --paginate --jq '.branch_policies[] | "\(.type):\(.name)"' 2> "$errors")"; then
  cat "$errors"
  echo "::error::Could not read the deployment branch policy of the $RELEASE_ENVIRONMENT environment. Dispatch the release again."
  exit 1
fi
# By type too: a tag named main is not the branch.
if [ "$allowed" != branch:main ]; then
  echo "::error::The $RELEASE_ENVIRONMENT environment has to let the branch main deploy, and nothing else: its deployment policy allows '$(paste -sd, - <<< "$allowed")'. See docs/release-process.md#who-releases."
  exit 1
fi
