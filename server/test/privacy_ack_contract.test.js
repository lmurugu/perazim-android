'use strict';

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const android = read('src/main/java/org/perazimchurch/app/presentation/ui/AuthDialog.java');
const server = read('server/src/phase-b/privacyNotice.js');
const draft = read('server/privacy-notice-v0.2.md');
const uiMatch = android.match(/chkPrivacyAcknowledgement\.setText\("([^"]+)"\)/);
const serverMatch = server.match(/const ACKNOWLEDGEMENT_TEXT = '([^']+)'/);
const draftMatch = draft.match(/^> (I acknowledge[^\n]+)$/m);
assert(uiMatch && serverMatch && draftMatch, 'Android, server and draft acknowledgement text must exist');
assert.strictEqual(uiMatch[1], serverMatch[1]);
assert.strictEqual(serverMatch[1], draftMatch[1]);
process.stdout.write('PASS: Android, server-stored and v0.2 draft acknowledgement wording matches exactly.\n');
