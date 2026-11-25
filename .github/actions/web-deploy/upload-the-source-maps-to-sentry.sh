#!/usr/bin/env bash
#
# Upload the source maps to Sentry. Run by .github/actions/web-deploy/action.yml.
#
set -eo pipefail

if [ -z "$SENTRY_ORG" ] || [ -z "$SENTRY_PROJECT" ]; then
  echo "::warning title=Web::SENTRY_AUTH_TOKEN is set without the SENTRY_ORG and SENTRY_PROJECT variables: no source map was uploaded. See docs/crash-reporting.md#the-web."
elif ! { npx --yes "@sentry/cli@${SENTRY_CLI_VERSION}" sourcemaps inject webApp/dist/assets \
  && npx --yes "@sentry/cli@${SENTRY_CLI_VERSION}" sourcemaps upload --release "$VERSION" webApp/dist/assets; }; then
  echo "::warning title=Web::The source maps didn't reach Sentry: this version's reports show the minified stack. See docs/crash-reporting.md#the-web."
fi
find webApp/dist -name '*.map' -delete
