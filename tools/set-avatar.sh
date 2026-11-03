#!/usr/bin/env bash
# Put a picture (your photo, a logo, anything .jpg/.png) in the big round socket of Copper Dex.
# The app cannot read your Google profile photo (Google does not give that to other apps), so pick the picture here.
# Usage: tools/set-avatar.sh <tv-ip>[:port] <image-file>       (remove it again with: tools/set-avatar.sh <tv-ip> --remove)
set -u
TV="${1:-}"; IMG="${2:-}"
[ -n "$TV" ] && [ -n "$IMG" ] || { echo "Usage: $0 <tv-ip>[:port] <image-file | --remove>"; exit 1; }
case "$TV" in *:*) ;; *) TV="$TV:5555";; esac
command -v adb >/dev/null || { echo "adb not found. Install Android Platform Tools."; exit 1; }
adb connect "$TV" >/dev/null 2>&1
ADB=(adb -s "$TV")
"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "TV not reachable at $TV."; exit 1; }
if [ "$IMG" = "--remove" ]; then
  "${ADB[@]}" shell "run-as com.copper.dex rm -f files/avatar.img"
  echo "Removed. Reopen Copper Dex."; exit 0
fi
[ -f "$IMG" ] || { echo "File not found: $IMG"; exit 1; }
"${ADB[@]}" push "$IMG" /data/local/tmp/copperdex-avatar >/dev/null || { echo "Could not copy the picture to the TV."; exit 1; }
"${ADB[@]}" shell "run-as com.copper.dex sh -c 'mkdir -p files; cat /data/local/tmp/copperdex-avatar > files/avatar.img'" || { echo "run-as failed (needs the debug APK)."; exit 1; }
"${ADB[@]}" shell "rm -f /data/local/tmp/copperdex-avatar"
echo "Done. Go to Home / reopen Copper Dex to see it."
