#!/usr/bin/env bash
# Disable (never uninstall) the apps listed in a file, or turn them back on with --undo.
# Usage: tools/debloat.sh <tv-ip>[:port] [--undo] [list-file]     (default list: tools/lists/tcl-g10.txt)
# The list has one package per line; "#" starts a comment. Packages the TV needs are refused.
set -u
TV="${1:-}"; shift || true
UNDO=0
if [ "${1:-}" = "--undo" ]; then UNDO=1; shift; fi
LIST="${1:-$(dirname "$0")/lists/tcl-g10.txt}"
[ -n "$TV" ] && [ -f "$LIST" ] || { echo "Usage: $0 <tv-ip>[:port] [--undo] [list-file]"; exit 1; }
case "$TV" in *:*) ;; *) TV="$TV:5555";; esac
command -v adb >/dev/null || { echo "adb not found. Install Android Platform Tools."; exit 1; }
adb connect "$TV" >/dev/null 2>&1
tv() { adb -s "$TV" shell "$@" 2>&1 | tr -d '\r'; }
adb -s "$TV" get-state >/dev/null 2>&1 || { echo "TV not reachable at $TV."; exit 1; }

# never touched, whatever the list says
PROTECT="com.copper.dex com.google.android.apps.tv.launcherx com.google.android.tungsten.setupwraith com.android.tv.settings com.android.providers.tv com.tcl.tv com.tcl.tvinput com.tcl.suspension com.tcl.dashboard com.tcl.systemserver com.tcl.providers.config com.tcl.initsetup com.tcl.factory.view com.tcl.autopair com.tcl.globalkeyoverlay com.google.android.katniss com.google.android.gms com.android.vending"

PKGS="$(tr -d '\r' < "$LIST" | sed -e 's/#.*$//' | awk 'NF{print $1}')"
INST="$(tv 'pm list packages')"
REC="${HOME:-.}/copper-dex-debloat-$(printf '%s' "$TV" | tr -c 'A-Za-z0-9._-' '_').txt"
if [ "$UNDO" = 0 ]; then { echo "# disabled before debloat:"; tv 'pm list packages -d'; } > "$REC" 2>/dev/null; echo "Recovery note: $REC"; fi

for p in $PKGS; do
  case " $PROTECT " in *" $p "*) echo "skip (protected)  $p"; continue;; esac
  printf '%s\n' "$INST" | grep -qx "package:$p" || { echo "skip (not installed) $p"; continue; }
  if [ "$UNDO" = 1 ]; then tv "pm enable --user 0 $p" >/dev/null && echo "enabled   $p"
  else tv "pm disable-user --user 0 $p" >/dev/null && echo "disabled  $p"; fi
done
echo "Done. Disabled packages now: $(tv 'pm list packages -d' | wc -l)"
