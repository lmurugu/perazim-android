#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
pg_root="$PWD/server/.runtime/pgsql/usr"
pg_tools="$pg_root/bin"
export LD_LIBRARY_PATH="$pg_root/lib64${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
unset DATABASE_URL PHASE_B_TEST_ADMIN_URL PHASE_B_SYNTHETIC PGHOST PGPORT PGUSER PGDATABASE PGSERVICE PGSERVICEFILE PGOPTIONS PGPASSWORD PGPASSFILE
cluster="$(mktemp -d /tmp/perazim-adm01.XXXXXX)"
cleanup() {
  local result=$?
  if [[ -f "$cluster/data/postmaster.pid" ]]; then
    if ! "$pg_tools/pg_ctl" -D "$cluster/data" -m fast -w stop >/dev/null; then
      echo "Could not stop owned ADM-01 cluster at $cluster" >&2
      exit 1
    fi
  fi
  rm -rf -- "$cluster"
  echo 'Removed owned ADM-01 cluster'
  exit "$result"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
port="$(node -e 'const s=require("node:net").createServer(); s.listen(0,"127.0.0.1",()=>{console.log(s.address().port); s.close()})')"
"$pg_tools/initdb" -D "$cluster/data" -U postgres -L "$pg_root/share/pgsql" --auth-local=trust --auth-host=trust --locale=C.UTF-8 >/dev/null
"$pg_tools/pg_ctl" -D "$cluster/data" -l "$cluster/postgres.log" -o "-h 127.0.0.1 -p $port -k $cluster -c dynamic_library_path=$pg_root/lib64/pgsql" -w start >/dev/null
node --version
"$pg_tools/postgres" --version
NODE_ENV=test ADM01_CLUSTER="$cluster/data" PHASE_B_TEST_ADMIN_URL="postgres://postgres@127.0.0.1:$port/postgres" \
  node server/test/phase_b_adm_01.test.js
