-- 可重复执行：为「组合持仓多维查询(PG)」补充适量演示行（按 product_code 去重插入）
INSERT INTO fin_portfolio_holding (region, product_type, product_code, trade_date, market_value, cost_basis, is_stressed, strategy_tags)
SELECT v.region, v.product_type, v.product_code, v.trade_date::date, v.market_value::numeric, v.cost_basis::numeric, v.is_stressed::boolean, v.strategy_tags
FROM (VALUES
  ('境内', '债券', 'BOND003', '2024-12-20',  2100000.0000,  2080000.0000, false, '短久期,票息保护'),
  ('境内', '股票', 'EQ003',   '2024-12-05',  1500000.2500,  1490000.0000, false, '红利低波,防御'),
  ('境内', '基金', 'FUND002', '2024-11-28',   980000.5000,   975000.0000, false, '短债增强'),
  ('境内', 'REITs', 'REIT001', '2024-10-18',  4200000.0000,  4300000.0000, true,  '仓储物流,分红型'),
  ('境内', '货币', 'MMF001',  '2024-12-31',   500000.0000,   500000.0000, false, 'T+0,现金管理'),
  ('境外', '债券', 'USBOND2', '2024-11-10',  2800000.0000,  2790000.0000, false, '投资级,曲线中性'),
  ('境外', '股票', 'US_EQ2',  '2024-12-18',  6600000.0000,  6400000.0000, false, '价值因子,美元计价'),
  ('境外', '基金', 'OFFS2',   '2024-11-25',  1100000.0000,  1090000.0000, false, '亚洲信用,分散'),
  ('境内', '股票', 'EQ004',   '2024-09-01',       0.0000,       0.0000, false, '新股申购,待上市'),
  ('境外', '债券', 'USBOND3', '2024-07-15',   750000.1250,   748000.0000, true,  '高收益,事件驱动')
) AS v(region, product_type, product_code, trade_date, market_value, cost_basis, is_stressed, strategy_tags)
WHERE NOT EXISTS (
  SELECT 1 FROM fin_portfolio_holding h
  WHERE h.product_code = v.product_code AND h.trade_date = v.trade_date::date
);
