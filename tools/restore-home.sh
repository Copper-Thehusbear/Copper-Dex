#!/usr/bin/env bash
# Undo tools/set-home.sh: turn the stock launcher and the setup wizard back on and give Home back to the stock launcher.
# Usage: tools/restore-home.sh <tv-ip>[:port]
set -u
TV="${1:-}"
[ -n "$TV" ] || { echo "Usage: $0 <tv-ip>[:port]"; exit 1; }
case "$TV" in *:*) ;; *) TV="$TV:5555";; esac
command -v adb >/dev/null || { echo "adb not found. Install Android Platform Tools."; exit 1; }
STOCK="${STOCK_LAUNCHER:-com.google.android.apps.tv.launcherx}"
SETUP=com.google.android.tungsten.setupwraith
adb connect "$TV" >/dev/null 2>&1
tv() { adb -s "$TV" shell "$@" 2>&1 | tr -d '\r'; }
adb -s "$TV" get-state >/dev/null 2>&1 || { echo "TV not reachable at $TV."; exit 1; }
tv "pm enable --user 0 $STOCK"
tv "pm enable --user 0 $SETUP"
tv "cmd package set-home-activity --user 0 \$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME $STOCK | tail -n 1)"
sleep 1
echo "Home now resolves to: $(tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | tail -n 1)"
