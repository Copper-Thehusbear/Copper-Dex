#!/usr/bin/env bash
# Make Copper Dex the TV's Home: the TV opens into it after booting, and the Home key returns to it.
# Nothing is uninstalled: the stock launcher and the setup wizard are only disabled, and
# tools/restore-home.sh turns both back on. If Home does not land on Copper Dex, this script undoes its own change.
#
# Usage: tools/set-home.sh <tv-ip>[:port]
# Before running it: install Copper Dex (tools/install-apk.sh), and if the Wi-Fi check below stops you,
# forget and re-join your Wi-Fi network in the TV's Settings > Network & Internet, then run this again.
set -u

TV="${1:-}"
[ -n "$TV" ] || { echo "Usage: $0 <tv-ip>[:port]"; exit 1; }
case "$TV" in *:*) ;; *) TV="$TV:5555";; esac
command -v adb >/dev/null || { echo "adb not found. Install Android Platform Tools."; exit 1; }

OUR=com.copper.dex
OUR_ACT=$OUR/.MainActivity
STOCK="${STOCK_LAUNCHER:-com.google.android.apps.tv.launcherx}"
SETUP=com.google.android.tungsten.setupwraith
HOME_Q='cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME'
REC="${HOME:-.}/copper-dex-recovery-$(printf '%s' "$TV" | tr -c 'A-Za-z0-9._-' '_').txt"

adb connect "$TV" >/dev/null 2>&1
ADB=(adb -s "$TV")
tv() { "${ADB[@]}" shell "$@" 2>&1 | tr -d '\r'; }
home_owner() { tv "$HOME_Q" | tail -n 1 | cut -d/ -f1; }
installed() { tv "pm list packages $1" | grep -qx "package:$1"; }

"${ADB[@]}" get-state >/dev/null 2>&1 || { echo "TV not reachable at $TV. Approve the debugging prompt on the TV and retry."; exit 1; }

echo "1/6 Checking the TV"
installed "$OUR" || { echo "Copper Dex is not installed. Run tools/install-apk.sh first."; exit 1; }
installed "$STOCK" || { echo "Stock launcher $STOCK not found. Set STOCK_LAUNCHER=<package> if your TV uses another one."; exit 1; }
CUR="$(home_owner)"
echo "    Home is currently: ${CUR:-unknown}"
if [ "$CUR" = "$OUR" ]; then echo "Copper Dex is already Home. Nothing to do."; exit 0; fi

echo "2/6 Checking the saved Wi-Fi network (disabling the setup wizard can delete a network it owns)"
if installed "$SETUP" && [ -z "${FORCE_SETUPWRAITH:-}" ]; then
  OWNERS="$(tv 'dumpsys wifi' | grep -o 'cname=[^ ]*' | sort -u)"
  if [ -z "$OWNERS" ]; then
    echo "    Could not read who owns the saved Wi-Fi networks, so I will not continue (the TV could lose Wi-Fi)."
    echo "    On Ethernet you can set FORCE_SETUPWRAITH=1 and run again."
    exit 1
  fi
  if printf '%s\n' "$OWNERS" | grep -q setupwraith; then
    echo "    A saved Wi-Fi network belongs to the setup wizard. Disabling it would delete that network and take"
    echo "    the TV offline. On the TV: Settings > Network & Internet > your Wi-Fi > Forget, then join it again."
    echo "    Then run this script again. (Nothing has been changed.)"
    exit 1
  fi
fi
echo "    OK"

echo "3/6 Saving how to undo this to $REC"
{
  echo "# Copper Dex Home setup on $TV. To undo, run tools/restore-home.sh $TV, or by hand:"
  echo "adb -s $TV shell pm enable --user 0 $STOCK"
  installed "$SETUP" && echo "adb -s $TV shell pm enable --user 0 $SETUP"
  echo "adb -s $TV shell cmd package set-home-activity --user 0 \$(adb -s $TV shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME $STOCK | tail -n 1)"
  echo "# Packages that were already disabled before this run:"
  tv 'pm list packages -d' | sed 's/^/# /'
} > "$REC" 2>/dev/null || echo "    (could not write the recovery file; continuing)"

echo "4/6 Starting Copper Dex"
OUT="$(tv "am start -W -n $OUR_ACT")"
printf '%s\n' "$OUT" | grep -q 'Status: ok' || { echo "Copper Dex did not start:"; echo "$OUT"; echo "Nothing was disabled."; exit 1; }

rollback() {
  echo "Rolling back..."
  tv "pm enable --user 0 $STOCK" >/dev/null
  installed "$SETUP" && tv "pm enable --user 0 $SETUP" >/dev/null
  tv "cmd package set-home-activity --user 0 \$(cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME $STOCK | tail -n 1)" >/dev/null
  echo "Home is now: $(home_owner). Recovery commands are in $REC"
}

echo "5/6 Handing Home over"
tv "pm disable-user --user 0 $STOCK" >/dev/null
if installed "$SETUP"; then tv "pm disable-user --user 0 $SETUP" >/dev/null; fi
tv "cmd package set-home-activity --user 0 $OUR_ACT" >/dev/null
sleep 2
NOW="$(home_owner)"
echo "    Home now resolves to: ${NOW:-nothing}"
if [ "$NOW" != "$OUR" ]; then
  echo "ERROR: Home did not land on Copper Dex."
  rollback
  exit 1
fi

echo "6/6 Optimising the app for fast start"
tv "cmd package compile -m speed -f $OUR" >/dev/null

echo
echo "Done. Press Home on the remote: it should open Copper Dex. Restart the TV to check it boots straight into it."
echo "To undo at any time: tools/restore-home.sh $TV"
echo "When you are finished with ADB, turn USB/Network debugging off on the TV (adb disconnect $TV)."
