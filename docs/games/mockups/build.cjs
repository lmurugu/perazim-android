// Game mockup renderer: HTML screens -> 1080x2340 PNG (360x780 CSS px @3x).
// Usage: node build.cjs <outDir> [gameId ...]
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const WORK = __dirname;
const ASSETS = path.join(WORK, 'assets');
const OUT = process.argv[2];
const ONLY = process.argv.slice(3);
fs.mkdirSync(ASSETS, { recursive: true });

// ---------- shared theme (frosted deep purple + orange, text ~1px smaller than default) ----------
const CSS = `
*{box-sizing:border-box}
html,body{margin:0;padding:0;background:#1a0524}
.screen{--t1:#fff;--t2:#F5ECF7;--muted:rgba(245,236,247,.74);--glass:rgba(255,255,255,.14);--glassb:rgba(255,255,255,.28);
  --board:rgba(255,255,255,.10);--acc:#E17D2F;--warn:#F2C94C;--ok:#7BD389;
  width:360px;height:780px;position:relative;overflow:hidden;color:var(--t1);font-family:Roboto,"DejaVu Sans",system-ui,sans-serif;font-size:13px;
  background:radial-gradient(120% 55% at 92% -8%,rgba(232,119,46,.38),transparent 60%),
    radial-gradient(90% 50% at 0% 100%,rgba(192,107,224,.38),transparent 62%),
    linear-gradient(180deg,#2D0938 0%,#4A1260 55%,#681A7D 100%)}
.screen.light{--t1:#2D0938;--t2:#4A1260;--muted:rgba(74,18,96,.72);--glass:rgba(255,255,255,.66);--glassb:rgba(104,26,125,.18);
  --board:rgba(255,255,255,.75);--warn:#C98A12;
  background:radial-gradient(120% 55% at 92% -8%,rgba(232,119,46,.22),transparent 60%),
    radial-gradient(90% 50% at 0% 100%,rgba(192,107,224,.22),transparent 62%),
    linear-gradient(180deg,#FBEFF8 0%,#F3DDF4 55%,#E7C3EE 100%)}
.abs{position:absolute}
.top{position:absolute;left:12px;right:12px;top:14px;height:54px;border-radius:18px;background:var(--glass);border:1px solid var(--glassb);
  display:flex;align-items:center;padding:0 8px;gap:6px}
.ib{width:38px;height:38px;border-radius:19px;display:grid;place-items:center;background:var(--glass);border:1px solid var(--glassb);font-size:15px;flex:none;color:var(--t1)}
.tt{flex:1;text-align:center;line-height:1.15;min-width:0}
.tt b{display:block;font-size:13.5px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.tt span{font-size:10.5px;color:var(--muted)}
.stars{font-size:12px;color:var(--warn);font-weight:700;white-space:nowrap;min-width:40px;text-align:right}
.glass{background:var(--glass);border:1px solid var(--glassb);border-radius:18px;padding:12px;}
.pill{height:34px;padding:0 14px;border-radius:17px;display:inline-flex;align-items:center;justify-content:center;font-size:12px;font-weight:600;background:var(--glass);border:1px solid var(--glassb);white-space:nowrap}
.pill.on{background:linear-gradient(180deg,#F39A4E,#E17D2F);color:#fff;border-color:transparent;box-shadow:0 0 14px rgba(232,119,46,.5)}
.btn{height:46px;border-radius:23px;display:flex;align-items:center;justify-content:center;font-weight:600;font-size:14px;flex:1;white-space:nowrap}
.btn.p{background:linear-gradient(180deg,#F39A4E,#E17D2F);color:#fff;box-shadow:0 0 18px rgba(232,119,46,.55)}
.btn.g{background:var(--glass);border:1px solid var(--glassb);color:var(--t1)}
.bottom{position:absolute;left:16px;right:16px;bottom:24px;display:flex;gap:10px}
.eyebrow{font-size:10.5px;letter-spacing:1.2px;text-transform:uppercase;color:var(--muted);font-weight:600}
.verse{font-family:"DejaVu Serif",Georgia,"Times New Roman",serif;font-size:14.5px;line-height:1.42}
.ref{font-size:11.5px;color:var(--muted);margin-top:6px}
.dim{color:var(--muted)}
.bubble{position:absolute;padding:8px 12px;border-radius:14px;background:rgba(242,201,76,.18);border:1px solid rgba(242,201,76,.75);color:var(--t1);font-size:12px;font-weight:600;box-shadow:0 0 16px rgba(242,201,76,.35)}
.light .bubble{background:rgba(242,201,76,.35);border-color:#C98A12}
.lvls{display:grid;grid-template-columns:repeat(5,1fr);gap:8px}
.lv{height:50px;border-radius:14px;background:var(--glass);border:1px solid var(--glassb);display:flex;flex-direction:column;align-items:center;justify-content:center;line-height:1.05}
.lv b{font-size:14px}.lv small{font-size:9px;color:var(--warn);letter-spacing:.5px}
.lv.cur{background:linear-gradient(180deg,#F39A4E,#E17D2F);border-color:transparent;color:#fff;box-shadow:0 0 14px rgba(232,119,46,.55)}
.lv.cur small{color:#fff}
.lv.dim2{opacity:.55}
.mini{width:40px;height:40px;border-radius:50%;display:grid;place-items:center;background:var(--glass);border:1px solid var(--glassb);font-weight:700;font-size:13px}
.pause{position:absolute;inset:0;background:rgba(20,4,30,.55);backdrop-filter:blur(3px)}
`;

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;');

function page(inner, light = false) {
  return `<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=360">
<style>${CSS}</style></head><body><div class="screen${light ? ' light' : ''}">${inner}</div></body></html>`;
}

function topbar({ title, sub = '', stars = '', right = '❚❚' }) {
  return `<div class="top"><div class="ib">‹</div><div class="tt"><b>${esc(title)}</b><span>${esc(sub)}</span></div>
<div class="stars">${stars}</div><div class="ib">${right}</div></div>`;
}

// Level grid used on start screens. states: array of stars (-1 = not played, 0-3 = stars)
function levelGrid(count, current, states) {
  let h = '<div class="lvls abs" style="left:16px;right:16px;top:330px">';
  for (let i = 1; i <= count; i++) {
    const s = states[i - 1] ?? -1;
    const cls = i === current ? ' cur' : '';
    const st = s >= 0 ? '★'.repeat(s) + '☆'.repeat(3 - s) : '';
    h += `<div class="lv${cls}"><b>${i}</b><small>${st}</small></div>`;
  }
  return h + '</div>';
}

function resultsBlock({ character, stars, title, sub, verse, ref, extra = '', light = false }) {
  const filled = '★'.repeat(stars) + '☆'.repeat(3 - stars);
  return `${topbar({ title, sub, stars: '' })}
<img class="abs" src="assets/${character}.png" style="left:110px;top:90px;width:140px;height:140px;object-fit:contain">
<div class="abs" style="left:0;right:0;top:238px;text-align:center;font-size:30px;letter-spacing:6px;color:var(--warn)">${filled}</div>
<div class="abs" style="left:0;right:0;top:284px;text-align:center;font-size:17px;font-weight:600">${esc(title === undefined ? '' : sub)}</div>
<div class="abs glass" style="left:20px;right:20px;top:330px;padding:16px 18px">
  <div class="verse">“${esc(verse)}”</div><div class="ref">— ${esc(ref)}</div>${extra}
</div>`;
}

// ---------- Vine Words ----------
// '.' = not part of the puzzle. Words: BRANCH (row 2), VINE (col 3), FRUIT (col 1)
const VW_GRID = ['...V..', '.F.I..', 'BRANCH', '.U.E..', '.I....', '.T....'];
const VW_WORDS = {
  BRANCH: [[2, 0], [2, 1], [2, 2], [2, 3], [2, 4], [2, 5]],
  VINE: [[0, 3], [1, 3], [2, 3], [3, 3]],
  FRUIT: [[1, 1], [2, 1], [3, 1], [4, 1], [5, 1]],
};
const VW_WHEEL = ['E', 'B', 'F', 'N', 'T', 'A', 'H', 'U', 'C', 'I', 'G', 'R', 'N', 'O', 'V'];

function vwGrid(revealed, hint = null) {
  let h = '<div class="abs glass" style="left:50px;top:90px;width:260px;padding:10px"><div style="display:grid;grid-template-columns:repeat(6,36px);gap:3px">';
  for (let r = 0; r < 6; r++) for (let c = 0; c < 6; c++) {
    const ch = VW_GRID[r][c];
    if (ch === '.') { h += '<div></div>'; continue; }
    const key = r + ',' + c;
    const on = revealed.has(key);
    const isHint = hint === key;
    h += `<div style="height:36px;border-radius:8px;display:grid;place-items:center;font-weight:700;font-size:17px;
      background:${on ? 'linear-gradient(180deg,#fff,#EADFF0)' : 'var(--board)'};color:${on ? '#2D0938' : 'var(--t1)'};
      border:${isHint ? '2px solid #F2C94C' : '1px solid var(--glassb)'};box-shadow:${isHint ? '0 0 12px rgba(242,201,76,.8)' : 'none'}">${on ? ch : ''}</div>`;
  }
  return h + '</div></div>';
}

function vwWheel(selected, centerHtml) {
  const cx = 180, cy = 592, R = 108, n = VW_WHEEL.length;
  const pos = VW_WHEEL.map((_, i) => {
    const a = (-90 + (i * 360) / n) * Math.PI / 180;
    return [cx + R * Math.cos(a), cy + R * Math.sin(a)];
  });
  let h = `<div class="abs" style="left:${cx - 96}px;top:${cy - 96}px;width:192px;height:192px;border-radius:50%;background:var(--board);border:1px solid var(--glassb)"></div>`;
  if (selected.length > 1) {
    const pts = selected.map((i) => pos[i].map((v) => v.toFixed(1)).join(',')).join(' ');
    h += `<svg class="abs" style="left:0;top:0" width="360" height="780"><polyline points="${pts}" fill="none" stroke="#F2C94C" stroke-width="7" stroke-linecap="round" stroke-linejoin="round" opacity=".75"/></svg>`;
  }
  VW_WHEEL.forEach((ch, i) => {
    const on = selected.includes(i);
    h += `<div class="abs" style="left:${pos[i][0] - 20}px;top:${pos[i][1] - 20}px;width:40px;height:40px;border-radius:20px;display:grid;place-items:center;font-weight:700;font-size:16px;
      background:${on ? 'linear-gradient(180deg,#F39A4E,#E17D2F)' : 'var(--glass)'};border:1px solid ${on ? 'transparent' : 'var(--glassb)'};color:${on ? '#fff' : 'var(--t1)'};box-shadow:${on ? '0 0 12px rgba(232,119,46,.6)' : 'none'}">${ch}</div>`;
  });
  h += `<div class="abs" style="left:${cx - 90}px;top:${cy - 26}px;width:180px;text-align:center;font-weight:700;font-size:17px;letter-spacing:3px">${centerHtml}</div>`;
  return h;
}

const vineWords = {
  id: 'vine_words',
  name: 'Vine Words',
  shots: {
    '01-start': () => page(
      topbar({ title: 'Vine Words', sub: 'Choose a level', stars: '★ 14' }) +
      `<div class="abs eyebrow" style="left:20px;top:86px">Passage</div>
      <div class="abs glass" style="left:16px;right:16px;top:104px;padding:14px">
        <div style="font-size:16px;font-weight:700">John 15:1-5</div>
        <div class="verse dim" style="margin-top:6px;font-size:12.5px">“I am the true vine, and my Father is the husbandman.”</div>
        <div class="ref">Words to find: vine · branch · fruit · abide</div>
      </div>
      <div class="abs eyebrow" style="left:20px;top:262px">Level 12 of 40</div>
      <div class="abs" style="left:16px;right:16px;top:286px;height:32px;display:flex;gap:6px;align-items:center">
        <span class="pill">Words: 3</span><span class="pill">Clue mode</span><span class="pill">Hints: free</span></div>
      ${levelGrid(20, 12, [3, 3, 2, 3, 3, 3, 2, 3, 3, 3, 2, -1])}`,
      true),
    '02-play': () => {
      const revealed = new Set();
      VW_WORDS.BRANCH.forEach(([r, c]) => revealed.add(r + ',' + c));
      VW_WORDS.VINE.forEach(([r, c]) => revealed.add(r + ',' + c));
      const sel = [VW_WHEEL.indexOf('B'), VW_WHEEL.indexOf('R'), VW_WHEEL.indexOf('A'), VW_WHEEL.indexOf('N')];
      return page(
        topbar({ title: 'Vine Words', sub: 'Level 12 · John 15:1-5', stars: '★ 3' }) +
        `<div class="abs pill" style="left:16px;top:86px">Found 2 of 3</div>
         <div class="abs pill" style="right:16px;top:86px">Bonus 1</div>
         ${vwGrid(revealed)}
         <div class="abs" style="left:16px;right:16px;top:356px;height:34px;display:flex;gap:8px">
           <span class="pill">Reveal a letter</span><span class="pill">Shuffle wheel</span></div>
         ${vwWheel(sel, '<span style="color:#F2C94C">BRAN</span>')}`
      );
    },
    '03-hint': () => {
      const revealed = new Set();
      VW_WORDS.BRANCH.forEach(([r, c]) => revealed.add(r + ',' + c));
      VW_WORDS.VINE.forEach(([r, c]) => revealed.add(r + ',' + c));
      const sel = [VW_WHEEL.indexOf('V'), VW_WHEEL.indexOf('I'), VW_WHEEL.indexOf('N'), VW_WHEEL.indexOf('O')];
      return page(
        topbar({ title: 'Vine Words', sub: 'Level 12 · Hint used', stars: '★ 2' }) +
        `<div class="abs pill" style="left:16px;top:86px">Found 2 of 3</div>
         ${vwGrid(revealed, '1,1')}
         <div class="bubble" style="left:16px;right:16px;top:356px;text-align:center">Hint: the first letter of the next word is F. Stars are capped at 2.</div>
         <div class="abs" style="left:16px;right:16px;top:404px;text-align:center;font-size:11px;color:var(--muted)">Not on this grid this time: VINO. No penalty, keep swiping.</div>
         ${vwWheel(sel, '<span style="color:#fff;opacity:.8">VINO</span>')}`
      );
    },
    '04-results': () => page(
      resultsBlock({
        character: 'game_char_shepherd', stars: 2,
        title: 'Level 12 complete', sub: 'Three words found, one hint used',
        verse: 'I am the vine, ye are the branches: He that abideth in me, and I in him, the same bringeth forth much fruit: for without me ye can do nothing.',
        ref: 'John 15:5 (KJV)',
      }) +
      `<div class="abs dim" style="left:20px;right:20px;top:548px;text-align:center;font-size:12px">3 of 3 words · 1 hint · 1 bonus word</div>
       <div class="bottom" style="bottom:28px"><div class="btn g">Read passage</div><div class="btn p">Next level →</div></div>`
    ),
  },
};

// ---------- Lamp Slide ----------
function paintingSvg() {
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400">
<defs><linearGradient id="s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2D0938"/><stop offset=".6" stop-color="#8E3A9E"/><stop offset="1" stop-color="#F0A35E"/></linearGradient>
<radialGradient id="g"><stop offset="0" stop-color="#FFE9A8" stop-opacity="1"/><stop offset="1" stop-color="#FFE9A8" stop-opacity="0"/></radialGradient></defs>
<rect width="400" height="400" fill="url(#s)"/>
<circle cx="300" cy="90" r="34" fill="#FFF2C9"/>
${[[60, 50], [110, 120], [200, 40], [360, 170], [40, 180], [250, 150], [330, 40]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="2" fill="#fff" opacity=".9"/>`).join('')}
<path d="M0 290 Q120 230 220 280 T400 270 V400 H0Z" fill="#4A1260"/>
<path d="M0 340 Q160 300 400 340 V400 H0Z" fill="#2D0938"/>
<path d="M190 400 Q210 340 250 300 L262 306 Q224 350 226 400Z" fill="#7A3A8A" opacity=".8"/>
<circle cx="135" cy="222" r="70" fill="url(#g)"/>
<rect x="131" y="238" width="8" height="78" fill="#3A1A2E"/>
<path d="M120 236 h30 l-5 -24 h-20 z" fill="#FFD27A"/>
<circle cx="135" cy="224" r="7" fill="#FFF6D8"/>
</svg>`;
}

function lsBoard(vals, opts = {}) {
  let h = '<div class="abs glass" style="left:30px;top:150px;width:300px;height:300px;padding:6px;display:grid;grid-template-columns:repeat(4,1fr);grid-template-rows:repeat(4,1fr);gap:4px">';
  vals.forEach((v, i) => {
    if (v === 0) {
      h += `<div style="border-radius:8px;border:2px dashed var(--glassb);background:rgba(0,0,0,.12)"></div>`;
      return;
    }
    const k = v - 1, col = k % 4, row = Math.floor(k / 4);
    const one = opts.one === i;
    h += `<div style="border-radius:8px;background-image:url(assets/painting.svg);background-size:400% 400%;background-position:${(col * 100 / 3).toFixed(2)}% ${(row * 100 / 3).toFixed(2)}%;box-shadow:0 2px 6px rgba(0,0,0,.3);border:${one ? '3px solid #F2C94C' : '1px solid rgba(255,255,255,.35)'};${one ? 'box-shadow:0 0 14px rgba(242,201,76,.8)' : ''}"></div>`;
  });
  return h + '</div>';
}

const LS_MID = [1, 2, 3, 4, 5, 6, 0, 8, 9, 10, 7, 12, 13, 14, 11, 15];

const lampSlide = {
  id: 'lamp_slide',
  name: 'Lamp Slide',
  shots: {
    '01-start': () => page(
      topbar({ title: 'Lamp Slide', sub: 'Choose a painting', stars: '★ 9' }) +
      `<div class="abs glass" style="left:16px;right:16px;top:86px;padding:12px;text-align:center">
         <img src="assets/painting.svg" style="width:250px;height:250px;border-radius:14px;display:block;margin:0 auto">
         <div style="font-size:14px;font-weight:700;margin-top:10px">Level 14 · The lamp on the path</div>
         <div class="dim" style="font-size:11.5px;margin-top:3px">4 × 4 board · par 28 moves</div>
       </div>
       <div class="abs eyebrow" style="left:20px;top:430px">Verse for this painting</div>
       <div class="abs glass" style="left:16px;right:16px;top:452px;padding:12px 14px">
         <div class="verse" style="font-size:13.5px">“Thy word is a lamp unto my feet, and a light unto my path.”</div>
         <div class="ref">Psalm 119:105 (KJV)</div></div>
       <div class="abs" style="left:16px;right:16px;top:540px;height:34px;display:flex;gap:6px"><span class="pill on">Guided start</span><span class="pill">Timer off</span><span class="pill">3×3 to 5×5</span></div>
       ${levelGrid(10, 14, [3, 3, 3, 2, 3, 1, 3, 3, 2, 3]).replace('top:330px', 'top:584px').replace('repeat(5,1fr)', 'repeat(10,1fr)').replace('gap:8px', 'gap:4px')}
       <div class="bottom"><div class="btn g">Pick a painting</div><div class="btn p">Start level 14</div></div>`
    ),
    '02-play': () => page(
      topbar({ title: 'Lamp Slide', sub: 'Level 14 · 4 × 4', stars: '★ 3' }) +
      `<div class="abs pill" style="left:16px;top:84px">Moves 23 · Par 28</div>
       <div class="abs pill" style="right:16px;top:84px">Undo ↶</div>
       ${lsBoard(LS_MID)}
       <div class="abs dim" style="left:16px;right:16px;top:470px;text-align:center;font-size:11.5px">Slide a tile next to the gap into it.</div>
       <div class="abs" style="left:16px;right:16px;top:520px;display:flex;gap:8px"><span class="pill">Show the picture</span><span class="pill">One move</span></div>
       <div class="abs eyebrow" style="left:20px;top:582px">Painting progress</div>
       <div class="abs" style="left:16px;right:16px;top:604px;height:8px;border-radius:4px;background:var(--glass)"><div style="width:58%;height:100%;border-radius:4px;background:linear-gradient(90deg,#F39A4E,#E17D2F)"></div></div>`
    ),
    '03-hint': () => page(
      topbar({ title: 'Lamp Slide', sub: 'Level 14 · One move used', stars: '★ 2' }) +
      `<div class="abs pill" style="left:16px;top:84px">Moves 24 · Par 28</div>
       <div class="abs pill on" style="right:16px;top:84px">One move</div>
       ${lsBoard(LS_MID, { one: 6 })}
       <div class="bubble" style="left:40px;right:40px;top:458px;text-align:center">Slide the tile at the left of the gap into the gap</div>
       <div class="abs glass" style="left:16px;right:16px;top:512px;padding:10px 12px;font-size:11.5px">Hints lower the star cap by one, never the points you have. Undo stays free.</div>
       <div class="abs" style="left:16px;right:16px;top:566px;display:flex;gap:8px"><span class="pill">Show the picture</span><span class="pill on">One move</span></div>`
    ),
    '04-results': () => page(
      topbar({ title: 'Lamp Slide', sub: 'Level 14 complete', stars: '★ 3' }) +
      `<img class="abs" src="assets/painting.svg" style="left:50px;top:88px;width:260px;height:260px;border-radius:16px">
       <div class="abs" style="left:0;right:0;top:362px;text-align:center;font-size:24px;letter-spacing:6px;color:var(--warn)">★★★</div>
       <div class="abs" style="left:0;right:0;top:400px;text-align:center;font-size:12px" class="dim">Moves 27 · par 28</div>
       <div class="abs glass" style="left:20px;right:20px;top:432px;padding:14px 16px">
         <div class="verse" style="font-size:14px">“Shew me thy ways, O LORD; teach me thy paths.”</div>
         <div class="ref">Psalm 25:4 (KJV)</div></div>
       <div class="bottom" style="bottom:28px"><div class="btn g">Play again</div><div class="btn p">Next level →</div></div>`
    ),
    '05-play-light': () => page(
      topbar({ title: 'Lamp Slide', sub: 'Level 14 · 4 × 4', stars: '★ 3' }) +
      `<div class="abs pill" style="left:16px;top:84px">Moves 23 · Par 28</div>
       <div class="abs pill" style="right:16px;top:84px">Undo ↶</div>
       ${lsBoard(LS_MID)}
       <div class="abs dim" style="left:16px;right:16px;top:470px;text-align:center;font-size:11.5px">Slide a tile next to the gap into it.</div>
       <div class="abs" style="left:16px;right:16px;top:520px;display:flex;gap:8px"><span class="pill">Show the picture</span><span class="pill">One move</span></div>
       <div class="abs eyebrow" style="left:20px;top:582px">Painting progress</div>
       <div class="abs" style="left:16px;right:16px;top:604px;height:8px;border-radius:4px;background:var(--glass)"><div style="width:58%;height:100%;border-radius:4px;background:linear-gradient(90deg,#F39A4E,#E17D2F)"></div></div>`,
      true
    ),
  },
};

// ---------- Story Mosaic ----------
function sceneSvg(kind) {
  const head = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400">`;
  if (kind === 'ark') return head + `
<defs><linearGradient id="k" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#F7C98B"/><stop offset="1" stop-color="#E9829A"/></linearGradient></defs>
<rect width="400" height="400" fill="url(#k)"/><circle cx="320" cy="80" r="36" fill="#FFF0B3"/>
<path d="M0 250 Q100 220 200 250 T400 240 V400 H0Z" fill="#8E4BA8"/>
<rect y="292" width="400" height="108" fill="#4B1F6B"/>
<path d="M60 226 H340 L302 292 H98 Z" fill="#8A4B22"/>
<rect x="92" y="150" width="216" height="78" fill="#C98745"/><path d="M84 152 L200 112 L316 152Z" fill="#8A4B22"/>
<rect x="190" y="172" width="22" height="56" fill="#5A2C12"/>
<rect x="118" y="172" width="24" height="20" fill="#FFE7A3"/><rect x="258" y="172" width="24" height="20" fill="#FFE7A3"/>
<rect x="60" y="130" width="10" height="20" fill="#FFF"/>
</svg>`;
  if (kind === 'dove') return head + `
<defs><linearGradient id="d" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#BFE3F0"/><stop offset="1" stop-color="#F6E7C9"/></linearGradient></defs>
<rect width="400" height="400" fill="url(#d)"/>
<rect y="262" width="400" height="138" fill="#5A3D8F"/>
<path d="M110 300 Q190 260 290 186" fill="none" stroke="#4F7A3A" stroke-width="7" stroke-linecap="round"/>
${[[150, 280, -30], [180, 268, 20], [220, 246, -20], [262, 214, 25], [290, 186, -40]].map(([x, y, a]) => `<ellipse cx="${x}" cy="${y}" rx="18" ry="8" fill="#6FA35C" transform="rotate(${a} ${x} ${y})"/>`).join('')}
<ellipse cx="200" cy="160" rx="42" ry="24" fill="#FFFFFF"/><circle cx="236" cy="142" r="15" fill="#FFFFFF"/>
<path d="M248 140 L264 146 L248 150Z" fill="#E17D2F"/><path d="M150 160 Q168 118 200 150 Z" fill="#E9EEF5"/>
</svg>`;
  // rainbow
  let arcs = '';
  const cols = ['#E84A5F', '#F39A4E', '#F2C94C', '#7BD389', '#4AA3DF', '#8E4BA8'];
  cols.forEach((c, i) => { const r = 190 - i * 12; arcs += `<path d="M${200 - r} 300 A${r} ${r} 0 0 1 ${200 + r} 300" fill="none" stroke="${c}" stroke-width="12" opacity=".92"/>`; });
  return head + `
<defs><linearGradient id="r" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#1E0B33"/><stop offset="1" stop-color="#8E3A9E"/></linearGradient></defs>
<rect width="400" height="400" fill="url(#r)"/>${arcs}
<path d="M0 300 Q120 250 220 300 T400 290 V400 H0Z" fill="#4A1260"/><rect y="330" width="400" height="70" fill="#2D0938"/>
</svg>`;
}

// tiles of a scene for an n x n puzzle; piece k is the k-th slice (row-major)
function pieceStyle(kind, n, k) {
  const col = k % n, row = Math.floor(k / n);
  const p = (v) => (v * 100 / (n - 1)).toFixed(2);
  return `background-image:url(assets/scene-${kind}.svg);background-size:${n * 100}% ${n * 100}%;background-position:${p(col)}% ${p(row)}%`;
}

function mosaicBoard(kind, n, placed, opts = {}) {
  const size = 300, gap = 4, cell = (size - gap * (n - 1) - 12) / n;
  let h = `<div class="abs glass" style="left:30px;top:150px;width:300px;height:300px;padding:6px;display:grid;grid-template-columns:repeat(${n},1fr);gap:${gap}px">`;
  for (let s = 0; s < n * n; s++) {
    if (placed.has(s)) {
      h += `<div style="border-radius:6px;${pieceStyle(kind, n, s)};box-shadow:0 2px 6px rgba(0,0,0,.3)"></div>`;
    } else if (opts.hint === s) {
      h += `<div style="border-radius:6px;border:3px dashed #F2C94C;background:rgba(242,201,76,.2);box-shadow:0 0 14px rgba(242,201,76,.7)"></div>`;
    } else if (opts.wrong === s) {
      h += `<div style="border-radius:6px;border:2px solid #F2C94C;background:rgba(242,201,76,.18)"></div>`;
    } else {
      h += `<div style="border-radius:6px;border:1.5px dashed var(--glassb);background:${opts.guided ? `url(assets/scene-${kind}.svg)` : 'rgba(0,0,0,.12)'};background-size:${n * 100}% ${n * 100}%;background-position:${(((s % n) * 100) / (n - 1)).toFixed(2)}% ${(((Math.floor(s / n)) * 100) / (n - 1)).toFixed(2)}%;opacity:${opts.guided ? 0.5 : 1}"></div>`;
    }
  }
  return h + '</div>';
}

function tray(kind, n, pieces, top = 470) {
  let h = `<div class="abs" style="left:0;right:0;top:${top}px;height:110px;display:flex;gap:10px;padding:0 16px;align-items:center;overflow:hidden">`;
  pieces.forEach((k) => {
    h += `<div style="flex:none;width:76px;height:76px;border-radius:10px;${pieceStyle(kind, n, k)};box-shadow:0 6px 14px rgba(0,0,0,.35);border:1px solid rgba(255,255,255,.4)"></div>`;
  });
  return h + '</div>';
}

const SCENES = {
  ark: { title: "Noah's ark", ref: 'Genesis 6:22', verse: 'Thus did Noah; according to all that God commanded him, so did he.' },
  dove: { title: 'The dove returns', ref: 'Genesis 8:11', verse: 'And the dove came in to him in the evening; and, lo, in her mouth was an olive leaf pluckt off: so Noah knew that the waters were abated from off the earth.' },
  rainbow: { title: 'The rainbow', ref: 'Genesis 9:13', verse: 'I do set my bow in the cloud, and it shall be for a token of a covenant between me and the earth.' },
};

const storyMosaic = {
  id: 'story_mosaic',
  name: 'Story Mosaic',
  shots: {
    '01-start': () => page(
      topbar({ title: 'Story Mosaic', sub: 'Chapter: Noah · Level 4', stars: '★ 6' }) +
      `<div class="abs glass" style="left:16px;right:16px;top:86px;padding:12px;display:flex;gap:12px;align-items:center">
         <img src="assets/scene-ark.svg" style="width:118px;height:118px;border-radius:12px;flex:none">
         <div><div style="font-size:15px;font-weight:700">Noah’s ark</div>
           <div class="dim" style="font-size:11.5px;margin-top:3px">Genesis 6 · 3 × 3 pieces</div>
           <div class="verse" style="font-size:12px;margin-top:8px">“Thus did Noah; according to all that God commanded him, so did he.”</div></div></div>
       <div class="abs eyebrow" style="left:20px;top:246px">How to play</div>
       <div class="abs" style="left:16px;right:16px;top:268px;height:34px;display:flex;gap:6px"><span class="pill on">Guided</span><span class="pill">Quiet</span><span class="pill">No timer</span></div>
       <div class="abs eyebrow" style="left:20px;top:318px">Chapters · all open</div>
       <div class="abs" style="left:16px;right:16px;top:340px;display:grid;grid-template-columns:repeat(3,1fr);gap:8px">
         ${['Creation', 'Noah', 'Abraham', 'Moses', 'Ruth', 'David', 'Daniel', 'Jonah', 'Nativity'].map((t, i) => `<div class="glass" style="padding:10px 6px;text-align:center;font-size:11.5px;${i === 1 ? 'background:linear-gradient(180deg,#F39A4E,#E17D2F);color:#fff;border-color:transparent' : ''}">${t}<div style="font-size:10px;opacity:.8;margin-top:3px">${i < 1 ? '★★★' : i === 1 ? 'now' : '○ open'}</div></div>`).join('')}
       </div>
       <div class="abs glass" style="left:16px;right:16px;top:544px;padding:10px 12px;font-size:11.5px">Every chapter is open from the start. Nothing is locked or paid.</div>
       <div class="bottom"><div class="btn g">Quiet mode</div><div class="btn p">Start scene 4</div></div>`
    ),
    '02-play': () => page(
      topbar({ title: 'Story Mosaic', sub: 'Noah’s ark · 3 × 3', stars: '★ 2' }) +
      `<div class="abs pill" style="left:16px;top:84px">Placed 5 of 9</div>
       <div class="abs pill" style="right:16px;top:84px">Guided</div>
       ${mosaicBoard('ark', 3, new Set([0, 1, 2, 3, 5]), { guided: true })}
       <div class="abs dim" style="left:16px;right:16px;top:458px;text-align:center;font-size:11.5px">Drag a piece onto its spot. It snaps when it is close.</div>
       ${tray('ark', 3, [6, 8, 4, 7], 484)}
       <div class="abs" style="left:16px;right:16px;top:600px;display:flex;gap:8px"><span class="pill">Show one spot</span></div>`
    ),
    '03-hint': () => page(
      topbar({ title: 'Story Mosaic', sub: 'Noah’s ark · Hint used', stars: '★ 2' }) +
      `
       ${mosaicBoard('ark', 3, new Set([0, 1, 2, 3, 5]), { guided: true, hint: 4, wrong: 8 })}
       <div class="bubble" style="left:16px;right:16px;top:110px;text-align:center">Hint: the centre spot is for the piece with the ark’s roof</div>
       <div class="abs" style="left:16px;right:16px;top:458px;text-align:center;font-size:11.5px">Not that spot. The piece slides back gently. No penalty.</div>
       ${tray('ark', 3, [6, 8, 4, 7], 484)}
       <div class="abs" style="left:16px;right:16px;top:600px;display:flex;gap:8px"><span class="pill on">Show one spot</span></div>`
    ),
    '04-results': () => page(
      topbar({ title: 'Story Mosaic', sub: 'Noah’s ark complete', stars: '★ 2' }) +
      `<img class="abs" src="assets/scene-ark.svg" style="left:30px;top:86px;width:300px;height:300px;border-radius:16px;box-shadow:0 0 30px rgba(242,201,76,.35)">
       <div class="abs" style="left:0;right:0;top:396px;text-align:center;font-size:22px;letter-spacing:6px;color:var(--warn)">★★☆</div>
       <div class="abs glass" style="left:20px;right:20px;top:434px;padding:14px 16px">
         <div class="verse" style="font-size:14px">“Thus did Noah; according to all that God commanded him, so did he.”</div>
         <div class="ref">Genesis 6:22 (KJV)</div></div>
       <div class="bottom" style="bottom:28px"><div class="btn g">Play again</div><div class="btn p">Continue →</div></div>`
    ),
  },
};

// ---------- helpers for the remaining games ----------
const C = (x, y, d, bg, extra = '') => `<div class="abs" style="left:${x}px;top:${y}px;width:${d}px;height:${d}px;border-radius:50%;background:${bg};${extra}"></div>`;
const RR = (x, y, w, h, bg, extra = '') => `<div class="abs" style="left:${x}px;top:${y}px;width:${w}px;height:${h}px;border-radius:8px;background:${bg};${extra}"></div>`;
const TX = (x, y, w, txt, extra = '') => `<div class="abs" style="left:${x}px;top:${y}px;width:${w}px;${extra}">${txt}</div>`;
const OPT = (y, label, extra = '') => `<div class="abs glass" style="left:16px;right:16px;top:${y}px;height:44px;display:flex;align-items:center;padding:0 14px;font-size:13px;${extra}">${label}</div>`;
const BUB = (y, txt) => `<div class="bubble" style="left:16px;right:16px;top:${y}px;text-align:center">${txt}</div>`;
const CARD = (y, h, inner, extra = '') => `<div class="abs glass" style="left:16px;right:16px;top:${y}px;height:${h}px;padding:12px;${extra}">${inner}</div>`;
const PILLS = (y, items) => `<div class="abs" style="left:16px;right:16px;top:${y}px;display:flex;gap:6px">${items.map(([t, on]) => `<span class="pill${on ? ' on' : ''}">${t}</span>`).join('')}</div>`;
const BOTTOM = (a, b) => `<div class="bottom" style="bottom:28px"><div class="btn g">${a}</div><div class="btn p">${b}</div></div>`;
const RESULT = (char, stars, name, sub, verse, ref) => resultsBlock({ character: char, stars, title: name, sub, verse, ref }) + BOTTOM('Play again', 'Next →');
const mk = (id, name, S) => ({ id, name, shots: {
  '01-start': () => page(S.start()), '02-play': () => page(S.play()), '03-hint': () => page(S.hint()), '04-results': () => page(S.result()),
} });
const sixOrd = (n) => Array.from({ length: n }, (_, i) => i);

// Manna Match (match-3, 7x7)
const MANNA_COL = ['#E8B86B', '#8E4BA8', '#4AA3DF', '#7BA35C', '#F2C94C'];
const mannaBoard = (hint = null, sel = null) => {
  let h = `<div class="abs glass" style="left:14px;top:122px;width:332px;height:332px;border-radius:18px"></div>`;
  for (let r = 0; r < 7; r++) for (let c = 0; c < 7; c++) {
    const t = (r * 3 + c * 2 + r * c) % 5;
    const hi = (hint && hint.some(([a, b]) => a === r && b === c)) || (sel && sel[0] === r && sel[1] === c);
    h += C(22 + c * 44 + 2, 132 + r * 44 + 2, 40, `radial-gradient(circle at 35% 30%,rgba(255,255,255,.6) 0,${MANNA_COL[t]} 60%)`,
      hi ? 'outline:3px solid #F2C94C;outline-offset:2px;box-shadow:0 0 14px rgba(242,201,76,.9)' : 'box-shadow:0 3px 6px rgba(0,0,0,.25)');
  }
  return h;
};
const manna = mk('manna', 'Manna Match', {
  start: () => topbar({ title: 'Manna Match', sub: 'Sinai journey · Level 6', stars: '★ 9' }) +
    CARD(86, 112, `<div class="eyebrow">Journey verse</div><div class="verse" style="margin-top:6px;font-size:12.5px">“Then said the LORD unto Moses, Behold, I will rain bread from heaven for you.”</div><div class="ref">Exodus 16:4 (KJV)</div>`) +
    PILLS(210, [['Goal: 12 bread', 1], ['Moves 20', 0], ['Rest: 1', 0]]) +
    levelGrid(15, 6, [3, 3, 2, 3, 3, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1]) +
    BOTTOM('Desert road', 'Start level 6'),
  play: () => topbar({ title: 'Manna Match', sub: 'Level 6 · Sinai', stars: '★ 2' }) +
    CARD(80, 36, `<span style="font-size:12px">Basket  bread 7/12 · fish 1/3 · jar 0</span>`, 'padding:6px 12px') +
    mannaBoard(null) +
    PILLS(470, [['Moves 14', 0], ['Rest (shuffle)', 0], ['Verse', 0]]) +
    TX(16, 510, 328, `<span class="dim" style="font-size:11.5px">Swap two neighbouring tiles. Three in a row clears. Tiles past the goal do nothing.</span>`),
  hint: () => topbar({ title: 'Manna Match', sub: 'Level 6 · Hint used', stars: '★ 2' }) +
    CARD(80, 36, `<span style="font-size:12px">Basket  bread 7/12 · fish 1/3 · jar 0</span>`, 'padding:6px 12px') +
    mannaBoard([[2, 2], [2, 3]]) +
    BUB(470, 'Hint: swap the two outlined tiles to line up three bread') +
    PILLS(520, [['Moves 14', 0], ['Rest (shuffle)', 0]]),
  result: () => RESULT('game_char_shepherd', 3, 'Manna Match', 'Sinai journey · Level 6 complete',
    'Then said the LORD unto Moses, Behold, I will rain bread from heaven for you; and the people shall go out and gather a certain rate every day.', 'Exodus 16:4 (KJV)'),
});

// Fruit of the Spirit (colour sort, 9 jars)
const FRUIT_COL = ['#E84A5F', '#F2C94C', '#E9EEF5', '#4AA3DF', '#F39A4E', '#7BD389', '#8E4BA8', '#C06BE0', '#6FA35C'];
const JAR = (x, y, layers, extra = '') => `<div class="abs" style="left:${x}px;top:${y}px;width:64px;height:140px;border-radius:6px 6px 22px 22px;border:2px solid var(--glassb);background:var(--board);overflow:hidden;${extra}">${layers.map((c, i) => `<div style="position:absolute;left:0;right:0;bottom:${i * 32}px;height:32px;background:${c}"></div>`).join('')}</div>`;
const jars = (sel = -1, hintJar = -1) => {
  let h = '';
  for (let j = 0; j < 9; j++) {
    const x = 30 + (j % 3) * 118, y = 128 + Math.floor(j / 3) * 150;
    const layers = [0, 1, 2, 3].map((k) => FRUIT_COL[(j + k * 2) % 9]);
    h += JAR(x, y, layers, j === sel ? 'outline:3px solid #F39A4E;outline-offset:3px' : (j === hintJar ? 'outline:3px dashed #F2C94C;outline-offset:3px' : ''));
  }
  h += JAR(90, 600, [], 'opacity:.6') + JAR(206, 600, [], 'opacity:.6');
  return h;
};
const fruit = mk('fruit', 'Fruit of the Spirit', {
  start: () => topbar({ title: 'Fruit of the Spirit', sub: 'Choose a jar puzzle', stars: '★ 11' }) +
    CARD(86, 100, `<div class="eyebrow">Galatians 5:22-23</div><div class="verse" style="margin-top:6px;font-size:12.5px">“But the fruit of the Spirit is love, joy, peace, longsuffering…”</div><div class="ref">Nine fruits, nine jars</div>`) +
    PILLS(200, [['Undo: free', 1], ['Hint: free', 1]]) +
    levelGrid(15, 5, [3, 3, 3, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('Preview', 'Start level 5'),
  play: () => topbar({ title: 'Fruit of the Spirit', sub: 'Level 5 · 3 of 9 sorted', stars: '★ 2' }) +
    PILLS(80, [['Moves 9', 0], ['Undo', 0], ['Hint', 0]]) + jars(4) +
    TX(16, 690, 328, `<span class="dim" style="font-size:11.5px;text-align:center;display:block">Tap a jar, then tap another to pour its top layer.</span>`),
  hint: () => topbar({ title: 'Fruit of the Spirit', sub: 'Level 5 · Hint used', stars: '★ 2' }) +
    PILLS(80, [['Moves 9', 0], ['Undo', 0], ['Hint', 1]]) + jars(-1, 1) +
    BUB(656, 'Hint: pour the top layer of the dashed jar onto the matching fruit'),
  result: () => RESULT('game_char_dove', 2, 'Fruit of the Spirit', 'Level 5 complete',
    'But the fruit of the Spirit is love, joy, peace, longsuffering, gentleness, goodness, faith, Meekness, temperance: against such there is no law.', 'Galatians 5:22-23 (KJV)'),
});

// Seven Trumpets (brick breaker)
const trumpets = mk('trumpet', 'Seven Trumpets', {
  start: () => topbar({ title: 'Seven Trumpets', sub: 'Jericho walls · Level 3', stars: '★ 5' }) +
    CARD(86, 112, `<div class="eyebrow">Joshua 6</div><div class="verse" style="margin-top:6px;font-size:12.5px">“So the people shouted when the priests blew with the trumpets.”</div><div class="ref">Joshua 6:20 (KJV)</div>`) +
    PILLS(210, [['Wider paddle: levels 1-3', 1], ['Slow motion off', 0]]) +
    levelGrid(15, 3, [3, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('Settings', 'Start level 3'),
  play: () => {
    let h = topbar({ title: 'Seven Trumpets', sub: 'Level 3 · Circuit 4 of 7', stars: '★ 1' });
    for (let r = 0; r < 6; r++) for (let c = 0; c < 8; c++) {
      if ((r * 5 + c * 3) % 7 === 0) continue;
      h += RR(16 + c * 41, 116 + r * 26, 39, 22, ['#B9A37A', '#A38C62', '#C9B48C'][(r + c) % 3], 'border:1px solid rgba(0,0,0,.2)');
    }
    return h + C(170, 470, 16, 'radial-gradient(circle,#FFF6C8 0,#F2C94C 70%)', 'box-shadow:0 0 16px rgba(242,201,76,.9)') +
      RR(136, 680, 92, 16, 'linear-gradient(180deg,#F39A4E,#E17D2F)', 'border-radius:8px;box-shadow:0 0 12px rgba(232,119,46,.6)') +
      PILLS(602, [['Balls 3', 0], ['Try again: 1 free', 0]]);
  },
  hint: () => {
    let h = topbar({ title: 'Seven Trumpets', sub: 'Level 3 · Hint used', stars: '★ 1' });
    for (let r = 0; r < 6; r++) for (let c = 0; c < 8; c++) {
      if ((r * 5 + c * 3) % 7 === 0) continue;
      h += RR(16 + c * 41, 116 + r * 26, 39, 22, ['#B9A37A', '#A38C62', '#C9B48C'][(r + c) % 3], 'border:1px solid rgba(0,0,0,.2)');
    }
    return h + `<svg class="abs" style="left:0;top:0" width="360" height="780"><line x1="178" y1="470" x2="60" y2="180" stroke="#F2C94C" stroke-width="2" stroke-dasharray="6 6" opacity=".9"/></svg>` +
      C(170, 470, 16, 'radial-gradient(circle,#FFF6C8 0,#F2C94C 70%)') +
      RR(136, 680, 92, 16, 'linear-gradient(180deg,#F39A4E,#E17D2F)', 'border-radius:8px') + BUB(560, 'Hint: the ball bounces off the left wall toward the cracked brick');
  },
  result: () => RESULT('game_char_explorer', 3, 'Seven Trumpets', 'Jericho walls fell · Level 3 complete',
    'So the people shouted when the priests blew with the trumpets: and it came to pass, when the people heard the sound of the trumpet, and the people shouted with a great shout, that the wall fell down flat.', 'Joshua 6:20 (KJV)'),
});

// Dove Pop (bubble shooter)
const DOVE_COL = ['#E9EEF5', '#4AA3DF', '#F2C94C', '#7BD389', '#E84A5F'];
const dovePop = (aim = true, peek = false) => {
  let h = '';
  for (let r = 0; r < 8; r++) for (let c = 0; c < (r % 2 ? 9 : 10); c++) {
    if ((r * 7 + c * 5) % 6 === 0) continue;
    const x = 18 + c * 34 + (r % 2 ? 17 : 0), y = 108 + r * 30;
    h += C(x, y, 32, `radial-gradient(circle at 35% 30%,#fff 0,${DOVE_COL[(r + c) % 5]} 65%)`, 'box-shadow:0 2px 5px rgba(0,0,0,.25)');
  }
  if (aim) h += `<svg class="abs" style="left:0;top:0" width="360" height="780"><line x1="180" y1="660" x2="250" y2="210" stroke="#F2C94C" stroke-width="2" stroke-dasharray="5 6"/></svg>`;
  h += C(164, 644, 32, `radial-gradient(circle at 35% 30%,#fff 0,${DOVE_COL[1]} 65%)`, 'box-shadow:0 0 12px rgba(74,163,223,.7)');
  if (peek) h += C(270, 690, 22, `radial-gradient(circle at 35% 30%,#fff 0,${DOVE_COL[3]} 65%)`, 'opacity:.85');
  return h;
};
const dove = mk('dove', 'Dove Pop', {
  start: () => topbar({ title: 'Dove Pop', sub: 'Noah’s window · Level 9', stars: '★ 17' }) +
    CARD(86, 112, `<div class="eyebrow">Genesis 8:11</div><div class="verse" style="margin-top:6px;font-size:12.5px">“And the dove came in to him in the evening; and, lo, in her mouth was an olive leaf pluckt off.”</div>`) +
    PILLS(210, [['Peek next bubble: free', 1], ['No timer', 1]]) +
    levelGrid(15, 9, [3, 3, 3, 3, 2, 3, 2, 3, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('Preview', 'Start level 9'),
  play: () => topbar({ title: 'Dove Pop', sub: 'Level 9 · 42 doves left', stars: '★ 3' }) + dovePop(true) +
    PILLS(738, [['Peek', 0]]).replace('top:738px', 'top:732px'),
  hint: () => topbar({ title: 'Dove Pop', sub: 'Level 9 · Peek used', stars: '★ 2' }) + dovePop(true, true) +
    BUB(728, 'Hint: the next dove is green. Aim near the lamp to pop three'),
  result: () => RESULT('game_char_dove', 3, 'Dove Pop', 'Level 9 complete · all doves home',
    'And the dove came in to him in the evening; and, lo, in her mouth was an olive leaf pluckt off: so Noah knew that the waters were abated from off the earth.', 'Genesis 8:11 (KJV)'),
});

// Shepherd's Path (endless lane runner)
const lanes = (shepherdLane = 1, lambs = true) => {
  let h = `<svg class="abs" style="left:0;top:0" width="360" height="780">
  <polygon points="150,120 210,120 330,660 30,660" fill="rgba(255,255,255,.12)" stroke="rgba(255,255,255,.3)"/>
  <line x1="180" y1="120" x2="180" y2="660" stroke="rgba(255,255,255,.2)" stroke-dasharray="10 10"/>
  <line x1="165" y1="120" x2="105" y2="660" stroke="rgba(255,255,255,.2)" stroke-dasharray="10 10"/>
  <line x1="195" y1="120" x2="255" y2="660" stroke="rgba(255,255,255,.2)" stroke-dasharray="10 10"/></svg>`;
  h += `<div class="abs" style="left:88px;top:330px;width:26px;height:26px;border-radius:50%;background:#3E6B2E;border:2px solid #7BD389"></div>`;
  if (lambs) h += C(245, 240, 16, '#fff', 'box-shadow:0 0 8px rgba(255,255,255,.8)') + C(118, 520, 16, '#fff');
  h += `<div class="abs" style="left:${shepherdLane === 0 ? 102 : shepherdLane === 1 ? 165 : 226}px;top:568px;width:58px;height:60px;background:url(assets/game_char_shepherd.png) center/contain no-repeat"></div>`;
  return h;
};
const shepherdPath = mk('path', "Shepherd's Path", {
  start: () => topbar({ title: "Shepherd's Path", sub: 'Psalm 23 · Hills of Judea', stars: '★ 4' }) +
    CARD(86, 112, `<div class="eyebrow">Psalm 23</div><div class="verse" style="margin-top:6px;font-size:12.5px">“The LORD is my shepherd; I shall not want.”</div><div class="ref">Psalm 23:1 (KJV)</div>`) +
    PILLS(210, [['No death. Lambs drop.', 1], ['Pace: calm', 1]]) +
    CARD(248, 62, `<div style="font-size:12.5px">Best run: 812 m · 14 lambs · Psalm lines unlocked at 100 m steps</div>`) +
    BOTTOM('Settings', 'Start run'),
  play: () => topbar({ title: "Shepherd's Path", sub: 'Run · 240 m', stars: '★ 14' }) + lanes(1) +
    PILLS(96, [['Lambs 4', 0], ['Distance 240 m', 0]]).replace('top:96px', 'top:94px') +
    TX(16, 690, 328, `<span class="dim" style="font-size:11.5px;text-align:center;display:block">Tap left or right to switch lanes.</span>`),
  hint: () => topbar({ title: "Shepherd's Path", sub: 'Run · Thorn ahead', stars: '★ 14' }) + lanes(2) +
    BUB(440, 'A thorn bush is ahead. Move right. Hitting it only pauses you and drops one lamb'),
  result: () => RESULT('game_char_shepherd', 3, "Shepherd's Path", 'Run complete · 812 m',
    'The LORD is my shepherd; I shall not want. He maketh me to lie down in green pastures: he leadeth me beside the still waters.', 'Psalm 23:1-2 (KJV)'),
});

// Daily Name (word guessing, 5-letter name, 6 rows)
const DN_COL = { g: '#7BD389', y: '#F2C94C', x: 'rgba(255,255,255,.22)' };
const dnRow = (y, word, marks) => word.split('').map((ch, i) => RR(44 + i * 56, y, 48, 48, marks ? DN_COL[marks[i]] : 'var(--glass)', `display:grid;place-items:center;font-weight:700;font-size:18px;color:${marks && marks[i] !== 'x' ? '#2D0938' : 'var(--t1)'};border:1px solid var(--glassb)`).replace('</div>', `${ch}</div>`)).join('');
const dnKeys = () => ['QWERTYUIOP', 'ASDFGHJKL', 'ZXCVBNM'].map((row, r) => `<div class="abs" style="left:0;right:0;top:${530 + r * 44}px;display:flex;justify-content:center;gap:4px">${row.split('').map((k) => `<span class="pill" style="width:28px;padding:0;justify-content:center">${k}</span>`).join('')}</div>`).join('');
const dailyName = mk('name', 'Daily Name', {
  start: () => topbar({ title: 'Daily Name', sub: 'Today’s name · 6 guesses', stars: '' }) +
    CARD(86, 110, `<div class="eyebrow">Today’s name: one per day, offline</div><div class="verse" style="margin-top:6px;font-size:12.5px">The name is taken from a short KJV story line you read after each guess.</div>`) +
    [0, 1, 2, 3, 4, 5].map((r) => dnRow(230 + r * 54, '     ', null)).join('') +
    TX(16, 570, 328, `<span class="dim" style="font-size:11.5px;text-align:center;display:block">No streaks. A missed day costs nothing.</span>`) +
    BOTTOM('How it works', 'Guess now'),
  play: () => topbar({ title: 'Daily Name', sub: 'Guess 3 of 6', stars: '' }) +
    dnRow(120, 'AARON', ['x', 'x', 'x', 'y', 'x']) +
    dnRow(176, 'MOSES', ['g', 'g', 'g', 'g', 'g']).replace('top:176px', 'top:176px') +
    dnRow(232, 'JOSE ', ['y', 'x', 'g', 'x', 'x']) +
    dnRow(288, '     ', null) + dnKeys() +
    TX(16, 470, 328, `<span class="dim" style="font-size:11.5px;text-align:center;display:block">Green: right letter, right place. Amber: in the name, other place. Grey: not in the name.</span>`),
  hint: () => topbar({ title: 'Daily Name', sub: 'Guess 3 of 6 · Hint', stars: '' }) +
    dnRow(120, 'AARON', ['x', 'x', 'x', 'y', 'x']) + dnRow(176, 'ISAAC', ['x', 'x', 'x', 'x', 'x']) +
    dnRow(232, 'JOSE ', ['y', 'x', 'g', 'x', 'x']) + dnRow(288, '     ', null) + dnKeys() +
    BUB(440, 'Hint: the name ends with the letter S. Story line after your next guess'),
  result: () => RESULT('game_char_explorer', 3, 'Daily Name', 'Solved in 3 guesses',
    'And she called his name Moses: and she said, Because I drew him out of the water.', 'Exodus 2:10 (KJV)'),
});

// ---------- Existing games (question-style) ----------
const qGame = (id, name, c) => {
  const optsHtml = (hi = -1) => c.opts.map((o, i) => OPT(222 + i * 56, `<span class="dim" style="width:22px">${'ABCD'[i]}</span>${o}`,
    i === hi ? 'border-color:#F2C94C;box-shadow:0 0 14px rgba(242,201,76,.55)' : '')).join('');
  const prog = `<div class="abs" style="left:16px;right:16px;top:150px;height:8px;border-radius:4px;background:var(--glass)"><div style="width:${c.pct}%;height:100%;border-radius:4px;background:linear-gradient(90deg,#F39A4E,#E17D2F)"></div></div>`;
  return mk(id, name, {
    start: () => topbar({ title: name, sub: c.sub, stars: `★ ${c.best}` }) +
      CARD(86, 112, `<div class="eyebrow">${c.eyebrow}</div><div class="verse" style="margin-top:6px;font-size:12.5px">${c.preview}</div>`) +
      PILLS(214, [[c.tag1, 1], [c.tag2, 0]]) + levelGrid(10, c.lvl, c.states) + BOTTOM('Settings', `Start level ${c.lvl}`),
    play: () => topbar({ title: name, sub: `Question ${c.q} of 10`, stars: `★ ${c.stars}` }) + prog +
      CARD(170, 110, `<div style="font-size:14px;font-weight:600;line-height:1.35">${c.prompt}</div>`) + optsHtml(-1) +
      PILLS(470, [['Hint: free', 0], ['Lives ♥♥♥', 0]]),
    hint: () => topbar({ title: name, sub: `Question ${c.q} of 10 · Hint`, stars: `★ ${c.stars}` }) + prog +
      CARD(170, 110, `<div style="font-size:14px;font-weight:600;line-height:1.35">${c.prompt}</div>`) + optsHtml(c.correct) +
      BUB(420, c.hint),
    result: () => RESULT(c.char, c.resStars, name, c.resSub, c.verse, c.ref),
  });
};
const quiz = qGame('trivia', 'Bible Quiz', { sub: 'Level 4 · Gospels', best: 12, eyebrow: 'Verse first', preview: '“For God so loved the world, that he gave his only begotten Son.”', tag1: 'Verse after each answer', tag2: 'Quick mode', lvl: 4, states: [3, 3, 2, -1, -1, -1, -1, -1, -1, -1], q: 6, pct: 60, stars: 2, prompt: 'Which prophet led Israel out of Egypt?', opts: ['Moses', 'Elijah', 'Samuel', 'Joshua'], correct: 0, hint: 'Hint: he was raised as the son of Pharaoh’s daughter, and he was a shepherd in Midian', char: 'game_char_shepherd', resStars: 2, resSub: 'Level 4 complete', verse: 'For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.', ref: 'John 3:16 (KJV)' });
const whoAmI = qGame('who_am_i', 'Who am I?', { sub: 'Riddle · Old Testament', best: 8, eyebrow: 'Clue 1 of 3', preview: '“I was sold by my brothers, yet I became a ruler in Egypt.”', tag1: 'Clues reveal slowly', tag2: 'No penalty for a clue', lvl: 2, states: [3, 2, -1, -1, -1, -1, -1, -1, -1, -1], q: 3, pct: 30, stars: 3, prompt: 'Clue 3: I was born in Ur of the Chaldees and was called the father of many nations. Who am I?', opts: ['Abraham', 'Isaac', 'Jacob', 'Noah'], correct: 0, hint: 'Hint: the next clue names the land of Canaan you were promised', char: 'game_char_explorer', resStars: 3, resSub: 'Riddle solved with 1 clue', verse: 'Now the LORD had said unto Abram, Get thee out of thy country, and from thy kindred, and from thy father’s house, unto a land that I will shew thee.', ref: 'Genesis 12:1 (KJV)' });
const oldNew = qGame('old_or_new', 'Old or New?', { sub: 'Quick facts', best: 7, eyebrow: 'Old or new?', preview: '“Jesus Christ the same yesterday, and to day, and for ever.”', tag1: 'Two answers', tag2: 'Fast', lvl: 3, states: [3, 3, 3, -1, -1, -1, -1, -1, -1, -1], q: 7, pct: 70, stars: 3, prompt: 'Is the Book of Ruth in the Old Testament or the New Testament?', opts: ['Old Testament', 'New Testament'], correct: 0, hint: 'Hint: it comes straight after Judges', char: 'game_char_dove', resStars: 3, resSub: 'Level 3 complete', verse: 'Jesus Christ the same yesterday, and to day, and for ever.', ref: 'Hebrews 13:8 (KJV)' });
const whoSaid = qGame('who_said_it', 'Who Said It?', { sub: 'Round 1 of 5', best: 4, eyebrow: 'Quote', preview: '“Am I my brother’s keeper?”', tag1: 'Round of 5', tag2: 'Quote then name', lvl: 1, states: [2, -1, -1, -1, -1, -1, -1, -1, -1, -1], q: 2, pct: 20, stars: 1, prompt: 'Who said: “Am I my brother’s keeper?”', opts: ['Cain', 'Abel', 'Esau', 'Judas'], correct: 0, hint: 'Hint: the speaker was asked where his brother was', char: 'game_char_shepherd', resStars: 2, resSub: 'Round 5 complete', verse: 'And the LORD said unto Cain, Where is Abel thy brother? And he said, I know not: Am I my brother’s keeper?', ref: 'Genesis 4:9 (KJV)' });
const fillGap = qGame('fill_gap', 'Fill the Gap', { sub: 'Proverbs', best: 9, eyebrow: 'Fill the gap', preview: '“Train up a child in the way he should go: and when he is old, he will not ____ from it.”', tag1: 'Verse after each answer', tag2: 'Hint: free', lvl: 5, states: [3, 3, 3, 3, 2, -1, -1, -1, -1, -1], q: 4, pct: 40, stars: 2, prompt: 'Train up a child in the way he should go: and when he is old, he will not ____ from it.', opts: ['depart', 'return', 'stumble', 'forget'], correct: 0, hint: 'Hint: the word means to leave', char: 'game_char_explorer', resStars: 3, resSub: 'Level 5 complete', verse: 'Train up a child in the way he should go: and when he is old, he will not depart from it.', ref: 'Proverbs 22:6 (KJV)' });
const ladder = qGame('ladder', 'Ladder of Faith', { sub: 'Rung 6 of 12', best: 6, eyebrow: 'Rung 6', preview: '“Trust in the LORD with all thine heart; and lean not unto thine own understanding.”', tag1: '3 lives', tag2: 'Pray beat every 5', lvl: 6, states: [3, 3, 3, 3, 3, -1, -1, -1, -1, -1], q: 6, pct: 50, stars: 3, prompt: 'Who led the people across the Red Sea on dry ground?', opts: ['Moses', 'Aaron', 'Caleb', 'Gideon'], correct: 0, hint: 'Ask the shepherd: removes two wrong answers, once per run', char: 'game_char_shepherd', resStars: 3, resSub: 'Rung 6 climbed · 5 right in a row', verse: 'Trust in the LORD with all thine heart; and lean not unto thine own understanding.', ref: 'Proverbs 3:5 (KJV)' });

const verseBuilder = mk('verse_builder', 'Verse Builder', {
  start: () => topbar({ title: 'Verse Builder', sub: 'Psalm 46 · Level 2', stars: '★ 4' }) +
    CARD(86, 112, `<div class="eyebrow">Put the words in order</div><div class="verse" style="margin-top:6px;font-size:12.5px">Words are shuffled. Tap them in the order of the verse.</div><div class="ref">Psalm 46:1 (KJV)</div>`) +
    levelGrid(10, 2, [3, 2, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('Show verse', 'Start level 2'),
  play: () => topbar({ title: 'Verse Builder', sub: 'Psalm 46:1 · 5 of 10', stars: '★ 2' }) +
    CARD(90, 110, `<div class="verse" style="font-size:15px">${['God', 'is', 'our', 'refuge', 'and', 'strength,', '', '', '', ''].map((w) => (w ? `<span style="display:inline-block;margin:2px 4px;padding:4px 8px;border-radius:10px;background:var(--glass);border:1px solid var(--glassb)">${w}</span>` : '<span style="display:inline-block;margin:2px 4px;width:50px;height:26px;border-radius:8px;border:1px dashed var(--glassb)"></span>')).join('')}</div>`) +
    TX(16, 230, 328, `<div style="display:flex;flex-wrap:wrap;gap:8px;justify-content:center">${['help', 'present', 'a', 'very', 'in', 'trouble.'].map((w) => `<span class="pill" style="height:40px;padding:0 14px">${w}</span>`).join('')}</div>`) +
    PILLS(470, [['Undo', 0], ['Hint: first word', 0], ['Check', 1]]),
  hint: () => topbar({ title: 'Verse Builder', sub: 'Psalm 46:1 · Hint used', stars: '★ 2' }) +
    CARD(90, 110, `<div class="verse" style="font-size:15px">${['God', 'is', 'our', 'refuge', 'and', 'strength,', 'a', '', '', ''].map((w) => (w ? `<span style="display:inline-block;margin:2px 4px;padding:4px 8px;border-radius:10px;background:var(--glass);border:1px solid var(--glassb)">${w}</span>` : '<span style="display:inline-block;margin:2px 4px;width:50px;height:26px;border-radius:8px;border:1px dashed #F2C94C"></span>')).join('')}</div>`) +
    BUB(420, 'Hint: the next word is “very”. Stars capped at 2'),
  result: () => RESULT('game_char_shepherd', 3, 'Verse Builder', 'Verse complete', 'God is our refuge and strength, a very present help in trouble.', 'Psalm 46:1 (KJV)'),
});

const wordSearch = mk('word_search', 'Word Search', {
  start: () => topbar({ title: 'Word Search', sub: 'Names of the Bible', stars: '★ 5' }) +
    CARD(86, 110, `<div class="eyebrow">Find the names</div><div class="verse" style="margin-top:6px;font-size:12.5px">Words run in any direction. Tap a start letter, then the end letter.</div>`) +
    levelGrid(10, 3, [3, 2, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('Word list', 'Start level 3'),
  play: () => {
    const L = 'ABRAHAMNOSESDAVIDRUTHELIJAHPETERJOHNMARYAMOSJOSEPHAARON';
    let h = topbar({ title: 'Word Search', sub: 'Level 3 · Found 2 of 6', stars: '★ 2' });
    h += `<div class="abs glass" style="left:14px;top:98px;width:332px;height:332px;padding:8px;display:grid;grid-template-columns:repeat(8,1fr);gap:3px">`;
    for (let i = 0; i < 64; i++) { const hit = [0, 1, 2, 8, 9, 10].includes(i); h += `<div style="display:grid;place-items:center;border-radius:6px;font-size:14px;font-weight:700;background:${hit ? '#F39A4E' : 'var(--board)'};color:${hit ? '#fff' : 'var(--t1)'}">${L[i % L.length]}</div>`; }
    h += '</div>';
    return h + PILLS(446, [['ABRAHAM ✓', 1], ['DAVID', 0], ['RUTH ✓', 1]]) + TX(16, 490, 328, `<span class="dim" style="font-size:11.5px">Words not found yet: DAVID · ELIJAH · JOHN · MARY</span>`);
  },
  hint: () => topbar({ title: 'Word Search', sub: 'Level 3 · Hint used', stars: '★ 2' }) +
    `<div class="abs glass" style="left:14px;top:98px;width:332px;height:332px;padding:8px;display:grid;grid-template-columns:repeat(8,1fr);gap:3px">` +
    Array.from({ length: 64 }, (_, i) => `<div style="display:grid;place-items:center;border-radius:6px;font-size:14px;font-weight:700;background:${i === 20 ? 'rgba(242,201,76,.45)' : 'var(--board)'};border:${i === 20 ? '2px solid #F2C94C' : 'none'}">${'ABRAHAMNOSESDAVIDRUTHELIJAHPETERJOHNMARYAMOSJOSEPHAARON'[i % 50]}</div>`).join('') + '</div>' +
    BUB(446, 'Hint: DAVID starts in the highlighted square. Letters are free'),
  result: () => RESULT('game_char_explorer', 2, 'Word Search', 'Level 3 complete · 6 of 6 names', 'Neither shall thy name any more be called Abram, but thy name shall be Abraham; for a father of many nations have I made thee.', 'Genesis 17:5 (KJV)'),
});

const memory = mk('memory_match', 'Memory Match', {
  start: () => topbar({ title: 'Memory Match', sub: 'Pairs · Level 2', stars: '★ 6' }) +
    CARD(86, 110, `<div class="eyebrow">Flip two tiles</div><div class="verse" style="margin-top:6px;font-size:12.5px">Match the pairs in as few moves as you can. No timer.</div>`) +
    levelGrid(10, 2, [3, 2, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('New deck', 'Start level 2'),
  play: () => {
    const cards = ['Ark', 'Dove', 'Ark', 'Star', 'Dove', 'Star', 'Lamp', 'Lamp'];
    let h = topbar({ title: 'Memory Match', sub: 'Level 2 · 3 moves', stars: '★ 2' });
    for (let i = 0; i < 16; i++) {
      const r = Math.floor(i / 4), c = i % 4, x = 22 + c * 80, y = 120 + r * 84;
      const up = [0, 2, 5].includes(i);
      h += `<div class="abs" style="left:${x}px;top:${y}px;width:72px;height:74px;border-radius:12px;display:grid;place-items:center;font-size:12px;font-weight:700;background:${up ? 'linear-gradient(180deg,#fff,#EADFF0)' : 'linear-gradient(180deg,rgba(255,255,255,.25),rgba(255,255,255,.08))'};color:#2D0938;border:1px solid var(--glassb)">${up ? cards[i % 8] : ''}</div>`;
    }
    return h + PILLS(468, [['Moves 3', 0], ['Pairs 1 of 4', 1]]);
  },
  hint: () => topbar({ title: 'Memory Match', sub: 'Level 2 · Mismatch', stars: '★ 2' }) +
    Array.from({ length: 16 }, (_, i) => { const r = Math.floor(i / 4), c = i % 4; const up = [0, 3].includes(i); return `<div class="abs" style="left:${22 + c * 80}px;top:${120 + r * 84}px;width:72px;height:74px;border-radius:12px;display:grid;place-items:center;font-size:12px;font-weight:700;background:${up ? 'linear-gradient(180deg,#fff,#EADFF0)' : 'linear-gradient(180deg,rgba(255,255,255,.25),rgba(255,255,255,.08))'};color:#2D0938;border:${up ? '2px solid #F2C94C' : '1px solid var(--glassb)'}">${up ? ['Ark', 'Star'][i === 0 ? 0 : 1] : ''}</div>`; }).join('') +
    BUB(470, 'Not a match. The two tiles turn back in a moment. No penalty'),
  result: () => RESULT('game_char_dove', 3, 'Memory Match', 'Level 2 complete · 6 moves', 'Thy word have I hid in mine heart, that I might not sin against thee.', 'Psalm 119:11 (KJV)'),
});

const timeline = mk('timeline', 'Story Timeline', {
  start: () => topbar({ title: 'Story Timeline', sub: 'Genesis to Exodus · Level 1', stars: '★ 3' }) +
    CARD(86, 110, `<div class="eyebrow">Tap the events in order</div><div class="verse" style="margin-top:6px;font-size:12.5px">Put the Bible events in the order they happen.</div>`) +
    levelGrid(10, 1, [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('New timeline', 'Start level 1'),
  play: () => {
    const ev = ['Creation', 'The flood', 'Abraham called', 'Exodus from Egypt', 'Crossing the Red Sea'];
    let h = topbar({ title: 'Story Timeline', sub: 'Level 1 · Placed 2 of 5', stars: '★ 1' });
    ev.slice(0, 2).forEach((e, i) => { h += CARD(90 + i * 62, 52, `<div style="font-weight:700;font-size:13px">${i + 1}. ${e}</div>`, 'padding:10px 14px'); });
    h += CARD(218, 52, `<div class="dim" style="font-size:13px">Place here</div>`, 'border-style:dashed;padding:10px 14px');
    ['Abraham called', 'Exodus from Egypt', 'Crossing the Red Sea'].forEach((e, i) => { h += CARD(300 + i * 62, 52, `<div style="font-size:13px">${e}</div>`, 'padding:10px 14px'); });
    return h;
  },
  hint: () => topbar({ title: 'Story Timeline', sub: 'Level 1 · Hint used', stars: '★ 1' }) +
    CARD(90, 52, `<div style="font-weight:700;font-size:13px">1. Creation</div>`, 'padding:10px 14px') +
    CARD(152, 52, `<div style="font-weight:700;font-size:13px">2. The flood</div>`, 'padding:10px 14px') +
    CARD(214, 52, `<div class="dim" style="font-size:13px">Place here</div>`, 'border:2px dashed #F2C94C;padding:10px 14px') +
    BUB(300, 'Hint: the call of Abraham comes before the Exodus from Egypt'),
  result: () => RESULT('game_char_explorer', 3, 'Story Timeline', 'Level 1 complete · 5 in order', 'In the beginning God created the heaven and the earth.', 'Genesis 1:1 (KJV)'),
});

const road = mk('road', 'Road to Jerusalem', {
  start: () => topbar({ title: 'Road to Jerusalem', sub: 'Roll to square 20', stars: '★ 4' }) +
    CARD(86, 110, `<div class="eyebrow">The road</div><div class="verse" style="margin-top:6px;font-size:12.5px">“…he stedfastly set his face to go to Jerusalem.”</div><div class="ref">Luke 9:51 (KJV)</div>`) +
    levelGrid(10, 2, [3, 2, -1, -1, -1, -1, -1, -1, -1, -1]) + BOTTOM('Rules', 'Start level 2'),
  play: () => {
    let h = topbar({ title: 'Road to Jerusalem', sub: 'You 8 · Friend 6', stars: '★ 2' });
    for (let i = 0; i < 20; i++) { const r = Math.floor(i / 5), c = r % 2 ? 4 - (i % 5) : i % 5; h += RR(22 + c * 66, 110 + r * 82, 56, 56, i === 7 ? 'linear-gradient(180deg,#F39A4E,#E17D2F)' : 'var(--glass)', `border:1px solid var(--glassb);display:grid;place-items:center;font-weight:700;font-size:13px;color:${i === 7 ? '#fff' : 'var(--t1)'}`).replace('</div>', `${i + 1}</div>`); }
    return h + C(42, 128, 16, '#fff', 'box-shadow:0 0 8px #fff') + PILLS(440, [['Roll the die', 1], ['Verse per square', 0]]);
  },
  hint: () => topbar({ title: 'Road to Jerusalem', sub: 'Square 8 · verse', stars: '★ 2' }) +
    CARD(90, 60, `<div class="verse" style="font-size:12.5px">“And it came to pass, when the time was come that he should be received up…”</div>`) +
    BUB(170, 'Hint: square 8 is a rest stop. Roll again next turn'),
  result: () => RESULT('game_char_shepherd', 3, 'Road to Jerusalem', 'You reached square 20 first', 'And it came to pass, when the time was come that he should be received up, he stedfastly set his face to go to Jerusalem.', 'Luke 9:51 (KJV)'),
});

const GAMES = { vine_words: vineWords, lamp_slide: lampSlide, story_mosaic: storyMosaic, manna, fruit, trumpet: trumpets, dove, path: shepherdPath, name: dailyName, trivia: quiz, who_am_i: whoAmI, verse_builder: verseBuilder, ladder, word_search: wordSearch, memory_match: memory, timeline, road, old_or_new: oldNew, who_said_it: whoSaid, fill_gap: fillGap };

// scenes and paintings as assets
fs.writeFileSync(path.join(ASSETS, 'painting.svg'), paintingSvg());
for (const k of Object.keys(SCENES)) fs.writeFileSync(path.join(ASSETS, `scene-${k}.svg`), sceneSvg(k));

async function main() {
  const ids = ONLY.length ? ONLY : Object.keys(GAMES);
  const browser = await chromium.launch({ executablePath: '/opt/pw-browsers/chromium', args: ['--no-sandbox'] }).catch(() => chromium.launch({ args: ['--no-sandbox'] }));
  const ctx = await browser.newContext({ viewport: { width: 360, height: 780 }, deviceScaleFactor: 3 });
  for (const id of ids) {
    const g = GAMES[id];
    const dir = path.join(OUT, id);
    fs.mkdirSync(dir, { recursive: true });
    for (const [name, make] of Object.entries(g.shots)) {
      const html = make();
      const tmp = path.join(WORK, `${id}-${name}.html`);
      fs.writeFileSync(tmp, html);
      const page = await ctx.newPage();
      await page.goto('file://' + tmp);
      await page.waitForTimeout(150);
      await page.locator('.screen').screenshot({ path: path.join(dir, `${name}.png`) });
      await page.close();
      console.log('wrote', path.join(dir, `${name}.png`));
    }
  }
  await browser.close();
}

main().catch((e) => { console.error(e); process.exit(1); });
