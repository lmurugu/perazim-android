// Compares Node JSON.stringify (the signer's canonical string form) against the proposed Java jsonString.
'use strict';
const fs = require('fs');
const { execFileSync } = require('child_process');
const cases = ['Peace ’ grace — hope', '\u0085', 'a</b>', 'quote " and \\ backslash', 'tab\tnl\ncr\rbs\bff\f',
  '\u0001\u001f', '  ', 'é €', '😀 emoji', ''];
const rnd = () => { const pool = [0x20, 0x22, 0x5c, 0x2f, 0x3c, 0x0a, 0x01, 0x7f, 0x85, 0xa0, 0x2019, 0x2028, 0x20ac, 0xe9, 0x4e2d];
  let s = ''; const n = 1 + Math.floor(Math.random() * 12); for (let i = 0; i < n; i++) s += String.fromCodePoint(pool[Math.floor(Math.random() * pool.length)]); return s; };
for (let i = 0; i < 500; i++) cases.push(rnd());
fs.writeFileSync('strings_in.jsonl', cases.map(s => JSON.stringify(s)).join('\n') + '\n');
const javaOut = execFileSync('java', ['-cp', 'out:/tmp/s2jar/json.jar', 'org.perazimchurch.app.ai.local.ProposedCanonicalHarness', 'strings_in.jsonl'],
  { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).split('\n').filter(l => l.length);
let bad = 0;
const toAscii = t => t.replace(/[\u007f-\uffff]/g, c => '\\u' + c.charCodeAt(0).toString(16).padStart(4, '0'));
cases.forEach((s, i) => { if (toAscii(JSON.stringify(s)) !== javaOut[i]) bad++; });
console.log('strings=' + cases.length + ' proposed-java-vs-node mismatches=' + bad);
