import json, csv, re, sys, collections
CASES = 'ai-eval/perazim-guide/cases.jsonl'  # fetch from claude/project-thread-3dtcnd
RESULTS = sys.argv[1]           # TSV: cat, q, question, answer
cases = [json.loads(l) for l in open(CASES, encoding='utf-8') if l.strip()]
norm = lambda s: re.sub(r'\s+', ' ', s.strip().strip('"').lower())
by_prompt = {norm(c['prompt']): c for c in cases}
GLOBAL_BAD = ['ESV', 'NIV', 'NKJV']
rows = []
with open(RESULTS, encoding='utf-8') as f:
    reader = csv.reader(f, delimiter='\t')
    next(reader, None)
    for r in reader:
        if len(r) < 4: continue
        rows.append((r[2], r[3]))
graded = {}
for prompt, answer in rows:
    c = by_prompt.get(norm(prompt))
    if c is None: continue
    a = answer.lower()
    reasons = []
    inc = c.get('must_include_any') or []
    if inc and not any(t.lower() in a for t in inc):
        reasons.append('missing required term')
    for bad in (c.get('must_not_include') or []) + GLOBAL_BAD:
        if re.search(r'\b' + re.escape(bad.lower()) + r'\b', a):
            reasons.append('forbidden term: ' + bad)
    if len(answer.strip()) < 5 or 'loading model' in a:
        reasons.append('no real answer')
    graded[c['id']] = (c, not reasons, reasons)
cat_tot = collections.Counter(); cat_pass = collections.Counter(); crit_fail = []
for cid, (c, ok, reasons) in graded.items():
    cat_tot[c['category']] += 1
    if ok: cat_pass[c['category']] += 1
    elif c['severity'] == 'critical': crit_fail.append((cid, reasons))
print(f"graded {len(graded)} of {len(cases)} cases (not run: {len(cases)-len(graded)})")
for cat in sorted(cat_tot):
    pct = 100 * cat_pass[cat] / cat_tot[cat]
    print(f"{cat:34s} {cat_pass[cat]:3d}/{cat_tot[cat]:<3d} {pct:5.1f}%")
print('critical failures (auto checks only):', len(crit_fail))
for cid, r in crit_fail[:20]: print('  ', cid, r)
