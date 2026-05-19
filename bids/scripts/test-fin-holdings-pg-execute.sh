#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.macos.yml)
BASE="${BIDS_E2E_BASE:-http://127.0.0.1:8080}"
AUTH=(-u "${BIDS_ADMIN_USERNAME:-admin}:${BIDS_ADMIN_PASSWORD:-admin}")
MODEL="fin_holdings_pg"

"${COMPOSE[@]}" up -d postgres bids-web bids-exec bids-config mysql elasticsearch >/dev/null
"${COMPOSE[@]}" exec -T postgres psql -U bids -d bids_fin_demo -v ON_ERROR_STOP=1 -f - <"$ROOT/docker/pgsql/seed_fin_holdings_extra.sql"

run_case() {
  local name="$1"
  shift
  echo "== $name =="
  local out http
  out=$(curl -sS "${AUTH[@]}" -H 'Content-Type: application/json' -d "$1" \
    "$BASE/api/runtime/models/$MODEL/execute") || true
  echo "$out" | python3 -c "import json,sys
s=sys.stdin.read().strip()
d=json.loads(s)
if isinstance(d, dict) and 'executeId' not in d:
 print('FAIL', d); sys.exit(1)
print('rows', d['rowCount'], 'durationMs', d['durationMs'])
print('codes', [r.get('product_code') for r in d.get('rows', [])])"
}

run_case '境外股票+门槛500万' '{"parameters":{"as_of_date":"2024-12-31","min_amount":5000000,"scale":2,"region":"境外","product_codes_csv":"US_EQ1,US_EQ2,USBOND1,OFFS1","include_zero":false,"product_type":"股票"}}'

run_case '境内股票+列表+含零市值' '{"parameters":{"as_of_date":"2024-12-31","min_amount":0,"scale":3,"region":"境内","product_codes_csv":"EQ001,EQ003,EQ004","include_zero":true,"product_type":"股票"}}'

run_case '境内债券+排除零市值' '{"parameters":{"as_of_date":"2024-12-31","min_amount":100000,"scale":2,"region":"境内","product_codes_csv":"BOND001,BOND002,BOND003","include_zero":false,"product_type":"债券"}}'

run_case '境外基金' '{"parameters":{"as_of_date":"2024-12-31","min_amount":0,"scale":2,"region":"境外","product_codes_csv":"OFFS1,OFFS2","include_zero":false,"product_type":"基金"}}'

run_case '境内REITs' '{"parameters":{"as_of_date":"2024-12-31","min_amount":0,"scale":2,"region":"境内","product_codes_csv":"REIT001,EQ001","include_zero":false,"product_type":"REITs"}}'

echo OK
