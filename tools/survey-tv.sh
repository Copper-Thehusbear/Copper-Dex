#!/usr/bin/env bash
# Read-only survey of a TV over adb. Changes nothing on the device.
# Usage: tools/survey-tv.sh <tv-ip>[:port]   (or no argument if already connected)
set -u

TARGET="${1:-}"
OUT="tv-survey-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$OUT"

command -v adb >/dev/null || { echo "adb not found. Install Android Platform Tools."; exit 1; }
[ -n "$TARGET" ] && { case "$TARGET" in *:*) ;; *) TARGET="$TARGET:5555";; esac; adb connect "$TARGET" >"$OUT/connect.txt" 2>&1; cat "$OUT/connect.txt"; }
ADB=(adb); [ -n "$TARGET" ] && ADB=(adb -s "$TARGET")
"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "No device. Approve the debugging prompt on the TV and retry."; exit 1; }

sh() { "${ADB[@]}" shell "$@" 2>&1; }
# "run name sh -c 'script'" sends the script as one string so the TV shell parses it, not adb.
run() {
  local name="$1"; shift; echo "- $name"
  if [ "${1:-}" = sh ] && [ "${2:-}" = -c ]; then sh "$3" >"$OUT/$name.txt"; else sh "$@" >"$OUT/$name.txt"; fi
}

echo "Writing results to $OUT/"

# 1. Device and firmware
run props getprop
run build-summary sh -c 'for p in ro.product.manufacturer ro.product.model ro.product.device ro.build.fingerprint ro.build.version.release ro.build.version.sdk ro.build.version.security_patch ro.boot.verifiedbootstate ro.boot.flash.locked ro.boot.vbmeta.device_state ro.debuggable ro.secure; do echo "$p=$(getprop $p)"; done'
run display wm size
run display-density wm density
run gpu-webview sh -c 'dumpsys SurfaceFlinger | head -60; echo ---; dumpsys webviewupdate'

# 2. Launchers and home (what would replace or coexist with the stock home)
run home-activities cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
run home-all sh -c 'pm query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME 2>/dev/null || cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
run leanback-apps sh -c 'cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER'

# 3. Packages
run packages-all pm list packages -f
run packages-system pm list packages -s
run packages-third-party pm list packages -3
run packages-disabled pm list packages -d
run packages-tcl sh -c 'pm list packages | grep -i -E "tcl|tv|home|launcher|setting|input|tuner"'

# 4. Do the intents used by Copper Dex resolve on this TV?
probe() { local name="$1"; shift; echo "- intent-$name"; { echo "args: $*"; sh cmd package resolve-activity --brief "$@"; } >"$OUT/intent-$name.txt"; }
probe settings -a android.settings.SETTINGS
probe wifi -a android.settings.WIFI_SETTINGS
probe tv-input-settings -a android.settings.TV_INPUT_SETTINGS
probe display -a android.settings.DISPLAY_SETTINGS
probe sound -a android.settings.SOUND_SETTINGS
probe bluetooth -a android.settings.BLUETOOTH_SETTINGS
probe app-settings -a android.settings.APPLICATION_SETTINGS
probe usage-access -a android.settings.USAGE_ACCESS_SETTINGS
probe accessibility -a android.settings.ACCESSIBILITY_SETTINGS
probe voice-command -a android.intent.action.VOICE_COMMAND
probe assist -a android.intent.action.ASSIST
probe live-channels -a android.intent.action.VIEW -d content://android.media.tv/channel
probe tv-input-picker -a android.intent.action.VIEW -d content://android.media.tv/passthrough
for pkg in com.google.android.tv com.tcl.tv com.tcl.livetv; do
  echo "- leanback-launch $pkg"
  sh cmd package resolve-activity --brief -c android.intent.category.LEANBACK_LAUNCHER "$pkg" >"$OUT/launch-$pkg.txt"
done

# 5. TV inputs, channels, HDMI-CEC (power / standby options)
run tv-inputs dumpsys tv_input
run hdmi-control dumpsys hdmi_control
run cec-settings sh -c 'settings list global | grep -i -E "hdmi|cec"; settings list system | grep -i -E "hdmi|cec"'
run power dumpsys power
run input-methods ime list -s

# 6. "Continue watching" / Watch Next: can a third-party app read it?
run content-providers sh -c 'dumpsys package providers | grep -i -E "tv|watch|epg" | head -80'
run tv-provider-permissions sh -c 'dumpsys package com.android.providers.tv | grep -i -E "permission|READ_EPG|WRITE_EPG" | head -60'
run watch-next-query content query --uri content://android.media.tv/watch_next_program
run channels-query content query --uri content://android.media.tv/channel

# 7. Recently used apps (needs Usage Access for an app; shell side only)
run recent-tasks sh -c 'dumpsys activity recents | head -120'
run usage-stats dumpsys usagestats

# 8. Installed Copper Dex
run copperdex sh -c 'pm list packages com.copper.dex; dumpsys package com.copper.dex | head -80'

# 9. Screens (take these with the TV on the screen named; skipped if screencap fails)
snap() { echo "- screenshot-$1"; "${ADB[@]}" exec-out screencap -p >"$OUT/screenshot-$1.png" 2>/dev/null || rm -f "$OUT/screenshot-$1.png"; }
echo
echo "Screenshots: put the TV on each screen when asked, then press Enter."
for screen in home settings; do
  read -r -p "Show the TV $screen screen, then press Enter (or just Enter to skip): " _
  snap "$screen"
done

echo
echo "Done. Zip and share the folder: zip -r $OUT.zip $OUT"
echo "Nothing was changed on the TV."
