package com.yeswater.bids.export.api;

import java.util.List;

/**
 * 导出服务客户端契约。
 */
public interface ExportClient {

    ExportEstimateResult estimate(ExportJobRequest request);

    ExportSyncResult syncExport(ExportJobRequest request);

    ExportTaskRef createTask(ExportJobRequest request);

    ExportTaskStatus getTask(String taskId);

    String getDownloadUrl(String taskId);

    void cancelTask(String taskId);

    List<ExportTaskSummary> listTasks(String username, int limit);
}
