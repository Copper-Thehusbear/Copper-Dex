# Copper Dex — Immersive Pokédex Android TV Launcher

Target: TCL V6C Google TV 55-inch. Native Java Android host with a local HTML/CSS/JS UI.

## Implemented in source
- Initial **closed Pokédex** with animated blue orb (breathing + scan sweep) and premium molded-shell effects.
- First **OK** opens the Pokédex; **Back** closes it.
- Animated 3D flap opens to a dual screen: left focused-app preview and right 4-column vertically scrollable app grid.
- App selection via D-pad and OK; the launcher performs zoom-to-left-display / fade-to-black before launching the actual Android TV app.
- Pokédex-themed Settings overlay with animation toggle, Wi-Fi, TV Inputs, and Android system Settings handoffs.
- Dynamic app discovery via PackageManager, clock, preview icons, browser preview fallback.
- Reduced-motion handling for CSS effects.

## Not yet verified
No APK has been built or tested on actual TCL hardware. CSS 3D graphics on Android WebView, launcher behavior, transitions, package visibility, input intents and performance need hardware validation. The themed Settings overlay is an application-level hub: Android's own Settings, permission prompts, and firmware UI cannot be replaced by a regular third-party app.

## Build
Open repository in Android Studio, install JDK 17 and Android SDK 34, use Gradle 8.7, and build `app` debug APK. A Gradle wrapper is not included yet. The debug build should be installed as a normal app first (do **not** disable stock Home). See actual device-specific pairing and firmware restrictions before assigning as HOME.

No debloat operations are performed by this repository; the stock launcher is untouched.

## Follow-up QA
Compile, test the closed→OK→open state, Back handling, D-pad grid scrolling, app launch resume, Settings navigation, and failure recovery on TCL. Revisit premium panel fidelity against the user-provided original Pokédex design and profile GPU usage on 4K/60Hz display.
