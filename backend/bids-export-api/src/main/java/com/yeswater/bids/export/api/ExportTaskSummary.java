package com.yeswater.bids.export.api;

import java.time.Instant;

/**
 * 导出任务列表项。
 */
public record ExportTaskSummary(
        String taskId,
        String modelCode,
        String status,
        String fileFormat,
        Instant createdAt,
        Instant finishedAt,
        boolean downloadReady
) {
}
