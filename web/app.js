(function () {
  'use strict';

  var cfg = window.PERAZIM_CONFIG || {};
  var API = String(cfg.apiBase || '').replace(/\/+$/, '');
  var FEED = String(cfg.feedBase || '').replace(/\/+$/, '');
  var TOKEN_KEY = 'perazim.token';
  var USER_KEY = 'perazim.user';
  var REASONS = [
    ['SPAM', 'Spam or scam'], ['HARASSMENT', 'Harassment or bullying'], ['HATE', 'Hate or contempt'],
    ['SEXUAL', 'Sexual content'], ['VIOLENCE', 'Violence or threats'], ['FALSE_TEACHING', 'False teaching'],
    ['PRIVACY', "Shares someone's personal information"], ['OTHER', 'Something else']
  ];

  var state = { token: null, user: null, blocked: {}, tab: 'home' };

  function $(id) { return document.getElementById(id); }

  // Build DOM nodes only. Member text is never parsed as HTML.
  function h(tag, props, children) {
    var node = document.createElement(tag);
    Object.keys(props || {}).forEach(function (key) {
      if (key === 'text') node.textContent = props[key];
      else if (key === 'on') Object.keys(props.on).forEach(function (ev) { node.addEventListener(ev, props.on[ev]); });
      else if (key === 'className') node.className = props[key];
      else node.setAttribute(key, props[key]);
    });
    (children || []).forEach(function (child) { if (child) node.appendChild(child); });
    return node;
  }

  function setStatus(message) { var s = $('status'); if (s) s.textContent = message || ''; }

  function api(path, options) {
    options = options || {};
    var headers = { 'Accept': 'application/json' };
    if (options.body) headers['Content-Type'] = 'application/json';
    if (state.token) headers.Authorization = 'Bearer ' + state.token;
    return fetch(API + path, {
      method: options.method || 'GET',
      headers: headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
      credentials: 'omit',
      cache: 'no-store'
    }).then(function (response) {
      return response.json().catch(function () { return null; }).then(function (data) {
        if (!response.ok) {
          var error = new Error((data && data.error) || ('Request failed (' + response.status + ')'));
          error.status = response.status;
          throw error;
        }
        return data;
      });
    }, function () {
      var error = new Error('The church server could not be reached. Check your connection and try again.');
      error.offline = true;
      throw error;
    });
  }

  function loadFeed(file) {
    return fetch(FEED + '/' + file, { cache: 'no-store' }).then(function (r) {
      if (!r.ok) throw new Error('Could not load ' + file);
      return r.json();
    });
  }

  function formatDate(ms) {
    if (!ms) return '';
    try { return new Date(Number(ms)).toLocaleDateString('en-KE', { day: 'numeric', month: 'short', year: 'numeric' }); }
    catch (_) { return ''; }
  }

  function setWho() {
    $('who').textContent = state.user ? state.user.name : 'Guest';
  }

  function saveSession(token, user) {
    state.token = token; state.user = user;
    try { localStorage.setItem(TOKEN_KEY, token); localStorage.setItem(USER_KEY, JSON.stringify(user)); } catch (_) { /* private mode */ }
    setWho();
  }

  function clearSession() {
    state.token = null; state.user = null; state.blocked = {};
    try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); } catch (_) { /* ignore */ }
    setWho();
  }

  function restoreSession() {
    try {
      var token = localStorage.getItem(TOKEN_KEY);
      var user = JSON.parse(localStorage.getItem(USER_KEY) || 'null');
      if (token && user) { state.token = token; state.user = user; }
    } catch (_) { /* ignore */ }
    setWho();
  }

  function loadBlocks() {
    if (!state.token) { state.blocked = {}; return Promise.resolve(); }
    return api('/api/community/blocks').then(function (data) {
      var map = {};
      (data.blocks || []).forEach(function (b) { map[b.user_id] = true; });
      state.blocked = map;
    }).catch(function () { /* keep the last known list */ });
  }

  // ---- Views -------------------------------------------------------------

  function viewHome() {
    var view = $('view');
    view.replaceChildren(
      h('section', { className: 'card glass' }, [
        h('p', { className: 'meta', text: 'Faith grows when we share life together.' }),
        h('p', { className: 'hero', text: 'Welcome to Perazim' }),
        h('p', { text: 'Sermons, prayer and community for the Perazim Mission Church family in Embu.' })
      ]),
      h('section', { className: 'card glass' }, [
        h('h2', { text: 'Get started' }),
        h('p', { className: 'meta', text: 'Sermons are open to everyone. Sign in to join the community, comment, and message members.' }),
        h('button', { text: 'Watch sermons', on: { click: function () { show('sermons'); } } }),
        h('button', { className: 'ghost', text: 'Open community', on: { click: function () { show('community'); } } })
      ]),
      h('p', { id: 'status', className: 'status', role: 'status' })
    );
  }

  function viewSermons() {
    var view = $('view');
    view.replaceChildren(h('p', { className: 'meta', id: 'status', role: 'status', text: 'Loading sermons…' }));
    loadFeed('sermons.json').then(function (data) {
      var items = (data.sermons || []).filter(function (s) { return s.active !== false; });
      if (!items.length) { setStatus('No sermons published yet.'); return; }
      var cards = items.map(function (s) {
        var link = s.youtube_url ? h('a', { href: s.youtube_url, target: '_blank', rel: 'noopener noreferrer', text: 'Watch on YouTube' }) : null;
        return h('article', { className: 'card glass' }, [
          h('h2', { text: s.title || 'Sermon' }),
          h('p', { className: 'meta', text: [s.speaker, s.date, s.series].filter(Boolean).join(' · ') }),
          link
        ]);
      });
      view.replaceChildren.apply(view, cards);
    }).catch(function () {
      setStatus('Sermons could not be loaded. Check your connection.');
    });
  }

  function postCard(post) {
    var own = state.user && post.author_id === state.user.id;
    var actions = [];
    if (state.token && !own) {
      actions.push(h('button', { className: 'ghost', text: 'Report', on: { click: function (e) { openReport(e.currentTarget, 'POST', post.id); } } }));
      actions.push(h('button', { className: 'ghost', text: 'Block ' + (post.author_name || 'member'), on: { click: function () { blockMember(post.author_id, post.author_name); } } }));
    }
    return h('article', { className: 'card glass' }, [
      h('p', { className: 'meta', text: [post.type, post.author_name, formatDate(post.created_at)].filter(Boolean).join(' · ') }),
      h('h2', { text: post.title || '' }),
      h('p', { text: post.content || '' }),
      h('p', { className: 'meta', text: (post.amen_count || 0) + ' amen · ' + (post.comment_count || 0) + ' comments' }),
      h('div', {}, actions)
    ]);
  }

  function openReport(anchor, targetType, targetId) {
    var select = h('select', { 'aria-label': 'Reason' }, REASONS.map(function (r) { return h('option', { value: r[0], text: r[1] }); }));
    var send = h('button', { text: 'Send report', on: { click: function () {
      send.disabled = true;
      api('/api/community/reports', { method: 'POST', body: { targetType: targetType, targetId: targetId, reason: select.value } })
        .then(function () { panel.replaceChildren(h('p', { className: 'meta', text: 'Thank you. The church team will review this report.' })); })
        .catch(function (err) { send.disabled = false; setStatus(err.message); });
    } } });
    var panel = h('div', { className: 'panel' }, [h('label', { text: 'Why are you reporting this?' }), select, send]);
    anchor.parentNode.appendChild(panel);
  }

  function blockMember(userId, name) {
    if (!userId) return;
    if (!window.confirm('Block ' + (name || 'this member') + '? You will no longer see their posts or comments, and neither of you can send messages.')) return;
    api('/api/community/blocks', { method: 'POST', body: { userId: userId } })
      .then(function () { return loadBlocks(); })
      .then(function () { viewCommunity(); })
      .catch(function (err) { setStatus(err.message); });
  }

  function viewCommunity() {
    var view = $('view');
    view.replaceChildren(h('p', { className: 'meta', id: 'status', role: 'status', text: 'Loading community…' }));
    var blocksReady = loadBlocks();
    blocksReady.then(function () { return api('/api/community/posts?limit=20'); }).then(function (data) {
      var posts = (data.posts || []).filter(function (p) { return !state.blocked[p.author_id]; });
      var children = [];
      if (!state.token) children.push(h('p', { className: 'meta', text: 'Reading is open to everyone. Sign in from Account to post, comment and report.' }));
      if (!posts.length) children.push(h('p', { className: 'meta', text: 'No discussions here yet.' }));
      posts.forEach(function (p) { children.push(postCard(p)); });
      children.push(h('p', { id: 'status', className: 'status', role: 'status' }));
      view.replaceChildren.apply(view, children);
    }).catch(function (err) {
      view.replaceChildren(h('p', { className: 'status', id: 'status', role: 'status', text: err.message }));
    });
  }

  function viewAccount() {
    var view = $('view');
    if (state.user) {
      view.replaceChildren(
        h('section', { className: 'card glass' }, [
          h('h2', { text: state.user.name || 'Member' }),
          h('p', { className: 'meta', text: 'Signed in' }),
          h('button', { className: 'ghost', text: 'Sign out', on: { click: function () { clearSession(); viewAccount(); } } }),
          h('p', { className: 'meta', text: 'Account changes, such as deleting your account, are in the Perazim app. You can also ask the church office for help.' })
        ]),
        h('p', { id: 'status', className: 'status', role: 'status' })
      );
      return;
    }
    var phone = h('input', { type: 'tel', autocomplete: 'tel', placeholder: '07… or +254…', 'aria-label': 'Phone number' });
    var pin = h('input', { type: 'password', inputmode: 'numeric', autocomplete: 'current-password', maxlength: '6', placeholder: 'PIN', 'aria-label': 'PIN' });
    var submit = h('button', { text: 'Sign in', on: { click: function () {
      if (!phone.value.trim() || !pin.value) { setStatus('Enter your phone number and PIN.'); return; }
      submit.disabled = true; setStatus('Signing in…');
      api('/api/auth/login', { method: 'POST', body: { phone: phone.value.trim(), pin: pin.value } })
        .then(function (data) { saveSession(data.token, data.user); return loadBlocks().then(function () { show('community'); }); })
        .catch(function (err) { submit.disabled = false; setStatus(err.message); });
    } } });
    view.replaceChildren(
      h('section', { className: 'card glass' }, [
        h('h2', { text: 'Sign in' }),
        h('p', { className: 'meta', text: 'Use the phone number and PIN from your Perazim account.' }),
        h('label', { text: 'Phone number' }), phone,
        h('label', { text: 'PIN' }), pin,
        submit
      ]),
      h('p', { id: 'status', className: 'status', role: 'status' })
    );
  }

  // ---- Navigation ---------------------------------------------------------

  var views = { home: viewHome, sermons: viewSermons, community: viewCommunity, account: viewAccount };

  function show(tab) {
    if (!views[tab]) tab = 'home';
    state.tab = tab;
    Array.prototype.forEach.call(document.querySelectorAll('.tab'), function (b) {
      var on = b.getAttribute('data-tab') === tab;
      b.classList.toggle('on', on);
      if (on) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
    });
    views[tab]();
    window.scrollTo(0, 0);
  }

  function boot() {
    restoreSession();
    Array.prototype.forEach.call(document.querySelectorAll('.tab'), function (b) {
      b.addEventListener('click', function () { show(b.getAttribute('data-tab')); });
    });
    if ('serviceWorker' in navigator && location.protocol.indexOf('http') === 0) {
      navigator.serviceWorker.register('sw.js').catch(function () { /* offline install is optional */ });
    }
    show('home');
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
