package com.yeswater.bids.export.api;

/**
 * 导出模式常量。
 */
public final class ExportModes {
    /** 同步导出 */
    public static final String SYNC = "SYNC";
    /** 异步导出 */
    public static final String ASYNC = "ASYNC";
    /** 建议异步（COUNT 超时等） */
    public static final String ASYNC_SUGGESTED = "ASYNC_SUGGESTED";

    private ExportModes() {
    }
}
