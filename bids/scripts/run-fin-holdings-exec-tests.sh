#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.macos.yml)
BASE="${BIDS_E2E_BASE:-http://127.0.0.1:8080}"
AUTH=(-u "${BIDS_ADMIN_USERNAME:-admin}:${BIDS_ADMIN_PASSWORD:-admin}")

"${COMPOSE[@]}" up -d postgres bids-web bids-exec bids-config mysql elasticsearch >/dev/null

echo "写入 PG 测试数据..."
"${COMPOSE[@]}" exec -T postgres psql -U bids -d bids_fin_demo -v ON_ERROR_STOP=1 -f - <"$ROOT/scripts/sql/pg_seed_holdings_exec_test.sql"

run_case() {
  local name="$1" json="$2"
  echo "== $name =="
  curl -sf "${AUTH[@]}" -H 'Content-Type: application/json' -d "$json" \
    "$BASE/api/runtime/models/fin_holdings_pg/execute" \
    | python3 -c "import json,sys;d=json.load(sys.stdin);print('rowCount',d.get('rowCount'),'durationMs',d.get('durationMs'));[print(r) for r in d.get('rows',[])[:12]]"
  echo
}

run_case '境内 T_ 系列 + 基金 + 排除零' '{
  "parameters": {
    "as_of_date": "2024-12-31",
    "min_amount": 500000,
    "scale": 2,
    "region": "境内",
    "product_codes_csv": "T_BOND_A,T_EQ_A,T_MM_A,FUND001",
    "include_zero": false,
    "product_type": "基金"
  }
}'

run_case '境内 全 T_ 含零市值' '{
  "parameters": {
    "as_of_date": "2024-12-31",
    "min_amount": 0,
    "scale": 1,
    "region": "境内",
    "product_codes_csv": "T_BOND_A,T_BOND_B,T_EQ_A,T_EQ_B,T_MM_A,T_MIX_A,EQ002",
    "include_zero": true
  }
}'

run_case '境外 T_ + 股票过滤' '{
  "parameters": {
    "as_of_date": "2024-12-31",
    "min_amount": 1000000,
    "scale": 3,
    "region": "境外",
    "product_codes_csv": "T_US_B1,T_US_EQ2,T_OFF_MM,T_OFF_MIX,US_EQ1",
    "include_zero": false,
    "product_type": "股票"
  }
}'

run_case '原表 BOND + 新 T_BOND 混合列表' '{
  "parameters": {
    "as_of_date": "2024-12-31",
    "min_amount": 2000000,
    "scale": 2,
    "region": "境内",
    "product_codes_csv": "BOND001,T_BOND_A,T_BOND_B",
    "include_zero": false
  }
}'

echo "OK"
