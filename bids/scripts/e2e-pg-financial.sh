#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.macos.yml)
BASE="${BIDS_E2E_BASE:-http://127.0.0.1:8080}"
AUTH=(-u "${BIDS_ADMIN_USERNAME:-admin}:${BIDS_ADMIN_PASSWORD:-admin}")

"${COMPOSE[@]}" up -d postgres mysql elasticsearch bids-config bids-exec bids-web

echo "等待 Web..."
for _ in $(seq 1 90); do
  if curl -sf "$BASE/" >/dev/null; then break; fi
  sleep 2
done
curl -sf "$BASE/" >/dev/null

echo "清理 MySQL 中同名配置（可重复跑）..."
"${COMPOSE[@]}" exec -T mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root}" bids -e "
DELETE FROM bids_form_field WHERE model_id IN (SELECT id FROM bids_sql_model WHERE code='fin_holdings_pg');
DELETE FROM bids_result_column WHERE model_id IN (SELECT id FROM bids_sql_model WHERE code='fin_holdings_pg');
DELETE FROM bids_model_permission WHERE model_id IN (SELECT id FROM bids_sql_model WHERE code='fin_holdings_pg');
DELETE FROM bids_sql_model WHERE code='fin_holdings_pg';
DELETE FROM bids_datasource WHERE code='pg_fin_demo';
" >/dev/null

echo "创建 PG 数据源..."
curl -sf "${AUTH[@]}" -H 'Content-Type: application/json' -d '{
  "code": "pg_fin_demo",
  "name": "本地PG财经演示库",
  "jdbcUrl": "jdbc:postgresql://postgres:5432/bids_fin_demo",
  "username": "bids",
  "password": "bids",
  "driverClassName": "org.postgresql.Driver",
  "sqlDialect": "POSTGRESQL",
  "maxPoolSize": 5,
  "active": true
}' "$BASE/api/config/datasources" | head -c 240
echo

MODEL_JSON=$(python3 <<'PY'
import json
sql = """SELECT h.region,
       h.product_type,
       h.product_code,
       h.trade_date,
       round(cast(h.market_value as numeric), cast(:scale as int)) AS market_value_rnd,
       h.cost_basis,
       h.is_stressed,
       array_to_string(string_to_array(h.strategy_tags, ','), '|') AS strategy_tag_list,
       (SELECT COALESCE(SUM(x.market_value), 0) FROM fin_portfolio_holding x WHERE x.region = h.region) AS region_mv_total
FROM fin_portfolio_holding h
WHERE h.trade_date <= :as_of_date
<#if include_zero?? && include_zero>
  AND h.market_value >= :min_amount
<#else>
  <#if min_amount?? && (min_amount > 0)>
  AND h.market_value >= :min_amount
  <#else>
  AND h.market_value <> 0
  </#if>
</#if>
  AND h.region = :region
  AND h.product_code IN (SELECT unnest(string_to_array(:product_codes_csv, ',')))
<#if product_type?? && (product_type?length > 0)>
  AND h.product_type = :product_type
</#if>
ORDER BY h.market_value DESC"""
doc = {
  "code": "fin_holdings_pg",
  "name": "组合持仓多维查询(PG)",
  "datasourceCode": "pg_fin_demo",
  "sqlTemplate": sql,
  "maxRows": 50,
  "fields": [
    {"fieldName": "as_of_date", "label": "截至日", "fieldType": "DATE", "required": True, "defaultValue": None, "optionsJson": None, "sortOrder": 1},
    {"fieldName": "min_amount", "label": "最小市值(元)", "fieldType": "NUMBER", "required": True, "defaultValue": "0", "optionsJson": None, "sortOrder": 2},
    {"fieldName": "scale", "label": "金额小数位", "fieldType": "NUMBER", "required": True, "defaultValue": "2", "optionsJson": None, "sortOrder": 3},
    {"fieldName": "region", "label": "市场区域", "fieldType": "TEXT", "required": True, "defaultValue": None, "optionsJson": "{\"distinctFrom\":{\"table\":\"fin_portfolio_holding\",\"column\":\"region\"}}", "sortOrder": 4},
    {"fieldName": "product_codes_csv", "label": "品种代码列表(逗号分隔)", "fieldType": "TEXT", "required": True, "defaultValue": None, "optionsJson": "{\"distinctFrom\":{\"table\":\"fin_portfolio_holding\",\"column\":\"product_code\",\"multiple\":true}}", "sortOrder": 5},
    {"fieldName": "include_zero", "label": "包含零市值", "fieldType": "BOOLEAN", "required": True, "defaultValue": "false", "optionsJson": None, "sortOrder": 6},
    {"fieldName": "product_type", "label": "资产类型(可选)", "fieldType": "TEXT", "required": False, "defaultValue": None, "optionsJson": "{\"distinctFrom\":{\"table\":\"fin_portfolio_holding\",\"column\":\"product_type\"}}", "sortOrder": 7},
  ],
  "columns": [
    {"columnName": "region", "label": "区域", "valueType": "TEXT", "visible": True, "maskType": None, "sortOrder": 1},
    {"columnName": "product_type", "label": "类型", "valueType": "TEXT", "visible": True, "maskType": None, "sortOrder": 2},
    {"columnName": "product_code", "label": "代码", "valueType": "TEXT", "visible": True, "maskType": None, "sortOrder": 3},
    {"columnName": "trade_date", "label": "交易日", "valueType": "DATE", "visible": True, "maskType": None, "sortOrder": 4},
    {"columnName": "market_value_rnd", "label": "市值(取位)", "valueType": "NUMBER", "visible": True, "maskType": None, "sortOrder": 5},
    {"columnName": "cost_basis", "label": "成本", "valueType": "NUMBER", "visible": True, "maskType": None, "sortOrder": 6},
    {"columnName": "is_stressed", "label": "压力情景", "valueType": "BOOLEAN", "visible": True, "maskType": None, "sortOrder": 7},
    {"columnName": "strategy_tag_list", "label": "策略标签(列表,|分隔)", "valueType": "TEXT", "visible": True, "maskType": None, "sortOrder": 8},
    {"columnName": "region_mv_total", "label": "区域市值合计", "valueType": "NUMBER", "visible": True, "maskType": None, "sortOrder": 9},
  ],
  "permissions": [],
}
print(json.dumps(doc, ensure_ascii=False))
PY
)

echo "创建 SQL 模型..."
CREATE_RESP=$(curl -sf "${AUTH[@]}" -H 'Content-Type: application/json' -d "$MODEL_JSON" "$BASE/api/config/models")
MID=$(python3 -c "import json,sys; print(json.load(sys.stdin)['model']['id'])" <<<"$CREATE_RESP")
echo "model id=$MID"

echo "校验..."
curl -sf "${AUTH[@]}" -X POST "$BASE/api/config/models/$MID/validate" | python3 -m json.tool

echo "发布..."
curl -sf "${AUTH[@]}" -X POST "$BASE/api/config/models/$MID/publish" >/dev/null

echo "拉取表单..."
curl -sf "${AUTH[@]}" "$BASE/api/runtime/models/fin_holdings_pg/form" | python3 -m json.tool | head -n 45

echo "执行(境内+代码列表+仅债券)..."
curl -sf "${AUTH[@]}" -H 'Content-Type: application/json' -d '{
  "parameters": {
    "as_of_date": "2024-12-31",
    "min_amount": 1000000,
    "scale": 2,
    "region": "境内",
    "product_codes_csv": "BOND001,EQ001,FUND001",
    "include_zero": false,
    "product_type": "债券"
  }
}' "$BASE/api/runtime/models/fin_holdings_pg/execute" | python3 -m json.tool

echo "执行(含零市值、不按类型过滤)..."
curl -sf "${AUTH[@]}" -H 'Content-Type: application/json' -d '{
  "parameters": {
    "as_of_date": "2024-12-31",
    "min_amount": 0,
    "scale": 3,
    "region": "境内",
    "product_codes_csv": "BOND001,EQ001,FUND001,EQ002",
    "include_zero": true
  }
}' "$BASE/api/runtime/models/fin_holdings_pg/execute" | python3 -m json.tool

echo "OK"
