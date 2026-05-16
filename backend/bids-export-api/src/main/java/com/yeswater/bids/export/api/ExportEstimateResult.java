package com.yeswater.bids.export.api;

/**
 * 导出行数预估结果。
 */
public record ExportEstimateResult(
        Long estimatedRows,
        String mode,
        int syncThresholdRows,
        int maxExportRows,
        boolean countTimedOut
) {
}
