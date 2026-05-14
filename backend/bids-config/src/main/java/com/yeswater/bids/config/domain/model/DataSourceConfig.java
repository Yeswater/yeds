package com.yeswater.bids.config.domain.model;

import com.yeswater.bids.sql.dialect.SqlDialectType;

public record DataSourceConfig(
        String id,
        String code,
        String name,
        String jdbcUrl,
        String username,
        String password,
        String driverClassName,
        SqlDialectType sqlDialect,
        int maxPoolSize,
        boolean active
) {
}
