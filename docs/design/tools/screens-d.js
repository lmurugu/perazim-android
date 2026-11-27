// Batch D: screens 22 to 28. Game copy and reflections are sample text.
'use strict';
const { glass, btn, P_, S_, Q_, chips, seg, tog, sq, field, row, VERSE, ic, esc } = require('./lib');

const LADDER_SVG = `<svg width="60" height="220" viewBox="0 0 60 220" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M14 6v208M46 6v208"/>${[30, 62, 94, 126, 158, 190].map((y) => `<path d="M14 ${y}h32"/>`).join('')}</svg>`;

module.exports = [
  {
    id: '22-game-ladder', name: 'Ladder of Faith', chrome: 'game', head: { title: 'Ladder of Faith', sub: 'Rung 4 of 12 · sample', icon: 'back', right: `<span class="chip" data-el="score">Score 40</span>` },
    purpose: 'Climb the ladder by answering Bible questions. Each rung gets a little harder.',
    plan: '4.9 (game, dark only until Levi approves a light set)',
    body: () => `
      <div style="display:flex;gap:14px;align-items:flex-start">
        <div data-el="ladder" style="color:var(--text2);display:flex;flex-direction:column;gap:0">${LADDER_SVG}</div>
        <div style="flex:1;display:flex;flex-direction:column;gap:10px">
          <div class="glass game" data-el="question"><div class="t-micro">Question 4</div><div class="t-title" style="margin-top:6px">Who was told to build the ark?</div></div>
          ${['Noah', 'Moses', 'Abraham', 'Jonah'].map((a, i) => `<div class="glass in" data-grp data-el="answer-${i + 1}" style="cursor:pointer;${i === 0 ? 'border-color:var(--glass-ctl-rim)' : ''}"><span class="t-sub" style="font-size:15px">${a}</span></div>`).join('')}
        </div>
      </div>
      <div style="display:flex;gap:10px">${Q_('Verse Peek', { icon: 'book2', el: 'verse-peek' })}${Q_('Ask for a hint', { icon: 'spark' })}</div>
      <p class="t-meta" data-el="feedback-note">Verse Peek is one gentle hint per run. Missing an answer does not end your climb at once.</p>
      ${P_('Answer', { el: 'primary-answer', cls: 'game' })}`,
    states: {
      error: `<div class="glass game" data-el="error-card"><div class="t-title">Questions could not load</div><p class="t-body" style="margin-top:6px">Your rung is saved. Try again.</p></div>`,
      empty: null,
      signedOut: null,
      offline: null,
    },
    notes: ['Verse Peek is described as a gentle hint with no point cost, per the plan. The integration branch string "costs 3 points" conflicts and is flagged.', 'Answer shown in the mock is the question sample (Genesis 6:14 KJV is the verse for the answer).', 'Ladder is drawn with simple lines.'],
  },
  {
    id: '23-game-fill-gap', name: 'Fill the Gap', chrome: 'game', head: { title: 'Fill the Gap', sub: 'Question 3 of 10 · sample', icon: 'back', right: `<span class="chip" data-el="score">Score 60</span>` },
    purpose: 'Fill in the missing word of a Bible verse.',
    plan: '4.9 (game, dark only until Levi approves a light set)',
    body: () => `
      <div class="glass game" data-el="sentence" style="text-align:center">
        <p class="t-title" style="line-height:1.5">The LORD is my <span style="display:inline-block;min-width:110px;border-bottom:2px solid var(--text);padding:0 6px;color:var(--text)">shepherd</span>; I shall not want.</p>
        <div class="t-meta" style="margin-top:12px">Psalm 23:1 · KJV</div>
      </div>
      <div data-el="answer-bank"><div class="lbl">Choose a word</div>${chips(['shepherd', 'light', 'king', 'rock'], 0)}</div>
      <div style="display:flex;gap:10px">${Q_('Hint', { icon: 'spark', el: 'hint' })}${Q_('Mercy: skip', { el: 'mercy' })}</div>
      ${P_('Check', { el: 'primary-check', cls: 'game' })}`,
    states: {
      error: `<div class="glass game" data-el="error-card"><div class="t-title">Verse could not load</div><p class="t-body" style="margin-top:6px">Your score is kept.</p></div>`,
      empty: null,
      signedOut: null,
      offline: null,
    },
    notes: ['Psalm 23:1 KJV is verbatim. The word bank is sample.', 'Hint and mercy have no point cost, per the plan rules.'],
  },
  {
    id: '24-daily-reading', name: 'Daily reading', chrome: 'dialog', head: { title: 'Daily reading', sub: 'Read · Reflect · Pray', icon: 'close' },
    purpose: 'Read today\'s passage, reflect on one question, and pray. Missing a day costs nothing.',
    plan: '4.10 (dialog or card)',
    body: () => `
      <div style="display:flex;gap:8px" data-el="routine-steps">${['Read', 'Reflect', 'Pray'].map((s, i) => `<span class="chip${i === 0 ? ' on' : ''}" data-el="step-${i + 1}">${s}</span>`).join('')}</div>
      ${glass(`<div class="eyebrow">Day 12 of 30 · sample</div><div class="t-title" style="margin-top:6px">Psalm 1:1–6</div><p class="verse" style="margin-top:10px"><span style="font-style:italic">Blessed is the man that walketh not in the counsel of the ungodly, nor standeth in the way of sinners, nor sitteth in the seat of the scornful.</span></p><p class="vref" style="font-size:12px;font-weight:700;color:var(--text2)">Psalm 1:1 · KJV</p>`, { el: 'passage' })}
      ${glass(`<div class="eyebrow">Reflect</div><p class="t-body" style="margin-top:6px;color:var(--text)">One question for today: where did you choose the wise path this week?</p>${field('Your answer (kept on this phone)', '', { el: 'reflect-field' })}`, { el: 'reflect' })}
      ${glass(`<div class="eyebrow">Pray</div><p class="t-body" style="margin-top:6px;color:var(--text)">Lord, let your word shape how I walk today. Amen.</p>`, { el: 'pray' })}
      <p class="t-meta" data-el="complete-note">Routine complete when all three steps are done. No streak to keep.</p>
      ${P_('Mark as read', { el: 'primary-mark-read' })}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No plan is running</div><p class="t-body" style="margin-top:6px">Choose a reading plan to start.</p><div style="margin-top:12px">${Q_('Choose a plan')}</div></div>`,
      error: `<div class="glass" data-el="error-card"><div class="t-title">Unable to load the reading catalog</div><p class="t-body" style="margin-top:6px">Your progress is kept on this phone.</p><div style="margin-top:12px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
      signedOut: null,
    },
    notes: ['Reading counts toward the routine\'s Read step with no extra points (Levi, 2026-11-21).', 'Passage and reflection text are sample; KJV for the passage quote.'],
  },
  {
    id: '25-notifications', name: 'Notifications', chrome: 'dialog', head: { title: 'Notifications', sub: 'Community', icon: 'close' },
    purpose: 'See community updates and turn notifications on or off.',
    plan: '5.2 (dialog)',
    body: () => `
      ${glass(`${row(`${ic('bell', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Notifications on this phone</div></div>${tog(true, 'master-toggle')}`)}`, { size: 'in', el: 'master' })}
      <div class="eyebrow">Today</div>
      ${[['Amen on your prayer request', '3 people joined in prayer (sample)'], ['New post in Prayer', 'A member shared a request (sample)']].map(([t, m], i) => glass(`<div style="display:flex;gap:12px;align-items:flex-start">${sq('bell', { size: 18, cls: 'ghost' })}<div><div class="t-sub" style="font-size:15px">${t}</div><div class="t-meta" style="margin-top:3px">${m}</div></div></div>`, { size: 'in', el: `notice-${i + 1}` })).join('')}
      <div class="eyebrow">Earlier</div>
      ${glass(`<div class="t-meta">Event reminder: Sunday service (sample)</div>`, { size: 'in', el: 'notice-3' })}
      ${Q_('Mark all as read', { el: 'mark-read' })}`,
    states: {
      empty: `<div class="glass" data-el="empty-state" style="text-align:center"><div class="t-title">No notifications yet</div><p class="t-body" style="margin-top:6px">Updates from the community appear here.</p></div>`,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in to see community notifications</div><div style="margin-top:12px">${Q_('Sign in', { go: '12-auth' })}</div></div>`,
      error: null,
    },
    notes: ['Notification text is sample.'],
  },
  {
    id: '26-qr-connection', name: 'Connection code', chrome: 'dialog', head: { title: 'Connection code', sub: 'Family or classroom', icon: 'close' },
    purpose: 'Show a code to connect a family member or classroom, or scan someone else\'s code.',
    plan: '5.2 (dialog)',
    body: () => `
      <div data-el="mode-switch">${seg(['Show my code', 'Scan a code'], 0)}</div>
      <div class="glass" data-el="qr-card" style="display:flex;flex-direction:column;align-items:center;gap:14px">
        <div class="qr" data-el="qr-placeholder" aria-label="Placeholder code, not a real code">${Array.from({ length: 81 }, (_, i) => `<i class="${[0, 1, 5, 9, 17, 18, 19, 21, 25, 26, 31, 35, 40, 44, 46, 49, 53, 58, 62, 66, 70, 71, 74, 75, 79, 80].includes(i) ? '' : 'o'}"></i>`).join('')}</div>
        <div class="t-title" data-el="code-text">ABCD-1234</div>
        <p class="t-meta" style="text-align:center">Sample code only. A real code is made when you open this screen.</p>
      </div>
      <p class="t-body" data-el="explain">Ask the other person to scan this code. It connects their phone to your family or classroom group.</p>
      ${P_('Scan a code', { icon: 'qr', el: 'primary-scan' })}`,
    states: {
      error: `<div class="glass" data-el="error-card"><div class="t-title">A code could not be made</div><p class="t-body" style="margin-top:6px">Check the connection and try again.</p><div style="margin-top:12px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in to connect a family member</div><div style="margin-top:12px">${Q_('Sign in', { go: '12-auth' })}</div></div>`,
      empty: null,
    },
    notes: ['The QR pattern is a placeholder grid, not a valid code.', 'Scan needs camera permission. The permission prompt is not drawn here.'],
  },
  {
    id: '27-reflections', name: 'Reflections', chrome: 'dialog', head: { title: 'Reflections', sub: 'Kept on this phone', icon: 'close' },
    purpose: 'Keep a private journal of what you read and learn.',
    plan: '5.2 (dialog)',
    body: () => `
      <p class="t-body" data-el="intro">Only you can see these entries on this phone. Nothing is shared unless you choose to.</p>
      ${[['27 Nov', 'Psalm 1:2', 'Delight in the law shapes what I do first. (sample)'], ['25 Nov', 'John 3:16', 'God loved first. (sample)']].map(([d, r, t], i) => glass(`<div style="display:flex;justify-content:space-between"><span class="eyebrow">${d}</span><span class="t-micro">${r}</span></div><p class="t-body" style="margin-top:8px;color:var(--text)">${t}</p>`, { size: 'in', el: `entry-${i + 1}` })).join('')}
      ${P_('New reflection', { icon: 'pen', el: 'primary-new' })}`,
    states: {
      empty: `<div class="glass" data-el="empty-state" style="text-align:center"><div class="t-title">No reflections yet</div><p class="t-body" style="margin-top:6px">Write one line after today's reading.</p></div>`,
      error: `<div class="glass" data-el="error-card"><div class="t-title">Could not save</div><p class="t-body" style="margin-top:6px">Your words are kept in this box until you try again.</p></div>`,
      signedOut: null,
    },
    notes: ['Entries are sample text. Reflections are private to the phone unless a later decision says otherwise.'],
  },
  {
    id: '28-local-bible-audio', name: 'Bible audio', chrome: 'dialog', head: { title: 'Bible audio', sub: 'Listen offline on this phone', icon: 'close' },
    purpose: 'Listen to a chapter of the Bible that is saved on this phone.',
    plan: '5.2, 4.3 (dialog)',
    body: () => `
      ${glass(`<div class="eyebrow">Now playing · KJV</div><div class="t-title" style="margin-top:6px">Genesis 1</div><p class="t-meta" style="margin-top:4px">Saved on this phone · sample audio</p><div style="margin-top:14px;height:6px;border-radius:6px;background:var(--glass-ctl);overflow:hidden"><div style="width:20%;height:100%;background:var(--brand-grad)"></div></div><div style="display:flex;justify-content:space-between;margin-top:6px" class="t-meta"><span>02:10</span><span>06:40</span></div>`, { el: 'player' })}
      <div data-el="transport" style="display:flex;align-items:center;justify-content:space-between;padding:0 6px">
        <button class="btn icon" aria-label="Back 15 seconds" style="width:56px;height:56px;min-width:56px">${ic('skipBack', 26)}</button>
        <button class="btn primary" aria-label="Play" data-el="primary-play" style="width:76px;min-width:76px;min-height:76px;border-radius:50%;padding:0">${ic('play', 30)}</button>
        <button class="btn icon" aria-label="Forward 30 seconds" style="width:56px;height:56px;min-width:56px">${ic('skipFwd', 26)}</button>
      </div>
      <div data-el="speed">${chips(['1x', '1.25x', '1.5x'], 0)}</div>
      <div class="eyebrow">Chapters on this phone</div>
      ${[['Genesis 1', 'Saved'], ['Genesis 2', 'Saved'], ['Genesis 3', 'Not saved']].map(([t, s], i) => row(`<div class="grow"><div class="t-sub" style="font-size:15px">${t}</div></div><span class="chip${s === 'Saved' ? ' on' : ''}">${s}</span>`, { el: `chapter-${i + 1}` })).join('')}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No Bible audio saved yet</div><p class="t-body" style="margin-top:6px">Audio saved on this phone plays without a network.</p></div>`,
      error: `<div class="glass" data-el="error-card"><div class="t-title">This chapter could not play</div><p class="t-body" style="margin-top:6px">Try another chapter.</p></div>`,
      signedOut: null,
    },
    notes: ['Audio licence is to be confirmed before any reader or narrator is named. The screen names no narrator.', 'Play is the one orange button.', 'Chapter list is sample; Genesis 1 to 3 are KJV chapters.'],
  },
];
