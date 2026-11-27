// Shared settings for the scratch tests. Nothing here touches the pilot.
// CLONE_SERVER: server tree with node_modules installed (default: this repo's server/).
// SCRATCH_PG: a throwaway Postgres 16 server, for example postgresql://postgres@127.0.0.1:55555
// MIG_B: tracked migrations (default: CLONE_SERVER/migrations). MIG_A is derived: 0001-0021 only.
const fs = require('fs');
const os = require('os');
const path = require('path');
const CLONE = process.env.CLONE_SERVER || path.resolve(__dirname, '../../server');
const SCRATCH_PG = process.env.SCRATCH_PG || 'postgresql://postgres@127.0.0.1:55555';
const MIG_B = process.env.MIG_B || path.join(CLONE, 'migrations');
let MIG_A = process.env.MIG_A;
if (!MIG_A) {
  MIG_A = path.join(os.tmpdir(), 'pilot-recon-mig-A');
  fs.rmSync(MIG_A, { recursive: true, force: true });
  fs.mkdirSync(MIG_A, { recursive: true });
  for (const f of fs.readdirSync(MIG_B)) if (!/^002[23]_/.test(f)) fs.copyFileSync(path.join(MIG_B, f), path.join(MIG_A, f));
}
module.exports = { CLONE, SCRATCH_PG, MIG_A, MIG_B };
