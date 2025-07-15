#!/bin/bash
# Generates CHANGELOG.md for the IntelliJ plugin based on git tags
set -euo pipefail

TAG="${1:-}"
if [[ -z "$TAG" ]]; then
  TAG="$(git describe --tags --abbrev=0)"
fi
PREV_TAG=""
if git rev-parse "$TAG^" >/dev/null 2>&1; then
  PREV_TAG="$(git describe --tags --abbrev=0 "$TAG^" 2>/dev/null || true)"
fi

CHANGELOG_FILE="intellij/CHANGELOG.md"
{
  echo "# IntelliJ Plugin Release Notes"
  echo
  echo "## $TAG"
  if [[ -n "$PREV_TAG" ]]; then
    git log "$PREV_TAG".."$TAG" --pretty=format:'- %s'
  else
    git log "$TAG" --pretty=format:'- %s'
  fi
} > "$CHANGELOG_FILE"

printf "Generated %s for tag %s\n" "$CHANGELOG_FILE" "$TAG"
