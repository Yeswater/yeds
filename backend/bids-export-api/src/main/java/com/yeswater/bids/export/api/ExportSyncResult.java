package com.yeswater.bids.export.api;

import java.io.InputStream;

/**
 * 同步导出结果（调用方负责关闭流）。
 */
public record ExportSyncResult(
        InputStream inputStream,
        String fileName,
        String contentType,
        long contentLength
) {
}
