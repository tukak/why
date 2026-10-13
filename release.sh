#!/usr/bin/env bash
# Usage: ./release.sh 1.2.3 "What changed, one line for the store"
# Sets versionName/versionCode, writes the English changelog, runs tests and lint, commits and tags v1.2.3,
# builds the signed APK (for GitHub and F-Droid) and AAB from that commit, then pushes, creates the GitHub release
# and uploads the AAB to the Google Play production track.
set -euo pipefail

version=${1:-}
notes=${2:-}
if [[ ! $version =~ ^([0-9]+)\.([0-9]{1,2})\.([0-9]{1,2})$ ]] || [[ -z $notes ]]; then
    echo "Usage: ./release.sh X.Y.Z \"What changed\" (minor and patch below 100)" >&2
    exit 1
fi
code=$(( BASH_REMATCH[1] * 10000 + 10#${BASH_REMATCH[2]} * 100 + 10#${BASH_REMATCH[3]} ))

cd "$(dirname "$0")"
gradle=app/build.gradle.kts
play_key=~/.android/keys/play-api.json
[[ -f $play_key ]] || { echo "Missing Google Play API key $play_key." >&2; exit 1; }
[[ $(git branch --show-current) == master ]] || { echo "Release from master." >&2; exit 1; }
[[ -z $(git status --porcelain) ]] || { echo "Commit or stash your changes first." >&2; exit 1; }
git fetch -q origin
[[ $(git rev-parse HEAD) == $(git rev-parse origin/master) ]] || { echo "master differs from origin/master." >&2; exit 1; }
git rev-parse -q --verify "refs/tags/v$version" >/dev/null && { echo "Tag v$version exists." >&2; exit 1; }

current=$(sed -nE 's/^ *versionCode = ([0-9]+)$/\1/p' $gradle)
(( code > current )) || { echo "versionCode $code must be higher than $current." >&2; exit 1; }

sed -i.bak -E "s/^( *versionCode = )[0-9]+$/\1$code/; s/^( *versionName = )\"[^\"]*\"$/\1\"$version\"/" $gradle && rm $gradle.bak
changelog=fastlane/metadata/android/en-US/changelogs/$code.txt
mkdir -p "$(dirname $changelog)"
printf '%s\n' "$notes" > $changelog

./gradlew -q :app:testDebugUnitTest :app:lintDebug

git add $gradle $changelog
git commit -q -m "Release $version"
git tag -a "v$version" -m "Release $version"

# Built from the tagged commit and signed by Gradle, so it matches F-Droid's reproducible build.
./gradlew -q :app:assembleRelease :app:bundleRelease
apk=app/build/outputs/apk/release/why-$version.apk
cp app/build/outputs/apk/release/app-release.apk "$apk"

git push -q origin master "v$version"
gh release create "v$version" --title "Why? $version" --notes-file $changelog "$apk"
fastlane supply --package_name cz.kutner.why --json_key $play_key --track production \
    --aab app/build/outputs/bundle/release/app-release.aab --skip_upload_metadata --skip_upload_images --skip_upload_screenshots
echo "Released $version (versionCode $code) to GitHub and Google Play. F-Droid builds the tag."
