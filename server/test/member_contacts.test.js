'use strict';

const assert = require('assert');
const { loadMemberContacts, classifyMemberPhone, classifyRegistrationPhone } = require('../src/phase-b/memberContacts');
const { validateAndNormalizePhone } = require('../src/routes/auth');

const contacts = loadMemberContacts();
assert.strictEqual(contacts.length, 65, 'loads all listed contacts from the canonical Android asset');

const numberedContact = contacts.find(contact => contact.phone_e164);
assert(numberedContact, 'fixture includes a contact with a number');
const matchingLocalPhone = `0${numberedContact.phone_e164.slice(4)}`;
const normalizedMatchingPhone = validateAndNormalizePhone(matchingLocalPhone);
assert.strictEqual(normalizedMatchingPhone, numberedContact.phone_e164, 'local input is normalized to the CSV E.164 value');
assert.strictEqual(classifyMemberPhone(normalizedMatchingPhone, contacts), numberedContact);
assert.strictEqual(classifyRegistrationPhone(normalizedMatchingPhone, contacts).status, 'VERIFIED_MEMBER');

let nonMatchingPhone = '+254700000000';
while (contacts.some(contact => contact.phone_e164 === nonMatchingPhone)) {
  const next = Number(nonMatchingPhone.slice(-8)) + 1;
  nonMatchingPhone = `+2547${String(next).padStart(8, '0')}`;
}
assert.strictEqual(classifyMemberPhone(nonMatchingPhone, contacts), null);
assert.strictEqual(classifyRegistrationPhone(validateAndNormalizePhone(`0${nonMatchingPhone.slice(4)}`), contacts).status, 'VERIFICATION_REVIEW');

const nameOnlyContact = contacts.find(contact => contact.phone_e164 === null);
assert(nameOnlyContact, 'fixture includes a name-only contact');
assert.strictEqual(classifyMemberPhone(null, contacts), null);
assert.strictEqual(classifyRegistrationPhone(null, contacts).status, 'VERIFICATION_REVIEW');

process.stdout.write('Member contact registration cases: match=CONFIRMED, non-match=MANUAL_REVIEW, no-number=MANUAL_REVIEW\n');
