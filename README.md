# Copper Dex

A Pokédex-styled home screen for Android / Google TV (built and tested on a TCL G10 Google TV, Android 12).

- **Native Android app**, no WebView or WebGL. The device body is one pre-rendered image (`app/src/main/assets/shell.png`); the screens, clock, list and keys are drawn on top with Canvas.
- **Left screen:** the focused app's own banner, full size. **Right screen:** a numbered Pokédex list (tabs FAVOURITES / INPUTS / ALL APPS, A-Z rail). **Six keys:** Settings, Network, Inputs, Live TV, Voice, Power (standby clock).
- **Remote:** Up/Down move, Left/Right switch tabs, OK opens, hold OK adds/removes a favourite, Up from the first row or Down from the last reaches the keys, Back goes to the first tab.
- Launcher icon and TV banner are a Poké Ball style ball drawn for this project.

## Build
`gradle :app:assembleDebug` (JDK 17, Android SDK 34). CI builds a debug APK on every push to `main` and on pull requests (artifact `Copper-Dex-debug-apk`); each build is signed with a fresh debug key, so uninstall the old APK before installing a new one.

## Install and make it Home
See [docs/HOME-CHECKLIST.md](docs/HOME-CHECKLIST.md) and [docs/HOME-SETUP.md](docs/HOME-SETUP.md). Tools: `tools/survey-tv.sh` (read-only survey), `tools/install-apk.sh`, `tools/set-home.sh`, `tools/restore-home.sh`, `tools/debloat.sh`.

## Design sources
The body image is rendered by `tools/render-shell.js` from the three.js scene in `tools/shell-scene/`. The design preview is `tools/mock-preview.html`. See THIRD-PARTY-NOTICES.md for licences.
