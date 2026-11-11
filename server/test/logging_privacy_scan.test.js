const assert = require('assert');
const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '../..');
const sourceRoots = ['src/main/java', 'server/src'].map(root => path.join(repoRoot, root));
const logger = /\b(?:Log\.[vdiew]|System\.out\.(?:print|println)|console\.(?:log|info|warn|error|debug))\s*\(/g;
const sensitiveDynamicValue = /(?:\b(?:userId|fromUserId|toUserId|peerId|memberId|accountId|authorId|prayerId|mutationId|freshGuestId|connectionId|notificationId|phone|token|testimony|content|payload|reason|message|authorName|query|cursor|effectiveNextCursor|email|pin|password)\b|(?<!get)\b[A-Za-z_$][\w$]*(?:Id|Phone|Token|Content|Payload|Reason|Testimony|AuthorName|Query|Cursor|Message|Email|Pin|Password)\b|\.getId\s*\(|\b(?:user|member|author|peer|prayer|mutation|account)[_.]id\b|\$\{\s*(?:userId|fromUserId|toUserId|peerId|phone|token|testimony|prayerId|content|payload|reason|message|authorName)\b|,\s*(?:err|error|itemErr)\s*\))/;

function stripStringLiterals(value) {
  return value.replace(/(["'`])(?:\\.|(?!\1)[\s\S])*?\1/g, '');
}

function findLoggerCalls(source) {
  const calls = [];
  logger.lastIndex = 0;
  let match;
  while ((match = logger.exec(source)) !== null) {
    let depth = 1;
    let quote = null;
    let escaped = false;
    let end = match.index + match[0].length;
    for (; end < source.length && depth > 0; end++) {
      const character = source[end];
      if (quote) {
        if (escaped) escaped = false;
        else if (character === '\\') escaped = true;
        else if (character === quote) quote = null;
        continue;
      }
      if (character === '"' || character === "'" || character === '`') quote = character;
      else if (character === '(') depth++;
      else if (character === ')') depth--;
    }
    if (depth === 0) calls.push(source.slice(match.index, end));
    logger.lastIndex = Math.max(logger.lastIndex, end);
  }
  return calls;
}

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
    const source = fs.readFileSync(file, 'utf8');
    for (const call of findLoggerCalls(source)) {
      if (sensitiveDynamicValue.test(stripStringLiterals(call))) {
        const index = source.indexOf(call);
        const line = source.slice(0, index).split(/\r?\n/).length;
        findings.push(`${path.relative(repoRoot, file)}:${line}`);
      }
    }
  }
}

assert.deepStrictEqual(findings, [], `Potential sensitive logging arguments found: ${findings.join(', ')}`);
console.log('PASS: no direct sensitive-field or raw-exception logging expressions in src/main/java or server/src');
