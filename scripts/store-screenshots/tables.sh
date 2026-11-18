#!/bin/bash

# The tables of scripts/store-screenshots (locales.tsv, screens.tsv, assets.tsv), for the scripts that capture the
# App Store screenshots (capture-ios.sh, capture-mac.sh), which source this file after setting TABLES to that folder.

# The rows of a table, without its # comment lines and its empty ones, as the iOS UI test reads them.
rows() { grep -v -e '^#' -e '^[[:space:]]*$' "$TABLES/$1.tsv"; }

# A column of the table's rows that a comma-separated list names by their first field (empty: every row), in the
# table's order. Fails on a name no row has: a capture narrowed to it would capture nothing and pass.
picked() {
  rows "$1" | awk -F'\t' -v table="$1" -v column="$2" -v list="$3" '
    BEGIN { count = split(list, names, ","); for (i = 1; i <= count; i++) asked[names[i]] = 1 }
    { seen[$1] = 1 }
    list == "" || ($1 in asked) { print $column }
    END {
      for (name in asked) if (!(name in seen)) { print table ".tsv has no " name > "/dev/stderr"; missing = 1 }
      exit missing
    }'
}
