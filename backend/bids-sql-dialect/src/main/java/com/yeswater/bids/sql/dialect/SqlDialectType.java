package com.yeswater.bids.sql.dialect;

/**
 * 业务数据源对应的 SQL 方言类型，用于解析校验与运行态查询封装。
 */
public enum SqlDialectType {
    /** MySQL / MariaDB 等兼容方言。 */
    MYSQL,
    /** PostgreSQL 方言。 */
    POSTGRESQL,
    /** openGauss（与 PostgreSQL 协议及语法高度兼容，单独标识便于扩展）。 */
    OPENGAUSS
}
