#!/usr/bin/env bash
# Install the Copper Dex debug APK on the TV and launch it. Does not touch the stock launcher.
# Usage: tools/install-apk.sh <tv-ip>[:port] [path/to/app-debug.apk]
# Without an APK path it tries `gh run download` for the latest successful build on main.
set -eu

TARGET="${1:-}"; APK="${2:-}"
[ -n "$TARGET" ] || { echo "Usage: $0 <tv-ip>[:port] [app-debug.apk]"; exit 1; }
case "$TARGET" in *:*) ;; *) TARGET="$TARGET:5555";; esac
command -v adb >/dev/null || { echo "adb not found. Install Android Platform Tools."; exit 1; }

if [ -z "$APK" ]; then
  command -v gh >/dev/null || { echo "No APK given and gh is not installed. Download 'Copper-Dex-debug-apk' from the GitHub Actions run, unzip it, and pass app-debug.apk."; exit 1; }
  DL="$(mktemp -d)"
  RUN="$(gh run list --branch main --workflow build-apk.yml --status success --limit 1 --json databaseId --jq '.[0].databaseId')"
  [ -n "$RUN" ] || { echo "No successful build found."; exit 1; }
  gh run download "$RUN" -n Copper-Dex-debug-apk -D "$DL"
  APK="$DL/app-debug.apk"
fi
[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }

adb connect "$TARGET"
adb -s "$TARGET" get-state >/dev/null || { echo "TV not reachable. Approve the debugging prompt on the TV and retry."; exit 1; }
adb -s "$TARGET" install -r "$APK"
adb -s "$TARGET" shell am start -n com.copper.dex/.MainActivity
echo "Installed and launched. Uninstall with: adb -s $TARGET uninstall com.copper.dex"
echo "Close network ADB when finished: adb disconnect $TARGET (and turn debugging off on the TV)."
