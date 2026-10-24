# At-home checklist

Goal: update Copper Dex on the TCL and bring back survey results. Nothing here disables or removes any stock app.

## Before you start
- Computer and TV on the same Wi-Fi. `adb` installed (Android Platform Tools). `git` installed.
- On the TV: Settings > Device Preferences > About > press OK on **Build** seven times, then Developer options > **USB debugging** on (and Network/ADB debugging if shown). Note the TV's IP (Settings > Network).

## 1. Get the latest files
```
git clone https://github.com/Copper-Thehusbear/Copper-Dex   # first time
git -C Copper-Dex pull                                       # later
```

## 2. Survey the TV (read-only)
```
cd Copper-Dex
tools/survey-tv.sh <TV-IP>
```
Approve "Allow USB debugging?" on the TV (choose Always allow). When asked, show the TV's Home screen, then Settings, and press Enter each time. Zip the `tv-survey-...` folder and send it to Claude.

## 3. Install Copper Dex
Either let the script fetch the latest build (needs the `gh` CLI, logged in):
```
tools/install-apk.sh <TV-IP>
```
or download `Copper-Dex-debug-apk` from the GitHub Actions page (latest green run on `main`), unzip, then:
```
tools/install-apk.sh <TV-IP> path/to/app-debug.apk
```

## 4. Look for these on the TV
- Closed cover first; OK opens it with the light sequence; Back closes it.
- D-pad scrolls the app grid; left screen follows the focused app.
- App launch and return; the six buttons (Settings, Network, Inputs, Live TV, Voice, Power).
- Smoothness. Note any stutter. Send a photo or `adb logcat | grep -i -E "copper|chromium|webview"` for problems.

## 5. When finished
```
adb disconnect <TV-IP>:5555
```
and turn USB/Network debugging off on the TV. To remove the app: `adb uninstall com.copper.dex`.
