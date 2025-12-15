package pproject.once_upon_a_time.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.buckets.image}")
    private String imageBucket;

    @Value("${supabase.buckets.audio}")
    private String audioBucket;

    @Value("${supabase.buckets.character}")
    private String characterBucket;

    private final RestTemplate restTemplate = new RestTemplate();

    private final List<String> imageTypes = List.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    // character는 audio와 동일한 MIME 허용 목록을 사용
    private final List<String> audioTypes = List.of("audio/mpeg", "audio/wav", "audio/mp4", "audio/ogg");

    /**
     * 버킷 타입별 업로드
     */
    public String uploadFile(MultipartFile file, FileType type) throws IOException {
        validateMimeType(file, type);

        String bucket = switch (type) {
            case IMAGE -> imageBucket;
            case AUDIO -> audioBucket;
            case CHARACTER -> characterBucket;
        };

        return uploadToBucket(file, bucket);
    }

    /**
     * PNG 바이트 배열을 지정한 파일명으로 이미지 버킷에 업로드한다.
     */
    public String uploadImageBytes(byte[] imageBytes, String fileName) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("이미지 데이터가 비어 있습니다.");
        }
        String objectName = (fileName == null || fileName.isBlank())
                ? UUID.randomUUID() + ".png"
                : fileName;
        return uploadToBucket(imageBytes, imageBucket, MediaType.IMAGE_PNG, objectName);
    }

    private void validateMimeType(MultipartFile file, FileType type) {
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("파일의 Content-Type을 확인할 수 없습니다.");
        }

        boolean allowed = switch (type) {
            case IMAGE -> imageTypes.contains(contentType);
            case AUDIO, CHARACTER -> audioTypes.contains(contentType);
        };

        if (!allowed) {
            throw new IllegalArgumentException("허용되지 않은 " + type.name().toLowerCase() + " MIME 타입입니다: " + contentType);
        }
    }

    private String uploadToBucket(MultipartFile file, String bucket) throws IOException {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        MediaType mediaType = file.getContentType() != null
                ? MediaType.parseMediaType(file.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return uploadToBucket(file.getBytes(), bucket, mediaType, fileName);
    }

    private String uploadToBucket(byte[] data, String bucket, MediaType mediaType, String fileName) {
        // Supabase 업로드 endpoint
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", serviceKey);
        headers.set("Authorization", "Bearer " + serviceKey);
        headers.setContentType(mediaType);

        HttpEntity<byte[]> request = new HttpEntity<>(data, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Supabase 업로드 실패: " + response.getBody());
        }

        // Public URL 반환
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
    }

    public enum FileType {
        IMAGE, AUDIO, CHARACTER
    }
}
