package com.yeswater.bids.export.interfaces.rest;

import com.yeswater.bids.export.api.DownloadUrlResponse;
import com.yeswater.bids.export.api.ExportEstimateResult;
import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.api.ExportTaskRef;
import com.yeswater.bids.export.api.ExportTaskSummary;
import com.yeswater.bids.export.application.ExportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/export/v1")
public class ExportV1Controller {
    private final ExportService exportService;

    public ExportV1Controller(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/estimate")
    public ExportEstimateResult estimate(@RequestBody ExportJobRequest request) {
        return exportService.estimate(request);
    }

    @PostMapping("/jobs/sync")
    public ResponseEntity<byte[]> syncExport(@RequestBody ExportJobRequest request) {
        var result = exportService.syncExport(request);
        try (var in = result.inputStream()) {
            byte[] bytes = in.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(result.fileName())
                            .build()
                            .toString())
                    .contentType(MediaType.parseMediaType(result.contentType()))
                    .body(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/jobs")
    public ExportTaskRef createTask(@RequestBody ExportJobRequest request) {
        return exportService.createTask(request);
    }

    @GetMapping("/jobs/{taskId}")
    public com.yeswater.bids.export.api.ExportTaskStatus getTask(@PathVariable String taskId) {
        return exportService.getTask(taskId);
    }

    @GetMapping("/jobs/{taskId}/download-url")
    public DownloadUrlResponse downloadUrl(@PathVariable String taskId) {
        return exportService.downloadUrl(taskId);
    }

    @PostMapping("/jobs/{taskId}/cancel")
    public void cancel(@PathVariable String taskId) {
        exportService.cancelTask(taskId);
    }

    @GetMapping("/jobs")
    public List<ExportTaskSummary> listTasks(@RequestParam String username, @RequestParam(defaultValue = "20") int limit) {
        return exportService.listTasks(username, limit);
    }
}
