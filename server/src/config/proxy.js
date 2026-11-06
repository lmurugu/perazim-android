// Opt in only when the API is private behind exactly one reverse proxy.
// Leave unset for direct/local access; never trust arbitrary forwarded headers.
function proxyTrustSetting(value) {
  if (value === undefined || value === '') return false;
  if (value === '1') return 1;
  throw new Error('TRUST_PROXY_HOPS must be unset or exactly 1 for the private single-proxy deployment');
}

module.exports = { proxyTrustSetting };
