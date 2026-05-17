package com.yeswater.bids.export.infrastructure.storage;

import java.nio.file.Path;

public interface ExportObjectStorage {
    void upload(Path localFile, String objectKey);

    String presignGetUrl(String objectKey);
}
