# Copper Dex 3D design (approved)

This is the approved final look. It is implemented in `app/src/main/assets/dex.js` (three.js r128, bundled in `assets/vendor`). A browser preview of the same scene, with demo data and a "Look" tuning panel, is in `docs/preview/copper-dex-3d-preview.html` (needs internet for three.js and the font).

## What it is
A real 3D Pokedex-style device, rendered live in the WebView: two body halves with a slim chrome hinge, glossy metallic red clear coat, ivory screen housing with a cut corner, glass screens (reflection streaks, edge darkening), raised silver "Copper The Husbear" lettering made from the font outlines, studio-light reflections, a mirrored glossy floor and a contact shadow.

## States and sequence
| State | What you see |
|---|---|
| closed | Folded device, centred. Blue lens dark, status LEDs off. |
| OK pressed (priming, 1.0 s) | Blue lens: flash, two blinks, steady. LEDs latch on one at a time (red, yellow, green) with a flash-in. |
| opening (1.6 s) | Blue burst, then steady blue; the flap swings open, the device slides to centre and rises into view. |
| open | Lens breathes (about 3.2 s cycle). LEDs blink at random with fades. Left screen shows the focused app large, right screen the app grid. |
| launching | Camera pushes into the left screen, fade to black, then the app starts. If the app never takes over, the view returns after 5 s. |
| closing | LEDs go out green, yellow, red; lens fades; flap closes. |

## Controls (D-pad)
- **OK** on the closed device starts the sequence. **Back** closes it (Back on the closed device does nothing).
- Arrows move through the 4-column, 3-row visible grid; it scrolls vertically.
- **Down** from the last grid row reaches the six buttons below the grid; **Up** returns. A yellow ring marks the focused button.
- Any key leaves Standby.

## The six buttons
| Button | Native call | Notes |
|---|---|---|
| Settings | `Settings.ACTION_SETTINGS` | |
| Network | `Settings.ACTION_WIFI_SETTINGS` | |
| Inputs | `android.settings.TV_INPUT_SETTINGS` | Opens TV input settings. Switching HDMI inputs directly needs system privileges a normal app does not have. |
| Live TV | `tv()` | Tries a TvContract channel view, then `com.google.android.tv`, `com.tcl.tv`, `com.tcl.livetv`, then input settings. **Verify on the TCL.** |
| Voice | `voice()` | `ACTION_VOICE_COMMAND`, falling back to `ACTION_ASSIST`. **Verify on the TCL.** |
| Power | none | A normal app cannot switch a TV off. It shows a dark Standby screen with a slowly moving clock; any key returns. Rename or remove it if you prefer. |

## Apps and pictures
- The app list comes from `CopperDex.apps()` (PackageManager). Pinned apps come first (`CONFIG.PINNED` in `dex.js`), the rest alphabetically.
- Left screen picture, in order: your own `assets/hero/<package>.png`, the app's TV banner (`CopperDex.banner(pkg)`), the app icon.
- Grid tiles use the app icon (96 px from the native side).

## Performance
Not measured on hardware yet. The scene renders a mirrored copy for the floor reflection and uses shadow maps. `watchPerf()` in `dex.js` times frames and, if the average stays slow, removes extras step by step: level 1 hides the reflection, level 2 turns off the shadow, level 3 lowers the render resolution. The pixel ratio is capped at 1.5.

## Tuning
- `CONFIG.AUTO_OPEN` (top of `dex.js`): `true` runs the opening sequence by itself 1.2 s after start. Default is off, so OK starts it.
- Red colour, gloss and exposure: `baseRed`, `mRed` (roughness, metalness) and `renderer.toneMappingExposure` in `dex.js`. The preview page has sliders for them; copy the values you like.
- Timings: `ledOnset`, `ledOff`, the `seq` table (lens flash and blinks), and the 1000 ms in `startPrime()`.

## Not verified yet
Built here only as static code: the Java side was syntax-checked but not compiled, and the whole scene was run against mocks, not a real WebView. Please test on the TCL: WebGL in the WebView, frame rate, Back, resume after launching an app, and the Live TV and Voice buttons.
