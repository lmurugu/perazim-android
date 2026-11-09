'use strict';

const assert = require('assert');
const { loadMemberContacts, classifyMemberPhone, classifyRegistrationPhone } = require('../src/phase-b/memberContacts');
const { validateAndNormalizePhone } = require('../src/routes/auth');

const syntheticContacts = [{ name: 'Synthetic Member', phone_e164: '+254712345678' }];
const matchingPhone = validateAndNormalizePhone('0712345678');
assert.strictEqual(matchingPhone, syntheticContacts[0].phone_e164);
assert.deepStrictEqual(loadMemberContacts(), [], 'legacy loader cannot read the private CSV');
assert.strictEqual(classifyMemberPhone(matchingPhone), null, 'no register is available to ordinary lookup code');
assert.strictEqual(classifyRegistrationPhone(matchingPhone, syntheticContacts).status, 'REGISTERED', 'even a synthetic match cannot verify a new account');
assert.strictEqual(classifyRegistrationPhone(matchingPhone, syntheticContacts).contact, null, 'registration receives no candidate record');

process.stdout.write('Contact lookup is disabled for registration; synthetic matches remain unverified.\n');
