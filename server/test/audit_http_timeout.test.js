'use strict';

const assert = require('assert/strict');
const http = require('http');
const { httpRequest } = require('./antigravity_security_audit.test');

(async () => {
  let assertions = 0;
  const server = http.createServer((req, res) => setTimeout(() => res.end('late'), 150));
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const port = server.address().port;
  assertions++;
  await assert.rejects(httpRequest(port, 'GET', '/timeout', {}, null, 25), /HTTP request timed out/);
  server.closeAllConnections();
  await new Promise(resolve => server.close(resolve));

  assertions++;
  await assert.rejects(httpRequest(port, 'GET', '/refused', {}, null, 100), /ECONNREFUSED|connect|socket/i);
  console.log(`HTTP timeout tests: ${assertions} assertions passed, 0 skipped`);
})().catch(err => {
  console.error('HTTP timeout test failed:', err);
  process.exitCode = 1;
});
