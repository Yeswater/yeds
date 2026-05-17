package com.yeswater.bids.export.application;

import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.domain.model.ExportTask;
import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import com.yeswater.bids.export.infrastructure.persistence.ExportTaskRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ExportAsyncRunner {
    private final ExportProperties properties;
    private final ExportTaskRepository taskRepository;
    private final ExportEngine exportEngine;

    public ExportAsyncRunner(ExportProperties properties, ExportTaskRepository taskRepository, ExportEngine exportEngine) {
        this.properties = properties;
        this.taskRepository = taskRepository;
        this.exportEngine = exportEngine;
    }

    @Async("exportTaskExecutor")
    public void run(String taskId, ExportJobRequest request) {
        ExportTask current = taskRepository.findById(taskId).orElse(null);
        if (current == null || current.status() == com.yeswater.bids.export.domain.model.ExportTaskStatus.CANCELLED) {
            return;
        }
        taskRepository.update(copy(current, com.yeswater.bids.export.domain.model.ExportTaskStatus.RUNNING, 5, null, Instant.now()));
        try {
            int maxRows = exportEngine.resolveMaxRows(request);
            ExportEngine.AsyncZipResult result = exportEngine.writeAsyncZip(request, taskId, (written, total) -> {
                int pct = total <= 0 ? 0 : (int) Math.min(99, written * 100 / total);
                ExportTask running = taskRepository.findById(taskId).orElse(null);
                if (running != null && running.status() == com.yeswater.bids.export.domain.model.ExportTaskStatus.RUNNING) {
                    taskRepository.update(copy(running, com.yeswater.bids.export.domain.model.ExportTaskStatus.RUNNING, pct, written, Instant.now()));
                }
            });
            Instant finished = Instant.now();
            Instant expires = finished.plus(properties.getDownloadUrlTtlSeconds(), ChronoUnit.SECONDS);
            ExportTask done = taskRepository.findById(taskId).orElseThrow();
            taskRepository.update(new ExportTask(
                    done.id(),
                    done.modelCode(),
                    done.username(),
                    done.parametersJson(),
                    done.finalSql(),
                    com.yeswater.bids.export.domain.model.ExportTaskStatus.SUCCESS,
                    done.mode(),
                    done.fileFormat(),
                    done.estimatedRows(),
                    result.written(),
                    result.truncated(),
                    100,
                    null,
                    properties.getRustfs().getBucket(),
                    result.objectKey(),
                    result.fileSizeBytes(),
                    expires,
                    done.createdAt(),
                    Instant.now(),
                    finished
            ));
        } catch (Exception e) {
            ExportTask failed = taskRepository.findById(taskId).orElse(null);
            if (failed != null && failed.status() != com.yeswater.bids.export.domain.model.ExportTaskStatus.CANCELLED) {
                taskRepository.update(new ExportTask(
                        failed.id(),
                        failed.modelCode(),
                        failed.username(),
                        failed.parametersJson(),
                        failed.finalSql(),
                        com.yeswater.bids.export.domain.model.ExportTaskStatus.FAILED,
                        failed.mode(),
                        failed.fileFormat(),
                        failed.estimatedRows(),
                        failed.actualRows(),
                        failed.truncated(),
                        failed.progressPct(),
                        e.getMessage(),
                        failed.rustfsBucket(),
                        failed.rustfsObjectKey(),
                        failed.fileSizeBytes(),
                        failed.downloadExpiresAt(),
                        failed.createdAt(),
                        Instant.now(),
                        Instant.now()
                ));
            }
        }
    }

    private ExportTask copy(ExportTask task, com.yeswater.bids.export.domain.model.ExportTaskStatus status, int progress, Long actualRows, Instant updatedAt) {
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
                actualRows,
                task.truncated(),
                progress,
                task.errorMessage(),
                task.rustfsBucket(),
                task.rustfsObjectKey(),
                task.fileSizeBytes(),
                task.downloadExpiresAt(),
                task.createdAt(),
                updatedAt,
                task.finishedAt()
        );
    }
}
