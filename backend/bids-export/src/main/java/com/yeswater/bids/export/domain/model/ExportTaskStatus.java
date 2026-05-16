package com.yeswater.bids.export.domain.model;

/**
 * 导出任务状态。
 */
public enum ExportTaskStatus {
    /** 等待执行 */
    PENDING,
    /** 执行中 */
    RUNNING,
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAILED,
    /** 已取消 */
    CANCELLED
}
