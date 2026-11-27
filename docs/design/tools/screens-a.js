// Batch A: screens 01 to 07. Copy is taken from the integration branch strings where a real
// string exists; anything else is marked "sample" in notes. Scripture is KJV, reference as Book Chapter:Verse.
'use strict';
const { glass, btn, P_, S_, Q_, chips, seg, tog, sq, photo, VERSE, ic, esc } = require('./lib');

const search = (t) => `<div class="field" style="color:var(--text2)">${ic('search', 18)}<span>${esc(t)}</span></div>`;
const LAUNCH = (lbl) => `<div class="eyebrow">${lbl}</div>`;

module.exports = [
  {
    id: '01-home', name: 'Home', chrome: 'tab', tab: 'home',
    purpose: 'See today\'s reading, read it, and reach the Sanctuary doors.',
    plan: '4.1, 5.1 (Tab 1)',
    body: () => `
      ${glass(`
        <div class="eyebrow" data-el="eyebrow">Today · Thu 27 Nov</div>
        <div class="t-hero" style="margin-top:8px" data-el="hero-title">Psalm 1:1–2</div>
        <div class="step" style="margin-top:12px" data-el="step-label">Step 1 of 3 · Read</div>
        <div style="margin-top:14px;display:grid;gap:10px">
          ${P_('Continue reading', { el: 'primary-continue', go: '02-bible-reader' })}
          ${Q_('Ask the Assistant', { icon: 'spark', go: '17-assistant-chat', el: 'assistant-button' })}
        </div>`, { size: 'xl', el: 'hero-today' })}
      ${glass(`${LAUNCH('Daily verse · KJV')}${VERSE('But his delight is in the law of the LORD; and in his law doth he meditate day and night.', 'Psalm 1:2', { el: 'daily-verse' })}`, { el: 'daily-verse-card' })}
      <div data-el="routine">
        <div class="eyebrow" style="margin-bottom:8px">Daily routine</div>
        <div style="display:flex;gap:10px">
          ${[['Read', 'book2'], ['Reflect', 'pen'], ['Pray', 'cross']].map(([l, i]) => glass(`<div style="display:flex;flex-direction:column;align-items:center;gap:6px;text-align:center">${ic(i, 20)}<div class="t-sub" style="font-size:14px">${l}</div><div class="t-micro">Not yet</div></div>`, { size: 'in', cls: 'grow' })).join('')}
        </div>
        <p class="t-meta" style="margin-top:8px">Missing a day costs nothing.</p>
      </div>
      <div data-el="sanctuary-doors">
        <div class="eyebrow" style="margin-bottom:8px">Sanctuary doors</div>
        <div style="display:flex;gap:10px">
          ${[['Bible', 'bible', '02-bible-reader'], ['Sermons', 'sermon', '04-sermons'], ['Worship', 'worship', '06-worship'], ['Prayer', 'cross', '08-prayer']].map(([l, i, go]) => `<div class="glass in" data-go="${go}" style="flex:1;display:flex;flex-direction:column;align-items:center;gap:6px;padding:12px 6px;cursor:pointer">${sq(i, { size: 20, cls: 'sq48' })}<div class="t-meta" style="font-weight:700;color:var(--text)">${l}</div></div>`).join('')}
        </div>
      </div>`,
    states: {
      empty: `<div class="glass" data-el="empty-state" style="text-align:center;padding:24px 18px"><div class="t-title">No reading plan is set</div><p class="t-body" style="margin-top:6px">Pick a plan in Daily reading. The Sanctuary doors still open.</p><div style="margin-top:14px">${Q_('Choose a reading plan', { go: '24-daily-reading' })}</div></div>`,
      signedOut: null,
      error: `<div class="glass" data-el="error-card"><div class="t-title">Today\'s reading could not load</div><p class="t-body" style="margin-top:6px">Unable to load the reading catalog. The Bible and the games still open.</p><div style="margin-top:14px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
    },
    notes: ['Tab 1 is Home. Routine steps use the copy Read, Reflect, Pray from the plan (4.1).', 'Psalm 1:1–2 is a sample reading; the real reading comes from the daily reading catalogue.', 'The hero has the one orange button (Continue reading). The Assistant button is a glass pill.'],
  },
  {
    id: '02-bible-reader', name: 'Bible reader', chrome: 'dialog', head: { title: 'Genesis 1', sub: 'King James Version (KJV)', icon: 'close', right: `<button class="btn icon" aria-label="More options" data-el="more">${ic('more', 20)}</button>` },
    purpose: 'Read a chapter, highlight a verse, compare translations and share a verse.',
    plan: '4.2',
    body: () => `
      <div data-el="reader-toolbar" class="glass in" style="display:flex;justify-content:space-between;align-items:center;padding:6px 8px">
        ${['highlight', 'bookmark', 'copy', 'share', 'mic'].map((i) => `<button class="btn icon" aria-label="${i}" style="border:0;background:transparent">${ic(i, 20)}</button>`).join('')}
      </div>
      <div data-el="translation-chips">${chips(['KJV', 'WEB', 'ASV', 'BSB'], 0)}</div>
      <div data-el="verses" class="reader" style="display:flex;flex-direction:column;gap:14px;font-size:17px;line-height:1.5;color:var(--text)">
        <p><sup class="sup">1</sup> <span class="hl">In the beginning God created the heaven and the earth.</span></p>
        <p><sup class="sup">2</sup> And the earth was without form, and void; and darkness was upon the face of the deep. And the Spirit of God moved upon the face of the waters.</p>
        <p><sup class="sup">3</sup> And God said, Let there be light: and there was light.</p>
      </div>
      <div style="display:flex;gap:10px;margin-top:auto" data-el="chapter-nav">
        ${Q_('Previous chapter')}${Q_('Next chapter')}
      </div>`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">This chapter is not on this phone</div><p class="t-body" style="margin-top:6px">Bible text is saved in the app. Pick another book, or check the storage setting.</p></div>`,
      error: `<div class="glass" data-el="error-card"><div class="t-title">This chapter could not be opened</div><p class="t-body" style="margin-top:6px">Try another chapter. Your highlights are kept on this phone.</p></div>`,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Reading works without an account</div><p class="t-body" style="margin-top:6px">Sign in to keep highlights and notes on your other phones.</p><div style="margin-top:12px">${Q_('Sign in', { go: '12-auth' })}</div></div>`,
    },
    notes: ['Translations shown are KJV, WEB, ASV and BSB (public domain, bundled). ESV, NIV, NKJV and SUV are not shown: their status is undecided (plan 4.2, 14.2).', 'Verse 1 highlight uses a purple tint, not a new colour.', 'Genesis 1:1–3 is KJV text.'],
  },
  {
    id: '03-bible-search', name: 'Bible search', chrome: 'dialog', head: { title: 'Search the Bible', sub: 'KJV first · works offline', icon: 'back' },
    purpose: 'Find a verse by word or reference, and read a short search overview.',
    plan: '4.2',
    body: () => `
      <div data-el="search-field">${search('love')}</div>
      <div data-el="filters">${chips(['KJV', 'WEB', 'ASV', 'BSB', 'Exact phrase'], 0)}</div>
      ${glass(`<div class="eyebrow">Search overview</div><div class="t-sub" style="margin-top:6px">love</div><p class="t-body" style="margin-top:8px">Dictionary entry shows here once the licence is confirmed. Sample text only.</p>`, { size: 'in', el: 'search-overview' })}
      <div class="eyebrow" data-el="results-label">Sample results · KJV</div>
      ${[
        ['John 3:16', 'For God so loved the world, that he gave his only begotten Son, that whosoever believeth in him should not perish, but have everlasting life.'],
        ['1 John 4:8', 'He that loveth not knoweth not God; for God is love.'],
        ['1 Corinthians 13:4', 'Charity suffereth long, and is kind; charity envieth not; charity vaunteth not itself, is not puffed up.'],
      ].map(([r, t], i) => glass(`<div class="t-sub" style="font-size:15px">${r}</div><p class="t-body" style="margin-top:4px">${esc(t)}</p>`, { size: 'in', el: `result-${i + 1}` })).join('')}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No verses match</div><p class="t-body" style="margin-top:6px">Try a shorter word, or a reference like John 3:16.</p></div>`,
      signedOut: null,
      error: `<div class="glass" data-el="error-card"><div class="t-title">Search could not run</div><p class="t-body" style="margin-top:6px">Search overview skipped. Verse search still works offline.</p></div>`,
    },
    notes: ['Result text is KJV as the AI rule requires. Results above are sample rows; the dictionary text is a placeholder until the Webster 1828 and Madan 1902 licences are confirmed (plan 1.1).', 'Search does not need an account, so signed-out state is the same screen.'],
  },
  {
    id: '04-sermons', name: 'Sermons', chrome: 'tab', tab: 'sermons',
    purpose: 'Watch the livestream or pick a recent sermon to play.',
    plan: '4.3, 5.1 (Tab 2)',
    body: () => `
      <div data-el="sermon-search">${search('Search title, preacher, passage…')}</div>
      <div data-el="sermon-filters">${chips(['Series', 'Downloaded', 'Video'], 2)}</div>
      ${glass(`<div class="eyebrow">Livestream broadcast</div><div class="t-title" style="margin-top:8px">Walking in Faith: Overcoming Fear in Difficult Times</div><p class="t-meta" style="margin-top:4px">Bishop Dr. David Mutweri</p><div style="margin-top:14px">${P_('Watch live broadcast', { icon: 'play', el: 'primary-live' })}</div>`, { size: 'xl', el: 'live-card' })}
      <div class="eyebrow">Recent messages</div>
      ${[['Supernatural Breakthrough', 'Bishop Dr. David Mutweri · 2 Samuel 5:17–21'], ['Sample sermon title', 'Preacher name · Passage reference']].map(([t, m], i) => glass(`<div style="display:flex;gap:12px;align-items:center">${photo('', { cls: 'sm' })}<div style="min-width:0"><div class="t-sub" style="font-size:15px">${t}</div><div class="t-meta" style="margin-top:4px">${m}</div></div></div>`, { size: 'in', el: `sermon-row-${i + 1}` })).join('')}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No sermons saved on this phone yet</div><p class="t-body" style="margin-top:6px">Sermons appear here when the church feed is reachable.</p></div>`,
      error: `<div class="banner" data-el="fallback-banner">${ic('alert', 18)}<span>Church feed unavailable. Showing sermons saved on this phone.</span></div>`,
      signedOut: null,
    },
    notes: ['Tab 2. Sermons are public, so there is no signed-out state.', 'Preacher name and sermon titles are the ones in the app build; placeholders are marked sample.', 'Mini-player is not shown because nothing is playing.'],
  },
  {
    id: '05-sermon-player', name: 'Now playing', chrome: 'dialog', head: { title: 'Now playing', sub: 'Sermons', icon: 'down' },
    purpose: 'Play, pause and skip a sermon with the church controls.',
    plan: '4.3 (dialog, dark media canvas undecided)',
    body: () => `
      <div data-el="artwork" style="display:flex;justify-content:center">${photo('Supernatural Breakthrough', { cls: 'big' }).replace('class="photo big"', 'class="photo big" style="width:100%;height:260px"')}</div>
      <div data-el="now-title"><div class="t-head">Supernatural Breakthrough</div><p class="t-meta" style="margin-top:6px">Bishop Dr. David Mutweri · 2 Samuel 5:17–21</p></div>
      <div data-el="progress">
        <div style="height:6px;border-radius:6px;background:var(--glass-ctl);overflow:hidden"><div style="width:33%;height:100%;background:var(--brand-grad)"></div></div>
        <div style="display:flex;justify-content:space-between;margin-top:8px" class="t-meta"><span>12:40</span><span>38:05</span></div>
      </div>
      <div data-el="transport" style="display:flex;align-items:center;justify-content:space-between;padding:0 8px">
        <button class="btn icon" aria-label="Back 15 seconds" style="width:56px;height:56px;min-width:56px">${ic('skipBack', 26)}</button>
        <button class="btn primary" aria-label="Pause" data-el="primary-pause" style="width:76px;min-width:76px;min-height:76px;border-radius:50%;padding:0">${ic('pause', 30)}</button>
        <button class="btn icon" aria-label="Forward 30 seconds" style="width:56px;height:56px;min-width:56px">${ic('skipFwd', 26)}</button>
      </div>
      <div style="display:flex;gap:10px" data-el="secondary-actions">${Q_('Downloaded', { icon: 'check' })}${Q_('Share', { icon: 'share' })}</div>`,
    states: {
      error: `<div class="glass" data-el="error-card"><div class="t-title">This sermon could not play</div><p class="t-body" style="margin-top:6px">Check the connection, or play a copy saved on this phone.</p><div style="margin-top:12px">${Q_('Play saved copy')}</div></div>`,
      empty: null,
      signedOut: null,
    },
    notes: ['The #1A0A20 media canvas is undecided (prompt 5.3). This file uses the standard canvas, and the choice is marked for Levi.', 'Play is the one orange button on this screen.'],
  },
  {
    id: '06-worship', name: 'Worship', chrome: 'tab', tab: 'worship',
    purpose: 'Find a hymn by number or lyric, and copy its chords.',
    plan: '4.4, 5.1 (Tab 3)',
    body: () => `
      <div data-el="hymn-search">${search('Search hymns by number or lyric keywords')}</div>
      <div data-el="hymn-filters">${chips(['Favorites', 'Worship', 'Faith', 'Grace', 'Praise'], 1)}</div>
      ${glass(`<div class="eyebrow">Now showing · Key G</div><div class="t-title" style="margin-top:6px">Amazing Grace</div><p class="t-meta" style="margin-top:4px">Classic Hymn · G – C – D</p><p class="t-body" style="margin-top:10px">G   C   G   D   G</p><div style="margin-top:12px;display:flex;gap:10px">${Q_('Copy chords', { icon: 'copy' })}${Q_('Transpose', { icon: 'more' })}</div>`, { size: 'xl', el: 'now-hymn' })}
      <div class="eyebrow">Hymns</div>
      ${[['#23', 'Great Is Thy Faithfulness', 'Classic hymn · G'], ['#48', 'Blessed Assurance', 'Classic hymn · D'], ['#112', 'Sample hymn title', 'Faith · C']].map(([n, t, m], i) => glass(`<div style="display:flex;align-items:center;gap:12px">${sq('worship', { size: 20, cls: 'ghost' })}<div style="flex:1;min-width:0"><div class="t-sub" style="font-size:15px">${n} ${t}</div><div class="t-meta" style="margin-top:3px">${m}</div></div>${ic('heart', 20)}</div>`, { size: 'in', el: `hymn-row-${i + 1}` })).join('')}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No hymns match</div><p class="t-body" style="margin-top:6px">Try a number, or one word from the lyric.</p></div>`,
      signedOut: null,
      error: null,
    },
    notes: ['Hymnal and chords work offline (plan 4.4), so there is no signed-out or error screen. Error uses the generic card if needed.', 'Hymn titles other than Amazing Grace are sample rows.'],
  },
  {
    id: '07-fellowship', name: 'Community feed', chrome: 'tab', tab: 'community',
    purpose: 'Read and post to the community feed, check the Inbox, and join in prayer.',
    plan: '4.5, 5.1 (Tab 4)',
    body: () => `
      <div data-el="sub-nav">${seg(['Feed', 'Inbox', 'Groups'], 0)}</div>
      ${glass(`<div class="eyebrow">Pinned · church event</div><div class="t-title" style="margin-top:6px">Sunday service and prayer</div><p class="t-meta" style="margin-top:4px">Embu Headquarters · Date set by the church office</p><div style="margin-top:12px">${Q_('View event')}</div>`, { el: 'pinned-event' })}
      ${glass(`<div class="t-meta" style="color:var(--text)">Share a thought, prayer or praise</div><div style="margin-top:12px">${P_('Create a community post', { icon: 'pen', el: 'primary-post' })}</div>`, { el: 'compose' })}
      <div class="eyebrow">Latest posts</div>
      ${[['Prayer', 'Please pray for a family facing a hard diagnosis this week. (sample post)', 12], ['Praise', 'Thank you, Lord, for a safe journey home. (sample post)', 7]].map(([tag, t, n], i) => glass(`<div class="eyebrow" style="color:var(--accent)">${tag}</div><p class="t-body" style="margin-top:6px;color:var(--text)">${t}</p><div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px"><span class="t-meta">Fellowship member</span>${Q_(`Amen · ${n}`, { icon: 'heart', cls: 'sm' })}</div>`, { el: `post-${i + 1}` })).join('')}`,
    states: {
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in to participate</div><p class="t-body" style="margin-top:6px">Guests can read posts. Sign in from Me to post, pray and join groups.</p><div style="margin-top:12px">${P_('Sign in', { go: '12-auth' })}</div></div>`,
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No discussions here yet</div><p class="t-body" style="margin-top:6px">Share a thought, prayer, or praise.</p></div>`,
      error: `<div class="glass" data-el="error-card"><div class="t-title">Events are unavailable</div><p class="t-body" style="margin-top:6px">Tap Refresh to try again.</p><div style="margin-top:12px">${Q_('Refresh', { icon: 'refresh' })}</div></div>`,
    },
    notes: ['Tab 4 is named Community (plan 5.1). The prompt lists it as Prayer. Label is undecided, see the README.', 'Post authors are shown as Fellowship member. No real names are in the mock.', 'Guests read only (plan 6.4). Report and block are not built, so there is no report action on posts.'],
  },
];
