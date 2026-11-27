// Batch B: screens 08 to 14. Sample names and posts are marked "sample" in notes.
'use strict';
const { glass, btn, P_, S_, Q_, chips, seg, tog, sq, field, row, VERSE, ic, esc } = require('./lib');

const search = (t) => `<div class="field" style="color:var(--text2)">${ic('search', 18)}<span>${esc(t)}</span></div>`;

module.exports = [
  {
    id: '08-prayer', name: 'Prayer wall', chrome: 'tab', tab: 'community',
    purpose: 'Read prayer requests, say amen, and share a request.',
    plan: '4.5 (Tab 4)',
    body: () => `
      <div data-el="sub-nav">${seg(['Prayer', 'Praise', 'Discussion'], 0)}</div>
      ${glass(`<div class="eyebrow">Daily guided prayer</div><div class="t-title" style="margin-top:6px">Thank, confess, ask</div><div style="margin-top:12px;display:grid;gap:8px">${['Thank God for one gift.', 'Bring one honest need.', 'Ask for one thing for someone else.'].map((t, i) => `<div style="display:flex;gap:10px;align-items:center"><span class="step">${i + 1}</span><span class="t-body" style="color:var(--text)">${t}</span></div>`).join('')}</div>`, { size: 'xl', el: 'guided-prayer' })}
      <div class="eyebrow">Prayer requests</div>
      ${[['Pray for wisdom at work this week. (sample)', 9], ['Pray for our church family travelling this week. (sample)', 4]].map(([t, n], i) => glass(`<div class="eyebrow" style="color:var(--accent)">Prayer</div><p class="t-body" style="margin-top:6px;color:var(--text)">${t}</p><div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px"><span class="t-meta">Fellowship member</span>${Q_(`Amen · ${n}`, { icon: 'heart', cls: 'sm' })}</div>`, { el: `request-${i + 1}` })).join('')}
      <div data-el="amen-toast" class="banner" style="justify-content:center">${ic('check', 18)}<span>Amen! Joined in prayer.</span></div>
      ${P_('Share a prayer request', { icon: 'pen', el: 'primary-share' })}`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No prayer requests yet</div><p class="t-body" style="margin-top:6px">Share a thought, prayer, or praise. Requests can be private to the church leaders.</p></div>`,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in to share and say amen</div><p class="t-body" style="margin-top:6px">Guests can read the prayer wall.</p><div style="margin-top:12px">${P_('Sign in', { go: '12-auth' })}</div></div>`,
    },
    notes: ['Requests are sample text. The audience choice (private to leaders) is part of the prayer-audience design, not yet built (plan 4.5).', 'Primary orange is the share action; amen is a glass pill.'],
  },
  {
    id: '09-groups', name: 'Groups', chrome: 'tab', tab: 'community',
    purpose: 'See the groups you belong to and the members in each.',
    plan: '4.5 (Tab 4, not yet verified on phone)',
    body: () => `
      <div data-el="sub-nav">${seg(['Feed', 'Inbox', 'Groups'], 2)}</div>
      <div data-el="group-search">${search('Search members by name')}</div>
      <div class="eyebrow">My groups</div>
      ${[['Prayer team', '12 members (sample)'], ['Choir', '18 members (sample)']].map(([t, m], i) => glass(`<div style="display:flex;gap:12px;align-items:center">${sq('community', { size: 22 })}<div style="flex:1"><div class="t-sub" style="font-size:15px">${t} (sample)</div><div class="t-meta" style="margin-top:3px">${m}</div></div>${Q_('Open', { cls: 'sm' })}</div>`, { size: 'in', el: `group-${i + 1}` })).join('')}
      <div class="eyebrow">Members · Prayer team</div>
      ${glass([['AM', 'Member name (sample)', 'Leader'], ['JK', 'Member name (sample)', 'Member']].map(([ini, n, role]) => row(`<span class="sq ghost" style="width:40px;min-width:40px;height:40px">${ini}</span><div class="grow"><div class="t-sub" style="font-size:15px">${n}</div></div><span class="chip${role === 'Leader' ? ' on' : ''}">${role}</span>`)).join(''), { el: 'member-list' })}
      <p class="t-meta" data-el="verification-note">Groups are not yet verified on the phone build. Membership rules decide who sees each group.</p>`,
    states: {
      empty: `<div class="glass" data-el="empty-state"><div class="t-title">No groups yet</div><p class="t-body" style="margin-top:6px">Your groups will appear here when an administrator adds you.</p></div>`,
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">Sign in to see your groups</div><p class="t-body" style="margin-top:6px">Groups are for members.</p><div style="margin-top:12px">${Q_('Sign in', { go: '12-auth' })}</div></div>`,
    },
    notes: ['Not yet verified on a phone build (plan 4.5). Member names and group names are sample text.', 'Adults only, pending safeguarding review for group visibility.'],
  },
  {
    id: '10-give', name: 'Give', chrome: 'tab', tab: 'give',
    purpose: 'Copy the church Paybill and account, then open M-Pesa to give.',
    plan: '4.6, 5.1 (Tab 5, centre orb)',
    body: () => `
      ${glass(`<div class="eyebrow">Generosity &amp; harvest · 2 Corinthians 9:7</div><div class="t-hero" style="margin-top:8px;font-size:24px">Give to Perazim Mission Church</div><p class="verse" style="margin-top:10px"><span class="t-body" style="font-style:italic;color:var(--text)">Every man according as he purposeth in his heart, so let him give; not grudgingly, or of necessity: for God loveth a cheerful giver.</span></p>`, { size: 'xl', el: 'give-hero' })}
      ${glass(`<div class="eyebrow">Safaricom M-Pesa Paybill</div><div class="t-title" style="margin-top:8px" data-el="paybill-value">[Paybill number, from app settings]</div><div style="display:flex;justify-content:space-between;margin-top:10px"><span class="t-meta">Account</span><span class="t-sub" style="font-size:14px">PERAZIM-TITHE</span></div>`, { el: 'paybill-card' })}
      <div data-el="fund-picker"><div class="lbl">Select giving fund</div>${chips(['Tithe', 'Offering', 'Missions & Outreach', 'Mercy Fund / Food Drive'], 0)}</div>
      <div class="fld" data-el="amount"><div class="lbl">Amount (KES, optional)</div><div class="field">${ic('send', 18)}<span>Enter amount in KES (e.g. 1000)</span></div></div>
      ${P_('Copy Paybill & account', { icon: 'copy', el: 'primary-copy' })}
      ${S_('Open M-Pesa', { icon: 'phone', el: 'open-mpesa' })}
      <p class="t-meta" data-el="giving-note">Giving is a copy-and-pay step. The treasurer reconciles it by hand.</p>`,
    states: {
      error: `<div class="glass" data-el="error-card"><div class="t-title">Paybill details could not load</div><p class="t-body" style="margin-top:6px">The number saved on this phone is shown instead, if there is one.</p><div style="margin-top:12px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
      offline: `<div class="banner" data-el="offline-banner">${ic('offline', 18)}<span>Offline. Copy the Paybill saved on this phone.</span></div>`,
      signedOut: null,
      empty: null,
    },
    notes: ['Paybill number is a placeholder: the real number comes from the app settings and is not written into the design.', 'The fund name Tithe is kept as built. The tithing question is undecided doctrine (plan 14.10), so the screen does not explain it.', 'Copy Paybill is the one orange button. Open M-Pesa is a purple secondary.', 'Scripture: 2 Corinthians 9:7 KJV, as the giving screen already shows.'],
  },
  {
    id: '11-me', name: 'Me', chrome: 'full', head: null,
    purpose: 'See your account, membership, privacy and offline storage, and sign in or out.',
    plan: '4.7 (top-bar profile)',
    body: () => `
      ${glass(`<div style="display:flex;gap:14px;align-items:center"><span class="sq" style="width:56px;height:56px;min-width:56px;border-radius:50%;font-size:18px;font-weight:700">GM</span><div><div class="t-title" data-el="display-name">Guest Pilgrim</div><div class="t-meta" style="margin-top:3px">Offline guest · sample</div></div></div>`, { el: 'profile-card' })}
      ${glass(`${row(`${ic('lock', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Account</div><div class="t-meta">Sign in to connect your phones</div></div>${Q_('Sign in', { cls: 'sm', go: '12-auth' })}`, { el: 'row-account' })}${row(`${ic('shield', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Privacy notice</div><div class="t-meta">Under review, not yet published</div></div>${ic('down', 18)}`, { el: 'row-privacy', go: '14-privacy-notice' })}${row(`${ic('pin', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Campus</div><div class="t-meta">Embu Headquarters</div></div>${ic('down', 18)}`, { el: 'row-campus' })}${row(`${ic('check', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Membership</div><div class="t-meta">Not requested</div></div>${Q_('Request', { cls: 'sm', go: '15-membership' })}`, { el: 'row-membership' })}`, { el: 'settings-list' })}
      ${glass(`${row(`${ic('offline', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Offline storage</div><div class="t-meta">24.8 MB kept on this phone (sample)</div></div>`, { el: 'row-storage' })}${row(`${ic('bell', 20)}<div class="grow"><div class="t-sub" style="font-size:15px">Notifications</div></div>${tog(true, 'notif-toggle')}`, { el: 'row-notifications', go: '25-notifications' })}`)}
      <div style="display:flex;justify-content:center">${btn('danger', 'Delete my account', { el: 'delete-account' })}</div>`,
    states: {
      signedOut: `<div class="glass" data-el="signed-out-card"><div class="t-title">You are not signed in</div><p class="t-body" style="margin-top:6px">Reading, games and the Bible work without an account.</p><div style="margin-top:12px">${P_('Sign in or create account', { go: '12-auth' })}</div></div>`,
    },
    notes: ['Delete my account is shown as text, not a red block, so it does not compete with the primary action.', 'Campus list as the plan gives it: Embu Headquarters, Nairobi Fellowship, Meru Outreach, Online Campus. This screen shows one campus only.', 'Privacy notice row shows its real status: under review, not published.'],
  },
  {
    id: '12-auth', name: 'Sign in', chrome: 'dialog', head: { title: 'Perazim Member Account', sub: 'Adults only', icon: 'close' },
    purpose: 'Sign in with phone and PIN, or create an account after reading the privacy notice.',
    plan: '5.2 (dialog)',
    body: () => `
      <div data-el="auth-tabs">${seg(['Sign In', 'Create Account'], 0)}</div>
      <p class="t-body" data-el="intro">Connect your prayer walk, streak and fellowship across your phones.</p>
      <div data-el="phone-field">${field('Phone number (Safaricom / Airtel)', '+254 7__ ___ ___', { icon: 'phone' })}</div>
      <div data-el="pin-field">${field('Security PIN (4–12 digits)', '••••', { icon: 'lock' })}</div>
      ${glass(`<div style="display:flex;gap:10px;align-items:flex-start">${tog(true, 'adult-check')}<span class="t-meta" style="color:var(--text)">I confirm that I am 18 years or older. Accounts are for adults only.</span></div>`, { size: 'in', el: 'age-confirm' })}
      <p class="t-meta" data-el="privacy-link">Read Perazim App Privacy Notice (draft, under review)</p>
      ${P_('Sign In', { el: 'primary-signin' })}
      ${Q_('Continue offline as guest', { el: 'guest-button', go: '13-onboarding' })}`,
    states: {
      error: `<div class="glass" data-el="error-card"><div class="t-title">Registration is not open yet</div><p class="t-body" style="margin-top:6px">The current privacy notice must load before an account can be created. Continue as a guest.</p></div>`,
      signedOut: null,
      loading: null,
      empty: null,
    },
    notes: ['Privacy notice is a draft and not published, so creating an account is blocked in the error state (plan 10.4).', 'Phone format and PIN length follow the current code (4 to 12 digits).', 'Age confirmation at sign-up is enforced by the server (plan 4.7).'],
  },
  {
    id: '13-onboarding', name: 'Welcome', chrome: 'bare', head: null,
    purpose: 'Meet the app, choose a campus, see the privacy summary, and start as a guest.',
    plan: '5.2, 6.1 (full screen)',
    body: () => `
      <div style="display:flex;flex-direction:column;align-items:center;gap:14px;text-align:center" data-el="welcome-hero">
        <img src="__LOGO__" alt="Perazim logo" width="120" height="120" style="width:120px;height:120px" data-el="logo">
        <div class="t-hero" style="font-size:24px">The Place of Great Breakthrough</div>
        <p class="t-body">Perazim Mission Church, Embu.</p>
      </div>
      ${glass(`${[['Holy Scriptures (KJV & Swahili)', 'book2', 'copy to check'], ['Church hymnal & chords', 'worship', ''], ['Daily devotionals & prayer wall', 'cross', ''], ['Audio sermons & media cache', 'sermon', '']].map(([t, i, n]) => row(`${ic(i, 20)}<div class="grow"><div class="t-sub" style="font-size:15px">${t}</div>${n ? `<div class="t-micro" style="font-style:italic">${n}</div>` : ''}</div>`)).join('')}`, { el: 'feature-list' })}
      <div data-el="campus"><div class="lbl">Your campus</div>${chips(['Embu Headquarters', 'Nairobi Fellowship', 'Meru Outreach', 'Online Campus'], 0)}</div>
      <p class="t-meta" data-el="privacy-summary">Your data stays on the church server. No ads. No third-party tracking.</p>
      ${P_('Get started', { el: 'primary-start' })}
      ${Q_('Continue offline as guest', { el: 'guest-button' })}`,
    states: {
      signedOut: null,
      empty: null,
      error: `<div class="glass" data-el="error-card"><div class="t-title">Could not check the privacy notice</div><p class="t-body" style="margin-top:6px">You can still start as a guest.</p><div style="margin-top:12px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
    },
    notes: ['Swahili feature copy must be checked by Levi before the reference image is final (prompt section 2).', 'Campus list is the plan\'s four names only.', 'The logo is shown unchanged, at 120 dp, and never on the orange button.'],
  },
  {
    id: '14-privacy-notice', name: 'Privacy notice', chrome: 'dialog', head: { title: 'Privacy notice', sub: 'Draft v0.2 · under review', icon: 'back' },
    purpose: 'Read what data the app keeps, who sees it, and how to delete it.',
    plan: '5.2, 10.4 (dialog)',
    body: () => `
      ${glass(`<div class="eyebrow">Status</div><p class="t-body" style="margin-top:6px;color:var(--text)">This notice is a draft. It has not been approved or published. Accepting is off until it is.</p>`, { size: 'in', el: 'status-note' })}
      <div data-el="notice-body" style="display:flex;flex-direction:column;gap:12px">
        <div><div class="t-sub" style="font-size:15px">What we keep</div><p class="t-body" style="margin-top:4px">Your phone number, your name and the PIN you set. Prayer and community posts you choose to share.</p></div>
        <div><div class="t-sub" style="font-size:15px">Who sees it</div><p class="t-body" style="margin-top:4px">The church administrators you allow. No advertising networks. No third-party analytics.</p></div>
        <div><div class="t-sub" style="font-size:15px">Your choices</div><p class="t-body" style="margin-top:4px">You can read as a guest. You can delete your account from Me.</p></div>
        <div><div class="t-sub" style="font-size:15px">Privacy contact</div><p class="t-body" style="margin-top:4px">[To be filled by Levi before publication]</p></div>
      </div>
      <div style="display:flex;gap:10px;align-items:center">${tog(false, 'read-check')}<span class="t-meta" style="color:var(--text)">I have read this notice</span></div>
      ${P_('Accept and continue', { el: 'primary-accept', disabled: true })}`,
    states: {
      error: `<div class="glass" data-el="error-card"><div class="t-title">Notice not published yet</div><p class="t-body" style="margin-top:6px">The current notice could not load. Try again later.</p><div style="margin-top:12px">${Q_('Try again', { icon: 'refresh' })}</div></div>`,
      loading: null,
      empty: null,
      signedOut: null,
    },
    notes: ['Notice text is placeholder wording for layout. The real text is the approved privacy notice, which only Levi can approve (plan 14.3).', 'Accept button is shown disabled, because acceptance is not possible until publication.'],
  },
];
