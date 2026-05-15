-- 财经演示：组合持仓（数值、字符串、布尔、逗号分隔标签；查询侧用 string_to_array 模拟列表）
CREATE TABLE IF NOT EXISTS fin_portfolio_holding (
    id              bigserial PRIMARY KEY,
    region          varchar(16)  NOT NULL,
    product_type    varchar(32)  NOT NULL,
    product_code    varchar(64)  NOT NULL,
    trade_date      date          NOT NULL,
    market_value    numeric(18, 4) NOT NULL,
    cost_basis      numeric(18, 4) NOT NULL,
    is_stressed     boolean       NOT NULL DEFAULT false,
    strategy_tags   varchar(256)
);

TRUNCATE fin_portfolio_holding RESTART IDENTITY;

INSERT INTO fin_portfolio_holding (region, product_type, product_code, trade_date, market_value, cost_basis, is_stressed, strategy_tags) VALUES
('境内', '债券', 'BOND001', '2024-06-28', 12500000.1250, 12200000.0000, false, '利率久期,骑乘策略'),
('境内', '债券', 'BOND002', '2024-09-15',  3200000.5000,  3150000.2500, true,  '信用下沉,行业轮动'),
('境内', '股票', 'EQ001',   '2024-11-01',  8800000.0000,  9100000.0000, false, '沪深300增强,分红再投'),
('境内', '股票', 'EQ002',   '2024-11-20',       0.0000,       0.0000, false, '观察仓,待调入'),
('境内', '基金', 'FUND001', '2024-10-10',  5600000.7500,  5480000.0000, false, '货基打底,流动性管理'),
('境外', '债券', 'USBOND1', '2024-08-01',  4100000.0000,  4050000.0000, false, '美债核心,汇率对冲'),
('境外', '股票', 'US_EQ1',  '2024-12-01', 15200000.3333, 14800000.0000, true,  '科技龙头,波动容忍'),
('境外', '基金', 'OFFS1',   '2024-12-15',   950000.1250,   940000.0000, false, '离岸固收+,多资产'),
('境内', '债券', 'BOND003', '2024-12-20',  2100000.0000,  2080000.0000, false, '短久期,票息保护'),
('境内', '股票', 'EQ003',   '2024-12-05',  1500000.2500,  1490000.0000, false, '红利低波,防御'),
('境内', '基金', 'FUND002', '2024-11-28',   980000.5000,   975000.0000, false, '短债增强'),
('境内', 'REITs', 'REIT001', '2024-10-18',  4200000.0000,  4300000.0000, true,  '仓储物流,分红型'),
('境内', '货币', 'MMF001',  '2024-12-31',   500000.0000,   500000.0000, false, 'T+0,现金管理'),
('境外', '债券', 'USBOND2', '2024-11-10',  2800000.0000,  2790000.0000, false, '投资级,曲线中性'),
('境外', '股票', 'US_EQ2',  '2024-12-18',  6600000.0000,  6400000.0000, false, '价值因子,美元计价'),
('境外', '基金', 'OFFS2',   '2024-11-25',  1100000.0000,  1090000.0000, false, '亚洲信用,分散'),
('境内', '股票', 'EQ004',   '2024-09-01',       0.0000,       0.0000, false, '新股申购,待上市'),
('境外', '债券', 'USBOND3', '2024-07-15',   750000.1250,   748000.0000, true,  '高收益,事件驱动');
