# Copper Dex
Pokédex-inspired custom Android TV home launcher for TCL V6C (55-inch), preserving Copper's red clamshell concept.

## State
**Source prototype only; APK NOT YET BUILT or tested on device.** There is no system debloat or stock launcher modification. UI uses offline HTML/CSS/JS loaded into a native Java WebView host. It dynamically discovers launchable apps on Android TV and accepts D-pad / OK / Back controls. Open with desktop browser to view a demo (demo tiles don't launch apps).

## Build / test
Install JDK 17, Android SDK Platform 34 and Android build-tools, and Gradle 8.7 or use Android Studio's Gradle integration. In repository root: `gradle assembleDebug` (or use Android Studio Build APK). Output: `app/build/outputs/apk/debug/app-debug.apk`. No Gradle wrapper JAR is committed. Add one with `gradle wrapper --gradle-version 8.7` if desired.

Use `adb connect TV_IP:5555` only after enabling debugging on TV and accepting pairing prompts. Use `adb install -r app/build/outputs/apk/debug/app-debug.apk` and launch Copper Dex via Apps. Do not set it as default Home until fully tested. Uninstall with `adb uninstall com.copper.dex`. Check firmware compatibility before switching Home.

## UI and integration
- `app/src/main/assets/index.html`, `style.css`, `app.js`: Copper Dex red chassis, opening flap, dual screen, live clock, dynamic 4x3 grid with pages, app preview and system buttons.
- `app/src/main/java/com/copper/dex/MainActivity.java`: Android TV bridge, icon generation, app launches and D-pad handling.
- Offline asset files. A local Chakra Petch font can be added via licensed assets in a subsequent iteration; currently uses system fallback.
- Base project is an independent implementation inspired by tv-vibehack; source attribution is not a claim of code import.

## Known issues and next checks
- UI is a closer functional interpretation, not exact line-by-line import of the user-supplied HTML; further fidelity work is needed.
- Verify TV remote focus, performance and HDMI/input settings across TCL firmware.
- Build/compile has not been executed and may reveal integration issues.
- Launcher is not configured as default and no debloat operations are included.
