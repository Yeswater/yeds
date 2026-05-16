package com.yeswater.bids.export.api;

/**
 * 导出任务状态详情。
 */
public record ExportTaskStatus(
        String taskId,
        String status,
        int progressPct,
        Long estimatedRows,
        Long actualRows,
        boolean truncated,
        String fileFormat,
        String errorMessage,
        boolean downloadReady
) {
}
