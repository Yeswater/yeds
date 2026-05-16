package com.yeswater.bids.export.infrastructure.storage;

import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConditionalOnProperty(name = "bids.export.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalExportObjectStorage implements ExportObjectStorage {
    private final Path baseDir;

    public LocalExportObjectStorage(ExportProperties properties) throws IOException {
        this.baseDir = Path.of(properties.getStorage().getLocalBaseDir());
        Files.createDirectories(baseDir);
    }

    @Override
    public void upload(Path localFile, String objectKey) {
        try {
            Path target = baseDir.resolve(objectKey);
            Files.createDirectories(target.getParent());
            Files.copy(localFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("上传本地存储失败", e);
        }
    }

    @Override
    public String presignGetUrl(String objectKey) {
        return "file://" + baseDir.resolve(objectKey).toAbsolutePath();
    }
}
