#!/usr/bin/env bash
# Sets the version in build.gradle and both manifests, and moves the
# CHANGELOG "Unreleased" section under the new version. Does not commit.
#
# Usage: scripts/bump-version.sh 0.5.1
set -euo pipefail

cd "$(dirname "$0")/.."

version="${1:-}"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Usage: $0 <major.minor.patch>" >&2
    exit 1
fi

perl -pi -e "s/^version '[^']*'/version '$version'/" build.gradle
for manifest in src/main/resources/manifest-chrome.json src/main/resources/manifest-firefox.json; do
    perl -pi -e "s/^(\s*\"version\": )\"[^\"]*\"/\${1}\"$version\"/" "$manifest"
done

if grep -q "^## \[$version\]" CHANGELOG.md; then
    echo "CHANGELOG.md already has a [$version] section, leaving it as is."
else
    today="$(date +%Y-%m-%d)"
    perl -pi -e "s/^## \[Unreleased\]\s*\$/## [Unreleased]\n\n## [$version] - $today\n/" CHANGELOG.md
fi

git --no-pager diff --stat
echo
echo "Next: review the diff, commit it, push develop, then run scripts/release.sh $version"
