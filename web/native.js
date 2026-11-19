// Native features for the store wrappers. Every function falls back to the browser,
// so the same files run in a normal web browser and inside Capacitor on iPhone and Android.
(function () {
  'use strict';

  function capacitor() { return window.Capacitor || null; }
  function isNative() {
    var c = capacitor();
    return !!(c && typeof c.isNativePlatform === 'function' && c.isNativePlatform());
  }
  function plugin(name) {
    var c = capacitor();
    return c && c.Plugins ? c.Plugins[name] || null : null;
  }
  function platform() {
    var c = capacitor();
    return c && typeof c.getPlatform === 'function' ? c.getPlatform() : 'web';
  }

  // Key-value storage. Native uses the Preferences plugin, which survives the web view
  // clearing its own storage. The browser falls back to localStorage.
  var kv = {
    get: function (key) {
      var prefs = plugin('Preferences');
      if (isNative() && prefs) {
        return prefs.get({ key: key }).then(function (r) { return r.value; }).catch(function () { return null; });
      }
      try { return Promise.resolve(localStorage.getItem(key)); } catch (_) { return Promise.resolve(null); }
    },
    set: function (key, value) {
      var prefs = plugin('Preferences');
      if (isNative() && prefs) {
        return prefs.set({ key: key, value: String(value) }).catch(function () { /* ignore */ });
      }
      try { localStorage.setItem(key, String(value)); } catch (_) { /* private mode */ }
      return Promise.resolve();
    },
    remove: function (key) {
      var prefs = plugin('Preferences');
      if (isNative() && prefs) {
        return prefs.remove({ key: key }).catch(function () { /* ignore */ });
      }
      try { localStorage.removeItem(key); } catch (_) { /* ignore */ }
      return Promise.resolve();
    }
  };

  // Shrinks a photo to at most 1600 px on its longest side and returns a JPEG data URL.
  function downscale(dataUrl) {
    return new Promise(function (resolve, reject) {
      var img = new Image();
      img.onload = function () {
        var max = 1600;
        var scale = Math.min(1, max / Math.max(img.width, img.height));
        var canvas = document.createElement('canvas');
        canvas.width = Math.round(img.width * scale);
        canvas.height = Math.round(img.height * scale);
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        resolve(canvas.toDataURL('image/jpeg', 0.85));
      };
      img.onerror = function () { reject(new Error('That file is not a photo we can read.')); };
      img.src = dataUrl;
    });
  }

  function readFileAsDataUrl(file) {
    return new Promise(function (resolve, reject) {
      var reader = new FileReader();
      reader.onload = function () { resolve(reader.result); };
      reader.onerror = function () { reject(new Error('The photo could not be read.')); };
      reader.readAsDataURL(file);
    });
  }

  // Native: the system photo picker from the Camera plugin. Browser: a file input.
  // Resolves to a JPEG data URL, or null if the member cancels.
  function pickPhoto(fileInput) {
    var camera = plugin('Camera');
    if (isNative() && camera) {
      return camera.getPhoto({ source: 'PHOTOS', resultType: 'base64', quality: 85, width: 1600, height: 1600, correctOrientation: true })
        .then(function (photo) {
          if (!photo || !photo.base64String) return null;
          return downscale('data:image/' + (photo.format || 'jpeg') + ';base64,' + photo.base64String);
        })
        .catch(function (err) {
          // Cancelling the picker is not an error for the member.
          if (err && /cancel/i.test(String(err.message || err))) return null;
          throw err;
        });
    }
    return new Promise(function (resolve, reject) {
      fileInput.onchange = function () {
        var file = fileInput.files && fileInput.files[0];
        fileInput.value = '';
        if (!file) return resolve(null);
        readFileAsDataUrl(file).then(downscale).then(resolve, reject);
      };
      fileInput.click();
    });
  }

  // Push notifications: asks for permission, then resolves to the device token (native only).
  var pushListenerAdded = false;
  var pushHandlers = [];
  function registerPush(onToken) {
    var push = plugin('PushNotifications');
    if (!isNative() || !push) return Promise.resolve(null);
    if (!pushListenerAdded) {
      pushListenerAdded = true;
      push.addListener('registration', function (token) {
        pushHandlers.forEach(function (fn) { fn(token.value); });
      });
      push.addListener('registrationError', function () { /* permission denied or no push setup */ });
    }
    if (onToken) pushHandlers.push(onToken);
    return push.requestPermissions().then(function (status) {
      if (status.receive !== 'granted') return null;
      return push.register().then(function () { return null; });
    }).catch(function () { return null; });
  }

  // Lock-screen and headset controls for audio, through the Media Session API that
  // WKWebView and Android WebView support. Background playback on iOS is enabled by the
  // audio background mode in Info.plist. This only sets the now-playing details.
  function setNowPlaying(audioEl, title, artist) {
    if (!('mediaSession' in navigator) || !window.MediaMetadata) return;
    navigator.mediaSession.metadata = new MediaMetadata({ title: title || 'Perazim', artist: artist || 'Perazim Mission Church' });
    navigator.mediaSession.setActionHandler('play', function () { audioEl.play(); });
    navigator.mediaSession.setActionHandler('pause', function () { audioEl.pause(); });
  }

  window.PerazimNative = {
    isNative: isNative,
    platform: platform,
    kv: kv,
    pickPhoto: pickPhoto,
    registerPush: registerPush,
    setNowPlaying: setNowPlaying
  };
})();
