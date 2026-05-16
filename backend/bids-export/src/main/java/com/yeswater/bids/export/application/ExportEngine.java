package com.yeswater.bids.export.application;

import com.yeswater.bids.export.api.ExportJobRequest;
import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import com.yeswater.bids.export.infrastructure.excel.FastExcelExportWriter;
import com.yeswater.bids.export.infrastructure.jdbc.JdbcExportQuery;
import com.yeswater.bids.export.infrastructure.storage.ExportObjectStorage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ExportEngine {
    private final ExportProperties properties;
    private final JdbcExportQuery jdbcExportQuery;
    private final FastExcelExportWriter excelWriter;
    private final ExportObjectStorage objectStorage;

    public ExportEngine(
            ExportProperties properties,
            JdbcExportQuery jdbcExportQuery,
            FastExcelExportWriter excelWriter,
            ExportObjectStorage objectStorage
    ) {
        this.properties = properties;
        this.jdbcExportQuery = jdbcExportQuery;
        this.excelWriter = excelWriter;
        this.objectStorage = objectStorage;
    }

    public int resolveMaxRows(ExportJobRequest request) {
        return Math.min(request.maxRows(), properties.getMaxRows());
    }

    public ExportWriteResult writeSyncXlsx(ExportJobRequest request) throws IOException {
        int maxRows = Math.min(resolveMaxRows(request), properties.getSyncThresholdRows());
        Path tempDir = createWorkDirectory("sync-");
        Path xlsx = tempDir.resolve(fileBaseName(request) + ".xlsx");
        WriteStats stats = writeToXlsx(request, xlsx, maxRows);
        byte[] bytes = Files.readAllBytes(xlsx);
        Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        return new ExportWriteResult(bytes, stats.written(), stats.truncated());
    }

    public AsyncZipResult writeAsyncZip(ExportJobRequest request, String taskId, ProgressListener listener) throws IOException {
        int maxRows = resolveMaxRows(request);
        Path workDir = createWorkDirectory("async-" + taskId + "-");
        List<Path> parts = new ArrayList<>();
        int shardSize = properties.getXlsxShardRows();
        List<Map<String, Object>> buffer = new ArrayList<>(shardSize);
        final int[] written = {0};
        final boolean[] truncated = {false};

        jdbcExportQuery.streamRows(request, maxRows, row -> {
            if (written[0] >= maxRows) {
                truncated[0] = true;
                return;
            }
            buffer.add(row);
            written[0]++;
            if (buffer.size() >= shardSize) {
                try {
                    flushPart(request, workDir, parts, buffer);
                    listener.onProgress(written[0], maxRows);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        if (!buffer.isEmpty()) {
            flushPart(request, workDir, parts, buffer);
        }
        listener.onProgress(written[0], maxRows);

        String zipName = fileBaseName(request) + "_" + taskId + ".zip";
        Path zipPath = workDir.resolve(zipName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            int idx = 1;
            for (Path part : parts) {
                ZipEntry entry = new ZipEntry(part.getFileName().toString());
                zos.putNextEntry(entry);
                Files.copy(part, zos);
                zos.closeEntry();
                idx++;
            }
        }

        String objectKey = "exports/" + DateTimeFormatter.ofPattern("yyyyMM").format(LocalDateTime.now())
                + "/" + taskId + ".zip";
        objectStorage.upload(zipPath, objectKey);
        long size = Files.size(zipPath);
        Files.walk(workDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        return new AsyncZipResult(objectKey, written[0], truncated[0], size);
    }

    public String presign(String objectKey) {
        return objectStorage.presignGetUrl(objectKey);
    }

    private void flushPart(ExportJobRequest request, Path workDir, List<Path> parts, List<Map<String, Object>> buffer)
            throws IOException {
        Path part = workDir.resolve(fileBaseName(request) + "_part" + (parts.size() + 1) + ".xlsx");
        excelWriter.writeXlsx(part, request.columns(), List.copyOf(buffer));
        parts.add(part);
        buffer.clear();
    }

    private WriteStats writeToXlsx(ExportJobRequest request, Path xlsx, int maxRows) throws IOException {
        List<Map<String, Object>> buffer = new ArrayList<>();
        final int[] written = {0};
        final boolean[] truncated = {false};
        jdbcExportQuery.streamRows(request, maxRows, row -> {
            buffer.add(row);
            written[0]++;
        });
        if (written[0] >= maxRows) {
            truncated[0] = true;
        }
        excelWriter.writeXlsx(xlsx, request.columns(), buffer);
        return new WriteStats(written[0], truncated[0]);
    }

    private Path createWorkDirectory(String prefix) throws IOException {
        Path baseDir = Path.of(properties.getTempDir());
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, prefix);
    }

    private String fileBaseName(ExportJobRequest request) {
        return request.modelCode() + "_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
    }

    public record ExportWriteResult(byte[] bytes, long written, boolean truncated) {
    }

    public record AsyncZipResult(String objectKey, long written, boolean truncated, long fileSizeBytes) {
    }

    public record WriteStats(int written, boolean truncated) {
    }

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(long written, long totalHint);
    }
}
