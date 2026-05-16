package com.yeswater.bids.export.api;

/**
 * 业务数据源连接信息（仅内网传递）。
 */
public record ExportDataSourceSpec(
        String jdbcUrl,
        String username,
        String password,
        String driverClassName
) {
}
