// Writes the screen rows of docs/design/INDEX.md between markers (keeps other rows intact).
'use strict';
const fs = require('fs');
const path = require('path');
const all = ['screens-a', 'screens-b', 'screens-c', 'screens-d']
  .filter((f) => fs.existsSync(path.join(__dirname, f + '.js')))
  .flatMap((f) => require('./' + f));
const rows = [];
for (const s of all) {
  const modes = s.modes || ['dark', 'light'];
  for (const m of modes) {
    rows.push(`| 02-screens/${s.id}/${s.id}_MOCK_${m}_v01.html | ${s.id} ${s.name} | ${m} | v01 | under review |`);
    rows.push(`| (project folder) design/02-screens/${s.id}/${s.id}_REF_${m}_v01.png | ${s.id} ${s.name} | ${m} | v01 | under review |`);
  }
  rows.push(`| 02-screens/${s.id}/${s.id}_WIRE_v01.png | ${s.id} ${s.name} | greyscale | v01 | under review |`);
}
const block = `<!-- screens:start -->\n| File | Screen | Mode | Version | Status |\n|---|---|---|---|---|\n${rows.join('\n')}\n<!-- screens:end -->`;
const file = path.resolve(__dirname, '..', 'INDEX.md');
let text = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '# Design folder index\n\n';
if (text.includes('<!-- screens:start -->')) {
  text = text.replace(/<!-- screens:start -->[\s\S]*?<!-- screens:end -->/, block);
} else {
  text += '\n## Screens (batch A to D)\n\n' + block + '\n';
}
fs.writeFileSync(file, text);
console.log('INDEX rows:', rows.length);
