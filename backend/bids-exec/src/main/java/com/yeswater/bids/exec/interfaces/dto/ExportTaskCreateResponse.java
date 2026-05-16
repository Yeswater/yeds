package com.yeswater.bids.exec.interfaces.dto;

public record ExportTaskCreateResponse(
        String taskId,
        String status,
        String mode,
        String fileFormat
) {
}
