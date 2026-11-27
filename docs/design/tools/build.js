// Builds the screen files for docs/design/02-screens/<NN-slug>/.
// Usage: node build.js [screenId ...]   (no ids = every screen defined in screens-*.js)
// Writes, per screen and mode: MOCK html (committed), REF png (1080x2340 at 2x of 360x780 dp... i.e. 3x),
// and one WIRE png per screen. Also checks for horizontal overflow at 320, 360 and 412 px.
'use strict';
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const lib = require('./lib');

const DESIGN = path.resolve(__dirname, '..');
const SCREENS_OUT = path.join(DESIGN, '02-screens');
// Full-quality REF PNGs (about 1.1 MB each) are written to the project folder, not git.
const REF_OUT = process.env.PZ_REF_OUT || '/mnt/project-files/design/02-screens';
const SCRATCH = process.env.PZ_SCRATCH || path.join(process.env.TMPDIR || '/tmp', 'pz-design-scratch');
fs.mkdirSync(SCRATCH, { recursive: true });

const all = ['screens-a', 'screens-b', 'screens-c', 'screens-d']
  .filter((f) => fs.existsSync(path.join(__dirname, f + '.js')))
  .flatMap((f) => require('./' + f));
const wanted = process.argv.slice(2);
const list = wanted.length ? all.filter((s) => wanted.includes(s.id)) : all;

(async () => {
  const browser = await chromium.launch();
  const report = [];
  for (const s of list) {
    const dir = path.join(SCREENS_OUT, s.id);
    fs.mkdirSync(dir, { recursive: true });
    const modes = s.modes || ['dark', 'light'];
    for (const mode of modes) {
      const mockHtml = lib.page(s, mode, { mock: true });
      const mockName = `${s.id}_MOCK_${mode}_v01.html`;
      fs.writeFileSync(path.join(dir, mockName), mockHtml);

      const refHtml = lib.page(s, mode, { ref: true });
      const refTmp = path.join(SCRATCH, `${s.id}_REF_${mode}.html`);
      fs.writeFileSync(refTmp, refHtml);
      fs.mkdirSync(path.join(REF_OUT, s.id), { recursive: true });
      const refPng = path.join(REF_OUT, s.id, `${s.id}_REF_${mode}_v01.png`);
      await shot(browser, refHtml, refPng, { width: 360, height: 780, dpr: 3 });
    }
    // Wireframe: greyscale, mode-neutral, names on each element.
    const wireHtml = lib.page(s, 'dark', { ref: true, wire: true });
    await shot(browser, wireHtml, path.join(dir, `${s.id}_WIRE_v01.png`), { width: 360, height: 780, dpr: 3 });

    // Overflow check on the mock at phone widths (dark, default state).
    const overflow = {};
    const ctx = await browser.newContext({ viewport: { width: 320, height: 780 } });
    const page = await ctx.newPage();
    await page.setContent(lib.page(s, 'dark', { mock: true }), { waitUntil: 'load' });
    for (const w of [320, 360, 412]) {
      await page.setViewportSize({ width: w, height: 780 });
      overflow[w] = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    }
    await ctx.close();
    report.push({ id: s.id, overflow });
    console.log(`built ${s.id}  overflow px @320/360/412: ${overflow[320]}/${overflow[360]}/${overflow[412]}`);
  }
  await browser.close();
  fs.writeFileSync(path.join(SCRATCH, 'overflow-report.json'), JSON.stringify(report, null, 2));
})().catch((e) => { console.error(e); process.exit(1); });

async function shot(browser, html, outPath, { width, height, dpr }) {
  const ctx = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: dpr });
  const page = await ctx.newPage();
  await page.setContent(html, { waitUntil: 'load' });
  await page.evaluate(() => document.fonts && document.fonts.ready);
  await page.screenshot({ path: outPath, clip: { x: 0, y: 0, width, height } });
  await ctx.close();
}
