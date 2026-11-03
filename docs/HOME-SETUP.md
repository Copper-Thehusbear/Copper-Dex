# Make Copper Dex the TV's Home

Sit in front of the TV for this, with the remote. Everything is reversible and nothing is uninstalled.

## Before you start
1. Install the APK (`tools/install-apk.sh <tv-ip> app-debug.apk`) and check it works from the Apps list.
2. TV and computer on the same Wi-Fi; Developer options > USB debugging on; `adb` in your PATH (Git Bash: `export PATH="$PATH:$HOME/platform-tools"`).
3. **Forget and re-join your Wi-Fi** on the TV (Settings > Network & Internet > your network > Forget, then join again). On Google TV, a network saved during first-run setup belongs to the setup wizard, and disabling the wizard deletes it. After re-joining it belongs to the system. `set-home.sh` checks this and stops if it is not safe.

## Switch Home
```
tools/set-home.sh <tv-ip>
```
It starts Copper Dex, disables the stock launcher and the setup wizard (they both answer Home), makes Copper Dex Home, and checks that Home really is Copper Dex. If it is not, it turns everything back on by itself. The commands to undo it are also saved in `~/copper-dex-recovery-<ip>.txt`.

Press Home on the remote, then restart the TV: it should boot straight into Copper Dex.

## Updating the app later
Builds are now signed with the key in `app/debug.keystore`, so a new build installs over the old one: run `tools/install-apk.sh <tv-ip> app-debug.apk` and do **not** uninstall first. (Uninstalling after Home has been switched would leave the TV with no Home until you install again.) The first build signed with this key still needs the old one uninstalled once, so do that before running `set-home.sh`.

## Optional: switch off apps you do not use
```
tools/debloat.sh <tv-ip>            # uses tools/lists/tcl-g10.txt
tools/debloat.sh <tv-ip> --undo     # turns them back on
```
Edit `tools/lists/tcl-g10.txt` first if you want more or fewer apps disabled. TV, inputs and the source panel are protected.

## Undo everything
```
tools/restore-home.sh <tv-ip>
tools/debloat.sh <tv-ip> --undo
```
If the TV ever shows a blank Home and the remote does nothing, connect `adb` and run `tools/restore-home.sh`.

## When you are done
`adb disconnect <tv-ip>:5555`, and turn USB/Network debugging off on the TV.

Note: if the TV powers on into the last HDMI input instead of Home, change that in the TV's Settings (power-on behaviour).
