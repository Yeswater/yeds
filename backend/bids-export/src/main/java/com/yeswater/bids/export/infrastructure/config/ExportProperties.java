package com.yeswater.bids.export.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bids.export")
public class ExportProperties {
    private String internalToken = "change-me";
    private int syncThresholdRows = 10000;
    private int maxRows = 500000;
    private int countTimeoutMs = 3000;
    private int fetchSize = 1000;
    private int xlsxShardRows = 10000;
    private int asyncPoolSize = 2;
    private int downloadUrlTtlSeconds = 900;
    private String tempDir = System.getProperty("java.io.tmpdir") + "/bids-export";
    private Storage storage = new Storage();
    private Rustfs rustfs = new Rustfs();

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    public int getSyncThresholdRows() {
        return syncThresholdRows;
    }

    public void setSyncThresholdRows(int syncThresholdRows) {
        this.syncThresholdRows = syncThresholdRows;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public int getCountTimeoutMs() {
        return countTimeoutMs;
    }

    public void setCountTimeoutMs(int countTimeoutMs) {
        this.countTimeoutMs = countTimeoutMs;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public int getXlsxShardRows() {
        return xlsxShardRows;
    }

    public void setXlsxShardRows(int xlsxShardRows) {
        this.xlsxShardRows = xlsxShardRows;
    }

    public int getAsyncPoolSize() {
        return asyncPoolSize;
    }

    public void setAsyncPoolSize(int asyncPoolSize) {
        this.asyncPoolSize = asyncPoolSize;
    }

    public int getDownloadUrlTtlSeconds() {
        return downloadUrlTtlSeconds;
    }

    public void setDownloadUrlTtlSeconds(int downloadUrlTtlSeconds) {
        this.downloadUrlTtlSeconds = downloadUrlTtlSeconds;
    }

    public String getTempDir() {
        return tempDir;
    }

    public void setTempDir(String tempDir) {
        this.tempDir = tempDir;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Rustfs getRustfs() {
        return rustfs;
    }

    public void setRustfs(Rustfs rustfs) {
        this.rustfs = rustfs;
    }

    public static class Storage {
        /** 存储类型：local | s3 */
        private String type = "local";
        private String localBaseDir = "/tmp/bids-export-files";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocalBaseDir() {
            return localBaseDir;
        }

        public void setLocalBaseDir(String localBaseDir) {
            this.localBaseDir = localBaseDir;
        }
    }

    public static class Rustfs {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "rustfsadmin";
        private String secretKey = "rustfsadmin";
        private String bucket = "bids-export";
        private String region = "us-east-1";
        private boolean pathStyleAccess = true;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public boolean isPathStyleAccess() {
            return pathStyleAccess;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }
}
