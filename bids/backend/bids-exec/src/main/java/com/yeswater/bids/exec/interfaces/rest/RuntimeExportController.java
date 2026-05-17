package com.yeswater.bids.exec.interfaces.rest;

import com.yeswater.bids.exec.application.RuntimeExportFacade;
import com.yeswater.bids.exec.interfaces.dto.ExportEstimateResponse;
import com.yeswater.bids.exec.interfaces.dto.ExportParametersRequest;
import com.yeswater.bids.exec.interfaces.dto.ExportTaskCreateResponse;
import com.yeswater.bids.export.api.ExportTaskStatus;
import com.yeswater.bids.export.api.ExportTaskSummary;
import org.springframework.core.io.InputStreamResource;
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

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeExportController {
    private final RuntimeExportFacade exportFacade;

    public RuntimeExportController(RuntimeExportFacade exportFacade) {
        this.exportFacade = exportFacade;
    }

    @PostMapping("/models/{modelCode}/export/estimate")
    public ExportEstimateResponse estimate(@PathVariable String modelCode, @RequestBody ExportParametersRequest request) {
        return exportFacade.estimate(modelCode, request);
    }

    @PostMapping("/models/{modelCode}/export")
    public ResponseEntity<InputStreamResource> syncExport(@PathVariable String modelCode, @RequestBody ExportParametersRequest request) {
        var result = exportFacade.syncExport(modelCode, request);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.fileName())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(result.contentType()))
                .contentLength(result.contentLength())
                .body(new InputStreamResource(result.inputStream()));
    }

    @PostMapping("/models/{modelCode}/export/tasks")
    public ExportTaskCreateResponse createTask(@PathVariable String modelCode, @RequestBody ExportParametersRequest request) {
        return exportFacade.createTask(modelCode, request);
    }

    @GetMapping("/export/tasks/{taskId}")
    public ExportTaskStatus getTask(@PathVariable String taskId) {
        return exportFacade.getTask(taskId);
    }

    @GetMapping("/export/tasks/{taskId}/download")
    public ResponseEntity<Void> download(@PathVariable String taskId) {
        String url = exportFacade.getDownloadUrl(taskId);
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    @PostMapping("/export/tasks/{taskId}/cancel")
    public void cancel(@PathVariable String taskId) {
        exportFacade.cancelTask(taskId);
    }

    @GetMapping("/export/tasks")
    public List<ExportTaskSummary> listTasks(@RequestParam(defaultValue = "20") int limit) {
        return exportFacade.listTasks(limit);
    }

}
