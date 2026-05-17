package com.yeswater.bids.export.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.bids.export.api.ExportClient;
import com.yeswater.bids.export.api.DownloadUrlResponse;
import com.yeswater.bids.export.api.ExportEstimateResult;
import com.yeswater.bids.export.api.ExportException;
import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.api.ExportSyncResult;
import com.yeswater.bids.export.api.ExportTaskRef;
import com.yeswater.bids.export.api.ExportTaskStatus;
import com.yeswater.bids.export.api.ExportTaskSummary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class HttpExportClient implements ExportClient {
    private static final String TOKEN_HEADER = "X-Bids-Internal-Token";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpExportClient(ExportClientProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(TOKEN_HEADER, properties.getInternalToken())
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(jsonConverter);
                })
                .build();
    }

    @Override
    public ExportEstimateResult estimate(ExportJobRequest request) {
        return postJson("/api/export/v1/estimate", request, ExportEstimateResult.class);
    }

    @Override
    public ExportSyncResult syncExport(ExportJobRequest request) {
        try {
            return restClient.post()
                    .uri("/api/export/v1/jobs/sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, res) -> {
                        if (res.getStatusCode().isError()) {
                            throw mapError(res.getStatusCode().value(), res.bodyTo(String.class));
                        }
                        byte[] bytes = res.bodyTo(byte[].class);
                        String disposition = res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                        String fileName = parseFileName(disposition);
                        String contentType = res.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                        return new ExportSyncResult(
                                new ByteArrayInputStream(bytes != null ? bytes : new byte[0]),
                                fileName != null ? fileName : "export.xlsx",
                                contentType != null ? contentType : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                bytes != null ? bytes.length : 0
                        );
                    });
        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportException(500, e.getMessage() != null ? e.getMessage() : "同步导出失败");
        }
    }

    @Override
    public ExportTaskRef createTask(ExportJobRequest request) {
        return postJson("/api/export/v1/jobs", request, ExportTaskRef.class);
    }

    @Override
    public ExportTaskStatus getTask(String taskId) {
        return getJson("/api/export/v1/jobs/" + taskId, ExportTaskStatus.class);
    }

    @Override
    public String getDownloadUrl(String taskId) {
        DownloadUrlResponse body = getJson("/api/export/v1/jobs/" + taskId + "/download-url", DownloadUrlResponse.class);
        return body.url();
    }

    @Override
    public void cancelTask(String taskId) {
        restClient.post()
                .uri("/api/export/v1/jobs/" + taskId + "/cancel")
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<ExportTaskSummary> listTasks(String username, int limit) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/export/v1/jobs")
                        .queryParam("username", username)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ExportTaskSummary>>() {
                });
    }

    private <T> T postJson(String path, Object body, Class<T> type) {
        try {
            return restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((req, res) -> readBody(res.getStatusCode().value(), res.bodyTo(String.class), type));
        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportException(500, e.getMessage() != null ? e.getMessage() : "调用导出服务失败");
        }
    }

    private <T> T getJson(String path, Class<T> type) {
        try {
            String raw = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(raw, type);
        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportException(500, e.getMessage() != null ? e.getMessage() : "调用导出服务失败");
        }
    }

    private <T> T getJson(String path, TypeReference<T> type) {
        try {
            String raw = restClient.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readValue(raw, type);
        } catch (ExportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportException(500, e.getMessage() != null ? e.getMessage() : "调用导出服务失败");
        }
    }

    private <T> T readBody(int status, String raw, Class<T> type) {
        if (status >= 400) {
            throw mapError(status, raw);
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception e) {
            throw new ExportException(500, "解析导出服务响应失败");
        }
    }

    private ExportException mapError(int status, String raw) {
        try {
            Map<String, Object> map = objectMapper.readValue(raw, new TypeReference<>() {
            });
            Object message = map.get("message");
            if (message != null) {
                return new ExportException(status, String.valueOf(message));
            }
        } catch (Exception ignored) {
        }
        return new ExportException(status, "导出服务错误：" + status);
    }

    private static String parseFileName(String disposition) {
        if (disposition == null) {
            return null;
        }
        for (String part : disposition.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("filename*=")) {
                String v = trimmed.substring("filename*=".length()).trim();
                int idx = v.indexOf("''");
                String encoded = idx >= 0 ? v.substring(idx + 2) : v;
                try {
                    return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
                } catch (IllegalArgumentException e) {
                    return encoded;
                }
            }
            if (trimmed.startsWith("filename=")) {
                String name = trimmed.substring("filename=".length()).replace("\"", "").trim();
                if (!name.startsWith("=?")) {
                    return name;
                }
            }
        }
        return null;
    }

}
