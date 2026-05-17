package com.yeswater.bids.export.api;

/**
 * 导出列定义。
 */
public record ExportColumnSpec(
        String columnName,
        String label,
        String maskType,
        boolean visible,
        int sortOrder
) {
}
