# Copper Dex — Visual Direction v2: Product Photography
Status: **Approved design direction; not yet implemented**  
Target: TCL V6C 55-inch Google TV; 1920 × 1080 composition.

## North Star
Copper Dex must look like a **real premium physical Pokédex photographed in a controlled product studio**, not a flat, cartoon-like CSS drawing or AAA-game UI. Keep the user-supplied classic red Pokédex reference as the **primary geometry reference**, combining recognizable Pokédex form with flagship consumer-electronics finish (precise industrial fit and finish, optical glass, refined reflections).

## Non-negotiable hardware geometry
- Two rigid connected halves and a believable hinge/spine. The closed cover must be the **same physical component** that opens into the right half; consistent pivots, dimensions and panel alignment.
- Raised upper module containing a large blue illuminated optical lens and three smaller, distinctly framed indicator LEDs. Raised-shell seam and real shadows under the top module.
- Deep left-hand **ivory display assembly with generous surrounding bezel and a chamfer/cut lower-left corner**, exactly in the spirit of the supplied reference photograph.
- Build visible layers: red molded shell → recessed/raised ivory housing → dark gasket/bezel → optically coated display glass → live display content.
- Right half has its own molded contours, screen glass, hardware controls and tactile lower features. The exterior closed cover needs a purposeful, photoreal exterior design, not plain block shapes.
- Physical cues: consistent wall thickness, seams, chamfers, metal hinge reflections, restrained speaker perforations, believable shadow contact and assembly tolerances.

## Material & lighting language
- Automotive-grade rich red painted polymer with controlled clear-coat highlights and subtle fine surface texture. Avoid plastic-looking saturated gradients and exaggerated cartoon outlines.
- Ivory satin injection-molded bezel, black gasket, glass with physically plausible soft specular reflections and legible underlying UI.
- Blue orb: multilayer convex lens, glass reflections, inner optics, restrained bloom. Runtime states: idle breathing; rotating scan highlight; stronger brief activation on opening/launch.
- Mini indicator LEDs: inset optical lenses, surrounding collar/gasket, subtle local illumination.
- Studio product lighting: broad softboxes, defined edge lights, realistic grounding shadow, polished premium appearance. No random emoji glyphs or pseudo-3D icon substitutions.

## Rendering/implementation strategy
- **Do not try to paint the complete physical product from flat CSS boxes again**; earlier mockups were rejected for looking toy-like.
- Establish one consistent physically modeled hardware asset or aligned photoreal render passes for CLOSED, OPEN and intermediate positions. Choose an approach that preserves a genuine hinge rotation without visual popping. WebView Android TV rendering performance must be measured.
- Overlay **live HTML/JS** on calibrated transparent screen regions for the app preview, 4-column scrollable app list (beyond 12 apps), D-pad focus and Copper settings. Use actual installed app icons at runtime on Android.
- Preserve local offline startup, no external font/image runtime dependencies, and low-end TV performance.
- Provide deterministic layout at 1920×1080 with letterbox scaling for other ratios.

## Interaction contract (keep from existing app)
1. Launch shows **closed** Pokédex; do not auto-open.
2. First D-pad OK activates orb and opens cover; only accept grid input after opening completes.
3. D-pad navigates apps; left display reflects focused icon; right grid scrolls vertically.
4. OK on app zooms toward LEFT glass screen, fades fully black, then hands over to app's own Android launch animation.
5. Back returns to closed view. Utility Settings is a themed launcher hub with system handoffs. Keep stock Google TV Home as a recovery path.
6. Keep Google Assistant/voice behavior native; **no Copper AI integration for now** and no cloud AI costs.

## Acceptance criteria for next design pass
- [ ] Closed and open states clearly look like photos of the **same real object**.
- [ ] Hinged cover rotation and thickness are convincing; no flat-card flip appearance.
- [ ] Raised upper module and the reference's ivory cut-corner left bezel are obvious at TV viewing distance.
- [ ] Orb highlights, glass reflections and inset LEDs read as real optics without overpowering text.
- [ ] At full-screen 1080p there are no ugly scaling artifacts, loose element seams, or stretching.
- [ ] Screen overlays remain sharp, D-pad usable, focused tile legible and app icons faithful.
- [ ] Animation is smooth on the actual TCL V6C and leaves stock home unchanged.
- [ ] Separate visual sign-off **before replacing current launcher UI or re-building release candidate**.

## Current baseline and scope
Current Java/WebView launcher **v0.2.0 debug APK** compiled successfully on GitHub Actions run 39292265310 (commit 353777c). It is an engineering baseline, **not an approved final visual** and has not been validated on device. The new photography-grade physical asset is **not yet created or integrated**.

Design reference: classic Pokédex photograph provided by the user in conversation (not copied into repository). Do not add the photo without an authorized reusable source asset.
