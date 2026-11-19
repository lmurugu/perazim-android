(function () {
  'use strict';

  var cfg = window.PERAZIM_CONFIG || {};
  var API = String(cfg.apiBase || '').replace(/\/+$/, '');
  var FEED = String(cfg.feedBase || '').replace(/\/+$/, '');
  var NATIVE = window.PerazimNative;
  var KV = NATIVE.kv;
  var TOKEN_KEY = 'perazim.token';
  var USER_KEY = 'perazim.user';
  var PUSH_KEY = 'perazim.pushToken';
  var CACHE_POSTS = 'cache.posts';
  var CACHE_SERMONS = 'cache.sermons';
  var REASONS = [
    ['SPAM', 'Spam or scam'], ['HARASSMENT', 'Harassment or bullying'], ['HATE', 'Hate or contempt'],
    ['SEXUAL', 'Sexual content'], ['VIOLENCE', 'Violence or threats'], ['FALSE_TEACHING', 'False teaching'],
    ['PRIVACY', "Shares someone's personal information"], ['OTHER', 'Something else']
  ];

  var state = { token: null, user: null, blocked: {}, tab: 'home' };

  function $(id) { return document.getElementById(id); }

  // Builds DOM nodes only. Member text is set with textContent and is never parsed as HTML.
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
          // An expired or revoked token ends the session on this phone. The member signs in again.
          if (response.status === 401 && state.token && path.indexOf('/api/auth/login') !== 0) clearLocal();
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
    KV.set(TOKEN_KEY, token);
    KV.set(USER_KEY, JSON.stringify(user));
    setWho();
    registerDevice();
  }

  // Removes this phone's saved session without a network call.
  function clearLocal() {
    KV.remove(TOKEN_KEY); KV.remove(USER_KEY); KV.remove(PUSH_KEY); KV.remove(CACHE_POSTS);
    state.token = null; state.user = null; state.blocked = {};
    setWho();
  }

  // Signing out first removes this device from push delivery, then clears the saved session.
  function clearSession() {
    return KV.get(PUSH_KEY).then(function (pushToken) {
      if (!pushToken || !state.token) return null;
      return api('/api/community/devices/' + encodeURIComponent(pushToken), { method: 'DELETE' }).catch(function () { return null; });
    }).then(clearLocal);
  }

  function registerDevice() {
    NATIVE.registerPush(function (pushToken) {
      if (!state.token) return;
      KV.set(PUSH_KEY, pushToken);
      api('/api/community/devices', { method: 'POST', body: { token: pushToken, platform: NATIVE.platform() } }).catch(function () { /* retried on next sign-in */ });
    });
  }

  function restoreSession() {
    return Promise.all([KV.get(TOKEN_KEY), KV.get(USER_KEY)]).then(function (values) {
      try {
        var user = JSON.parse(values[1] || 'null');
        if (values[0] && user) { state.token = values[0]; state.user = user; }
      } catch (_) { /* ignore a damaged saved session */ }
      setWho();
    });
  }

  function loadBlocks() {
    if (!state.token) { state.blocked = {}; return Promise.resolve(); }
    return api('/api/community/blocks').then(function (data) {
      var map = {};
      (data.blocks || []).forEach(function (b) { map[b.user_id] = true; });
      state.blocked = map;
    }).catch(function () { /* keep the last known list */ });
  }

  // Saved copies: shown when the server cannot be reached.
  function saveCache(key, value) { KV.set(key, JSON.stringify({ at: Date.now(), value: value })); }
  function readCache(key) {
    return KV.get(key).then(function (raw) {
      try { var item = JSON.parse(raw || 'null'); return item ? item.value : null; } catch (_) { return null; }
    });
  }

  // ---- Views -------------------------------------------------------------

  function viewHome() {
    $('view').replaceChildren(
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

  function sermonCard(s) {
    var children = [
      h('h2', { text: s.title || 'Sermon' }),
      h('p', { className: 'meta', text: [s.speaker, s.date, s.series].filter(Boolean).join(' · ') })
    ];
    if (s.audio_url && /^https:\/\//.test(s.audio_url)) {
      var audio = h('audio', { controls: '', preload: 'none', src: s.audio_url, 'aria-label': 'Sermon audio' });
      // Lock-screen controls follow the sermon that is playing.
      audio.addEventListener('play', function () { NATIVE.setNowPlaying(audio, s.title, s.speaker); });
      children.push(audio);
    }
    if (s.youtube_url && /^https:\/\//.test(s.youtube_url)) {
      children.push(h('a', { href: s.youtube_url, target: '_blank', rel: 'noopener noreferrer', text: 'Watch on YouTube' }));
    }
    return h('article', { className: 'card glass' }, children);
  }

  function renderSermons(items, stale) {
    var children = [];
    if (stale) children.push(h('p', { className: 'status', text: 'Showing saved sermons. Check your connection to see the latest.' }));
    if (!items.length) children.push(h('p', { className: 'meta', text: 'No sermons published yet.' }));
    items.forEach(function (s) { children.push(sermonCard(s)); });
    $('view').replaceChildren.apply($('view'), children);
  }

  function viewSermons() {
    $('view').replaceChildren(h('p', { className: 'meta', id: 'status', role: 'status', text: 'Loading sermons…' }));
    loadFeed('sermons.json').then(function (data) {
      var items = (data.sermons || []).filter(function (s) { return s.active !== false; });
      saveCache(CACHE_SERMONS, items);
      renderSermons(items, false);
    }).catch(function () {
      readCache(CACHE_SERMONS).then(function (items) {
        if (items) renderSermons(items, true);
        else setStatus('Sermons could not be loaded. Check your connection.');
      });
    });
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
      .then(loadBlocks)
      .then(viewCommunity)
      .catch(function (err) { setStatus(err.message); });
  }

  function postCard(post) {
    var own = state.user && post.author_id === state.user.id;
    var actions = [];
    if (state.token && !own) {
      actions.push(h('button', { className: 'ghost', text: 'Report', on: { click: function (e) { openReport(e.currentTarget, 'POST', post.id); } } }));
      actions.push(h('button', { className: 'ghost', text: 'Block ' + (post.author_name || 'member'), on: { click: function () { blockMember(post.author_id, post.author_name); } } }));
    }
    var children = [
      h('p', { className: 'meta', text: [post.type, post.author_name, formatDate(post.created_at)].filter(Boolean).join(' · ') }),
      h('h2', { text: post.title || '' }),
      h('p', { text: post.content || '' })
    ];
    // Photos are served only from the server's own /uploads path.
    if (post.media_url && /^\/uploads\/att_[A-Za-z0-9_.-]+$/.test(post.media_url)) {
      children.push(h('img', { className: 'photo', src: API + post.media_url, alt: 'Photo shared by ' + (post.author_name || 'a member'), loading: 'lazy' }));
    }
    children.push(h('p', { className: 'meta', text: (post.amen_count || 0) + ' amen · ' + (post.comment_count || 0) + ' comments' }));
    children.push(h('div', {}, actions));
    return h('article', { className: 'card glass' }, children);
  }

  // Composer: title and text, with an optional photo from the system picker.
  function composer() {
    var title = h('input', { type: 'text', maxlength: '120', placeholder: 'Title', 'aria-label': 'Post title' });
    var content = h('textarea', { rows: '4', maxlength: '2000', placeholder: 'Share a thought, prayer or praise', 'aria-label': 'Post text' });
    var photoState = h('p', { className: 'meta', text: '' });
    var photoData = null;
    var fileInput = h('input', { type: 'file', accept: 'image/*', hidden: '' });
    var post = h('button', { text: 'Post', on: { click: function () {
      if (!title.value.trim() || !content.value.trim()) { setStatus('Add a title and some text before posting.'); return; }
      post.disabled = true; setStatus('Posting…');
      var upload = photoData
        ? api('/api/community/media/upload', { method: 'POST', body: { base64Data: photoData, filename: 'photo.jpg' } }).then(function (r) { return r.mediaUrl; })
        : Promise.resolve('');
      upload.then(function (mediaUrl) {
        return api('/api/community/posts', { method: 'POST', body: { type: 'GENERAL', title: title.value.trim(), content: content.value.trim(), mediaUrl: mediaUrl } });
      }).then(viewCommunity)
        .catch(function (err) { post.disabled = false; setStatus(err.message); });
    } } });
    var pick = h('button', { className: 'ghost', text: 'Add photo', on: { click: function () {
      NATIVE.pickPhoto(fileInput).then(function (dataUrl) {
        if (!dataUrl) return;
        photoData = dataUrl;
        photoState.textContent = 'Photo ready to post.';
      }).catch(function (err) { setStatus(err.message); });
    } } });
    return h('section', { className: 'card glass' }, [
      h('h2', { text: 'Share with the church' }),
      h('label', { text: 'Title' }), title,
      h('label', { text: 'Text' }), content,
      pick, photoState, post, fileInput
    ]);
  }

  function renderPosts(data, stale) {
    var posts = (data.posts || []).filter(function (p) { return !state.blocked[p.author_id]; });
    var children = [];
    if (state.token) children.push(composer());
    else children.push(h('p', { className: 'meta', text: 'Reading is open to everyone. Sign in from Account to post, comment and report.' }));
    if (stale) children.push(h('p', { className: 'status', text: 'Showing saved discussions. Check your connection to see the latest.' }));
    if (!posts.length) children.push(h('p', { className: 'meta', text: 'No discussions here yet.' }));
    posts.forEach(function (p) { children.push(postCard(p)); });
    children.push(h('p', { id: 'status', className: 'status', role: 'status' }));
    $('view').replaceChildren.apply($('view'), children);
  }

  function viewCommunity() {
    $('view').replaceChildren(h('p', { className: 'meta', id: 'status', role: 'status', text: 'Loading community…' }));
    loadBlocks().then(function () { return api('/api/community/posts?limit=20'); }).then(function (data) {
      saveCache(CACHE_POSTS, { posts: data.posts || [] });
      renderPosts(data, false);
    }).catch(function (err) {
      readCache(CACHE_POSTS).then(function (saved) {
        if (saved && err.offline) { renderPosts(saved, true); return; }
        $('view').replaceChildren(h('p', { className: 'status', id: 'status', role: 'status', text: err.message }));
      });
    });
  }

  function viewAccount() {
    var view = $('view');
    if (state.user) {
      view.replaceChildren(
        h('section', { className: 'card glass' }, [
          h('h2', { text: state.user.name || 'Member' }),
          h('p', { className: 'meta', text: 'Signed in' }),
          h('button', { className: 'ghost', text: 'Sign out', on: { click: function () {
            clearSession().then(function () { show('account'); });
          } } }),
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
        .then(function (data) { saveSession(data.token, data.user); return loadBlocks(); })
        .then(function () { show('community'); })
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
    Array.prototype.forEach.call(document.querySelectorAll('.tab'), function (b) {
      b.addEventListener('click', function () { show(b.getAttribute('data-tab')); });
    });
    restoreSession().then(function () {
      if (state.token) registerDevice();
      return loadBlocks();
    }).then(function () { show('home'); });
    // The service worker is for browsers. Native apps load the bundled files.
    if ('serviceWorker' in navigator && location.protocol.indexOf('http') === 0 && !NATIVE.isNative()) {
      navigator.serviceWorker.register('sw.js').catch(function () { /* offline install is optional */ });
    }
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot);
  else boot();
})();
