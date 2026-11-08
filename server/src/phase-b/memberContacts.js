'use strict';

const fs = require('fs');
const path = require('path');

// Keep the Android bundled asset as the only maintained source. Resolve it
// relative to this module so the server can load it in any working directory.
const CONTACTS_CSV = path.resolve(__dirname, '../../../src/main/assets/member_contacts.csv');

function parseCsvLine(line) {
  const values = [];
  let value = '';
  let quoted = false;
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      if (quoted && line[i + 1] === '"') { value += '"'; i++; }
      else quoted = !quoted;
    } else if (char === ',' && !quoted) {
      values.push(value);
      value = '';
    } else value += char;
  }
  if (quoted) throw new Error('Malformed member contacts CSV');
  values.push(value);
  return values;
}

function loadMemberContacts(filePath = CONTACTS_CSV) {
  const lines = fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '').split(/\r?\n/).filter(line => line.trim());
  if (!lines.length || parseCsvLine(lines[0]).join(',') !== 'name,phone_local,phone_e164') {
    throw new Error('Member contacts CSV has an unexpected header');
  }
  return lines.slice(1).map(line => {
    const columns = parseCsvLine(line);
    if (columns.length !== 3 || !columns[0].trim()) throw new Error('Malformed member contacts CSV row');
    const phone = columns[2].trim();
    if (phone && !/^\+254[17]\d{8}$/.test(phone)) throw new Error('Member contacts CSV contains an invalid E.164 number');
    return { name: columns[0].trim(), phone_e164: phone || null };
  });
}

function classifyMemberPhone(normalizedPhone, contacts = loadMemberContacts()) {
  if (typeof normalizedPhone !== 'string' || !/^\+254[17]\d{8}$/.test(normalizedPhone)) return null;
  return contacts.find(contact => contact.phone_e164 === normalizedPhone) || null;
}

function classifyRegistrationPhone(normalizedPhone, contacts = loadMemberContacts()) {
  const contact = classifyMemberPhone(normalizedPhone, contacts);
  return { status: contact ? 'VERIFIED_MEMBER' : 'VERIFICATION_REVIEW', contact };
}

module.exports = { CONTACTS_CSV, loadMemberContacts, classifyMemberPhone, classifyRegistrationPhone };
