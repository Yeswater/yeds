-- 已有库：为 fin_holdings_pg 表单字段写入 distinctFrom（MySQL bids 库执行）
UPDATE bids_form_field f
INNER JOIN bids_sql_model m ON f.model_id = m.id AND m.code = 'fin_holdings_pg'
SET f.options_json = '{"distinctFrom":{"table":"fin_portfolio_holding","column":"region"}}'
WHERE f.field_name = 'region';

UPDATE bids_form_field f
INNER JOIN bids_sql_model m ON f.model_id = m.id AND m.code = 'fin_holdings_pg'
SET f.options_json = '{"distinctFrom":{"table":"fin_portfolio_holding","column":"product_code","multiple":true}}'
WHERE f.field_name = 'product_codes_csv';

UPDATE bids_form_field f
INNER JOIN bids_sql_model m ON f.model_id = m.id AND m.code = 'fin_holdings_pg'
SET f.options_json = '{"distinctFrom":{"table":"fin_portfolio_holding","column":"product_type"}}'
WHERE f.field_name = 'product_type';
