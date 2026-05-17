package com.yeswater.bids.export.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.bids.export.api.DownloadUrlResponse;
import com.yeswater.bids.export.api.ExportEstimateResult;
import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.api.ExportModes;
import com.yeswater.bids.export.api.ExportSyncResult;
import com.yeswater.bids.export.api.ExportTaskRef;
import com.yeswater.bids.export.api.ExportTaskStatus;
import com.yeswater.bids.export.api.ExportTaskSummary;
import com.yeswater.bids.export.domain.model.ExportTask;
import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import com.yeswater.bids.export.infrastructure.jdbc.JdbcExportQuery;
import com.yeswater.bids.export.infrastructure.persistence.ExportTaskRepository;
import com.yeswater.bids.export.infrastructure.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ExportService {
    private final ExportProperties properties;
    private final ExportTaskRepository taskRepository;
    private final ExportEngine exportEngine;
    private final JdbcExportQuery jdbcExportQuery;
    private final ExportAsyncRunner asyncRunner;
    private final ObjectMapper objectMapper;

    public ExportService(
            ExportProperties properties,
            ExportTaskRepository taskRepository,
            ExportEngine exportEngine,
            JdbcExportQuery jdbcExportQuery,
            ExportAsyncRunner asyncRunner,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.exportEngine = exportEngine;
        this.jdbcExportQuery = jdbcExportQuery;
        this.asyncRunner = asyncRunner;
        this.objectMapper = objectMapper;
    }

    public ExportEstimateResult estimate(ExportJobRequest request) {
        Long count = exportEngine.resolveMaxRows(request) > 0 ? jdbcCount(request) : 0L;
        boolean timedOut = count == null;
        String mode;
        if (timedOut) {
            mode = ExportModes.ASYNC_SUGGESTED;
        } else if (count <= properties.getSyncThresholdRows()) {
            mode = ExportModes.SYNC;
        } else {
            mode = ExportModes.ASYNC;
        }
        return new ExportEstimateResult(
                count,
                mode,
                properties.getSyncThresholdRows(),
                properties.getMaxRows(),
                timedOut
        );
    }

    public ExportSyncResult syncExport(ExportJobRequest request) {
        Long count = jdbcCount(request);
        if (count != null && count > properties.getSyncThresholdRows()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "数据量超过同步上限，请使用异步导出");
        }
        try {
            ExportEngine.ExportWriteResult result = exportEngine.writeSyncXlsx(request);
            String fileName = buildSyncExportFileName(request.modelCode());
            return new ExportSyncResult(
                    new ByteArrayInputStream(result.bytes()),
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    result.bytes().length
            );
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "同步导出失败：" + detail);
        }
    }

    public ExportTaskRef createTask(ExportJobRequest request) {
        if (taskRepository.countRunningByUser(request.username()) > 0) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "已有进行中的导出任务");
        }
        String taskId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Long estimated = jdbcCount(request);
        ExportTask task = new ExportTask(
                taskId,
                request.modelCode(),
                request.username(),
                toJson(request.parameters()),
                request.finalSql(),
                com.yeswater.bids.export.domain.model.ExportTaskStatus.PENDING,
                "ASYNC",
                "zip",
                estimated,
                null,
                false,
                0,
                null,
                null,
                null,
                null,
                null,
                now,
                now,
                null
        );
        taskRepository.insert(task);
        asyncRunner.run(taskId, request);
        return new ExportTaskRef(taskId, com.yeswater.bids.export.domain.model.ExportTaskStatus.PENDING.name(), "ASYNC", "zip");
    }

    public com.yeswater.bids.export.api.ExportTaskStatus getTask(String taskId) {
        ExportTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        return toApiStatus(task);
    }

    public DownloadUrlResponse downloadUrl(String taskId) {
        ExportTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        if (task.status() != com.yeswater.bids.export.domain.model.ExportTaskStatus.SUCCESS || task.rustfsObjectKey() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "文件尚未就绪");
        }
        return new DownloadUrlResponse(exportEngine.presign(task.rustfsObjectKey()));
    }

    public void cancelTask(String taskId) {
        ExportTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "任务不存在"));
        if (task.status() != com.yeswater.bids.export.domain.model.ExportTaskStatus.PENDING
                && task.status() != com.yeswater.bids.export.domain.model.ExportTaskStatus.RUNNING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "任务无法取消");
        }
        taskRepository.update(copy(task, com.yeswater.bids.export.domain.model.ExportTaskStatus.CANCELLED, task.progressPct(),
                task.actualRows(), "用户取消", Instant.now(), Instant.now()));
    }

    public List<ExportTaskSummary> listTasks(String username, int limit) {
        return taskRepository.listByUser(username, limit).stream()
                .map(task -> new ExportTaskSummary(
                        task.id(),
                        task.modelCode(),
                        task.status().name(),
                        task.fileFormat(),
                        task.createdAt(),
                        task.finishedAt(),
                        task.status() == com.yeswater.bids.export.domain.model.ExportTaskStatus.SUCCESS
                ))
                .toList();
    }

    private static String buildSyncExportFileName(String modelCode) {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return modelCode + "_" + timestamp + ".xlsx";
    }

    private Long jdbcCount(ExportJobRequest request) {
        return jdbcExportQuery.countWithTimeout(request);
    }

    private com.yeswater.bids.export.api.ExportTaskStatus toApiStatus(ExportTask task) {
        return new com.yeswater.bids.export.api.ExportTaskStatus(
                task.id(),
                task.status().name(),
                task.progressPct(),
                task.estimatedRows(),
                task.actualRows(),
                task.truncated(),
                task.fileFormat(),
                task.errorMessage(),
                task.status() == com.yeswater.bids.export.domain.model.ExportTaskStatus.SUCCESS
        );
    }

    private ExportTask copy(
            ExportTask task,
            com.yeswater.bids.export.domain.model.ExportTaskStatus status,
            int progress,
            Long actualRows,
            String error,
            Instant updatedAt,
            Instant finishedAt
    ) {
        return new ExportTask(
                task.id(),
                task.modelCode(),
                task.username(),
                task.parametersJson(),
                task.finalSql(),
                status,
                task.mode(),
                task.fileFormat(),
                task.estimatedRows(),
                actualRows != null ? actualRows : task.actualRows(),
                task.truncated(),
                progress,
                error,
                task.rustfsBucket(),
                task.rustfsObjectKey(),
                task.fileSizeBytes(),
                task.downloadExpiresAt(),
                task.createdAt(),
                updatedAt,
                finishedAt != null ? finishedAt : task.finishedAt()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
