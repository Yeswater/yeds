package com.yeswater.bids.exec.interfaces.dto;

public record ExportEstimateResponse(
        Long estimatedRows,
        String mode,
        int syncThresholdRows,
        int maxExportRows,
        boolean countTimedOut
) {
}
