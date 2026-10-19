# Copper Dex

Custom Pokédex-inspired Android TV home launcher for **TCL V6C 55-inch Google TV**.

This branch replaces the former Shopee Affiliate codebase, workflows and database configuration. The starter application has an animated clamshell-style red shell, blue glass dual displays, D-pad navigation, live clock, Android TV application discovery, app launching, settings and Wi-Fi shortcuts.

Inspired by Gathin23/tv-vibehack (MIT). This is an independent implementation, not a vendored copy of their launcher. The UI is an initial functional adaptation of Copper's HTML concept; it is not yet a pixel-perfect translation of the complete original design.

## Build
Install Android Studio, JDK 17, and Android SDK Platform 34. Open this folder as a Gradle project, sync, and build the `app` module. This project requires Gradle 8.7 (Android Studio can install the matching Gradle distribution). Debug APK path: `app/build/outputs/apk/debug/app-debug.apk`.

Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, then launch Copper Dex from Apps. Test D-pad and OK, application launching, and returning to Home before assigning this application as the default launcher.

## Safety
No ADB debloating, no removal or disabling of vendor packages, no forced home selection, no network permission. The original launcher remains installed. Reset the default Home in system settings or uninstall with `adb uninstall com.copper.dex`.

## UI & platform
- `app/src/main/assets/index.html`: offline interface. Replace it with Copper's full original HTML once app bridge integration is complete.
- `app/src/main/java/com/copper/dex/MainActivity.java`: WebView host, Android package discovery and launch bridge.
- `CopperDex.apps()` returns JSON with launchable app labels, package names and base64 PNG icons.
- `CopperDex.open(pkg)`, `CopperDex.settings()`, `CopperDex.wifi()`, `CopperDex.inputs()`.

Limitations: the device firmware may restrict changing inputs and third-party Home selection. WebView 3D animation performance must be profiled on hardware. No TV tested yet.
