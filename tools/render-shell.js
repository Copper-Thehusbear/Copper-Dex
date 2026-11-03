/* Renders the full-quality Pokedex body (blank screens) to a PNG plus the screen rectangles, for the native launcher.
   Usage: node tools/render-shell.js <out-dir> [width] [height]   (needs playwright and a chromium; see below) */
const path = require('path');
const fs = require('fs');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
(async () => {
  const out = process.argv[2] || '.';
  const W = parseInt(process.argv[3] || '1920', 10), H = parseInt(process.argv[4] || '1080', 10);
  const browser = await chromium.launch({
    executablePath: process.env.CHROMIUM || undefined,
    args: ['--use-gl=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist', '--allow-file-access-from-files', '--no-sandbox'],
  });
  const page = await browser.newPage({ viewport: { width: W, height: H } });
  const errors = [];
  page.on('pageerror', e => errors.push(String(e)));
  const url = 'file://' + path.resolve(__dirname, 'shell-scene/index.html') + '?tier=1&debug=1&fps=0&pr=1&aa=1&nodegrade=1&lowmem=0&keys=' + (process.env.KEYS || '2') + (process.env.CAP ? '&cap=' + process.env.CAP : '');
  await page.goto(url);
  await page.waitForFunction(() => window.__dex, null, { timeout: 60000 });
  await page.evaluate(() => window.__dex.setTarget(1));
  await page.waitForFunction(() => window.__dex.state() === 'open', null, { timeout: 120000 });
  await page.waitForTimeout(3000);
  const rects = await page.evaluate(() => {
    const d = window.__dex, T = d.THREE, m = d.mats, cam = d.camera;
    d.renderer.toneMappingExposure = 0.5;
    const dark = new T.Color(0x04070d);
    [m.L, m.R, m.C, m.N].forEach(x => { x.map = null; x.color = dark.clone(); x.transparent = false; x.needsUpdate = true; });
    m.N.visible = false; m.SL.visible = false; m.SR.visible = false; m.glass.visible = false; m.btn.forEach(b => { b.visible = false; });
    d.scene.updateMatrixWorld(true);
    const wp = new T.Vector3(), find = {};
    const mats = { left: m.L, right: m.R, clock: m.C, name: m.N };
    d.D.pivot.traverse(o => {
      if (!o.isMesh) return;
      for (const k in mats) if (o.material === mats[k]) {
        const pos = o.geometry.attributes.position; let x0 = 1e9, y0 = 1e9, x1 = -1e9, y1 = -1e9;
        for (let i = 0; i < pos.count; i++) {
          wp.fromBufferAttribute(pos, i).applyMatrix4(o.matrixWorld).project(cam);
          x0 = Math.min(x0, wp.x); x1 = Math.max(x1, wp.x); y0 = Math.min(y0, wp.y); y1 = Math.max(y1, wp.y);
        }
        (find[k] = find[k] || []).push({ x0, y0, x1, y1 });
      }
    });
    const vw = innerWidth, vh = innerHeight, res = {};
    const poly = [];
    d.D.pivot.traverse(o => {
      if (!o.isMesh || o.material !== m.C) return;
      const pos = o.geometry.attributes.position, seen = {};
      for (let i = 0; i < pos.count; i++) {
        wp.fromBufferAttribute(pos, i).applyMatrix4(o.matrixWorld).project(cam);
        const px = Math.round((wp.x + 1) / 2 * vw), py = Math.round((1 - wp.y) / 2 * vh);
        if (!seen[px + ',' + py]) { seen[px + ',' + py] = 1; poly.push([px, py]); }
      }
    });
    if (poly.length) {
      const cx = poly.reduce((a, p) => a + p[0], 0) / poly.length, cy = poly.reduce((a, p) => a + p[1], 0) / poly.length;
      poly.sort((a, b) => Math.atan2(a[1] - cy, a[0] - cx) - Math.atan2(b[1] - cy, b[0] - cx));
      res.clockPoly = poly;
    }
    for (const k in find) {
      const r = find[k][0];
      res[k] = { x: Math.round((r.x0 + 1) / 2 * vw), y: Math.round((1 - r.y1) / 2 * vh), w: Math.round((r.x1 - r.x0) / 2 * vw), h: Math.round((r.y1 - r.y0) / 2 * vh) };
    }
    return res;
  });
  await page.waitForTimeout(1500);
  fs.mkdirSync(out, { recursive: true });
  await page.screenshot({ path: path.join(out, 'shell.png') });
  fs.writeFileSync(path.join(out, 'shell.json'), JSON.stringify({ width: W, height: H, rects }, null, 2));
  console.log(JSON.stringify(rects), 'errors', JSON.stringify(errors.slice(0, 3)));
  await browser.close();
})();
