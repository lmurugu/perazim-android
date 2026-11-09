'use strict';

const { validateAndNormalizePhone } = require('../routes/auth');

function normalizeName(value) {
  return typeof value === 'string' ? value.trim().replace(/\s+/g, ' ').toLowerCase() : '';
}

function matchesMembershipIdentity(account, name, phone) {
  if (!account?.is_active) return false;
  const registeredName = normalizeName(account.registered_name);
  const registeredPhone = validateAndNormalizePhone(account.phone);
  return Boolean(registeredName && registeredPhone &&
    registeredName === normalizeName(name) && registeredPhone === validateAndNormalizePhone(phone));
}

async function readMembershipAccount(client, userId, lock = false) {
  const { rows: [account] } = await client.query(
    `SELECT u.phone,u.is_active,i.registered_name FROM users u
     LEFT JOIN membership_account_identities i ON i.user_id=u.id
     WHERE u.id=$1${lock ? ' FOR UPDATE OF u' : ''}`, [userId]
  );
  return account;
}

module.exports = { matchesMembershipIdentity, normalizeName, readMembershipAccount };
