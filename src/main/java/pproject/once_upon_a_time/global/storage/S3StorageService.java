package pproject.once_upon_a_time.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presign-ttl-seconds}")
    private long presignTtlSeconds;

    public String createPresignedGetUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(presignTtlSeconds))
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public String createPresignedPutUrl(String key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(presignTtlSeconds))
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    public void uploadJson(String key, String json) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("application/json")
            .build();
        s3Client.putObject(request, RequestBody.fromBytes(json.getBytes(StandardCharsets.UTF_8)));
    }

    public void uploadBytes(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build();
        s3Client.putObject(request, RequestBody.fromBytes(data));
    }

    public void uploadFile(String key, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String safeContentType = contentType == null ? "application/octet-stream" : contentType;
        uploadBytes(key, file.getBytes(), safeContentType);
    }
}
