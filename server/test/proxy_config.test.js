// Pure request/configuration checks: no HTTP listener, database, or member data.
const assert = require('node:assert/strict');
const express = require('express');
const { proxyTrustSetting } = require('../src/config/proxy');

assert.equal(proxyTrustSetting(undefined), false);
assert.equal(proxyTrustSetting(''), false);
assert.equal(proxyTrustSetting('1'), 1);
for (const invalid of ['true', 'false', '0', '2', 'loopback', '*']) {
  assert.throws(() => proxyTrustSetting(invalid), /TRUST_PROXY_HOPS/);
}

function requestIp(setting, forwarded) {
  const app = express();
  app.set('trust proxy', setting);
  const req = Object.create(app.request);
  req.socket = { remoteAddress: '172.20.0.3' };
  req.connection = req.socket;
  req.headers = { 'x-forwarded-for': forwarded };
  return req.ip;
}

// Direct/local mode ignores client-supplied forwarding headers.
assert.equal(requestIp(false, '203.0.113.11'), '172.20.0.3');
// One private proxy reports its immediate client; spoofed extra hops do not win.
assert.equal(requestIp(1, '198.51.100.99, 203.0.113.11'), '203.0.113.11');
assert.equal(requestIp(1, '203.0.113.12'), '203.0.113.12');
console.log('Proxy configuration and request-IP unit checks passed (no network/DB)');
