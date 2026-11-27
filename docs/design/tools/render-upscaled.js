// Renders the x2 copies (design prompt section 8.2 and 8.3, kind UP) and one overview contact sheet per mode.
//
// Each screen REF frame is 360 x 780 dp and its REF PNG is 3x (1080 x 2340). The x2 copy is 6x device pixels
// (2160 x 4680), so the same HTML is laid out again by Chromium at a higher scale. No pixels are
// interpolated, which is why this is re-rendering and not upscaling.
// DS-01's REF is a 1080 css px sheet at 1x, so its x2 copy is the same layout at 2x (2160 px wide).
// Both x2 copies are therefore 2160 px wide per unit of REF width.
//
// Usage:  node render-upscaled.js [screenId ...] [DS-01] [--no-sheets]
//         (no ids = every screen in screens-*.js, plus DS-01, plus the two contact sheets)
// Env:    PZ_UP_OUT   output folder   (default /mnt/project-files/design/03-upscaled)
//         PZ_VERSION  file version    (default v01; existing files are never overwritten)
//         PZ_SCALE    screen device scale (default 6)
//         PZ_DS_SCALE DS-01 device scale  (default 2)
//         PZ_SCRATCH  scratch folder  (default the OS temp dir)
//         PZ_NO_INDEX=1 leaves INDEX.md unchanged
'use strict';
const fs = require('fs');
const os = require('os');
const path = require('path');
const { chromium } = require('playwright');
const lib = require('./lib');

const DESIGN = path.resolve(__dirname, '..');
const OUT = process.env.PZ_UP_OUT || '/mnt/project-files/design/03-upscaled';
const VERSION = process.env.PZ_VERSION || 'v01';
const SCALE = Number(process.env.PZ_SCALE || 6);          // screens: 360 dp frame at 3x REF, so 6x is the x2 copy
const DS_SCALE = Number(process.env.PZ_DS_SCALE || 2);    // DS-01: 1080 css px at 1x REF, so 2x is the x2 copy
const DS_WIDTH = 1080;
const SCRATCH = process.env.PZ_SCRATCH || path.join(os.tmpdir(), 'pz-upscale-scratch');
const FRAME = { width: 360, height: 780 };
const DS_ID = 'DS-01';
const SCREEN_FILES = ['screens-a', 'screens-b', 'screens-c', 'screens-d'];

const screens = SCREEN_FILES
  .filter((f) => fs.existsSync(path.join(__dirname, f + '.js')))
  .flatMap((f) => require('./' + f));

const args = process.argv.slice(2);
const flags = new Set(args.filter((a) => a.startsWith('--')));
const wanted = args.filter((a) => !a.startsWith('--'));
const screenList = wanted.length ? screens.filter((s) => wanted.includes(s.id)) : screens;
const doDs = !wanted.length || wanted.includes(DS_ID);
const doSheets = !wanted.length && !flags.has('--no-sheets');

const modesOf = (s) => s.modes || ['dark', 'light'];
const log = [];
const note = (msg) => { log.push(msg); console.log(msg); };

async function shotFrame(browser, html, file) {
  // Same page setup as build.js (REF path), at SCALE device pixels per dp.
  const ctx = await browser.newContext({ viewport: FRAME, deviceScaleFactor: SCALE });
  const page = await ctx.newPage();
  await page.setContent(html, { waitUntil: 'load' });
  await page.evaluate(() => document.fonts && document.fonts.ready);
  await page.screenshot({ path: file, clip: { x: 0, y: 0, width: FRAME.width, height: FRAME.height } });
  await ctx.close();
}

async function shotSheet(browser, src, file) {
  // The DS-01 REF is a 1080 css px wide sheet at 1x (1080 x 8604). Its x2 copy is the same layout at 2x,
  // so the width stays 1080 css px and the scale is DS_SCALE (2), giving 2160 px wide.
  // It is a long sheet, so it is captured full page.
  const ctx = await browser.newContext({ viewport: { width: DS_WIDTH, height: 800 }, deviceScaleFactor: DS_SCALE });
  const page = await ctx.newPage();
  await page.goto('file://' + src, { waitUntil: 'load' });
  await page.evaluate(() => document.fonts && document.fonts.ready);
  await page.screenshot({ path: file, fullPage: true });
  await ctx.close();
}

function sheetHtml(mode, items) {
  const dark = mode === 'dark';
  const bg = dark ? '#120C16' : '#F7F1FA';
  const fg = dark ? '#F3E9F7' : '#2A1830';
  const soft = dark ? '#A67BB3' : '#6B4A78';
  const cells = items.map((it) => {
    const cls = it.ds ? ' class="ds"' : '';
    return `<figure${cls}><img src="${encodeURI('file://' + it.src)}" alt=""><figcaption>${it.label}</figcaption></figure>`;
  }).join('\n');
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><title>Perazim design overview · ${mode}</title>
<style>
body{margin:0;padding:32px;box-sizing:border-box;width:1800px;background:${bg};color:${fg};font:500 15px/1.4 "DejaVu Sans",sans-serif}
h1{font-size:22px;margin:0 0 4px;font-weight:700}
p{margin:0 0 24px;color:${soft};font-size:14px}
.grid{display:grid;grid-template-columns:260px repeat(7,1fr);gap:20px 16px;align-items:start}
figure{margin:0}
img{display:block;width:100%;height:auto;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.25)}
figcaption{margin-top:8px;font-size:13px;color:${soft};white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.ds{grid-column:1;grid-row:1 / span 4}
</style></head>
<body>
<h1>Perazim design overview · ${mode} · ${VERSION}</h1>
<p>DS-01 design system at left, then the 28 screens in order. Each image is the x2 copy of its REF frame (2160 by 4680 px for screens). Under review.</p>
<div class="grid">
${cells}
</div>
</body></html>`;
}

function labelFor(id, mode) {
  const s = screens.find((x) => x.id === id);
  return { screen: s, name: s ? `${id} · ${s.name}` : id, mode };
}

async function buildSheet(browser, mode, items) {
  const name = `00-overview_CONTACT_${mode}_${VERSION}.png`;
  const file = path.join(OUT, name);
  if (fs.existsSync(file)) { note(`skip (exists) ${name}`); return; }
  const htmlPath = path.join(SCRATCH, `contact-${mode}.html`);
  fs.writeFileSync(htmlPath, sheetHtml(mode, items));
  const ctx = await browser.newContext({ viewport: { width: 1800, height: 1000 }, deviceScaleFactor: 2 });
  const page = await ctx.newPage();
  await page.goto('file://' + htmlPath, { waitUntil: 'load' });
  await page.evaluate(() => Promise.all([...document.images].map((i) => i.decode().catch(() => {}))));
  await page.screenshot({ path: file, fullPage: true });
  await ctx.close();
  note(`wrote ${name}`);
}

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  fs.mkdirSync(SCRATCH, { recursive: true });
  const browser = await chromium.launch();
  const failures = [];

  // Writes one file, never overwriting an existing one (rule 8.4: keep the old file and add a new version).
  async function emit(name, fn) {
    const file = path.join(OUT, name);
    if (fs.existsSync(file)) { note(`skip (exists) ${name}`); return; }
    try { await fn(file); note(`wrote ${name}`); }
    catch (e) { failures.push({ name, error: String(e && e.message || e).split('\n')[0] }); note(`FAILED ${name}: ${failures[failures.length - 1].error}`); }
  }

  for (const s of screenList) {
    for (const mode of modesOf(s)) {
      const name = `${s.id}_REF_${mode}_x2_${VERSION}.png`;
      await emit(name, (file) => shotFrame(browser, lib.page(s, mode, { ref: true }), file));
    }
  }

  if (doDs) {
    for (const mode of ['dark', 'light']) {
      const src = path.join(DESIGN, '01-design-system', `DS-01_MOCK_${mode}_v01.html`);
      const name = `DS-01_design-system-reference_${mode}_x2_${VERSION}.png`;
      await emit(name, (file) => shotSheet(browser, src, file));
    }
  }

  if (doSheets) {
    for (const mode of ['dark', 'light']) {
      const items = [{ ds: true, src: path.join(OUT, `DS-01_design-system-reference_${mode}_x2_${VERSION}.png`), label: `DS-01 · Design system (${mode})` }];
      for (const s of screens) {
        const { name } = labelFor(s.id, mode);
        items.push({ src: path.join(OUT, `${s.id}_REF_${mode}_x2_${VERSION}.png`), label: name });
      }
      const missing = items.filter((it) => !fs.existsSync(it.src));
      if (missing.length) { failures.push({ name: `contact-${mode}`, error: `${missing.length} source image(s) missing` }); note(`SKIPPED contact sheet ${mode}: ${missing.length} source image(s) missing`); continue; }
      await buildSheet(browser, mode, items);
    }
  }

  await browser.close();

  if (!process.env.PZ_NO_INDEX) updateIndex();
  const count = fs.readdirSync(OUT).filter((f) => f.endsWith('.png')).length;
  note(`done: ${count} PNG files in ${OUT}; failures: ${failures.length}`);
  if (failures.length) { console.log(JSON.stringify(failures, null, 2)); process.exitCode = 1; }
})().catch((e) => { console.error(e); process.exit(1); });

// Rewrites the upscaled rows of INDEX.md between markers, leaving every other row intact.
function updateIndex() {
  const indexFile = path.join(DESIGN, 'INDEX.md');
  if (!fs.existsSync(indexFile)) { note('INDEX.md not found, not updated'); return; }
  const files = fs.readdirSync(OUT).filter((f) => f.endsWith('.png')).sort();
  const rows = files.map((f) => {
    const mode = /_(dark|light)_/.exec(f);
    let label;
    if (f.startsWith('00-overview_CONTACT')) label = 'Overview contact sheet';
    else if (f.startsWith(DS_ID)) label = 'DS-01 Design system';
    else {
      const id = f.slice(0, f.indexOf('_REF_'));
      const s = screens.find((x) => x.id === id);
      label = s ? `${s.id} ${s.name}` : id;
    }
    return `| (project folder) design/03-upscaled/${f} | ${label} | ${mode ? mode[1] : 'both'} | ${VERSION} | under review |`;
  });
  const block = `<!-- upscaled:start -->\n| File | Screen | Mode | Version | Status |\n|---|---|---|---|---|\n${rows.join('\n')}\n<!-- upscaled:end -->`;
  let text = fs.readFileSync(indexFile, 'utf8');
  if (text.includes('<!-- upscaled:start -->')) {
    text = text.replace(/<!-- upscaled:start -->[\s\S]*?<!-- upscaled:end -->/, block);
  } else {
    text += `\n## Upscaled x2 copies (03-upscaled)\n\nEach is a 2x copy of its REF frame (2160 by 4680 px), re-rendered from the same HTML. Rendered by tools/render-upscaled.js. Contact sheets are overviews, not design files.\n\n${block}\n`;
  }
  fs.writeFileSync(indexFile, text);
  note(`INDEX.md upscaled rows: ${rows.length}`);
}
