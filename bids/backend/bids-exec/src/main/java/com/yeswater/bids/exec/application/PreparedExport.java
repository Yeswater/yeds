package com.yeswater.bids.exec.application;

import com.yeswater.bids.exec.domain.model.DataSourceConfig;
import com.yeswater.bids.exec.domain.model.ResultColumn;
import com.yeswater.bids.exec.domain.model.SqlModel;

import java.util.List;
import java.util.Map;

/**
 * 运行态校验并渲染后的导出上下文。
 */
public record PreparedExport(
        SqlModel model,
        DataSourceConfig dataSource,
        String finalSql,
        Map<String, Object> bindParameters,
        List<ResultColumn> columns
) {
}
