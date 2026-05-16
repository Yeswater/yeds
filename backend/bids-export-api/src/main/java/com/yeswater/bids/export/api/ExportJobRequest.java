package com.yeswater.bids.export.api;

import java.util.List;
import java.util.Map;

/**
 * 运行态组装后的导出任务请求。
 */
public record ExportJobRequest(
        String requestId,
        String modelCode,
        String modelName,
        String username,
        String finalSql,
        Map<String, Object> parameters,
        int maxRows,
        List<ExportColumnSpec> columns,
        ExportDataSourceSpec dataSource,
        String sqlDialect
) {
}
