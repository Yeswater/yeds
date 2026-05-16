package com.yeswater.bids.export.api;

/**
 * 异步导出任务引用。
 */
public record ExportTaskRef(
        String taskId,
        String status,
        String mode,
        String fileFormat
) {
}
