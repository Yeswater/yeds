package com.yeswater.bids.exec.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.bids.exec.domain.model.ExecuteLog;
import com.yeswater.bids.exec.domain.model.ResultColumn;
import com.yeswater.bids.exec.infrastructure.persistence.ConfigQueryRepository;
import com.yeswater.foundation.common.web.ApiException;
import com.yeswater.bids.exec.interfaces.dto.ExportEstimateResponse;
import com.yeswater.bids.exec.interfaces.dto.ExportParametersRequest;
import com.yeswater.bids.exec.interfaces.dto.ExportTaskCreateResponse;
import com.yeswater.bids.export.api.ExportClient;
import com.yeswater.bids.export.api.ExportColumnSpec;
import com.yeswater.bids.export.api.ExportDataSourceSpec;
import com.yeswater.bids.export.api.ExportEstimateResult;
import com.yeswater.bids.export.api.ExportException;
import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.api.ExportSyncResult;
import com.yeswater.bids.export.api.ExportTaskRef;
import com.yeswater.bids.export.api.ExportTaskStatus;
import com.yeswater.bids.export.api.ExportTaskSummary;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RuntimeExportFacade {
    private final RuntimeService runtimeService;
    private final ExportClient exportClient;
    private final ConfigQueryRepository configRepository;
    private final ObjectMapper objectMapper;

    public RuntimeExportFacade(
            RuntimeService runtimeService,
            ExportClient exportClient,
            ConfigQueryRepository configRepository,
            ObjectMapper objectMapper
    ) {
        this.runtimeService = runtimeService;
        this.exportClient = exportClient;
        this.configRepository = configRepository;
        this.objectMapper = objectMapper;
    }

    public ExportEstimateResponse estimate(String modelCode, ExportParametersRequest request) {
        try {
            ExportEstimateResult result = exportClient.estimate(buildJob(modelCode, request, UUID.randomUUID().toString()));
            return new ExportEstimateResponse(
                    result.estimatedRows(),
                    result.mode(),
                    result.syncThresholdRows(),
                    result.maxExportRows(),
                    result.countTimedOut()
            );
        } catch (ExportException e) {
            throw toApi(e);
        }
    }

    public ExportSyncResult syncExport(String modelCode, ExportParametersRequest request) {
        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        PreparedExport prepared = runtimeService.prepareExport(modelCode, request.parameters());
        try {
            ExportSyncResult result = exportClient.syncExport(buildJob(prepared, requestId));
            saveAudit(requestId, prepared, true, null, System.currentTimeMillis() - start, 0);
            return result;
        } catch (Exception e) {
            saveAudit(requestId, prepared, false, e.getMessage(), System.currentTimeMillis() - start, 0);
            if (e instanceof ExportException exportException) {
                throw toApi(exportException);
            }
            throw e;
        }
    }

    public ExportTaskCreateResponse createTask(String modelCode, ExportParametersRequest request) {
        String requestId = UUID.randomUUID().toString();
        PreparedExport prepared = runtimeService.prepareExport(modelCode, request.parameters());
        try {
            ExportTaskRef ref = exportClient.createTask(buildJob(prepared, requestId));
            saveAudit(requestId, prepared, true, null, 0, 0);
            return new ExportTaskCreateResponse(ref.taskId(), ref.status(), ref.mode(), ref.fileFormat());
        } catch (ExportException e) {
            saveAudit(requestId, prepared, false, e.getMessage(), 0, 0);
            throw toApi(e);
        }
    }

    public ExportTaskStatus getTask(String taskId) {
        try {
            return exportClient.getTask(taskId);
        } catch (ExportException e) {
            throw toApi(e);
        }
    }

    public String getDownloadUrl(String taskId) {
        try {
            return exportClient.getDownloadUrl(taskId);
        } catch (ExportException e) {
            throw toApi(e);
        }
    }

    public void cancelTask(String taskId) {
        try {
            exportClient.cancelTask(taskId);
        } catch (ExportException e) {
            throw toApi(e);
        }
    }

    public List<ExportTaskSummary> listTasks(int limit) {
        try {
            return exportClient.listTasks(currentUsername(), limit);
        } catch (ExportException e) {
            throw toApi(e);
        }
    }

    private ExportJobRequest buildJob(String modelCode, ExportParametersRequest request, String requestId) {
        PreparedExport prepared = runtimeService.prepareExport(modelCode, request.parameters());
        return buildJob(prepared, requestId);
    }

    private ExportJobRequest buildJob(PreparedExport prepared, String requestId) {
        List<ExportColumnSpec> columns = prepared.columns().stream()
                .map(c -> new ExportColumnSpec(
                        c.columnName(),
                        c.label(),
                        c.maskType(),
                        c.visible(),
                        c.sortOrder()))
                .toList();
        var ds = prepared.dataSource();
        return new ExportJobRequest(
                requestId,
                prepared.model().code(),
                prepared.model().name(),
                currentUsername(),
                prepared.finalSql(),
                prepared.bindParameters(),
                prepared.model().maxRows(),
                columns,
                new ExportDataSourceSpec(
                        ds.jdbcUrl(),
                        ds.username(),
                        ds.password(),
                        ds.driverClassName()),
                ds.sqlDialect().name()
        );
    }

    private void saveAudit(String requestId, PreparedExport prepared, boolean success, String error, long durationMs, int rowCount) {
        ExecuteLog log = new ExecuteLog(
                UUID.randomUUID().toString(),
                requestId,
                prepared.model().code(),
                currentUsername(),
                prepared.finalSql(),
                toJson(prepared.bindParameters()),
                success,
                error,
                durationMs,
                rowCount,
                null
        );
        configRepository.saveExecuteLog(log);
    }

    private String toJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private ApiException toApi(ExportException e) {
        HttpStatus status = HttpStatus.resolve(e.getHttpStatus());
        return new ApiException(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
