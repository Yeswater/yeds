package com.yeswater.bids.export.infrastructure.storage;

import com.yeswater.bids.export.infrastructure.config.ExportProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "bids.export.storage.type", havingValue = "s3")
public class S3ExportObjectStorage implements ExportObjectStorage {
    private final ExportProperties properties;
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ExportObjectStorage(ExportProperties properties) {
        this.properties = properties;
        var rustfs = properties.getRustfs();
        this.bucket = rustfs.getBucket();
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(rustfs.isPathStyleAccess())
                .build();
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(rustfs.getAccessKey(), rustfs.getSecretKey()));
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(rustfs.getEndpoint()))
                .region(Region.of(rustfs.getRegion()))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Configuration)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(rustfs.getEndpoint()))
                .region(Region.of(rustfs.getRegion()))
                .credentialsProvider(credentials)
                .serviceConfiguration(s3Configuration)
                .build();
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }

    @Override
    public void upload(Path localFile, String objectKey) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(objectKey).build(),
                localFile
        );
    }

    @Override
    public String presignGetUrl(String objectKey) {
        var getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.getDownloadUrlTtlSeconds()))
                .getObjectRequest(getObjectRequest)
                .build()).url().toString();
    }
}
