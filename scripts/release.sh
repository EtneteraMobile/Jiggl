#!/usr/bin/env bash
# Builds and publishes a release to the Chrome Web Store and Firefox Add-ons,
# then tags it and creates a GitHub Release.
#
# Usage: scripts/release.sh 0.5.0
#
# Credentials are read from .release.env (see .release.env.example).
# If one store upload succeeded and the other failed, rerun with
# SKIP_CHROME=1 or SKIP_FIREFOX=1 to skip the store that already has it.
set -euo pipefail

cd "$(dirname "$0")/.."

CWS_CLI="chrome-webstore-upload-cli@4"
WEB_EXT="web-ext@10"

die() { echo "error: $*" >&2; exit 1; }
step() { echo; echo "==> $*"; }

version="${1:-}"
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "usage: $0 <major.minor.patch>"

# --- Credentials ------------------------------------------------------------

[[ -f .release.env ]] || die ".release.env not found, copy .release.env.example and fill it in"
set -a; source .release.env; set +a

required=()
[[ "${SKIP_CHROME:-}" == 1 ]] || required+=(CWS_EXTENSION_ID CWS_PUBLISHER_ID CWS_CLIENT_ID CWS_CLIENT_SECRET CWS_REFRESH_TOKEN)
[[ "${SKIP_FIREFOX:-}" == 1 ]] || required+=(AMO_API_KEY AMO_API_SECRET)
# ${arr[@]+...} keeps an empty array from tripping `set -u` on macOS bash 3.2
for name in ${required[@]+"${required[@]}"}; do
    [[ -n "${!name:-}" ]] || die "$name is not set in .release.env"
done
command -v gh >/dev/null || die "gh CLI is required"
command -v npx >/dev/null || die "Node.js (npx) is required"

# --- Repository state -------------------------------------------------------

step "Checking repository state"
[[ "$(git branch --show-current)" == develop ]] || die "not on develop"
[[ -z "$(git status --porcelain)" ]] || die "working tree is not clean"
git fetch --quiet origin develop --tags
[[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/develop)" ]] || die "develop is not in sync with origin/develop, push or pull first"
if git rev-parse -q --verify "refs/tags/$version" >/dev/null; then
    die "tag $version already exists"
fi

gradle_version="$(sed -n "s/^version '\(.*\)'/\1/p" build.gradle)"
[[ "$gradle_version" == "$version" ]] || die "build.gradle has version $gradle_version, run scripts/bump-version.sh $version"
for manifest in src/main/resources/manifest-chrome.json src/main/resources/manifest-firefox.json; do
    grep -q "\"version\": \"$version\"" "$manifest" || die "$manifest does not have version $version"
done

# Changelog section for this version, without its heading
notes="$(awk -v v="$version" '
    index($0, "## [" v "]") == 1 { found = 1; next }
    found && /^## \[/ { exit }
    found { print }
' CHANGELOG.md | sed -e '/./,$!d')"
[[ -n "$notes" ]] || die "CHANGELOG.md has no entries under [$version], run scripts/bump-version.sh $version"

# --- Build ------------------------------------------------------------------

dist="build/distributions"
release_dir="build/release"
chrome_zip="$dist/Jiggl-$version-chrome.zip"
firefox_zip="$dist/Jiggl-$version-firefox.zip"
source_zip="$dist/Jiggl-$version-source.zip"

step "Testing and building"
./gradlew clean test
./gradlew bundle -Pbrowser=chrome
./gradlew bundle -Pbrowser=firefox
git archive --format=zip -o "$source_zip" HEAD

rm -rf "$release_dir"
mkdir -p "$release_dir/firefox"
unzip -q "$firefox_zip" -d "$release_dir/firefox"
printf '%s\n' "$notes" > "$release_dir/notes.md"
# AMO release notes are plain text, so turn "### Added" into "Added:"
NOTES="$(sed -e 's/^### \(.*\)$/\1:/' "$release_dir/notes.md")" node -e '
    const notes = process.env.NOTES;
    process.stdout.write(JSON.stringify({ version: { release_notes: { "en-US": notes } } }, null, 2));
' > "$release_dir/amo-metadata.json"

echo
echo "Release $version"
echo "----------------"
cat "$release_dir/notes.md"
echo
ls -lh "$chrome_zip" "$firefox_zip" "$source_zip"
echo
read -r -p "Publish $version to the stores, tag it and create the GitHub Release? [y/N] " answer
[[ "$answer" == y || "$answer" == Y ]] || die "aborted"

# --- Publish ----------------------------------------------------------------

if [[ "${SKIP_CHROME:-}" == 1 ]]; then
    step "Skipping Chrome Web Store"
else
    step "Uploading to Chrome Web Store and submitting for review"
    EXTENSION_ID="$CWS_EXTENSION_ID" \
    PUBLISHER_ID="$CWS_PUBLISHER_ID" \
    CLIENT_ID="$CWS_CLIENT_ID" \
    CLIENT_SECRET="$CWS_CLIENT_SECRET" \
    REFRESH_TOKEN="$CWS_REFRESH_TOKEN" \
        npx --yes --package="$CWS_CLI" chrome-webstore-upload --source "$chrome_zip"
fi

if [[ "${SKIP_FIREFOX:-}" == 1 ]]; then
    step "Skipping Firefox Add-ons"
else
    step "Uploading to Firefox Add-ons with source code"
    WEB_EXT_API_KEY="$AMO_API_KEY" \
    WEB_EXT_API_SECRET="$AMO_API_SECRET" \
        npx --yes "$WEB_EXT" sign \
            --channel listed \
            --source-dir "$release_dir/firefox" \
            --artifacts-dir "$release_dir/web-ext-artifacts" \
            --upload-source-code "$source_zip" \
            --amo-metadata "$release_dir/amo-metadata.json" \
            --approval-timeout 0
fi

# --- Tag and GitHub Release -------------------------------------------------

step "Tagging $version and creating the GitHub Release"
git tag -a "$version" -m "Release $version"
git push origin "$version"
gh release create "$version" "$chrome_zip" "$firefox_zip" "$source_zip" \
    --title "$version" \
    --notes-file "$release_dir/notes.md"

echo
echo "Done. Both stores now review $version; that usually takes 1 to 3 days."
