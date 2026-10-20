# Copper Dex — Immersive Pokédex Android TV Launcher

Target: TCL V6C Google TV 55-inch. Native Java Android host with a local HTML/CSS/JS UI.

## Approved visual direction
See [docs/VISUAL-DIRECTION-V2.md](docs/VISUAL-DIRECTION-V2.md) for the Product Photography redesign brief and acceptance criteria. This is the **approved target**, not the current APK appearance. The existing v0.2.0 build is retained as the functional baseline.

## Implemented in source
- Initial **closed Pokédex** with animated blue orb (breathing + scan sweep) and premium molded-shell effects.
- First **OK** opens the Pokédex; **Back** closes it.
- Animated 3D flap opens to a dual screen: left focused-app preview and right 4-column vertically scrollable app grid.
- App selection via D-pad and OK; the launcher performs zoom-to-left-display / fade-to-black before launching the actual Android TV app.
- Pokédex-themed Settings overlay with animation toggle, Wi-Fi, TV Inputs, and Android system Settings handoffs.
- Dynamic app discovery via PackageManager, clock, preview icons, browser preview fallback.
- Reduced-motion handling for CSS effects.

## Not yet verified
A debug APK has been built successfully in GitHub Actions run 39292265310, but has **not** been tested on actual TCL hardware. CSS 3D graphics on Android WebView, launcher behavior, transitions, package visibility, input intents and performance need hardware validation. The themed Settings overlay is an application-level hub: Android's own Settings, permission prompts, and firmware UI cannot be replaced by a regular third-party app.

## Build
Open repository in Android Studio, install JDK 17 and Android SDK 34, use Gradle 8.7, and build `app` debug APK. A Gradle wrapper is not included yet. The debug build should be installed as a normal app first (do **not** disable stock Home). See actual device-specific pairing and firmware restrictions before assigning as HOME.

No debloat operations are performed by this repository; the stock launcher is untouched.

## Follow-up QA
Compile, test the closed→OK→open state, Back handling, D-pad grid scrolling, app launch resume, Settings navigation, and failure recovery on TCL. Revisit premium panel fidelity against the user-provided original Pokédex design and profile GPU usage on 4K/60Hz display.

## Automated APK Build
GitHub Actions workflow `.github/workflows/build-apk.yml` assembles debug APK on pushes and manual workflow runs. Download `Copper-Dex-debug-apk` from the successful run's Artifacts section. Never publish a debug APK as a signed release.

## Device smoke test (no default Home change)
1. Enable developer debugging on the TCL television and approve the connected PC.
2. Install with `adb install -r app-debug.apk`.
3. Launch explicitly using `adb shell am start -n com.copper.dex/.MainActivity`.
4. Verify initial closed cover, first OK opens, D-pad list, Wi-Fi and settings shortcuts, Back closes, and launch transition.
5. Revert with `adb uninstall com.copper.dex`. Do not disable the stock Home launcher.
