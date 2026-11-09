'use strict';

function validateAdminTarget(raw, expectedPort) {
  if (!raw) throw new Error('Admin connection URL is required');
  let url;
  try { url = new URL(raw); } catch { throw new Error('Invalid admin connection URL'); }
  if (!['postgres:', 'postgresql:'].includes(url.protocol)) throw new Error('Admin connection must use PostgreSQL');
  if (url.hostname.toLowerCase() !== '127.0.0.1') throw new Error('Admin connection target must be runner loopback');
  if (!url.port || Number(url.port) !== Number(expectedPort)) throw new Error('Admin connection port does not match owned loopback cluster');
  if (decodeURIComponent(url.pathname.slice(1)) !== 'postgres') throw new Error('Admin connection must target maintenance database postgres');
  if (url.username !== 'postgres') throw new Error('Admin connection must use runner postgres role');
  if (url.search || url.hash) throw new Error('Unexpected PostgreSQL connection parameters or URL fragment');
  return Object.freeze({ host: '127.0.0.1', port: Number(url.port), database: 'postgres', user: 'postgres', connectionString: url.toString() });
}

function assertConnectedOwnedCluster(row, expectedPort, expectedDataDir) {
  const actualAddr = String(row && row.addr || '').split('/')[0];
  const path = require('path');
  const actualDataDir = row && row.data_dir ? path.resolve(row.data_dir) : '';
  if (!row || row.db !== 'postgres' || row.user !== 'postgres' || actualAddr !== '127.0.0.1' || Number(row.port) !== Number(expectedPort) || actualDataDir !== path.resolve(expectedDataDir || '')) {
    throw new Error(`Connected PostgreSQL identity mismatch: db=${row && row.db}, user=${row && row.user}, addr=${row && row.addr}, port=${row && row.port}, data_dir=${actualDataDir}`);
  }
}

module.exports = { validateAdminTarget, assertConnectedOwnedCluster };
