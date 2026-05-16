package com.yeswater.bids.export.domain.model;

import java.time.Instant;

public record ExportTask(
        String id,
        String modelCode,
        String username,
        String parametersJson,
        String finalSql,
        ExportTaskStatus status,
        String mode,
        String fileFormat,
        Long estimatedRows,
        Long actualRows,
        boolean truncated,
        int progressPct,
        String errorMessage,
        String rustfsBucket,
        String rustfsObjectKey,
        Long fileSizeBytes,
        Instant downloadExpiresAt,
        Instant createdAt,
        Instant updatedAt,
        Instant finishedAt
) {
}
