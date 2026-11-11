const assert = require('assert');
const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '../..');
const sourceRoots = ['src/main/java', 'server/src'].map(root => path.join(repoRoot, root));
const logger = /\b(?:Log\.[vdiew]|System\.out\.(?:print|println)|console\.(?:log|info|warn|error|debug))\s*\(/;
const sensitiveDynamicValue = /(?:\+\s*(?:userId|fromUserId|toUserId|peerId|phone|token|testimony|prayerId|content|payload|reason|message|authorName)\b|,\s*(?:err|error|itemErr)\b|\$\{\s*(?:userId|fromUserId|toUserId|peerId|phone|token|testimony|prayerId|content|payload|reason|message|authorName)\b)/;

function sourceFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
    const fullPath = path.join(directory, entry.name);
    if (entry.isDirectory()) return sourceFiles(fullPath);
    return /\.(?:java|kt|js)$/.test(entry.name) ? [fullPath] : [];
  });
}

const findings = [];
for (const root of sourceRoots) {
  for (const file of sourceFiles(root)) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    for (let index = 0; index < lines.length; index++) {
      if (logger.test(lines[index]) && sensitiveDynamicValue.test(lines[index])) {
        findings.push(`${path.relative(repoRoot, file)}:${index + 1}`);
      }
    }
  }
}

assert.deepStrictEqual(findings, [], `Potential sensitive logging arguments found: ${findings.join(', ')}`);
console.log('PASS: no direct sensitive-field or raw-exception logging expressions in src/main/java or server/src');
