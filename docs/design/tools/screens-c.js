// Batch C: screens 15 to 21. Assistant screens are black and white (prompt 6.11).
// Game copy is sample text. Light game canvas for Lamp Slide only (prompt 6.10, 11.5).
'use strict';
const { glass, btn, P_, S_, Q_, chips, seg, tog, sq, field, row, VERSE, ic, esc } = require('./lib');

const LIGHT_GAME_CANVAS = 'linear-gradient(160deg,#FBEFF8 0%,#F8DBD7 45%,#E0B1EB 100%)';
const bwBubble = (t) => `<div style="align-self:flex-end;max-width:82%;background:var(--glass-ctl);border:1px solid var(--glass-rim);border-radius:18px 18px 4px 18px;padding:12px 14px" class="t-body"><span style="color:var(--text)">${esc(t)}</span></div>`;

module.exports = [
  {
    id: '15-membership', name: 'Membership', chrome: 'dialog', head: { title: 'Membership', sub: 'Church register', icon: 'close' },
    purpose: 'Ask to join the church membership register and see where the request stands.',
    plan: '5.2, 4.7 (dialog)',
    body: () => `
      ${glass(`<div class="eyebrow">Your request</div><div class="t-title" style="margin-top:6px" data-el="status">Under review</div><p class="t-meta" style="margin-top:4px">An administrator reviews each request.</p>`, { size: 'in', el: 'status-card' })}
      <div data-el="form">${field('Full name as in the church register', 'Full name (sample)', { icon: 'profile' })}</div>
      <div data-el="campus-picker"><div class="lbl">Campus</div>${chips(['Embu Headquarters', 'Nairobi Fellowship', 'Meru Outreach', 'Online Campus'], 0)}</div>
      <div style="display:flex;gap:10px;align-items:flex-start">${tog(true, 'confirm')}<span class="t-meta" style="color:var(--text)">I confirm the details are mine and I am 18 or older.</span></div>
      ${P_('Send membership request', { el: 'primary-send' })}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">Not requested yet</div><p class="t-body" style="margin-top:6px">Send a request to join the register. Your details stay with the church office.</p></div>`,
      error: `<div class="glass" data-el="error-card"><div class="t-title">The request did not send</div><p class="t-body" style="margin-top:6px">Check the connection. Your details are kept on this phone.</p><div style="margin-top:12px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in to request membership</div><div style="margin-top:12px">${Q_('Sign in', { go: '12-auth' })}</div></div>`,
    },
    notes: ['Membership rules and the register are the church\'s decision. The copy here only describes the flow.', 'Name shown is sample text.'],
  },
  {
    id: '16-pin-upgrade', name: 'PIN upgrade', chrome: 'dialog', head: { title: 'Update your PIN', sub: 'Move from the older PIN', icon: 'close' },
    purpose: 'Replace an older PIN with a new 4 to 12 digit PIN.',
    plan: '5.2 (dialog)',
    body: () => `
      <p class="t-body" data-el="intro">Your sign-in now uses a longer PIN. Enter your current PIN, then choose a new one.</p>
      ${[['Current PIN', 'Enter current PIN'], ['New PIN (4–12 digits)', 'Choose a new PIN'], ['Confirm new PIN', 'Enter it again']].map(([l, h], i) => `<div data-el="pin-${i + 1}">${field(l, h, { icon: 'lock' })}</div>`).join('')}
      <div style="display:grid;gap:10px;margin-top:6px">${P_('Update PIN', { el: 'primary-update' })}${Q_('Not now', { el: 'later' })}</div>`,
    states: {
      error: `<div class="glass" data-el="error-card"><div class="t-title">That PIN is not right</div><p class="t-body" style="margin-top:6px">Check the current PIN and try again.</p></div>`,
      empty: null,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in first</div><div style="margin-top:12px">${Q_('Sign in', { go: '12-auth' })}</div></div>`,
    },
    notes: ['The PIN length (4 to 12 digits) follows the current code.'],
  },
  {
    id: '17-assistant-chat', name: 'Perazim Assistant', chrome: 'full', bw: true,
    head: { title: 'Perazim Assistant', sub: 'Perazim Guide · on-phone', icon: 'back', right: `<button class="btn icon" aria-label="Assistant settings" data-go="18-assistant-memory">${ic('gear', 20)}</button>` },
    purpose: 'Ask a faith question, see the verses behind the answer, and give feedback.',
    plan: '4.8, 6.11 (full screen, black and white)',
    body: () => `
      <div data-el="chat" style="display:flex;flex-direction:column;gap:14px">
        ${bwBubble('What does the Bible say about worry?')}
        <div class="glass" data-el="answer" style="border-color:#333">
          <p class="t-body" style="color:var(--text)">Jesus tells us not to be anxious about tomorrow, and Paul tells us to bring our requests to God in prayer.</p>
          <div class="t-micro" style="margin-top:10px;color:var(--text2)">Sources</div>
          <p class="verse" style="margin-top:6px"><span style="font-style:italic">Take therefore no thought for the morrow: for the morrow shall take thought for the things of itself. Sufficient unto the day is the evil thereof.</span></p><p class="vref" style="font-size:12px;font-weight:700;color:var(--text2)">Matthew 6:34 · KJV</p>
          <p class="verse" style="margin-top:10px"><span style="font-style:italic">Be careful for nothing; but in every thing by prayer and supplication with thanksgiving let your requests be made known unto God.</span></p><p class="vref" style="font-size:12px;font-weight:700;color:var(--text2)">Philippians 4:6 · KJV</p>
        </div>
        <div data-el="feedback" style="display:flex;gap:10px;align-items:center"><span class="t-meta" style="color:var(--text)">Was this helpful?</span>${Q_('Yes', { cls: 'sm' })}${Q_('No', { cls: 'sm' })}</div>
        <p class="t-micro" data-el="disclaimer">The Assistant is a guide, not a pastor. Ask a pastor about personal matters.</p>
      </div>
      <div class="field" data-el="composer" style="margin-top:auto;color:var(--text2)"><span>Ask a faith question</span><span style="margin-left:auto">${ic('send', 18)}</span></div>`,
    states: {
      loading: null,
      empty: `<div class="glass" data-el="empty-state" style="border-color:#333"><div class="t-title">Ask a question to start</div><p class="t-body" style="margin-top:6px">Answers use the approved library and show their verses.</p></div>`,
      error: `<div class="glass" data-el="error-card" style="border-color:#333"><div class="t-title">The guide could not answer</div><p class="t-body" style="margin-top:6px">Try again, or ask a pastor.</p></div>`,
      offline: null,
      signedOut: null,
    },
    notes: ['Black and white only, Levi\'s own choice (prompt 6.11). No purple, no orange, no glow. The logo sits in the header unchanged.', 'Answer text and verses are sample; the KJV text is verified against the bundled KJV.', 'Crisis reply is a separate fixed screen, not drawn here, because its numbers are being checked (plan 4.8).'],
  },
  {
    id: '18-assistant-memory', name: 'What the guide remembers', chrome: 'full', bw: true,
    head: { title: 'What the guide remembers', sub: 'Everything here is opt-in', icon: 'back' },
    purpose: 'Choose what the Assistant keeps on this phone, and what, if anything, is shared.',
    plan: '5.2 (full screen, black and white)',
    body: () => `
      ${[['Remember my reading preferences', 'Kept on this phone only', false], ['Remember my recent questions', 'Kept on this phone only. Clear them any time.', false], ['Share unanswered questions to improve the guide', 'Off by default. No name or account is sent.', false]].map(([t, m, on], i) => glass(`<div style="display:flex;gap:12px;align-items:center"><div class="grow" style="flex:1"><div class="t-sub" style="font-size:15px">${t}</div><div class="t-meta" style="margin-top:4px">${m}</div></div>${tog(on, `opt-${i + 1}`)}</div>`, { size: 'in', el: `option-${i + 1}` })).join('')}
      <p class="t-meta" data-el="privacy-line">You can turn any option off at any time. Nothing here changes your account.</p>
      <div style="margin-top:auto;display:grid;gap:10px">${btn('bw-primary', 'Clear what the guide remembers', { el: 'clear-button' })}${Q_('Done', { el: 'done' })}</div>`,
    states: {
      empty: null,
      error: `<div class="glass" data-el="error-card" style="border-color:#333"><div class="t-title">Settings could not save</div><p class="t-body" style="margin-top:6px">Your choice is not changed. Try again.</p></div>`,
      signedOut: null,
    },
    notes: ['Black and white, as the Assistant screens are. The primary action is white on black here, not orange.', 'Sharing unanswered questions is opt-in and private (plan 4.8, gap log).'],
  },
  {
    id: '19-games-hub', name: 'Bible games', chrome: 'dialog', head: { title: 'Bible games', sub: 'Free and offline', icon: 'close' },
    purpose: 'Pick a Bible game to play. Every game works offline with no money involved.',
    plan: '4.9 (dialog)',
    body: () => `
      <div data-el="hub-chips">${chips(['All', 'Quiz', 'Word', 'Story'], 0)}</div>
      <div class="grid2" data-el="game-grid">
        ${[['Vine Words', 'Word', 'Vine Words'], ['Lamp Slide', 'Puzzle', 'Lamp Slide'], ['Ladder of Faith', 'Quiz', 'Ladder'], ['Fill the Gap', 'Verse', 'Fill the Gap'], ['Who Am I', 'Quiz', 'Who Am I'], ['Memory Match', 'Memory', 'Memory']].map(([t, k, s], i) => glass(`<div style="display:flex;justify-content:space-between;align-items:center">${sq('dice', { size: 20 })}<span class="t-micro">${k}</span></div><div class="t-sub" style="font-size:15px;margin-top:10px">${t}</div><div class="t-meta" style="margin-top:4px">One line of sample text.</div><div class="t-micro" style="margin-top:6px">Best score: none yet</div><div style="margin-top:10px">${S_('Play', { cls: 'sm', el: `play-${i + 1}` })}</div>`, { size: 'in', el: `game-card-${i + 1}` })).join('')}
      </div>
      <p class="t-micro" data-el="hub-note">Bible character names and game copy are sample. Leaderboard is off for now (adults only, pending review).</p>`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No games in this filter</div><div style="margin-top:12px">${Q_('Show all', { el: 'show-all' })}</div></div>`,
      signedOut: null,
      error: null,
    },
    notes: ['The one orange button rule: no orange here, the Play buttons are purple secondary.', 'Game copy is sample text. The real one-line descriptions come from the catalog.'],
  },
  {
    id: '20-game-vine-words', name: 'Vine Words', chrome: 'game', head: { title: 'Vine Words', sub: 'Level 3 · sample', icon: 'back', right: `<span class="chip" data-el="score">Score 120</span>` },
    purpose: 'Build the hidden word from the letters on the vine wheel.',
    plan: '4.9 (game, dark only until Levi approves a light set)',
    body: () => `
      <div class="glass game" data-el="word-slots" style="text-align:center">
        <div class="t-micro">Find the word</div>
        <div style="display:flex;justify-content:center;gap:10px;margin-top:10px">${['V', 'I', '', 'E'].map((l) => `<span style="width:48px;height:56px;border-radius:12px;border:1.5px solid var(--glass-rim);background:var(--glass-ctl);display:grid;place-items:center;font-size:24px;font-weight:700">${l}</span>`).join('')}</div>
      </div>
      <div data-el="letter-wheel" style="position:relative;width:260px;height:260px;margin:4px auto">
        ${['V', 'I', 'N', 'E', 'R', 'A', 'G'].map((l, i) => { const a = (i / 7) * Math.PI * 2 - Math.PI / 2; const x = 130 + 92 * Math.cos(a) - 27; const y = 130 + 92 * Math.sin(a) - 27; return `<span style="position:absolute;left:${x.toFixed(0)}px;top:${y.toFixed(0)}px;width:54px;height:54px;border-radius:50%;display:grid;place-items:center;font-size:22px;font-weight:700;background:var(--glass);border:1px solid var(--glass-rim)">${l}</span>`; }).join('')}
        <span style="position:absolute;left:95px;top:95px;width:70px;height:70px;border-radius:50%;display:grid;place-items:center;background:var(--glass-ctl);font-size:12px;font-weight:700">Shuffle</span>
      </div>
      <div style="display:flex;gap:10px">${Q_('Hint', { icon: 'spark', el: 'hint' })}${Q_('Mercy: skip', { el: 'mercy' })}</div>
      ${P_('Check word', { el: 'primary-check', cls: 'game' })}`,
    states: {
      error: `<div class="glass game" data-el="error-card"><div class="t-title">Word bank could not load</div><p class="t-body" style="margin-top:6px">Your score is kept. Try again.</p></div>`,
      empty: null,
      signedOut: null,
      offline: null,
    },
    notes: ['Hint and mercy are gentle help with no point cost (plan 4.9).', 'Code string "Verse Peek (costs 3 points)" on the integration branch conflicts with the plan rule. Flagged for Codex and Levi, not designed around.', 'Letter wheel positions are a drawing, not the game engine.'],
  },
  {
    id: '21-game-lamp-slide', name: 'Lamp Slide', chrome: 'game', head: { title: 'Lamp Slide', sub: 'Level 2 · sample', icon: 'back', right: `<span class="chip" data-el="score">Score 80</span>` },
    purpose: 'Slide the tiles to bring the lamp to the path, using a verse as the guide.',
    plan: '4.9 (game, has the approved light variant)',
    canvas: { light: LIGHT_GAME_CANVAS },
    modes: ['dark', 'light'],
    body: () => `
      <div class="glass game" data-el="verse-card" style="text-align:center"><p class="verse"><span style="font-style:italic">Thy word is a lamp unto my feet, and a light unto my path.</span></p><p class="vref" style="font-size:12px;font-weight:700;color:var(--text2)">Psalm 119:105 · KJV</p></div>
      <div data-el="slide-board" style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;padding:12px;border-radius:14px;background:var(--glass);border:1px solid var(--glass-rim)">
        ${['L', 'A', 'M', 'P', '', 'S', 'L', 'I', 'D'].map((l) => `<span style="aspect-ratio:1;border-radius:10px;display:grid;place-items:center;font-size:24px;font-weight:700;${l ? 'background:var(--glass-ctl);border:1px solid var(--glass-ctl-rim)' : 'background:transparent;border:1.5px dashed var(--glass-rim)'}">${l}</span>`).join('')}
      </div>
      <div style="display:flex;gap:10px">${Q_('Hint', { icon: 'spark' })}${Q_('Mercy: skip')}</div>
      ${P_('Check', { el: 'primary-check', cls: 'game' })}`,
    states: {
      error: `<div class="glass game" data-el="error-card"><div class="t-title">Puzzle could not load</div><p class="t-body" style="margin-top:6px">Try a different level.</p></div>`,
      empty: null,
      signedOut: null,
      offline: null,
    },
    notes: ['The light canvas uses the three frozen light game stops (#FBEFF8, #F8DBD7, #E0B1EB) as a 160 degree gradient. The exact stop positions are approximated: check against the approved mock before final.', 'Verse is KJV, Psalm 119:105.'],
  },
];
