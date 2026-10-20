# Copper Dex — Immersive Pokédex Android TV Launcher

Target: TCL V6C Google TV 55-inch. Native Java Android host with a local WebGL (three.js) UI.

## Approved visual direction
The approved final look is the real 3D Pokedex scene described in [docs/DESIGN-3D.md](docs/DESIGN-3D.md). The earlier brief in [docs/VISUAL-DIRECTION-V2.md](docs/VISUAL-DIRECTION-V2.md) explains why flat CSS boxes were rejected.

## Implemented in source (v0.3.0)
- Live **3D Pokedex** (three.js r128, bundled offline): glossy metallic body, slim chrome hinge, glass screens, raised silver lettering, reflective floor.
- **Closed device** with a dark lens and LEDs. **OK** runs the light sequence (blue flash and two blinks, red-yellow-green LEDs latching on), then the flap opens. **Back** closes it.
- **Open device**: left screen shows the focused app large (your own art in `assets/hero/`, else the TV banner, else the icon) with a date and time display on the body; right screen is a 4-column, vertically scrolling grid of the real installed apps.
- **Six buttons** under the grid: Settings, Network, Inputs, Live TV, Voice, Power (Standby screen).
- App launch with a camera push into the left screen and a fade, return handling via `copperResume`.
- Idle behaviour: breathing lens, randomly blinking LEDs with fades.
- Automatic quality fallback if the TV renders slowly.

## Not yet verified
Nothing here has run on the TCL. The Java changes (`banner`, `tv`, `voice`) were syntax-checked but not compiled; the scene was exercised against mocks only. WebGL performance in the Android WebView, the Live TV and Voice intents, and resume behaviour need hardware validation. See the checklist in [docs/DESIGN-3D.md](docs/DESIGN-3D.md).

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
