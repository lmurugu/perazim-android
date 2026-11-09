'use strict';

const assert = require('assert/strict');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
const { migrate } = require('../src/migrations');

async function run() {
  assert.notEqual(process.env.NODE_ENV, 'production');
  const adminUrl = new URL(process.env.PHASE_B_TEST_ADMIN_URL || '');
  assert.equal(adminUrl.pathname, '/postgres');
  assert(['127.0.0.1', 'localhost', '[::1]'].includes(adminUrl.hostname));
  const suffix = crypto.randomBytes(8).toString('hex');
  const database = `perazim_phaseb_${suffix}_test`;
  const pilot = `synthetic_pilot_${suffix}`;
  const admin = new Pool({ connectionString: adminUrl.toString() });
  const db = require('../src/db');
  let pool, server;
  let checks = 0;
  function check(actual, expected, message) { assert.equal(actual, expected, message); checks++; }
  try {
    await admin.query(`CREATE DATABASE "${database}"`);
    adminUrl.pathname = '/' + database;
    Object.assign(process.env, {
      DATABASE_URL: adminUrl.toString(), NODE_ENV: 'test', PHASE_B_SYNTHETIC: 'true',
      JWT_SECRET: crypto.randomBytes(48).toString('hex'), PHASE_B_ADMIN_KEY: crypto.randomBytes(32).toString('hex')
    });
    pool = new Pool({ connectionString: adminUrl.toString() });
    await migrate(pool);
    // Synthetic publication fixture for exercising registration acknowledgement.
    await pool.query(`UPDATE privacy_notice_versions SET publication_status='PUBLISHED',effective_at=$1,is_active=TRUE WHERE version='0.1'`, [Date.now()]);
    const pin = '853692';
    const now = Date.now();
    await pool.query(`INSERT INTO users(id,phone,pin_hash,name,role,is_active,created_at,updated_at)
      VALUES($1,'+254700009882',$2,'Synthetic Pilot','MEMBER',TRUE,$3,$3)`, [pilot, await bcrypt.hash(pin, 10), now]);
    const { bootstrap, recover } = require('../scripts/bootstrap-synthetic-admin');
    const options = { db: database, confirm: database, account: pilot, approval: 'SYNTHETIC_BOOTSTRAP_APPROVAL' };
    const enrollment = await bootstrap(pool, options);
    await assert.rejects(() => bootstrap(pool, options), /already bootstrapped/); checks++;
    check((await pool.query('SELECT role FROM users WHERE id=$1', [pilot])).rows[0].role, 'MEMBER', 'bootstrap does not change organisational role');
    await db.initDb();
    const { app } = require('../src/index');
    server = await new Promise(resolve => { const value = app.listen(0, '127.0.0.1', () => resolve(value)); });
    const base = `http://127.0.0.1:${server.address().port}`;
    async function request(method, path, token, body, privileged) {
      const response = await fetch(base + path, { method, headers: {
        'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}),
        ...(privileged ? { 'X-Privileged-Session': privileged } : {})
      }, ...(body !== undefined ? { body: JSON.stringify(body) } : {}) });
      return { status: response.status, body: await response.json() };
    }
    const currentNotice = await request('GET', '/api/privacy/notice/current');
    check(currentNotice.status, 200, 'synthetic current notice fixture is available');
    const registrationAcknowledgement = {
      privacy_notice_version: currentNotice.body.version,
      privacy_notice_hash: currentNotice.body.sha256,
      privacy_notice_presented_at: currentNotice.body.presented_at,
      privacy_notice_acknowledged: true
    };
    const login = await request('POST', '/api/auth/login', null, { phone: '+254700009882', pin });
    check(login.status, 200);
    const token = login.body.token;
    const pendingNoticeGate = await request('GET', '/api/auth/me', token);
    check(pendingNoticeGate.status, 428, 'protected account data requires the current acknowledgement');
    check(Object.keys(pendingNoticeGate.body).sort().join(','), 'code,sha256,version', 'privacy gate discloses only server-selected notice version and hash');
    const pilotPresentation = await request('POST', '/api/privacy/presentation', token, {});
    check(pilotPresentation.status, 200, 'synthetic pilot receives a presentation receipt');
    if (!pilotPresentation.body.acknowledged) {
      const pilotAcknowledgement = await request('POST', '/api/privacy/acknowledge', token, { receipt: pilotPresentation.body.receipt });
      check(pilotAcknowledgement.status, 200, 'synthetic pilot acknowledges the current notice');
    }
    const { totpAt, matchingCounter, encryptSecret, decryptSecret } = require('../src/phase-b/totp');
    check(totpAt('GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ', 1), '287082', 'RFC4226 vector');
    const encrypted = encryptSecret('SYNTHETIC_SECRET');
    check(decryptSecret(encrypted), 'SYNTHETIC_SECRET');
    await assert.rejects(async () => decryptSecret(encrypted.slice(0, -2) + (encrypted.endsWith('00') ? '01' : '00'))); checks++;
    const counter = Math.floor(Date.now() / 30000);
    const code = totpAt(enrollment.totp_secret, counter);
    check(matchingCounter(enrollment.totp_secret, code, Date.now(), counter), null, 'counter replay rejected');
    check((await request('GET', '/api/admin/audit', token)).status, 401, 'step-up required');
    const step = await request('POST', '/api/admin/security/step-up', token, { pin, code });
    check(step.status, 200, JSON.stringify(step.body));
    const privileged = step.body.token;
    check((await request('POST', '/api/admin/security/step-up', token, { pin, code })).status, 401, 'TOTP replay denied');
    async function privilegedRequest(method, path, body) { return request(method, '/api/admin' + path, token, body, privileged); }
    const users = [];
    for (let i = 0; i < 3; i++) {
      const registered = await request('POST', '/api/auth/register', null, {
        name: `Synthetic Claim ${i}`, phone: `+25470000989${i}`, pin, role: 'ADMIN', membership_status: 'VERIFIED_MEMBER', ...registrationAcknowledgement
      });
      check(registered.status, 201);
      users.push(registered.body);
      check((await request('GET', '/api/membership/status', registered.body.token)).body.status, 'REGISTERED');
    }
    check((await request('GET', '/api/membership/status')).status, 401);
    check((await request('GET', '/api/membership/member-resource', users[0].token)).status, 403);
    check((await request('GET', '/api/admin/membership/records', users[0].token, undefined, privileged)).status, 403, 'non-pilot cannot borrow privileged session');
    const records = [
      { reference: 'SYNTHETIC_A', full_name: 'Synthetic Claim 0', phone: '+254700009890' },
      { reference: 'SYNTHETIC_B', full_name: 'Synthetic Claim 1', phone: '+254700009891' },
      { reference: 'SYNTHETIC_C', full_name: 'Synthetic Claim 1', phone: '+254700009891' }
    ];
    const imported = await privilegedRequest('POST', '/membership/import', { source: 'SYNTHETIC_FIXTURE', records, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' });
    check(imported.status, 201, JSON.stringify(imported.body));
    check(imported.body.counts.accepted, 1, 'unambiguous complete row is accepted');
    check(imported.body.counts.shared_phone, 2, 'shared phone rows are staged for review');
    check(imported.body.counts.possible_duplicate, 2, 'possible duplicate identities are separately reported');
    const replayedImport = await privilegedRequest('POST', '/membership/import', { source: 'SYNTHETIC_FIXTURE', records, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' });
    check(replayedImport.status, 200, 'identical batch replay returns the existing import');
    check(replayedImport.body.replayed, true, 'identical batch replay is identified');
    const changedReplayRecords = records.map((row, index) => index === 0 ? { ...row, full_name: 'Synthetic Claim Zero Corrected' } : row);
    const changedReplay = await privilegedRequest('POST', '/membership/import', { source: 'SYNTHETIC_FIXTURE', records: changedReplayRecords, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' });
    check(changedReplay.status, 201, 'changed replay is staged for review');
    check(changedReplay.body.counts.changed_batch_replay, records.length, 'changed replay flags each row as a revision candidate');
    check(changedReplay.body.counts.accepted, 0, 'changed replay promotes no replacement records');
    check((await pool.query(`SELECT count(*)::int AS n FROM membership_records WHERE reference='SYNTHETIC_A'`)).rows[0].n, 1, 'changed replay preserves the prior operational record');

    const stagedBatch = await privilegedRequest('POST', '/membership/import', {
      source: 'SYNTHETIC_INCOMPLETE_FIXTURE', records: [
        { reference: 'SYNTHETIC_NAME_ONLY', full_name: 'Synthetic Name Only' },
        { reference: 'SYNTHETIC_DUPLICATE_REF', full_name: 'Synthetic Duplicate A', phone: '+254700009899' },
        { reference: 'synthetic_duplicate_ref', full_name: 'Synthetic Duplicate B', phone: '+254700009898' }
      ], request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST'
    });
    check(stagedBatch.status, 201);
    check(stagedBatch.body.counts.incomplete, 1, 'name-only row is staged without a fabricated phone');
    check(stagedBatch.body.counts.duplicate_reference, 2, 'duplicate references are staged and counted');
    const stagedRows = await privilegedRequest('GET', `/membership/imports/${stagedBatch.body.import_id}/staging`);
    check(stagedRows.status, 200);
    const nameOnlyRow = stagedRows.body.rows.find(row => row.source_reference === 'SYNTHETIC_NAME_ONLY');
    check(nameOnlyRow.row_status, 'INCOMPLETE');
    const promotedNameOnly = await privilegedRequest('POST', `/membership/import-staging/${nameOnlyRow.id}/promote`, {
      reference: 'SYNTHETIC_NAME_ONLY_REVIEWED', full_name: 'Synthetic Name Only', phone: '+254700009873',
      reviewed_conflicts: false, request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFIRMED'
    });
    check(promotedNameOnly.status, 201, 'explicit admin review promotes corrected staged data as an unverified record');
    check(promotedNameOnly.body.membership_status, 'UNVERIFIED');

    const legacyImport = await privilegedRequest('POST', '/membership/import', {
      source: 'SYNTHETIC_LEGACY_RECONCILIATION', records: [
        { reference: 'SYNTHETIC_LEGACY_RECORD', full_name: 'Synthetic Legacy Member', phone: '+254700009876' }
      ], request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST'
    });
    check(legacyImport.status, 201);
    const legacyRecord = (await privilegedRequest('GET', '/membership/records')).body.records.find(row => row.reference === 'SYNTHETIC_LEGACY_RECORD');
    const legacyAccount = await request('POST', '/api/auth/register', null, {
      name: 'Synthetic Mutable Display', phone: '+254700009876', pin, ...registrationAcknowledgement
    });
    check(legacyAccount.status, 201);
    await pool.query('DELETE FROM membership_account_identities WHERE user_id=$1', [legacyAccount.body.user.id]);
    check((await request('PUT', '/api/community/profile', legacyAccount.body.token, { name: 'Synthetic Changed Display' })).status, 200);
    const legacyRequestId = crypto.randomUUID();
    const reconciliation = await request('POST', '/api/membership/identity-reconciliation', legacyAccount.body.token, {
      request_id: legacyRequestId, proposed_name: 'Synthetic Legacy Member', proposed_phone: '+254700009876'
    });
    check(reconciliation.status, 202, 'legacy account can request separate identity review');
    assert.ok(!JSON.stringify(reconciliation.body).includes('SYNTHETIC_LEGACY_RECORD')); checks++;
    check((await request('GET', '/api/membership/status', legacyAccount.body.token)).body.status, 'REGISTERED', 'reconciliation request does not verify membership');
    check((await request('GET', '/api/admin/membership/identity-reconciliation', legacyAccount.body.token, undefined, privileged)).status, 403, 'ordinary account cannot read administrator reconciliation queue');
    const reconciliationQueue = await privilegedRequest('GET', '/membership/identity-reconciliation');
    check(reconciliationQueue.status, 200);
    const legacyRequest = reconciliationQueue.body.requests.find(row => row.request_id === legacyRequestId);
    check(legacyRequest.proposed_name, 'Synthetic Legacy Member');
    await pool.query('UPDATE membership_records SET revision=revision+1,updated_at=$2 WHERE id=$1', [legacyRecord.id, Date.now()]);
    const staleIdentityDecision = await privilegedRequest('POST', `/membership/identity-reconciliation/${legacyRequest.id}/decision`, {
      decision: 'APPROVE', record_id: legacyRecord.id, record_revision: legacyRecord.revision,
      request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFIRMED'
    });
    check(staleIdentityDecision.status, 409, 'stale reviewed record revision cannot approve reconciliation');
    check(staleIdentityDecision.body.code, 'RECONCILIATION_STALE_RECORD');
    check((await pool.query('SELECT count(*)::int AS n FROM membership_account_identities WHERE user_id=$1', [legacyAccount.body.user.id])).rows[0].n, 0, 'stale reconciliation creates no identity snapshot');
    const currentLegacyRecord = (await pool.query('SELECT revision FROM membership_records WHERE id=$1', [legacyRecord.id])).rows[0];
    const identityDecision = await privilegedRequest('POST', `/membership/identity-reconciliation/${legacyRequest.id}/decision`, {
      decision: 'APPROVE', record_id: legacyRecord.id, record_revision: currentLegacyRecord.revision, request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFIRMED'
    });
    check(identityDecision.status, 200, 'pilot can approve identity snapshot only after matching an authorized record');
    check(identityDecision.body.verified, false, 'identity approval does not link or verify membership');
    assert.deepEqual((await pool.query('SELECT registered_name,source FROM membership_account_identities WHERE user_id=$1', [legacyAccount.body.user.id])).rows[0],
      { registered_name: 'Synthetic Legacy Member', source: 'ADMIN_RECONCILIATION' }); checks++;
    check((await request('GET', '/api/membership/status', legacyAccount.body.token)).body.status, 'REGISTERED', 'approved reconciliation remains unverified pending a separate membership decision');
    for (let i = 0; i < users.length; i++) {
      const body = { request_id: crypto.randomUUID(), name: users[i].user.name, phone: users[i].user.phone };
      const claim = await request('POST', '/api/membership/claims', users[i].token, body);
      check(claim.status, 202);
      check(Object.keys(claim.body).sort().join(','), 'message,received,request_id', 'neutral receipt contains no matching metadata');
      check((await request('GET', '/api/membership/status', users[i].token)).body.status, 'VERIFICATION_PENDING', 'exact, missing and duplicate matches have identical public state');
      if (i === 0) {
        const duplicate = await request('POST', '/api/membership/claims', users[i].token, body);
        check(duplicate.status, 202, 'request replay is safely acknowledged');
        check((await request('POST', '/api/membership/claims', users[i].token, { ...body, request_id: crypto.randomUUID() })).status, 202);
        check((await pool.query('SELECT count(*)::int AS count FROM membership_claims WHERE user_id=$1', [users[i].user.id])).rows[0].count, 1, 'one open claim per account prevents flooding the review queue');
      }
    }
    const listing = await privilegedRequest('GET', '/membership/claims');
    check(listing.status, 200, JSON.stringify(listing.body));
    const claim = listing.body.claims.find(value => value.user_id === users[0].user.id);
    const registry = await privilegedRequest('GET', '/membership/records');
    check(registry.status, 200);
    const record = registry.body.records.find(value => value.reference === 'SYNTHETIC_A');
    const decided = await privilegedRequest('POST', `/membership/claims/${claim.id}/decision`, { decision: 'APPROVE', record_id: record.id, request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFIRMED' });
    check(decided.status, 200, JSON.stringify(decided.body));
    check((await request('GET', '/api/membership/status', users[0].token)).body.status, 'VERIFIED_MEMBER');
    const notice = (await request('GET', '/api/community/notifications', users[0].token)).body.notifications[0];
    check(notice.type, 'MEMBERSHIP_STATUS');
    check(notice.body, 'Open Membership in your profile to check your status.', 'notification does not expose register information');
    check((await request('GET', '/api/community/notifications', users[1].token)).body.notifications.length, 0, 'another account cannot see the notification');
    check((await request('GET', '/api/membership/member-resource', users[0].token)).status, 200);
    check((await request('GET', '/api/admin/audit', users[0].token, undefined, privileged)).status, 403, 'verified member is not administrator');
    await request('POST', '/api/membership/claims', users[0].token, { request_id: crypto.randomUUID(), name: 'Different synthetic claim', phone: users[0].user.phone });
    const newClaims = await privilegedRequest('GET', '/membership/claims');
    const additional = newClaims.body.claims.find(value => value.user_id === users[0].user.id);
    check((await privilegedRequest('POST', `/membership/claims/${additional.id}/decision`, { decision: 'REJECT', request_id: crypto.randomUUID(), reason: 'INSUFFICIENT_EVIDENCE' })).status, 200);
    check((await request('GET', '/api/membership/status', users[0].token)).body.status, 'VERIFIED_MEMBER', 'rejected additional attempt preserves verified link');
    check((await privilegedRequest('POST', `/membership/users/${users[0].user.id}/revoke`, { request_id: crypto.randomUUID(), reason: 'VERIFICATION_ERROR' })).status, 200);
    check((await request('GET', '/api/membership/member-resource', users[0].token)).status, 403);
    // Revocation must survive another first-use/self-service permission refresh.
    await request('GET', '/api/access/permissions', users[2].token);
    await pool.query("UPDATE user_permissions SET revoked_at=$2,revoked_by=$1 WHERE user_id=$1 AND permission_code='membership.claim.self.submit'", [users[2].user.id, Date.now()]);
    check((await request('POST', '/api/membership/claims', users[2].token, { request_id: crypto.randomUUID(), name: users[2].user.name, phone: users[2].user.phone })).status, 403, 'revoked baseline grant not recreated');
    // AUD-01-01: register as one identity, claim someone else's exact register
    // details, then have the real pilot try to approve the substitution.
    const outsider = await request('POST', '/api/auth/register', null, {
      name: 'Synthetic Different Account', phone: '+254700009896', pin, ...registrationAcknowledgement
    });
    check(outsider.status, 201);
    const auditImport = await privilegedRequest('POST', '/membership/import', {
      source: 'SYNTHETIC_AUD_01', records: [
        { reference: 'SYNTHETIC_OTHER_PERSON', full_name: 'Synthetic Other Person', phone: '+254700009895' },
        { reference: 'SYNTHETIC_SHARED_PHONE', full_name: 'Synthetic Other Person', phone: outsider.body.user.phone },
        { reference: 'SYNTHETIC_NEEDS_CORRECTION', full_name: 'Synthetic Incorrect Name', phone: '+254700009897' }
      ], request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST'
    });
    check(auditImport.status, 201);
    const auditRegistry = (await privilegedRequest('GET', '/membership/records')).body.records;
    const otherRecord = auditRegistry.find(row => row.reference === 'SYNTHETIC_OTHER_PERSON');
    const correctionRecord = auditRegistry.find(row => row.reference === 'SYNTHETIC_NEEDS_CORRECTION');
    const claimBody = (name, phone) => ({ name, phone, request_id: crypto.randomUUID() });
    const submit = (bearer, body) => request('POST', '/api/membership/claims', bearer, body);
    const approval = (claimId, recordId) => privilegedRequest('POST', `/membership/claims/${claimId}/decision`, {
      decision: 'APPROVE', record_id: recordId, request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFIRMED'
    });
    const expectNeutral = (response, body) => {
      check(response.status, 202);
      assert.deepEqual(response.body, {
        received: true, request_id: body.request_id, message: 'Your request has been received for review.'
      }); checks++;
    };
    const substituted = claimBody(otherRecord.full_name, otherRecord.phone);
    expectNeutral(await submit(outsider.body.token, substituted), substituted);
    const queue = (await privilegedRequest('GET', '/membership/claims')).body.claims;
    const substitutedClaim = queue.find(row => row.user_id === outsider.body.user.id);
    check(substitutedClaim.status, 'REVIEW', 'substituted identity stays in private review');
    const denied = await approval(substitutedClaim.id, otherRecord.id);
    check(denied.status, 409, 'pilot cannot approve the exact victim details');
    check(denied.body.code, 'CLAIM_IDENTITY_MISMATCH');
    const concurrentClaim = claimBody(otherRecord.full_name, otherRecord.phone);
    const [concurrentReceipt, concurrentDenial] = await Promise.all([
      submit(outsider.body.token, concurrentClaim), approval(substitutedClaim.id, otherRecord.id)
    ]);
    expectNeutral(concurrentReceipt, concurrentClaim);
    check(concurrentDenial.status, 409);
    check(concurrentDenial.body.code, 'CLAIM_IDENTITY_MISMATCH');
    check((await request('GET', '/api/membership/status', outsider.body.token)).body.status, 'VERIFICATION_PENDING');
    check((await request('GET', '/api/membership/member-resource', outsider.body.token)).status, 403);
    check((await pool.query('SELECT count(*)::int AS count FROM membership_claims WHERE user_id=$1', [outsider.body.user.id])).rows[0].count, 1);
    const fixLink = (userId, recordId) => privilegedRequest('POST', `/membership/users/${userId}/correct`, {
      record_id: recordId, request_id: crypto.randomUUID(), reason: 'VERIFICATION_ERROR'
    });
    check((await fixLink(outsider.body.user.id, otherRecord.id)).status, 409, 'link correction cannot create an initial verification');
    check((await privilegedRequest('POST', `/membership/claims/${substitutedClaim.id}/decision`, {
      decision: 'REJECT', request_id: crypto.randomUUID(), reason: 'IDENTITY_CONFLICT'
    })).status, 200);
    const sharedPhoneRecord = auditRegistry.find(row => row.reference === 'SYNTHETIC_SHARED_PHONE');
    const changedProfile = await request('PUT', '/api/community/profile', outsider.body.token, { name: sharedPhoneRecord.full_name });
    check(changedProfile.status, 200);
    check(changedProfile.body.user.name, sharedPhoneRecord.full_name, 'display name remains editable');
    const profileSpoof = claimBody(sharedPhoneRecord.full_name, sharedPhoneRecord.phone);
    expectNeutral(await submit(outsider.body.token, profileSpoof), profileSpoof);
    const spoofedClaim = (await privilegedRequest('GET', '/membership/claims')).body.claims.find(row => row.request_id === profileSpoof.request_id);
    check(spoofedClaim.status, 'REVIEW');
    check((await approval(spoofedClaim.id, sharedPhoneRecord.id)).body.code, 'CLAIM_IDENTITY_MISMATCH');

    const claimant = await request('POST', '/api/auth/register', null, {
      name: 'Synthetic Corrected Name', phone: correctionRecord.phone, pin, ...registrationAcknowledgement
    });
    check(claimant.status, 201);
    const ownClaim = claimBody(claimant.body.user.name, claimant.body.user.phone);
    expectNeutral(await submit(claimant.body.token, ownClaim), ownClaim);
    const review = (await privilegedRequest('GET', '/membership/claims')).body.claims.find(row => row.user_id === claimant.body.user.id);
    check((await approval(review.id, correctionRecord.id)).body.code, 'RECORD_MISMATCH');
    const correctionPath = `/api/admin/membership/records/${correctionRecord.id}`;
    const patchBody = (name, phone) => ({ full_name: name, phone, request_id: crypto.randomUUID(), reason: 'IDENTITY_CORRECTION' });
    const firstCorrection = patchBody('Synthetic Intermediate Name', correctionRecord.phone);
    check((await request('PATCH', correctionPath, claimant.body.token, firstCorrection, privileged)).status, 403);
    check((await request('PATCH', correctionPath, token, firstCorrection, privileged)).status, 200);
    check((await request('PATCH', correctionPath, token, firstCorrection, privileged)).status, 409, 'replay must not add a revision');
    const finalCorrection = patchBody(claimant.body.user.name, correctionRecord.phone);
    check((await request('PATCH', correctionPath, token, finalCorrection, privileged)).status, 200);
    check((await request('GET', '/api/membership/status', claimant.body.token)).body.status, 'VERIFICATION_PENDING', 'register correction alone grants no access');
    check((await fixLink(claimant.body.user.id, correctionRecord.id)).status, 409, 'corrected register still requires approval');

    // Simulate an authorised account-data correction in the fixture. The bearer
    // still carries the original identity and must not override the database.
    await pool.query("UPDATE membership_account_identities SET registered_name='Synthetic Stale Bearer Fixture' WHERE user_id=$1", [claimant.body.user.id]);
    check((await approval(review.id, correctionRecord.id)).body.code, 'CLAIM_IDENTITY_MISMATCH');
    await pool.query('UPDATE membership_account_identities SET registered_name=$2 WHERE user_id=$1', [claimant.body.user.id, claimant.body.user.name]);
    const decisions = await Promise.all([approval(review.id, correctionRecord.id), approval(review.id, correctionRecord.id)]);
    assert.deepEqual(decisions.map(result => result.status).sort(), [200, 409]); checks++;
    check((await request('GET', '/api/membership/member-resource', claimant.body.token)).status, 200);
    check((await fixLink(claimant.body.user.id, otherRecord.id)).body.code, 'RECORD_MISMATCH');
    check((await request('PATCH', correctionPath, token, patchBody(otherRecord.full_name, otherRecord.phone), privileged)).body.code, 'RECORD_MISMATCH');

    // AUD-01-04: both revisions are recoverable, failed changes leave no audit
    // or revision entries, and the general audit contains references only.
    const revisions = await pool.query(
      'SELECT revision,full_name,phone FROM membership_record_revisions WHERE record_id=$1 ORDER BY revision', [correctionRecord.id]
    );
    assert.deepEqual(revisions.rows, [
      { revision: 1, full_name: correctionRecord.full_name, phone: correctionRecord.phone },
      { revision: 2, full_name: firstCorrection.full_name, phone: correctionRecord.phone }
    ]); checks++;
    const liveRevision = await pool.query('SELECT revision,full_name,phone FROM membership_records WHERE id=$1', [correctionRecord.id]);
    assert.deepEqual(liveRevision.rows, [{ revision: 3, full_name: finalCorrection.full_name, phone: correctionRecord.phone }]); checks++;
    const corrections = await pool.query(
      `SELECT actor_id,resource_type,resource_id,outcome,reason,request_id,created_at FROM admin_audit_log
       WHERE action='MEMBERSHIP_RECORD_CORRECT' AND resource_id LIKE $1 ORDER BY resource_id`, [correctionRecord.id + '%']
    );
    assert.deepEqual(corrections.rows.map(({ created_at, ...event }) => event), [
      { actor_id: pilot, resource_type: 'membership_record', resource_id: correctionRecord.id + ':1:2',
        outcome: 'SUCCESS', reason: 'IDENTITY_CORRECTION', request_id: firstCorrection.request_id },
      { actor_id: pilot, resource_type: 'membership_record', resource_id: correctionRecord.id + ':2:3',
        outcome: 'SUCCESS', reason: 'IDENTITY_CORRECTION', request_id: finalCorrection.request_id }
    ]); checks++;
    assert(Number(corrections.rows[0].created_at) <= Number(corrections.rows[1].created_at)); checks++;
    await assert.rejects(() => pool.query('DELETE FROM membership_record_revisions WHERE record_id=$1', [correctionRecord.id]), /append-only/); checks++;
    check((await request('GET', '/api/admin/audit', outsider.body.token, undefined, privileged)).status, 403);

    const originalGrant = await pool.query("SELECT id FROM user_permissions WHERE user_id=$1 AND permission_code='membership.record.read'", [pilot]);
    check((await privilegedRequest('DELETE', `/permissions/${pilot}/${originalGrant.rows[0].id}`, { request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' })).status, 200);
    check((await privilegedRequest('GET', '/membership/records')).status, 403, 'revoked admin permission effective immediately');
    const scoped = await privilegedRequest('POST', `/permissions/${pilot}`, {
      code: 'membership.record.read', scope: record.id, request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST'
    });
    check(scoped.status, 201);
    const scopedRecords = await privilegedRequest('GET', '/membership/records');
    check(scopedRecords.status, 200);
    check(scopedRecords.body.records.map(value => value.id).join(','), record.id, 'scope filters out all other register entries');
    await pool.query("UPDATE permissions SET active=FALSE WHERE code='membership.record.read'");
    check((await privilegedRequest('GET', '/membership/records')).status, 403, 'disabled permission denies even an otherwise valid assignment');
    await pool.query("UPDATE permissions SET active=TRUE WHERE code='membership.record.read'");
    await pool.query('UPDATE user_permissions SET effective_at=$2,expires_at=$3 WHERE id=$1', [scoped.body.grant.id, Date.now() - 2000, Date.now() - 1000]);
    check((await privilegedRequest('GET', '/membership/records')).status, 403, 'expired scoped assignment denied');
    check((await privilegedRequest('POST', `/permissions/${users[0].user.id}`, { code: 'admin.permissions.grant', request_id: crypto.randomUUID(), reason: 'SYNTHETIC_TEST' })).status, 403, 'cannot create another administrator');

    check((await privilegedRequest('GET', '/audit')).status, 200);
    const audit = await pool.query('SELECT action FROM admin_audit_log');
    assert(audit.rows.some(row => row.action.includes('READ'))); checks++;
    await assert.rejects(() => pool.query("UPDATE admin_audit_log SET outcome='ALTERED'"), /append-only/); checks++;
    check((await privilegedRequest('POST', '/security/revoke-sessions', { compromised: true, reason: 'ACCOUNT_COMPROMISE', request_id: crypto.randomUUID() })).status, 200);
    check((await request('GET', '/api/auth/me', token)).status, 401, 'compromised bearer rejected');
    const recovery = await recover(pool, { ...options, approval: 'SYNTHETIC_RECOVERY_APPROVAL', newPin: '792631' });
    await new Promise(resolve => setTimeout(resolve, Math.max(0, recovery.auth_not_before - Date.now() + 30)));
    check((await request('GET', '/api/auth/me', token)).status, 401, 'recovered account rejects old bearer');
    check((await request('POST', '/api/auth/login', null, { phone: '+254700009882', pin: '792631' })).status, 200, 'same identity can authenticate after recovery');
  } finally {
    if (server) await new Promise(resolve => server.close(resolve));
    await db.closeDb();
    if (pool) await pool.end();
    try {
      for (let attempt = 0; ; attempt++) {
        try { await admin.query(`DROP DATABASE IF EXISTS "${database}"`); break; }
        catch (error) {
          if (error.code !== '55006' || attempt === 4) throw error;
          await new Promise(resolve => setTimeout(resolve, 150));
        }
      }
    } finally { await admin.end(); }
  }
    console.log(`Phase B workflow gate passed: ${checks} assertions; synthetic bootstrap, neutral claims, review, verification, revocation, permission isolation, audit, TOTP and recovery.`);
}
run().catch(error => { console.error('Phase B workflow gate failed:', error.message); process.exitCode = 1; });
